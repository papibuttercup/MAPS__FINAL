package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements ThriftShopAdapter.OnShopClickListener {
    private RecyclerView recyclerShops;
    private ThriftShopAdapter adapter;
    private List<ThriftShop> shopList;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize RecyclerView for thrift shops
        recyclerShops = view.findViewById(R.id.recyclerShops);
        recyclerShops.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Initialize shop list and adapter
        shopList = new ArrayList<>();
        adapter = new ThriftShopAdapter(getContext(), shopList, this);
        recyclerShops.setAdapter(adapter);
        
        // Load thrift shops
        loadThriftShops();
        
        return view;
    }

    private void loadThriftShops() {
        db.collection("sellers")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                shopList.clear();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    ThriftShop shop = new ThriftShop();
                    shop.setId(document.getId());
                    shop.setName(document.getString("shopName"));
                    shop.setType("Thrift Shop");
                    // Use shopLocation or locationName if available
                    String shopLocation = document.getString("shopLocation");
                    String locationName = document.getString("locationName");
                    if (shopLocation != null && !shopLocation.isEmpty()) {
                        shop.setLocation(shopLocation);
                    } else if (locationName != null && !locationName.isEmpty()) {
                        shop.setLocation(locationName);
                    } else {
                        shop.setLocation("");
                    }
                    shop.setCoverPhotoUri(document.getString("coverPhotoUri"));
                    // Handle potential null values for coordinates
                    Double latitude = document.getDouble("latitude");
                    Double longitude = document.getDouble("longitude");
                    if (latitude != null && longitude != null) {
                        shop.setLatitude(latitude);
                        shop.setLongitude(longitude);
                    }
                    shop.setDescription(document.getString("description"));
                    shopList.add(shop);
                }
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Loaded " + shopList.size() + " shops", 
                                 Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error loading shops: " + e.getMessage(), 
                                 Toast.LENGTH_SHORT).show();
                }
            });
    }

    @Override
    public void onVisitShop(ThriftShop shop) {
        // Open ShopProductsFragment and pass sellerId and shopName
        ShopProductsFragment fragment = ShopProductsFragment.newInstance(shop.getId(), shop.getName());
        requireActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment) // Make sure this is your main container ID
            .addToBackStack(null)
            .commit();
    }

    @Override
    public void onViewLocation(ThriftShop shop) {
        // TODO: Navigate to map with shop location
        Toast.makeText(getContext(), "Viewing location of: " + shop.getName(), 
                     Toast.LENGTH_SHORT).show();
    }
} 