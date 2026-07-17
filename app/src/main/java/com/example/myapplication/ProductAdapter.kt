package com.example.myapplication

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.databinding.ItemProductBinding

// Data class for ProductModel was moved to SupabaseManager.kt

class ProductAdapter(private val products: List<SupabaseManager.ProductModel>, private val isSeller: Boolean = false) :
    RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    interface OnEditProductListener {
        fun onEditProduct(product: SupabaseManager.ProductModel)
    }
    private var editListener: OnEditProductListener? = null
    fun setOnEditProductListener(listener: OnEditProductListener) {
        this.editListener = listener
    }

    inner class ProductViewHolder(val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.binding.txtProductName.text = product.name
        holder.binding.txtProductPrice.text = "₱${product.price}"
        holder.binding.textProductStock.text = "Stock: ${product.stock}"
        
        Glide.with(holder.itemView.context)
            .load(product.cover_photo_url)
            .placeholder(R.drawable.placeholder_image)
            .into(holder.binding.imgProduct)

        if (isSeller) {
            holder.binding.btnEditProduct.visibility = android.view.View.VISIBLE
            holder.binding.btnEditProduct.setOnClickListener {
                editListener?.onEditProduct(product)
            }
            holder.binding.btnDeleteProduct.visibility = android.view.View.VISIBLE
            holder.binding.btnDeleteProduct.setOnClickListener {
                AlertDialog.Builder(holder.itemView.context)
                    .setTitle("Delete Product")
                    .setMessage("Are you sure you want to delete this product?")
                    .setPositiveButton("Delete") { _, _ ->
                        val prodId = product.id ?: ""
                        SupabaseManager.deleteProduct(prodId, object : SupabaseManager.SupabaseCallback {
                            override fun onResult(success: Boolean, error: String?) {
                                if (success) {
                                    (holder.itemView.context as? android.app.Activity)?.runOnUiThread {
                                        (products as? MutableList<SupabaseManager.ProductModel>)?.let {
                                            val currentPos = holder.adapterPosition
                                            if (currentPos != RecyclerView.NO_POSITION) {
                                                it.removeAt(currentPos)
                                                notifyItemRemoved(currentPos)
                                            }
                                        }
                                        Toast.makeText(holder.itemView.context, "Product deleted", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    (holder.itemView.context as? android.app.Activity)?.runOnUiThread {
                                        Toast.makeText(holder.itemView.context, "Error: $error", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        })
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        } else {
            holder.binding.btnEditProduct.visibility = android.view.View.GONE
            holder.binding.btnDeleteProduct.visibility = android.view.View.GONE
        }
    }

    override fun getItemCount() = products.size
} 