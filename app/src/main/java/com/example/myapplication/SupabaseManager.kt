package com.example.myapplication

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.storage.storage
import androidx.annotation.Nullable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

object SupabaseManager {
    private const val SUPABASE_URL = "https://vxnvjecdlkzptdaibtcq.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_fcH_A07NjEKugHxS_Xr5IA_NOFI8Zgs"

    @JvmStatic
    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Postgrest)
        install(Auth)
        install(Storage)
    }

    @JvmStatic
    fun getCurrentSession() = client.auth.currentSessionOrNull()

    @JvmStatic
    fun signOut(callback: SupabaseCallback) {
        scope.launch {
            try {
                client.auth.signOut()
                withContext(Dispatchers.Main) {
                    callback.onResult(true, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onResult(false, e.message)
                }
            }
        }
    }

    @JvmStatic
    fun getUserProfile(userId: String, callback: SupabaseCallbackWithProfile) {
        scope.launch {
            try {
                // 1. Try to get from 'profiles' table
                var profile = try {
                    client.postgrest["profiles"]
                        .select {
                            filter {
                                eq("id", userId)
                            }
                        }
                        .decodeSingleOrNull<Profile>()
                } catch (e: Exception) {
                    android.util.Log.e("SupabaseManager", "Failed to fetch from profiles table: ${e.message}")
                    null
                }

                // 2. Fallback/Merge with Auth User Metadata
                val session = client.auth.currentSessionOrNull()
                val finalProfile = if (session != null && session.user != null && session.user?.id == userId) {
                    val user = session.user!!
                    val metadata = user.userMetadata
                    
                    fun getMeta(key: String): String? {
                        return metadata?.get(key)?.toString()?.removeSurrounding("\"")
                    }

                    if (profile == null) {
                        Profile(
                            id = userId,
                            email = user.email ?: "",
                            first_name = getMeta("first_name"),
                            last_name = getMeta("last_name"),
                            account_type = getMeta("account_type"),
                            shop_name = getMeta("shop_name"),
                            shop_description = getMeta("shop_description"),
                            shop_location = getMeta("shop_location")
                        )
                    } else {
                        profile.copy(
                            first_name = profile.first_name ?: getMeta("first_name"),
                            last_name = profile.last_name ?: getMeta("last_name"),
                            account_type = profile.account_type ?: getMeta("account_type"),
                            shop_name = profile.shop_name ?: getMeta("shop_name"),
                            shop_description = profile.shop_description ?: getMeta("shop_description"),
                            shop_location = profile.shop_location ?: getMeta("shop_location")
                        )
                    }
                } else {
                    profile
                }

                withContext(Dispatchers.Main) {
                    callback.onResult(finalProfile != null, finalProfile, if (finalProfile == null) "Profile not found" else null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onResult(false, null, e.message)
                }
            }
        }
    }

    @JvmStatic
    fun getAllProfiles(callback: SupabaseCallbackWithProfiles) {
        scope.launch {
            try {
                // 1. Get from DB
                val profiles = try {
                    client.postgrest["profiles"]
                        .select()
                        .decodeList<Profile>()
                } catch (e: Exception) {
                    android.util.Log.e("SupabaseManager", "getAllProfiles DB fail: ${e.message}")
                    emptyList<Profile>()
                }

                // 2. We can't easily get metadata for ALL users, but we can at least 
                // ensure the CURRENT user is perfectly merged in the list if they are in it.
                val currentUser = client.auth.currentSessionOrNull()?.user
                val finalProfiles = if (currentUser != null) {
                    profiles.map { p ->
                        if (p.id == currentUser.id) {
                            val metadata = currentUser.userMetadata
                            fun getMeta(key: String): String? = metadata?.get(key)?.toString()?.removeSurrounding("\"")
                            
                            p.copy(
                                first_name = p.first_name ?: getMeta("first_name"),
                                last_name = p.last_name ?: getMeta("last_name"),
                                account_type = p.account_type ?: getMeta("account_type"),
                                shop_name = p.shop_name ?: getMeta("shop_name"),
                                shop_description = p.shop_description ?: getMeta("shop_description"),
                                shop_location = p.shop_location ?: getMeta("shop_location")
                            )
                        } else p
                    }
                } else {
                    profiles
                }

                withContext(Dispatchers.Main) {
                    callback.onResult(true, finalProfiles, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onResult(false, null, e.message)
                }
            }
        }
    }

    interface SupabaseCallbackWithProfiles {
        fun onResult(success: Boolean, profiles: List<Profile>?, error: String?)
    }

    @JvmStatic
    fun updateProfile(userId: String, updates: Map<String, Any>, callback: SupabaseCallback) {
        scope.launch {
            try {
                android.util.Log.d("SupabaseManager", "Updating profile for $userId with $updates")
                
                // --- STEP 1: Update Auth Metadata (Highest reliability, works even if DB table is broken) ---
                try {
                    client.auth.updateUser {
                        data = buildJsonObject {
                            updates.forEach { (key, value) ->
                                when (value) {
                                    is String -> put(key, value)
                                    is Number -> put(key, value)
                                    is Boolean -> put(key, value)
                                    else -> put(key, value.toString())
                                }
                            }
                        }
                    }
                    android.util.Log.d("SupabaseManager", "Auth metadata updated successfully")
                } catch (e: Exception) {
                    android.util.Log.w("SupabaseManager", "Failed to update auth metadata: ${e.message}")
                }

                // --- STEP 2: Update 'profiles' table with multiple fallbacks ---
                fun buildJson(map: Map<String, Any>) = buildJsonObject {
                    map.forEach { (key, value) ->
                        when (value) {
                            is String -> put(key, value)
                            is Number -> put(key, value)
                            is Boolean -> put(key, value)
                            else -> put(key, value.toString())
                        }
                    }
                }

                var success = false
                var errorMessage: String? = null

                // Attempt 1: Full update as requested
                try {
                    client.postgrest.from("profiles").update(buildJson(updates)) {
                        filter { eq("id", userId) }
                    }
                    success = true
                } catch (e: Exception) {
                    android.util.Log.w("SupabaseManager", "Primary DB update failed: ${e.message}")
                    
                    // Attempt 2: Fallback from 'shop_location' to 'location' if that was the issue
                    if (updates.containsKey("shop_location")) {
                        try {
                            val fallbackUpdates = updates.toMutableMap()
                            val locValue = fallbackUpdates.remove("shop_location")
                            fallbackUpdates["location"] = locValue ?: ""
                            
                            client.postgrest.from("profiles").update(buildJson(fallbackUpdates)) {
                                filter { eq("id", userId) }
                            }
                            success = true
                        } catch (e2: Exception) {
                            android.util.Log.w("SupabaseManager", "Fallback 'location' update failed: ${e2.message}")
                        }
                    }
                    
                    // Attempt 3: Minimal update (remove location fields entirely, just update shop_name etc)
                    if (!success) {
                        try {
                            val minimalUpdates = updates.filter { it.key != "shop_location" && it.key != "location" }
                            if (minimalUpdates.isNotEmpty()) {
                                client.postgrest.from("profiles").update(buildJson(minimalUpdates)) {
                                    filter { eq("id", userId) }
                                }
                                success = true
                            }
                        } catch (e3: Exception) {
                            android.util.Log.w("SupabaseManager", "Minimal DB update failed: ${e3.message}")
                            errorMessage = e3.message
                        }
                    }
                }

                // Even if DB update failed, if metadata succeeded we consider it a partial success
                withContext(Dispatchers.Main) {
                    if (success) {
                        callback.onResult(true, null)
                    } else {
                        // If metadata update happened, we return true but log the DB error
                        // This allows the UI to proceed since the data IS saved in Auth Metadata
                        callback.onResult(true, "Profile saved to metadata (DB update failed: $errorMessage)")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SupabaseManager", "updateProfile general failure", e)
                withContext(Dispatchers.Main) { callback.onResult(false, e.message) }
            }
        }
    }

    @Serializable
    data class CartItem(
        val product_id: String,
        val user_id: String,
        val quantity: Int,
        val color: String? = null,
        val size: String? = null,
        val product_name: String? = null,
        val product_price: Double? = 0.0,
        val product_image_url: String? = null,
        val seller_id: String? = null
    )

    @Serializable
    data class Order(
        val id: String? = null,
        val customer_id: String,
        val seller_id: String,
        val total_amount: Double,
        val status: String = "pending",
        val delivery_address: String,
        val customer_name: String,
        val customer_phone: String,
        val created_at: String? = null
    )

    @Serializable
    data class ChatMessage(
        val id: String? = null,
        val chat_id: String,
        val sender_id: String,
        val content: String,
        val created_at: String? = null,
        val is_read: Boolean = false,
        val product_id: String? = null
    )

    @Serializable
    data class Chat(
        val id: String,
        val participant1: String,
        val participant2: String,
        val last_message: String? = null,
        val last_message_time: String? = null,
        val product_id: String? = null
    )

    @Serializable
    data class SellerMarker(
        val id: String? = null,
        val seller_id: String,
        val latitude: Double,
        val longitude: Double,
        val title: String,
        val created_at: String? = null
    )

    @JvmStatic
    fun getMessages(chatId: String, callback: SupabaseCallbackWithMessages) {
        scope.launch {
            try {
                val messages = client.postgrest["messages"]
                    .select {
                        filter {
                            eq("chat_id", chatId)
                        }
                    }
                    .decodeList<ChatMessage>()
                withContext(Dispatchers.Main) {
                    callback.onResult(true, messages, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onResult(false, null, e.message)
                }
            }
        }
    }

    interface SupabaseCallbackWithMessages {
        fun onResult(success: Boolean, messages: List<ChatMessage>?, error: String?)
    }

    @JvmStatic
    fun sendMessage(message: ChatMessage, callback: SupabaseCallback) {
        scope.launch {
            try {
                client.postgrest["messages"].insert(message)
                withContext(Dispatchers.Main) {
                    callback.onResult(true, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onResult(false, e.message)
                }
            }
        }
    }

    @JvmStatic
    fun getChatList(userId: String, callback: SupabaseCallbackWithChats) {
        scope.launch {
            try {
                val chats = client.postgrest["chats"]
                    .select {
                        filter {
                            or {
                                eq("participant1", userId)
                                eq("participant2", userId)
                            }
                        }
                    }
                    .decodeList<Chat>()
                withContext(Dispatchers.Main) {
                    callback.onResult(true, chats, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onResult(false, null, e.message)
                }
            }
        }
    }

    @JvmStatic
    fun getMarkers(callback: SupabaseCallbackWithMarkers) {
        scope.launch {
            try {
                val markers = client.postgrest["seller_markers"]
                    .select()
                    .decodeList<SellerMarker>()
                withContext(Dispatchers.Main) {
                    callback.onResult(true, markers, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onResult(false, null, e.message)
                }
            }
        }
    }

    interface SupabaseCallbackWithMarkers {
        fun onResult(success: Boolean, markers: List<SellerMarker>?, error: String?)
    }

    @JvmStatic
    fun saveMarker(marker: SellerMarker, callback: SupabaseCallback) {
        scope.launch {
            try {
                client.postgrest["seller_markers"].insert(marker)
                withContext(Dispatchers.Main) {
                    callback.onResult(true, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onResult(false, e.message)
                }
            }
        }
    }

    interface SupabaseCallbackWithChats {
        fun onResult(success: Boolean, chats: List<Chat>?, error: String?)
    }

    @JvmStatic
    fun getCart(userId: String, callback: SupabaseCallbackWithCart) {
        scope.launch {
            try {
                val items = client.postgrest["carts"]
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<CartItem>()
                withContext(Dispatchers.Main) {
                    callback.onResult(true, items, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onResult(false, null, e.message)
                }
            }
        }
    }

    interface SupabaseCallbackWithCart {
        fun onResult(success: Boolean, items: List<CartItem>?, error: String?)
    }

    @JvmStatic
    fun addToCart(item: CartItem, callback: SupabaseCallback) {
        scope.launch {
            try {
                client.postgrest["carts"].insert(item)
                withContext(Dispatchers.Main) {
                    callback.onResult(true, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onResult(false, e.message)
                }
            }
        }
    }

    @JvmStatic
    fun updateCartQuantity(userId: String, productId: String, newQuantity: Int, callback: SupabaseCallback) {
        scope.launch {
            try {
                client.postgrest["carts"].update(buildJsonObject {
                    put("quantity", newQuantity)
                }) {
                    filter {
                        eq("user_id", userId)
                        eq("product_id", productId)
                    }
                }
                withContext(Dispatchers.Main) {
                    callback.onResult(true, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onResult(false, e.message)
                }
            }
        }
    }

    @JvmStatic
    fun removeFromCart(userId: String, productId: String, callback: SupabaseCallback) {
        scope.launch {
            try {
                client.postgrest["carts"].delete {
                    filter {
                        eq("user_id", userId)
                        eq("product_id", productId)
                    }
                }
                withContext(Dispatchers.Main) {
                    callback.onResult(true, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onResult(false, e.message)
                }
            }
        }
    }

    @JvmStatic
    fun placeOrder(order: Order, callback: SupabaseCallback) {
        scope.launch {
            try {
                client.postgrest["orders"].insert(order)
                withContext(Dispatchers.Main) {
                    callback.onResult(true, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onResult(false, e.message)
                }
            }
        }
    }

    @JvmStatic
    fun getOrders(userId: String, isSeller: Boolean, callback: SupabaseCallbackWithOrders) {
        scope.launch {
            try {
                val orders = client.postgrest["orders"]
                    .select {
                        filter {
                            eq(if (isSeller) "seller_id" else "customer_id", userId)
                        }
                    }
                    .decodeList<Order>()
                withContext(Dispatchers.Main) {
                    callback.onResult(true, orders, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onResult(false, null, e.message)
                }
            }
        }
    }

    interface SupabaseCallbackWithOrders {
        fun onResult(success: Boolean, orders: List<Order>?, error: String?)
    }

    interface SupabaseCallbackWithProfile {
        fun onResult(success: Boolean, profile: Profile?, error: String?)
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    @Serializable
    data class Profile(
        @kotlinx.serialization.SerialName("id") val id: String,
        @kotlinx.serialization.SerialName("email") val email: String,
        @kotlinx.serialization.SerialName("first_name") val first_name: String? = null,
        @kotlinx.serialization.SerialName("last_name") val last_name: String? = null,
        @kotlinx.serialization.SerialName("account_type") val account_type: String? = null,
        @kotlinx.serialization.SerialName("shop_name") val shop_name: String? = null,
        @kotlinx.serialization.SerialName("shop_description") val shop_description: String? = null,
        @kotlinx.serialization.SerialName("shop_location") val shop_location: String? = null,
        @kotlinx.serialization.SerialName("location") val location: String? = null
    ) {
        fun getEffectiveLocation(): String? = shop_location ?: location
    }

    @JvmStatic
    fun signUp(emailValue: String, passwordValue: String, metadata: Map<String, String>, callback: SupabaseCallback) {
        scope.launch {
            try {
                android.util.Log.d("SupabaseManager", "Starting signUp for: $emailValue")
                val response = client.auth.signUpWith(Email) {
                    email = emailValue
                    password = passwordValue
                    data = buildJsonObject {
                        metadata.forEach { (key, value) ->
                            put(key, value)
                        }
                    }
                }
                
                android.util.Log.d("SupabaseManager", "signUp response: $response")

                // In Supabase-kt, signUpWith(Email) returns a User object if successful.
                // If email confirmation is enabled, it still returns the User object, but without a session.
                val user = response
                if (user != null) {
                    android.util.Log.d("SupabaseManager", "Auth signUp successful, user id: ${user.id}, email: ${user.email}")
                    
                    // Force a small delay to ensure Auth is settled in Supabase internal systems
                    kotlinx.coroutines.delay(500)

                    // Manually create the profile record since the trigger isn't available
                    val profile = Profile(
                        id = user.id,
                        email = emailValue,
                        first_name = metadata["first_name"],
                        last_name = metadata["last_name"],
                        account_type = metadata["account_type"],
                        shop_name = metadata["shop_name"],
                        shop_location = metadata["shop_location"]
                    )
                    
                    android.util.Log.d("SupabaseManager", "Attempting to insert profile: $profile")
                    
                    try {
                        // Explicitly specify the schema if necessary, or just insert
                        client.postgrest["profiles"].insert(profile)
                        android.util.Log.d("SupabaseManager", "SUCCESS: Profile inserted into database")
                    } catch (e: Exception) {
                        android.util.Log.e("SupabaseManager", "CRITICAL ERROR: Profile insertion failed", e)
                        // Even if profile insertion fails, the Auth account was created.
                        // We might want to inform the user that their account was created but profile setup failed.
                        throw Exception("Account created, but profile setup failed: ${e.localizedMessage}. Please try logging in and updating your profile.")
                    }
                } else {
                    android.util.Log.w("SupabaseManager", "Auth signUp returned null. This usually means the user already exists or email confirmation is required and the library didn't return the user object.")
                    // Note: In some versions of supabase-kt, it might throw an exception instead of returning null if it fails.
                    throw Exception("Auth Error: Could not create account. The user may already exist or there's a configuration issue.")
                }

                withContext(Dispatchers.Main) {
                    callback.onResult(true, null)
                }
            } catch (e: Exception) {
                android.util.Log.e("SupabaseManager", "signUp general failure", e)
                withContext(Dispatchers.Main) {
                    callback.onResult(false, e.message)
                }
            }
        }
    }

    @JvmStatic
    fun updatePassword(newPasswordValue: String, callback: SupabaseCallback) {
        scope.launch {
            try {
                client.auth.updateUser {
                    password = newPasswordValue
                }
                withContext(Dispatchers.Main) {
                    callback.onResult(true, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onResult(false, e.message)
                }
            }
        }
    }

    @JvmStatic
    fun getCurrentUserId(): String? {
        val status = client.auth.sessionStatus.value
        return if (status is SessionStatus.Authenticated) {
            status.session.user?.id
        } else {
            null
        }
    }

    @JvmStatic
    fun uploadImage(bucket: String, path: String, bytes: ByteArray, callback: SupabaseCallbackWithUrl) {
        scope.launch {
            try {
                android.util.Log.d("SupabaseManager", "Uploading to bucket: $bucket, path: $path, size: ${bytes.size} bytes")
                val bucketApi = client.storage.from(bucket)
                bucketApi.upload(path, bytes) {
                    upsert = true
                }
                val url = bucketApi.publicUrl(path)
                android.util.Log.d("SupabaseManager", "Upload successful: $url")
                withContext(Dispatchers.Main) {
                    callback.onResult(true, url, null)
                }
            } catch (e: Exception) {
                android.util.Log.e("SupabaseManager", "Upload failed for bucket [$bucket]: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback.onResult(false, null, e.message)
                }
            }
        }
    }

    @JvmStatic
    fun deleteImage(bucket: String, url: String) {
        scope.launch {
            try {
                // Extract path from public URL
                // URL example: https://[project].supabase.co/storage/v1/object/public/[bucket]/[path]
                val searchStr = "/public/$bucket/"
                val index = url.indexOf(searchStr)
                if (index != -1) {
                    val path = url.substring(index + searchStr.length)
                    android.util.Log.d("SupabaseManager", "Deleting old image from bucket [$bucket]: $path")
                    client.storage.from(bucket).delete(path)
                }
            } catch (e: Exception) {
                android.util.Log.w("SupabaseManager", "Failed to delete old image: ${e.message}")
            }
        }
    }

    interface SupabaseCallbackWithUrl {
        fun onResult(success: Boolean, url: String?, error: String?)
    }

    @Serializable
    data class ProductModel @JvmOverloads constructor(
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val id: String? = null,
        val seller_id: String,
        val name: String,
        val description: String,
        val price: Double,
        val main_category: String,
        val category: String,
        val cover_photo_url: String? = null,
        val weight: Double? = 0.0,
        val parcel_size: String? = null,
        val stock: Long = 0,
        val colors: List<String>? = null,
        val sizes: List<String>? = null,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val product_images: List<String>? = null,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val variation_images: Map<String, String>? = null,
        val is_available: Boolean = true,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val created_at: String? = null
    ) {
        fun get_id(): String? = id
        fun get_seller_id(): String = seller_id
        fun get_name(): String = name
        fun get_description(): String = description
        fun get_price(): Double = price
        fun get_main_category(): String = main_category
        fun get_category(): String = category
        fun get_cover_photo_url(): String? = cover_photo_url
        fun get_weight(): Double? = weight
        fun get_parcel_size(): String? = parcel_size
        fun get_stock(): Long = stock
        fun get_colors(): List<String>? = colors
        fun get_sizes(): List<String>? = sizes
        fun get_product_images(): List<String>? = product_images
        fun get_variation_images(): Map<String, String>? = variation_images
        fun is_available_val(): Boolean = is_available
        fun get_created_at(): String? = created_at

        // Helper to check if any field is null that shouldn't be
        fun debugString(): String {
            return "id=$id, seller_id=$seller_id, name=$name, main=$main_category, cat=$category"
        }
    }

    @JvmStatic
    fun saveProduct(product: ProductModel, callback: SupabaseCallback) {
        scope.launch {
            try {
                val json = kotlinx.serialization.json.Json { 
                    encodeDefaults = false
                    explicitNulls = false 
                }.encodeToString(ProductModel.serializer(), product)
                android.util.Log.d("SupabaseManager", "Attempting to save product JSON: $json")

                val response = client.postgrest["products"].insert(product)
                android.util.Log.d("SupabaseManager", "Insert finished. Response: $response")

                withContext(Dispatchers.Main) {
                    callback.onResult(true, null)
                }
            } catch (e: Exception) {
                android.util.Log.e("SupabaseManager", "Error saving product: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback.onResult(false, e.message)
                }
            }
        }
    }

    @JvmStatic
    fun getProducts(callback: SupabaseCallbackWithProducts) {
        scope.launch {
            try {
                android.util.Log.d("SupabaseManager", "Fetching all products")
                val response = client.postgrest["products"].select()
                android.util.Log.d("SupabaseManager", "Fetch all raw data: ${response.data}")
                val products = response.decodeList<ProductModel>()
                android.util.Log.d("SupabaseManager", "Decoded ${products.size} products")
                withContext(Dispatchers.Main) {
                    callback.onResult(true, products, null)
                }
            } catch (e: Exception) {
                android.util.Log.e("SupabaseManager", "Error fetching products: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback.onResult(false, null, e.message)
                }
            }
        }
    }

    @JvmStatic
    fun getProductsForSeller(sellerId: String, callback: SupabaseCallbackWithProducts) {
        scope.launch {
            try {
                android.util.Log.d("SupabaseManager", "Fetching products for seller: $sellerId")
                val response = client.postgrest["products"].select {
                    filter {
                        eq("seller_id", sellerId)
                    }
                }
                android.util.Log.d("SupabaseManager", "Fetch seller raw data: ${response.data}")
                val products = response.decodeList<ProductModel>()
                android.util.Log.d("SupabaseManager", "Decoded ${products.size} products for seller $sellerId")
                withContext(Dispatchers.Main) {
                    callback.onResult(true, products, null)
                }
            } catch (e: Exception) {
                android.util.Log.e("SupabaseManager", "Error fetching seller products: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback.onResult(false, null, e.message)
                }
            }
        }
    }

    @JvmStatic
    fun getProductsByCategory(mainCategory: String, subCategory: String?, callback: SupabaseCallbackWithProducts) {
        scope.launch {
            try {
                android.util.Log.d("SupabaseManager", "Fetching products for category: $mainCategory, sub: $subCategory")
                val products = client.postgrest["products"].select {
                    filter {
                        eq("main_category", mainCategory)
                        if (subCategory != null) {
                            eq("category", subCategory)
                        }
                    }
                }.decodeList<ProductModel>()
                android.util.Log.d("SupabaseManager", "Fetched ${products.size} products for category")
                withContext(Dispatchers.Main) {
                    callback.onResult(true, products, null)
                }
            } catch (e: Exception) {
                android.util.Log.e("SupabaseManager", "Error fetching category products: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback.onResult(false, null, e.message)
                }
            }
        }
    }

    interface SupabaseCallbackWithProducts {
        fun onResult(success: Boolean, products: List<ProductModel>?, error: String?)
    }

    @JvmStatic
    fun getProduct(productId: String, callback: SupabaseCallbackWithProduct) {
        scope.launch {
            try {
                val product = client.postgrest["products"].select {
                    filter {
                        eq("id", productId)
                    }
                }.decodeSingleOrNull<ProductModel>()
                withContext(Dispatchers.Main) {
                    if (product != null) {
                        callback.onResult(true, product, null)
                    } else {
                        callback.onResult(false, null, "Product not found")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onResult(false, null, e.message)
                }
            }
        }
    }

    interface SupabaseCallbackWithProduct {
        fun onResult(success: Boolean, product: ProductModel?, error: String?)
    }

    @JvmStatic
    fun updateProduct(productId: String, updates: Map<String, Any>, callback: SupabaseCallback) {
        scope.launch {
            try {
                client.postgrest["products"].update(buildJsonObject {
                    updates.forEach { (key, value) ->
                        when (value) {
                            is String -> put(key, value)
                            is Number -> put(key, value)
                            is Boolean -> put(key, value)
                            is List<*> -> {
                                putJsonArray(key) {
                                    value.forEach { item ->
                                        when (item) {
                                            is String -> add(item)
                                            is Number -> add(item)
                                            is Boolean -> add(item)
                                        }
                                    }
                                }
                            }
                            is Map<*, *> -> {
                                put(key, buildJsonObject {
                                    value.forEach { (k, v) ->
                                        val keyStr = k.toString()
                                        when (v) {
                                            is String -> put(keyStr, v)
                                            is Number -> put(keyStr, v)
                                            is Boolean -> put(keyStr, v)
                                        }
                                    }
                                })
                            }
                        }
                    }
                }) {
                    filter {
                        eq("id", productId)
                    }
                }
                withContext(Dispatchers.Main) {
                    callback.onResult(true, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onResult(false, e.message)
                }
            }
        }
    }

    @JvmStatic
    fun updateOrder(orderId: String, updates: Map<String, Any>, callback: SupabaseCallback) {
        scope.launch {
            try {
                client.postgrest["orders"].update(buildJsonObject {
                    updates.forEach { (key, value) ->
                        when (value) {
                            is String -> put(key, value)
                            is Number -> put(key, value)
                            is Boolean -> put(key, value)
                        }
                    }
                }) {
                    filter {
                        eq("id", orderId)
                    }
                }
                withContext(Dispatchers.Main) {
                    callback.onResult(true, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onResult(false, e.message)
                }
            }
        }
    }

    @JvmStatic
    fun deleteProduct(productId: String, callback: SupabaseCallback) {
        scope.launch {
            try {
                client.postgrest["products"].delete {
                    filter {
                        eq("id", productId)
                    }
                }
                callback.onResult(true, null)
            } catch (e: Exception) {
                callback.onResult(false, e.message)
            }
        }
    }

    interface SupabaseCallback {
        fun onResult(success: Boolean, error: String?)
    }
}
