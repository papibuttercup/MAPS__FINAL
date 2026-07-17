package com.example.myapplication;

import android.content.Intent;
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
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements ThriftShopAdapter.OnShopClickListener {
    private RecyclerView recyclerShops;
    private ThriftShopAdapter adapter;
    private List<ThriftShop> shopList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        // Chat icon click listener
        View chatIcon = view.findViewById(R.id.message_icon);
        if (chatIcon != null) {
            chatIcon.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), CustomerChatListActivity.class);
                startActivity(intent);
            });
        }

        // Cart icon click listener
        View cartIcon = view.findViewById(R.id.cart_icon);
        if (cartIcon != null) {
            cartIcon.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), CartActivity.class);
                startActivity(intent);
            });
        }

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
        SupabaseManager.getAllProfiles(new SupabaseManager.SupabaseCallbackWithProfiles() {
            @Override
            public void onResult(boolean success, List<SupabaseManager.Profile> profiles, String error) {
                if (!isAdded()) return;

                if (success && profiles != null) {
                    shopList.clear();
                    for (SupabaseManager.Profile profile : profiles) {
                        if ("seller".equals(profile.getAccount_type())) {
                            // Filter out "empty" shops (unnamed and no description/location)
                            String shopName = profile.getShop_name();
                            String shopDesc = profile.getShop_description();
                            String shopLoc = profile.getShop_location();
                            
                            if ((shopName == null || shopName.isEmpty()) && 
                                (shopDesc == null || shopDesc.isEmpty()) &&
                                (shopLoc == null || shopLoc.isEmpty())) {
                                continue; // Skip incomplete/stray profiles
                            }

                            ThriftShop shop = new ThriftShop();
                            shop.setId(profile.getId());
                            shop.setName(profile.getShop_name() != null && !profile.getShop_name().isEmpty() ? profile.getShop_name() : "Unnamed Shop");
                            shop.setType("Thrift Shop");
                            
                            // Use description for subtitle if available
                            String desc = profile.getShop_description();
                            if (desc == null || desc.isEmpty()) {
                                desc = "No description";
                            }
                            shop.setLocation(desc);

                            // Set cover photo from shop_location (which stores the URL)
                            String coverUrl = profile.getShop_location();
                            if (coverUrl != null && (coverUrl.startsWith("http") || coverUrl.contains("storage"))) {
                                shop.setCoverPhotoUri(coverUrl);
                            } else {
                                shop.setCoverPhotoUri(null);
                            }
                            
                            shop.setLatitude(0.0);
                            shop.setLongitude(0.0);
                            shop.setDescription(desc);
                            shopList.add(shop);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    
                    if (shopList.isEmpty()) {
                        Toast.makeText(getContext(), "No thrift shops found", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Found " + shopList.size() + " thrift shops", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Error loading shops: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onVisitShop(ThriftShop shop) {
        // Open ShopProductsFragment and pass sellerId and shopName
        ShopProductsFragment fragment = ShopProductsFragment.newInstance(shop.getId(), shop.getName());
        requireActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit();
    }

    @Override
    public void onViewLocation(ThriftShop shop) {
        // Check if shop has location set
        if (shop.getLatitude() == 0.0 && shop.getLongitude() == 0.0) {
            Toast.makeText(getContext(), "This shop has not set their location yet", Toast.LENGTH_LONG).show();
            return;
        }

        // Launch ShopLocationActivity
        Intent intent = new Intent(getActivity(), ShopLocationActivity.class);
        intent.putExtra("sellerId", shop.getId());
        intent.putExtra("shopName", shop.getName());
        startActivity(intent);
    }
}
