package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.DocumentReference;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.Marker;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.location.LocationComponent;
import org.maplibre.android.location.LocationComponentActivationOptions;
import org.maplibre.android.location.modes.CameraMode;
import org.maplibre.android.location.modes.RenderMode;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.OnMapReadyCallback;
import org.maplibre.android.maps.Style;
import org.maplibre.android.style.layers.SymbolLayer;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.android.style.layers.PropertyFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import android.widget.RatingBar;
import android.widget.Button;
import android.widget.ImageButton;

import java.util.concurrent.TimeUnit;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.cardview.widget.CardView;
import android.widget.EditText;
import android.widget.FrameLayout;

import android.animation.ValueAnimator;
import android.view.inputmethod.InputMethodManager;

import android.widget.ProgressBar;

public class Maps extends AppCompatActivity implements OnMapReadyCallback {
    private static final int PERMISSIONS_REQUEST_LOCATION = 99;
    private static final String MARKERS_PREF = "saved_markers";
    private static final String MARKERS_KEY = "markers_list";
    private AutocompleteAdapter autocompleteAdapter;
    private AutoCompleteTextView searchInput;
    private OkHttpClient httpClient;
    private GeocodingService geocodingService;
    private MapView mapView;
    private MapLibreMap maplibreMap;
    private MapLibreMap map;

    private boolean isSearchExpanded = true;

    private LocationComponent locationComponent;
    private FloatingActionButton setLocationFab;
    private boolean isTracking = false;
    private LatLng lastKnownLocation;
    private View menuIcon;
    private ImageView searchIcon;
    private boolean isVerifiedSeller = false;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private BottomSheetDialog collapsedSheetDialog;
    private BottomNavigationView bottomNavigation;

    private CardView searchBar;
    private ImageView closeSearch;

    private LatLng selectedShopLatLng = null;
    private String selectedShopLocationName = null;
    private Marker shopMarker = null;

    private ProgressBar searchProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapLibre.getInstance(this);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        checkSellerVerification();
        @SuppressLint("InflateParams")
        View rootView = LayoutInflater.from(this).inflate(R.layout.activity_main, null);
        setContentView(rootView);

        mapView = findViewById(R.id.mapView);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        searchBar = findViewById(R.id.searchBar);
        searchInput = findViewById(R.id.searchInput);
        searchIcon = findViewById(R.id.searchIcon);
        closeSearch = findViewById(R.id.closeSearch);
        searchProgress = findViewById(R.id.searchProgress);

        // Initial state: collapsed
        collapseSearchBar(false);

        searchIcon.setOnClickListener(v -> expandSearchBar());
        closeSearch.setOnClickListener(v -> collapseSearchBar(true));
        searchInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) collapseSearchBar(true);
        });

        // Show/hide close icon based on text
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                closeSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // Search on action
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.getAction() == KeyEvent.ACTION_DOWN &&
                 event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                performSearch();
                return true;
            }
            return false;
        });

        // Set up autocomplete adapter
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        autocompleteAdapter = new AutocompleteAdapter(this, httpClient);
        searchInput.setAdapter(autocompleteAdapter);
        searchInput.setThreshold(1);
        searchInput.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = (String) parent.getItemAtPosition(position);
            searchInput.setText(selectedItem);
            performSearch();
        });

        searchInput.setDropDownAnchor(R.id.searchBar);
        searchInput.setDropDownWidth(searchBar.getWidth());

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        setupBottomNavigation();

        // Handle intent for shop location selection
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("locationName")) {
            selectedShopLocationName = intent.getStringExtra("locationName");
            LatLng center = new LatLng(16.4023, 120.5960); // Default: Baguio
            mapView.getMapAsync(mapLibreMap -> {
                mapLibreMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 15f));
                Toast.makeText(this, "Tap on the map to set your shop location, then click 'Set This Location'", Toast.LENGTH_LONG).show();
                // Only allow tap-to-place-marker, no marker click listeners
                mapLibreMap.addOnMapClickListener(point -> {
                    selectedShopLatLng = point;
                    if (shopMarker != null) mapLibreMap.removeMarker(shopMarker);
                    shopMarker = mapLibreMap.addMarker(new MarkerOptions().position(point).title(selectedShopLocationName));
                    new AlertDialog.Builder(Maps.this)
                        .setTitle("Set This Location?")
                        .setMessage("Do you want to set this as your shop location?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            if (selectedShopLatLng != null && selectedShopLocationName != null) {
                                Intent result = new Intent();
                                result.putExtra("latitude", selectedShopLatLng.getLatitude());
                                result.putExtra("longitude", selectedShopLatLng.getLongitude());
                                result.putExtra("locationName", selectedShopLocationName);
                                setResult(RESULT_OK, result);
                                finish();
                            }
                        })
                        .setNegativeButton("No", (dialog, which) -> {
                            if (shopMarker != null) mapLibreMap.removeMarker(shopMarker);
                            selectedShopLatLng = null;
                        })
                        .show();
                    return true;
                });
            });
            return;
        }

        final Handler debounceHandler = new Handler(Looper.getMainLooper());
        final int DEBOUNCE_DELAY = 300;
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            private Runnable debounceRunnable;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                searchProgress.setVisibility(View.VISIBLE);
                debounceRunnable = () -> autocompleteAdapter.getFilter().filter(s);
                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_DELAY);
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    // Utility method for dp to px
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void checkSellerVerification() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            isVerifiedSeller = false;
            return;
        }

        db.collection("sellers")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String status = documentSnapshot.getString("verificationStatus");
                        isVerifiedSeller = "approved".equals(status);
                        if (isVerifiedSeller) {
                            showToast("Verified seller access granted");
                        }
                    } else {
                        isVerifiedSeller = false;
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Maps", "Error checking verification status", e);
                    isVerifiedSeller = false;
                });
    }

    private void performSearch() {
        String searchText = searchInput.getText().toString().trim();
        if (!searchText.isEmpty()) {
            searchLocation(searchText);
        } else {
            showToast("Please enter a location");
        }
    }

    private class AutocompleteAdapter extends ArrayAdapter<String> implements Filterable {
        private List<String> suggestions;
        private final OkHttpClient httpClient;
        private boolean lastHadError = false;

        public AutocompleteAdapter(Context context, OkHttpClient httpClient) {
            super(context, android.R.layout.simple_dropdown_item_1line);
            this.suggestions = new ArrayList<>();
            this.httpClient = httpClient;
        }

        @Override
        public int getCount() {
            return suggestions.size();
        }

        @Nullable
        @Override
        public String getItem(int position) {
            return position < suggestions.size() ? suggestions.get(position) : null;
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    if (constraint != null && constraint.length() > 0) {
                        List<String> newSuggestions = fetchSuggestions(constraint.toString());
                        results.values = newSuggestions;
                        results.count = newSuggestions.size();
                    } else {
                        results.values = new ArrayList<String>();
                        results.count = 0;
                    }
                    return results;
                }

                @Override
                @SuppressWarnings("unchecked")
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    searchProgress.setVisibility(View.GONE);
                    suggestions.clear();
                    if (results != null && results.values != null) {
                        List<String> newSuggestions = (List<String>) results.values;
                        if (lastHadError) {
                            suggestions.add("Network error");
                            lastHadError = false;
                        } else if (newSuggestions.isEmpty()) {
                            suggestions.add("No results found");
                        } else {
                            suggestions.addAll(newSuggestions);
                        }
                    } else {
                        suggestions.add("No results found");
                    }
                    notifyDataSetChanged();
                }
            };
        }

        private List<String> fetchSuggestions(String query) {
            List<String> newSuggestions = new ArrayList<>();
            String apiKey = BuildConfig.MAPTILER_API_KEY;
            String url = "https://api.maptiler.com/geocoding/" + query + ".json?key=" + apiKey + "&limit=5&autocomplete=true";
            Request request = new Request.Builder().url(url).build();
            try {
                Response response = httpClient.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonObject jsonObject = new Gson().fromJson(responseBody, JsonObject.class);
                    JsonArray features = jsonObject.getAsJsonArray("features");
                    if (features != null) {
                        for (int i = 0; i < features.size(); i++) {
                            JsonObject feature = features.get(i).getAsJsonObject();
                            String placeName = feature.get("place_name").getAsString();
                            newSuggestions.add(placeName);
                        }
                    }
                } else {
                    lastHadError = true;
                }
            } catch (IOException e) {
                Log.e("Autocomplete", "Error fetching suggestions", e);
                lastHadError = true;
            }
            return newSuggestions;
        }
    }

    @Override
    public void onMapReady(@NonNull MapLibreMap mapLibreMap) {
        this.maplibreMap = mapLibreMap;

        String mapTilerApiKey = BuildConfig.MAPTILER_API_KEY;
        String mapId = "streets-v2";
        String styleUrl = "https://api.maptiler.com/maps/" + mapId + "/style.json?key=" + mapTilerApiKey;

        mapLibreMap.setStyle(new Style.Builder().fromUri(styleUrl), this::setupMapStyle);
    }

    private void setupMapStyle(@NonNull Style style) {
        maplibreMap.setCameraPosition(new CameraPosition.Builder()
                .target(new LatLng(16.3835, 120.5924))
                .zoom(15.0)
                .build());

        if (checkLocationPermission()) {
            enableLocationComponent(style);
        }

        // Only set marker click listeners if NOT in shop location selection mode
        Intent intent = getIntent();
        if (intent == null || !intent.hasExtra("locationName")) {
            maplibreMap.setOnMarkerClickListener(marker -> {
                String shopName = marker.getTitle();
                String locationName = "Sample Location";
                showCollapsedBottomSheet(shopName, locationName);
                return true;
            });
        }
    }

    private void loadAllSellerMarkers() {
        db.collection("seller_markers")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    Double lat = document.getDouble("latitude");
                    Double lng = document.getDouble("longitude");
                    String title = document.getString("title");
                    
                    if (lat != null && lng != null && title != null) {
                        MarkerOptions markerOptions = new MarkerOptions()
                            .position(new LatLng(lat, lng))
                            .title(title);
                        
                        maplibreMap.addMarker(markerOptions);
                    }
                }
            })
            .addOnFailureListener(e -> {
                String errorMessage = "Error loading markers: " + e.getMessage();
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                Log.e("Maps", errorMessage, e);
            });
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_LOCATION);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_LOCATION &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (maplibreMap != null) {
                maplibreMap.getStyle(this::enableLocationComponent);
            }
        }
    }

    private void enableLocationComponent(@NonNull Style style) {
        try {
            LocationComponentActivationOptions options =
                    LocationComponentActivationOptions.builder(this, style)
                            .useDefaultLocationEngine(true)
                            .build();

            locationComponent = maplibreMap.getLocationComponent();
            locationComponent.activateLocationComponent(options);

            if (checkLocationPermission()) {
                locationComponent.setLocationComponentEnabled(true);
                locationComponent.setCameraMode(CameraMode.TRACKING);
                locationComponent.setRenderMode(RenderMode.COMPASS);
            }
        } catch (Exception e) {
            Log.e("LocationComponent", "Error enabling location component", e);
        }
    }

    private void addMarker(double lat, double lng, String title) {
        if (!isVerifiedSeller) {
            showToast("Only verified sellers can add markers");
            return;
        }
        LatLng position = new LatLng(lat, lng);
        maplibreMap.addMarker(new MarkerOptions()
                .position(position)
                .title(title));
        saveMarkerToFirestore(lat, lng, title);
        maplibreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
        showToast("Marker added: " + title);
    }

    private void saveMarkerToFirestore(double lat, double lng, String title) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            showToast("Please sign in to add markers");
            return;
        }
        String userId = currentUser.getUid();
        Map<String, Object> markerData = new HashMap<>();
        markerData.put("latitude", lat);
        markerData.put("longitude", lng);
        markerData.put("title", title);
        markerData.put("sellerId", userId);
        markerData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
        db.collection("seller_markers")
            .add(markerData)
            .addOnSuccessListener(documentReference -> showToast("Location saved"))
            .addOnFailureListener(e -> showToast("Error saving location: " + e.getMessage()));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void searchLocation(String address) {
        String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
        String url = "https://api.maptiler.com/geocoding/" + encodedAddress + ".json?key=" + getString(R.string.maptiler_api_key);

        Request request = new Request.Builder()
                .url(url)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(Maps.this, "Error searching location: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(Maps.this, "Error searching location", Toast.LENGTH_SHORT).show());
                    return;
                }

                String responseData = response.body().string();
                JsonObject jsonResponse = new Gson().fromJson(responseData, JsonObject.class);
                JsonArray features = jsonResponse.getAsJsonArray("features");

                if (features != null && features.size() > 0) {
                    JsonObject firstResult = features.get(0).getAsJsonObject();
                    JsonArray coordinates = firstResult.getAsJsonObject("geometry").getAsJsonArray("coordinates");
                    double longitude = coordinates.get(0).getAsDouble();
                    double latitude = coordinates.get(1).getAsDouble();

                    runOnUiThread(() -> {
                        // Move camera to location only
                        LatLng location = new LatLng(latitude, longitude);
                        maplibreMap.setCameraPosition(new CameraPosition.Builder()
                                .target(location)
                                .zoom(15.0)
                                .build());
                        searchInput.setText("");
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(Maps.this, "No results found", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    private void showCollapsedBottomSheet(String shopName, String locationName) {
        try {
            if (collapsedSheetDialog != null && collapsedSheetDialog.isShowing()) {
                collapsedSheetDialog.dismiss();
            }

            View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_collapsed, null);
            collapsedSheetDialog = new BottomSheetDialog(this);
            collapsedSheetDialog.setContentView(bottomSheetView);

            TextView shopNameView = bottomSheetView.findViewById(R.id.shopName);
            TextView locationNameView = bottomSheetView.findViewById(R.id.locationName);
            Button expandButton = bottomSheetView.findViewById(R.id.expandButton);

            if (shopNameView == null || locationNameView == null || expandButton == null) {
                Log.e("Maps", "Failed to find views in bottom sheet");
                showToast("Error loading shop details");
                return;
            }

            shopNameView.setText(shopName);
            locationNameView.setText(locationName);

            expandButton.setOnClickListener(v -> {
                collapsedSheetDialog.dismiss();
                showOrderItemSheet(shopName, locationName);
            });

            collapsedSheetDialog.show();
        } catch (Exception e) {
            Log.e("Maps", "Error showing bottom sheet", e);
            showToast("Error loading shop details");
        }
    }

    private void showOrderItemSheet(String shopName, String locationName) {
        getSellerIdForShop(shopName, new SellerIdCallback() {
            @Override
            public void onSuccess(String sellerId) {
                showProductListBottomSheet(sellerId);
            }

            @Override
            public void onFailure(String error) {
                showToast("Error: " + error);
            }
        });
    }

    private void showProductListBottomSheet(String sellerId) {
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_product_list, null);
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(bottomSheetView);

        RecyclerView recyclerView = bottomSheetView.findViewById(R.id.productRecyclerView);
        TextView emptyView = bottomSheetView.findViewById(R.id.emptyView);
        ImageButton closeButton = bottomSheetView.findViewById(R.id.closeProductList);

        if (recyclerView == null || emptyView == null || closeButton == null) {
            Log.e("Maps", "Failed to find views in bottom sheet");
            showToast("Error loading products");
            return;
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        closeButton.setOnClickListener(v -> bottomSheetDialog.dismiss());

        List<Product> products = new ArrayList<>();
        db.collection("products")
            .whereEqualTo("sellerId", sellerId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    Product product = document.toObject(Product.class);
                    if (product != null && product.isAvailable) {
                        product.id = document.getId();
                        products.add(product);
                    }
                }

                if (products.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                    ProductAdapter adapter = new ProductAdapter(products, false);
                    recyclerView.setAdapter(adapter);
                }
            })
            .addOnFailureListener(e -> {
                Log.e("Maps", "Error fetching products", e);
                showToast("Error loading products");
            });

        bottomSheetDialog.show();
    }

    private void placeOrder(Product product) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            showToast("Please login to place an order");
            return;
        }

        // Use Firestore's auto-generated order ID
        DocumentReference orderRef = db.collection("orders").document();
        String orderId = orderRef.getId();

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", orderId);
        orderData.put("productId", product.id);
        orderData.put("productName", product.name);
        orderData.put("price", product.price);
        orderData.put("sellerId", product.sellerId);
        orderData.put("customerId", currentUser.getUid());
        orderData.put("status", "pending");
        orderData.put("timestamp", FieldValue.serverTimestamp());
        orderData.put("quantity", 1); // Default quantity for map orders

        // Start transaction to update stock and create order
        db.runTransaction(transaction -> {
            // Get current product data
            DocumentSnapshot productDoc = transaction.get(
                db.collection("products").document(product.id)
            );

            if (!productDoc.exists()) {
                throw new FirebaseFirestoreException("Product not found", 
                    FirebaseFirestoreException.Code.NOT_FOUND);
            }

            // Get current stock entries
            List<Map<String, Object>> stockEntries = 
                (List<Map<String, Object>>) productDoc.get("stockEntries");
            if (stockEntries == null) {
                throw new FirebaseFirestoreException("No stock entries found", 
                    FirebaseFirestoreException.Code.NOT_FOUND);
            }

            // Find and update the first available stock entry
            boolean stockUpdated = false;
            for (Map<String, Object> entry : stockEntries) {
                Long stock = (Long) entry.get("stock");
                if (stock != null && stock > 0) {
                    entry.put("stock", stock - 1);
                    stockUpdated = true;
                    break;
                }
            }

            if (!stockUpdated) {
                throw new FirebaseFirestoreException("No stock available", 
                    FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }

            // Update product stock entries
            transaction.update(
                db.collection("products").document(product.id),
                "stockEntries", stockEntries
            );

            // Create order
            transaction.set(orderRef, orderData);

            return null;
        })
        .addOnSuccessListener(aVoid -> {
            showToast("Order placed successfully");
        })
        .addOnFailureListener(e -> {
            String errorMessage = "Failed to place order: ";
            if (e instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException fe = (FirebaseFirestoreException) e;
                switch (fe.getCode()) {
                    case NOT_FOUND:
                        errorMessage += "Product or stock entry not found";
                        break;
                    case FAILED_PRECONDITION:
                        errorMessage += "No stock available";
                        break;
                    default:
                        errorMessage += e.getMessage();
                }
            } else {
                errorMessage += e.getMessage();
            }
            showToast(errorMessage);
            Log.e("Maps", "Error placing order", e);
        });
    }

    private class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
        private final List<Product> products;
        private final boolean isSeller;

        ProductAdapter(List<Product> products, boolean isSeller) {
            this.products = products;
            this.isSeller = isSeller;
        }

        @NonNull
        @Override
        public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_product, parent, false);
            return new ProductViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
            Product product = products.get(position);
            holder.productName.setText(product.name);
            holder.productPrice.setText(String.format("$%s", product.price));
            RequestOptions requestOptions = new RequestOptions()
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .centerCrop();
            String imageUrl = null;
            if (product.coverPhotoUri != null && !product.coverPhotoUri.isEmpty()) {
                imageUrl = product.coverPhotoUri;
            } else if (product.productImageUris != null && !product.productImageUris.isEmpty()) {
                imageUrl = product.productImageUris.get(0);
            }
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .apply(requestOptions)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.productImage);
            } else {
                holder.productImage.setImageResource(R.drawable.placeholder_image);
            }
            // Hide the edit pen for customers
            android.widget.ImageButton editButton = holder.itemView.findViewById(R.id.btnEditProduct);
            if (editButton != null) {
                editButton.setVisibility(android.view.View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return products.size();
        }

        class ProductViewHolder extends RecyclerView.ViewHolder {
            ImageView productImage;
            TextView productName, productPrice;

            ProductViewHolder(@NonNull View itemView) {
                super(itemView);
                productImage = itemView.findViewById(R.id.imgProduct);
                productName = itemView.findViewById(R.id.txtProductName);
                productPrice = itemView.findViewById(R.id.txtProductPrice);
            }
        }
    }

    private interface OnOrderClickListener {
        void onOrder(Product product);
    }

    private void updateAverageRating(String sellerId, TextView shopRatingView) {
        db.collection("sellers").document(sellerId)
            .collection("ratings").get()
            .addOnSuccessListener(querySnapshot -> {
                double sum = 0;
                int count = 0;
                for (DocumentSnapshot doc : querySnapshot) {
                    Number rating = (Number) doc.get("rating");
                    if (rating != null) {
                        sum += rating.doubleValue();
                        count++;
                    }
                }
                double avg = count > 0 ? sum / count : 0;
                db.collection("sellers").document(sellerId)
                    .update("averageRating", avg)
                    .addOnSuccessListener(aVoid -> {
                        if (shopRatingView != null) shopRatingView.setText("★ " + String.format("%.1f", avg));
                    });
            });
    }

    private interface SellerIdCallback {
        void onSuccess(String sellerId);
        void onFailure(String error);
    }

    private void getSellerIdForShop(String shopName, SellerIdCallback callback) {
        if (shopName == null || shopName.isEmpty()) {
            callback.onFailure("Invalid shop name");
            return;
        }

        db.collection("seller_markers")
                .whereEqualTo("title", shopName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String sellerId = queryDocumentSnapshots.getDocuments().get(0).getString("sellerId");
                        if (sellerId != null) {
                            db.collection("sellers")
                                    .document(sellerId)
                                    .get()
                                    .addOnSuccessListener(sellerDoc -> {
                                        if (sellerDoc.exists() && "approved".equals(sellerDoc.getString("verificationStatus"))) {
                                            callback.onSuccess(sellerId);
                                        } else {
                                            callback.onFailure("Shop is not verified");
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("Maps", "Error finding seller", e);
                                        callback.onFailure("Error finding shop");
                                    });
                        } else {
                            callback.onFailure("Shop not found");
                        }
                    } else {
                        callback.onFailure("Shop not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Maps", "Error finding marker", e);
                    callback.onFailure("Error finding shop");
                });
    }

    private void setupMapTiler() {
        mapView.getMapAsync(mapLibreMap -> {
            this.map = mapLibreMap;
            
            String styleUrl = "https://api.maptiler.com/maps/streets/style.json?key=" + getString(R.string.maptiler_api_key);
            map.setStyle(new Style.Builder().fromUri(styleUrl), style -> {
                map.getStyle().addSource(new GeoJsonSource("markers"));
                
                map.getStyle().addLayer(new SymbolLayer("markers", "markers")
                    .withProperties(
                        PropertyFactory.iconImage("marker"),
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconIgnorePlacement(true)
                    ));
                
                loadAllSellerMarkers();
            });
        });
    }

    private void setupBottomNavigation() {
        Log.d("Maps", "Setting up bottom navigation");
        if (bottomNavigation != null) {
            Log.d("Maps", "Bottom Navigation is not null, proceeding with setup");
            bottomNavigation.setSelectedItemId(R.id.navigation_for_you);
            bottomNavigation.setVisibility(View.VISIBLE);
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                Log.d("Maps", "Navigation item selected: " + itemId);
                if (itemId == R.id.navigation_for_you) {
                    return true;
                } else if (itemId == R.id.navigation_home) {
                    finish();
                    return true;
                } else if (itemId == R.id.navigation_account) {
                    startActivity(new Intent(Maps.this, AccountActivity.class));
                    return true;
                }
                return false;
            });
        } else {
            Log.e("Maps", "Bottom Navigation is null in setupBottomNavigation");
        }
    }

    private void zoomToUserLocation() {
        if (locationComponent != null && locationComponent.isLocationComponentEnabled()) {
            Location lastKnownLocation = locationComponent.getLastKnownLocation();
            if (lastKnownLocation != null) {
                maplibreMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()),
                        15.0
                    )
                );
            }
        }
    }

    private void expandSearchBar() {
        int targetWidth = ViewGroup.LayoutParams.MATCH_PARENT;
        ValueAnimator animator = ValueAnimator.ofInt(searchBar.getWidth(), dpToPx(320));
        animator.setDuration(250);
        animator.addUpdateListener(animation -> {
            int val = (Integer) animation.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = searchBar.getLayoutParams();
            layoutParams.width = val;
            searchBar.setLayoutParams(layoutParams);
            searchInput.setDropDownWidth(val);
        });
        animator.start();
        searchInput.setVisibility(View.VISIBLE);
        closeSearch.setVisibility(searchInput.getText().length() > 0 ? View.VISIBLE : View.GONE);
        searchInput.requestFocus();
        searchInput.post(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
        });
    }

    private void collapseSearchBar(boolean clearText) {
        ValueAnimator animator = ValueAnimator.ofInt(searchBar.getWidth(), dpToPx(48));
        animator.setDuration(250);
        animator.addUpdateListener(animation -> {
            int val = (Integer) animation.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = searchBar.getLayoutParams();
            layoutParams.width = val;
            searchBar.setLayoutParams(layoutParams);
            searchInput.setDropDownWidth(val);
        });
        animator.start();
        searchInput.setVisibility(View.GONE);
        closeSearch.setVisibility(View.GONE);
        if (clearText) searchInput.setText("");
        searchBar.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
    }
}