package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class MyAddressesActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private AddressAdapter adapter;
    private List<Address> addressList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_addresses);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        findViewById(R.id.btnAddAddress).setOnClickListener(v -> {
            startActivity(new Intent(this, AddAddressActivity.class));
        });

        recyclerView = findViewById(R.id.recyclerViewAddresses);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        addressList = new ArrayList<>();
        adapter = new AddressAdapter(addressList);
        recyclerView.setAdapter(adapter);

        loadAddresses();
    }

    private void loadAddresses() {
        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId).collection("addresses")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                addressList.clear();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Address address = doc.toObject(Address.class);
                    addressList.add(address);
                }
                adapter.notifyDataSetChanged();
            });
    }

    private static class Address {
        public String fullName;
        public String phoneNumber;
        public String street;
        public String region;
        public boolean isDefault;
    }

    private class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.ViewHolder> {
        private List<Address> list;

        public AddressAdapter(List<Address> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_address, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Address address = list.get(position);
            holder.txtName.setText(address.fullName);
            holder.txtPhone.setText(address.phoneNumber);
            holder.txtAddress1.setText(address.street);
            holder.txtAddress2.setText(address.region);
            holder.txtDefault.setVisibility(address.isDefault ? View.VISIBLE : View.GONE);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtName, txtPhone, txtAddress1, txtAddress2, txtDefault;

            public ViewHolder(@NonNull View view) {
                super(view);
                txtName = view.findViewById(R.id.txtName);
                txtPhone = view.findViewById(R.id.txtPhone);
                txtAddress1 = view.findViewById(R.id.txtAddressLine1);
                txtAddress2 = view.findViewById(R.id.txtAddressLine2);
                txtDefault = view.findViewById(R.id.txtDefault);
            }
        }
    }
}
