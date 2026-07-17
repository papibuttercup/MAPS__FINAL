package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
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
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.bumptech.glide.Glide;

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

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.cardview.widget.CardView;
import android.animation.ValueAnimator;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;

public class Maps extends AppCompatActivity implements OnMapReadyCallback {
    private static final int PERMISSIONS_REQUEST_LOCATION = 99;
    private AutocompleteAdapter autocompleteAdapter;
    private AutoCompleteTextView searchInput;
    private OkHttpClient httpClient;
    private MapView mapView;
    private MapLibreMap maplibreMap;

    private LocationComponent locationComponent;
    private boolean isVerifiedSeller = false;
    private String userId;

    private CardView searchBar;
    private ImageView closeSearch, searchIcon;
    private ProgressBar searchProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapLibre.getInstance(this);
        userId = SupabaseManager.getCurrentUserId();
        checkSellerVerification();
        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        searchBar = findViewById(R.id.searchBar);
        searchIcon = findViewById(R.id.searchIcon);
        searchInput = findViewById(R.id.searchInput);
        closeSearch = findViewById(R.id.closeSearch);
        searchProgress = findViewById(R.id.searchProgress);

        searchIcon.setOnClickListener(v -> expandSearch());
        closeSearch.setOnClickListener(v -> collapseSearch());

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = searchInput.getText().toString();
                if (!query.isEmpty()) {
                    zoomToLocation(query);
                    collapseSearch();
                }
                return true;
            }
            return false;
        });

        httpClient = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).build();
        autocompleteAdapter = new AutocompleteAdapter(this, httpClient);
        searchInput.setAdapter(autocompleteAdapter);
        searchInput.setOnItemClickListener((parent, v, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            zoomToLocation(selected);
        });
        
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        setupBottomNavigation();
    }

    private void checkSellerVerification() {
        if (userId == null) return;
        SupabaseManager.getUserProfile(userId, new SupabaseManager.SupabaseCallbackWithProfile() {
            @Override public void onResult(boolean success, SupabaseManager.Profile profile, String error) {
                isVerifiedSeller = success && profile != null && "seller".equals(profile.getAccount_type());
            }
        });
    }

    @Override public void onMapReady(@NonNull MapLibreMap mapLibreMap) {
        this.maplibreMap = mapLibreMap;
        String url = "https://api.maptiler.com/maps/streets-v2/style.json?key=" + getString(R.string.maptiler_api_key);
        mapLibreMap.setStyle(new Style.Builder().fromUri(url), style -> {
            if (checkLocationPermission()) enableLocationComponent(style);
            loadAllSellerMarkers();
            
            // Set default view to Burnham Park, Baguio City
            maplibreMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(16.4124, 120.5940), 15));

            if ("pick_location".equals(getIntent().getStringExtra("mode"))) {
                Toast.makeText(this, "Long press on map to pick shop location", Toast.LENGTH_LONG).show();
                maplibreMap.addOnMapLongClickListener(point -> {
                    new AlertDialog.Builder(this)
                        .setTitle("Pick this location?")
                        .setMessage("Set this as your shop location?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            Intent result = new Intent();
                            result.putExtra("latitude", point.getLatitude());
                            result.putExtra("longitude", point.getLongitude());
                            setResult(RESULT_OK, result);
                            finish();
                        })
                        .setNegativeButton("No", null)
                        .show();
                    return true;
                });
            }
        });
    }

    private void loadAllSellerMarkers() {
        SupabaseManager.getMarkers(new SupabaseManager.SupabaseCallbackWithMarkers() {
            @Override public void onResult(boolean success, List<SupabaseManager.SellerMarker> markers, String error) {
                if (success && markers != null) {
                    for (SupabaseManager.SellerMarker m : markers) {
                        maplibreMap.addMarker(new MarkerOptions().position(new LatLng(m.getLatitude(), m.getLongitude())).title(m.getTitle()));
                    }
                }
            }
        });
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void enableLocationComponent(@NonNull Style style) {
        locationComponent = maplibreMap.getLocationComponent();
        locationComponent.activateLocationComponent(LocationComponentActivationOptions.builder(this, style).useDefaultLocationEngine(true).build());
        if (checkLocationPermission()) {
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.COMPASS);
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        if (nav != null) {
            nav.setSelectedItemId(R.id.navigation_favorites);
            nav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.navigation_home) { finish(); return true; }
                if (id == R.id.navigation_account) { startActivity(new Intent(this, AccountActivity.class)); return true; }
                return true;
            });
        }
    }

    private void expandSearch() {
        searchIcon.setVisibility(View.GONE);
        searchInput.setVisibility(View.VISIBLE);
        closeSearch.setVisibility(View.VISIBLE);
        
        ViewGroup.LayoutParams params = searchBar.getLayoutParams();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        searchBar.setLayoutParams(params);
        
        // Show keyboard
        searchInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
    }

    private void collapseSearch() {
        searchIcon.setVisibility(View.VISIBLE);
        searchInput.setVisibility(View.GONE);
        closeSearch.setVisibility(View.GONE);
        
        int size = (int) (48 * getResources().getDisplayMetrics().density);
        ViewGroup.LayoutParams params = searchBar.getLayoutParams();
        params.width = size;
        searchBar.setLayoutParams(params);
        
        // Hide keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
    }

    private void zoomToLocation(String placeName) {
        String url = "https://api.maptiler.com/geocoding/" + Uri.encode(placeName) + ".json?key=" + getString(R.string.maptiler_api_key) + "&limit=1";
        httpClient.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(Maps.this, "Search failed", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response res) throws IOException {
                if (res.isSuccessful()) {
                    JsonObject json = new Gson().fromJson(res.body().string(), JsonObject.class);
                    JsonArray features = json.getAsJsonArray("features");
                    if (features.size() > 0) {
                        JsonArray center = features.get(0).getAsJsonObject().getAsJsonArray("center");
                        double lng = center.get(0).getAsDouble();
                        double lat = center.get(1).getAsDouble();
                        runOnUiThread(() -> {
                            maplibreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 15));
                        });
                    }
                }
            }
        });
    }

    private class AutocompleteAdapter extends ArrayAdapter<String> implements Filterable {
        private List<String> suggestions = new ArrayList<>();
        private OkHttpClient client;
        AutocompleteAdapter(Context ctx, OkHttpClient c) { super(ctx, android.R.layout.simple_dropdown_item_1line); client = c; }
        @Override public int getCount() { return suggestions.size(); }
        @Override public String getItem(int p) { return suggestions.get(p); }
        @Override public Filter getFilter() {
            return new Filter() {
                @Override protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults r = new FilterResults();
                    if (constraint != null) { List<String> s = fetch(constraint.toString()); r.values = s; r.count = s.size(); }
                    return r;
                }
                @Override protected void publishResults(CharSequence c, FilterResults r) {
                    suggestions.clear(); if (r != null && r.values != null) suggestions.addAll((List<String>) r.values);
                    notifyDataSetChanged();
                }
            };
        }
        private List<String> fetch(String q) {
            List<String> s = new ArrayList<>();
            String url = "https://api.maptiler.com/geocoding/" + q + ".json?key=" + getString(R.string.maptiler_api_key) + "&limit=5";
            try {
                Response res = client.newCall(new Request.Builder().url(url).build()).execute();
                if (res.isSuccessful()) {
                    JsonObject json = new Gson().fromJson(res.body().string(), JsonObject.class);
                    JsonArray features = json.getAsJsonArray("features");
                    for (int i = 0; i < features.size(); i++) s.add(features.get(i).getAsJsonObject().get("place_name").getAsString());
                }
            } catch (Exception e) {}
            return s;
        }
    }

    @Override protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause() { super.onPause(); mapView.onPause(); }
    @Override protected void onStop() { super.onStop(); mapView.onStop(); }
    @Override protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); mapView.onSaveInstanceState(out); }
}
