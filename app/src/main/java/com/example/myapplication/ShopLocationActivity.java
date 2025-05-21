package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.activity.OnBackPressedCallback;
import com.google.firebase.firestore.FirebaseFirestore;
import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.Marker;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.OnMapReadyCallback;
import org.maplibre.android.maps.Style;

public class ShopLocationActivity extends AppCompatActivity implements OnMapReadyCallback {
    private MapView mapView;
    private MapLibreMap maplibreMap;
    private TextView tvShopName;
    private TextView tvNoLocation;
    private FirebaseFirestore db;
    private String sellerId;
    private String shopName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize MapLibre
        MapLibre.getInstance(this);

        setContentView(R.layout.activity_shop_location);

        // Handle back press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        // Get seller ID and shop name from intent
        sellerId = getIntent().getStringExtra("sellerId");
        shopName = getIntent().getStringExtra("shopName");

        if (sellerId == null || shopName == null) {
            Toast.makeText(this, "Error: Missing shop information", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        mapView = findViewById(R.id.mapView);
        tvShopName = findViewById(R.id.tvShopName);
        tvNoLocation = findViewById(R.id.tvNoLocation);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        tvShopName.setText(shopName + " Location");

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Set up map
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull MapLibreMap mapLibreMap) {
        this.maplibreMap = mapLibreMap;

        String mapTilerApiKey = BuildConfig.MAPTILER_API_KEY;
        String mapId = "streets-v2";
        String styleUrl = "https://api.maptiler.com/maps/" + mapId + "/style.json?key=" + mapTilerApiKey;
        mapLibreMap.setStyle(new Style.Builder().fromUri(styleUrl), style -> {
            // Load shop location
            loadShopLocation();
        });
    }

    private void loadShopLocation() {
        db.collection("sellers").document(sellerId)
            .get()
            .addOnSuccessListener(document -> {
                Double latitude = document.getDouble("latitude");
                Double longitude = document.getDouble("longitude");
                String locationName = document.getString("locationName");

                if (latitude != null && longitude != null) {
                    // Show location on map
                    LatLng shopLocation = new LatLng(latitude, longitude);
                    maplibreMap.addMarker(new MarkerOptions()
                        .position(shopLocation)
                        .title(shopName)
                        .snippet(locationName));
                    
                    // Move camera to shop location
                    maplibreMap.setCameraPosition(new CameraPosition.Builder()
                        .target(shopLocation)
                        .zoom(15.0)
                        .build());
                    
                    tvNoLocation.setVisibility(View.GONE);
                } else {
                    // Show "Location not set" message
                    tvNoLocation.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "This shop has not set their location yet", Toast.LENGTH_LONG).show();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error loading shop location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            });
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
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
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
} 