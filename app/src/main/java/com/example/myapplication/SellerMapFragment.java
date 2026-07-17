package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.app.AlertDialog;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

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

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class SellerMapFragment extends Fragment implements OnMapReadyCallback {
    private static final int PERMISSIONS_REQUEST_LOCATION = 99;
    private MapView mapView;
    private MapLibreMap maplibreMap;
    private LocationComponent locationComponent;
    private FloatingActionButton gpsFab;
    private boolean isTracking = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_seller_map, container, false);
        
        mapView = view.findViewById(R.id.mapView);
        gpsFab = view.findViewById(R.id.gpsFab);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        gpsFab.setOnClickListener(v -> {
            if (checkLocationPermission()) {
                toggleGpsTracking();
            } else {
                requestLocationPermission();
            }
        });

        return view;
    }

    @Override
    public void onMapReady(@NonNull MapLibreMap mapLibreMap) {
        this.maplibreMap = mapLibreMap;

        String mapTilerApiKey = BuildConfig.MAPTILER_API_KEY;
        String mapId = "streets-v2";
        String styleUrl = "https://api.maptiler.com/maps/" + mapId + "/style.json?key=" + mapTilerApiKey;

        mapLibreMap.setStyle(new Style.Builder().fromUri(styleUrl), style -> {
            enableLocationComponent(style);
            loadSellerMarkers();
            
            // Enable map gestures
            mapLibreMap.getUiSettings().setScrollGesturesEnabled(true);
            mapLibreMap.getUiSettings().setZoomGesturesEnabled(true);
            mapLibreMap.getUiSettings().setRotateGesturesEnabled(true);
            mapLibreMap.getUiSettings().setTiltGesturesEnabled(true);
            
            // Add marker click listener for editing/deleting
            maplibreMap.setOnMarkerClickListener(marker -> {
                showMarkerOptionsDialog(marker);
                return true;
            });
            
            // If we have location permission, zoom to user's location
            if (checkLocationPermission()) {
                zoomToUserLocation();
            }

            maplibreMap.addOnMapClickListener(point -> {
                new AlertDialog.Builder(requireContext())
                    .setTitle("Set This Location?")
                    .setMessage("Do you want to set this as your shop location?")
                    .setPositiveButton("Yes", (dialog, which) -> addMarker(point.getLatitude(), point.getLongitude(), "My Shop Location"))
                    .setNegativeButton("No", null)
                    .show();
                return true;
            });
        });
    }

    private void enableLocationComponent(@NonNull Style style) {
        if (maplibreMap != null) {
            locationComponent = maplibreMap.getLocationComponent();
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(requireContext(), style).build()
            );
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.NORMAL);
        }
    }

    private void toggleGpsTracking() {
        if (locationComponent != null) {
            if (!isTracking) {
                locationComponent.setCameraMode(CameraMode.TRACKING);
                locationComponent.setRenderMode(RenderMode.NORMAL);
                isTracking = true;
                // Zoom to user's location when tracking is enabled
                zoomToUserLocation();
            } else {
                locationComponent.setCameraMode(CameraMode.NONE);
                locationComponent.setRenderMode(RenderMode.NORMAL);
                isTracking = false;
            }
        }
    }

    private void zoomToUserLocation() {
        if (locationComponent != null && locationComponent.isLocationComponentEnabled()) {
            // Get the last known location
            android.location.Location lastLocation = locationComponent.getLastKnownLocation();
            if (lastLocation != null) {
                // Create a camera position with the user's location and zoom level
                CameraPosition position = new CameraPosition.Builder()
                    .target(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()))
                    .zoom(15) // Adjust zoom level as needed (higher number = more zoomed in)
                    .build();
                
                // Animate the camera to the position
                maplibreMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 1000);
            }
        }
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
            PERMISSIONS_REQUEST_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, enable location component
                if (maplibreMap != null) {
                    maplibreMap.getStyle(style -> {
                        enableLocationComponent(style);
                        zoomToUserLocation();
                    });
                }
            } else {
                Toast.makeText(requireContext(), "Location permission is required for this feature",
                    Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addMarker(double lat, double lng, String title) {
        if (maplibreMap != null) {
            MarkerOptions markerOptions = new MarkerOptions()
                .position(new LatLng(lat, lng))
                .title(title);

            Marker marker = maplibreMap.addMarker(markerOptions);
            saveMarkerToSupabase(lat, lng, title);
        }
    }

    private void saveMarkerToSupabase(double lat, double lng, String title) {
        String userId = SupabaseManager.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(requireContext(), "Please sign in to add markers", Toast.LENGTH_SHORT).show();
            return;
        }

        SupabaseManager.SellerMarker marker = new SupabaseManager.SellerMarker(
            null, userId, lat, lng, title, null
        );

        SupabaseManager.saveMarker(marker, new SupabaseManager.SupabaseCallback() {
            @Override
            public void onResult(boolean success, String error) {
                if (success) {
                    Toast.makeText(requireContext(), "Location saved", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Error saving location: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadSellerMarkers() {
        SupabaseManager.getMarkers(new SupabaseManager.SupabaseCallbackWithMarkers() {
            @Override
            public void onResult(boolean success, List<SupabaseManager.SellerMarker> markers, String error) {
                if (success && markers != null) {
                    String currentUserId = SupabaseManager.getCurrentUserId();
                    for (SupabaseManager.SellerMarker marker : markers) {
                        // Only show markers for current seller if intended, or all markers?
                        // Previous logic filtered by userId, so I'll keep that if currentUserId is not null
                        if (currentUserId != null && marker.getSeller_id().equals(currentUserId)) {
                            if (maplibreMap != null) {
                                maplibreMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(marker.getLatitude(), marker.getLongitude()))
                                    .title(marker.getTitle()));
                            }
                        }
                    }
                } else if (error != null) {
                    Toast.makeText(requireContext(), "Error loading markers: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showMarkerOptionsDialog(Marker marker) {
        String[] options = {"Edit Name", "Remove Marker"};
        
        new AlertDialog.Builder(requireContext())
            .setTitle("Marker Options")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // Edit Name
                        showEditNameDialog(marker);
                        break;
                    case 1: // Remove Marker
                        showDeleteConfirmationDialog(marker);
                        break;
                }
            })
            .show();
    }

    private void showEditNameDialog(Marker marker) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_marker_name, null);
        EditText nameInput = dialogView.findViewById(R.id.markerNameInput);
        nameInput.setText(marker.getTitle());

        new AlertDialog.Builder(requireContext())
            .setTitle("Edit Shop Name")
            .setView(dialogView)
            .setPositiveButton("Save", (dialog, which) -> {
                String newName = nameInput.getText().toString().trim();
                if (!newName.isEmpty()) {
                    updateMarkerName(marker, newName);
                } else {
                    Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void updateMarkerName(Marker marker, String newName) {
        // Update marker title on map
        marker.setTitle(newName);
        // TODO: Implement updateMarker in SupabaseManager if needed
        Toast.makeText(requireContext(), "Update functionality coming soon to Supabase", Toast.LENGTH_SHORT).show();
    }

    private void showDeleteConfirmationDialog(Marker marker) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Remove Marker")
            .setMessage("Are you sure you want to remove this location?")
            .setPositiveButton("Yes", (dialog, which) -> deleteMarker(marker))
            .setNegativeButton("No", null)
            .show();
    }

    private void deleteMarker(Marker marker) {
        // TODO: Implement deleteMarker in SupabaseManager if needed
        marker.remove();
        Toast.makeText(requireContext(), "Location removed from map", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
