package com.example.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class UsersFragment extends Fragment {
    private LinearLayout container;
    private LinearLayout cardsContainer;
    private FirebaseFirestore db;
    private int customerCount = 0;
    private int sellerCount = 0;
    private TextView countView;
    private Spinner filterSpinner;
    private String currentFilter = "all"; // "all", "customers", or "sellers"
    private List<AccountData> customerAccounts = new ArrayList<>();
    private List<AccountData> sellerAccounts = new ArrayList<>();
    private static final int COLOR_ACTIVE = Color.parseColor("#2196F3"); // Material Blue
    private static final int COLOR_INACTIVE = Color.parseColor("#757575"); // Material Gray

    // Helper class to hold account data
    private static class AccountData {
        String userId, name, email, type;
        boolean isDisabled, isSeller;
        AccountData(String userId, String name, String email, String type, boolean isDisabled, boolean isSeller) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.type = type;
            this.isDisabled = isDisabled;
            this.isSeller = isSeller;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            ScrollView scrollView = new ScrollView(requireContext());
            this.container = new LinearLayout(requireContext());
            this.container.setOrientation(LinearLayout.VERTICAL);
            scrollView.addView(this.container);
            db = FirebaseFirestore.getInstance();

            // Create filter spinner
            filterSpinner = new Spinner(requireContext());
            String[] filterOptions = {"All Accounts", "Customer Accounts", "Seller Accounts"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), 
                android.R.layout.simple_spinner_item, filterOptions);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            filterSpinner.setAdapter(adapter);

            // Add views to container
            LinearLayout topContainer = new LinearLayout(requireContext());
            topContainer.setOrientation(LinearLayout.VERTICAL);
            topContainer.setPadding(32, 32, 32, 16);
            topContainer.addView(filterSpinner);
            
            countView = new TextView(requireContext());
            countView.setPadding(0, 16, 0, 0);
            topContainer.addView(countView);
            
            this.container.addView(topContainer);

            // NEW: cards container
            cardsContainer = new LinearLayout(requireContext());
            cardsContainer.setOrientation(LinearLayout.VERTICAL);
            this.container.addView(cardsContainer);

            // Set spinner listener
            filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        switch (position) {
                            case 0:
                                currentFilter = "all";
                                break;
                            case 1:
                                currentFilter = "customers";
                                break;
                            case 2:
                                currentFilter = "sellers";
                                break;
                        }
                        filterAccounts();
                    } catch (Exception e) {
                        showError(e);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    currentFilter = "all";
                    filterAccounts();
                }
            });

            loadAccounts();
            return scrollView;
        } catch (Exception e) {
            showError(e);
            return new TextView(requireContext());
        }
    }

    private void loadAccounts() {
        try {
            customerCount = 0;
            sellerCount = 0;
            customerAccounts.clear();
            sellerAccounts.clear();

            db.collection("users").get().addOnSuccessListener(usersSnap -> {
                try {
                    customerCount = usersSnap.size();
                    if (customerCount == 0) {
                        Toast.makeText(requireContext(), "No users found in Firestore.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(requireContext(), "Loaded " + customerCount + " users.", Toast.LENGTH_SHORT).show();
                    }
                    for (QueryDocumentSnapshot doc : usersSnap) {
                        String userId = doc.getId();
                        String name = (doc.getString("firstName") + " " + doc.getString("lastName")).trim();
                        String email = doc.getString("email");
                        String type = doc.getString("accountType");
                        boolean isDisabled = doc.getBoolean("isDisabled") != null && doc.getBoolean("isDisabled");
                        customerAccounts.add(new AccountData(userId, name, email, type, isDisabled, false));
                    }
                    db.collection("sellers").get().addOnSuccessListener(sellersSnap -> {
                        try {
                            sellerCount = sellersSnap.size();
                            if (sellerCount == 0) {
                                Toast.makeText(requireContext(), "No sellers found in Firestore.", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(requireContext(), "Loaded " + sellerCount + " sellers.", Toast.LENGTH_SHORT).show();
                            }
                            for (QueryDocumentSnapshot doc : sellersSnap) {
                                String userId = doc.getId();
                                String name = (doc.getString("firstName") + " " + doc.getString("lastName")).trim();
                                String email = doc.getString("email");
                                String type = "seller";
                                boolean isDisabled = doc.getBoolean("isDisabled") != null && doc.getBoolean("isDisabled");
                                sellerAccounts.add(new AccountData(userId, name, email, type, isDisabled, true));
                            }
                            filterAccounts();
                        } catch (Exception e) {
                            showError(e);
                        }
                    }).addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Failed to load sellers: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        showError(e);
                    });
                } catch (Exception e) {
                    showError(e);
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(requireContext(), "Failed to load users: " + e.getMessage(), Toast.LENGTH_LONG).show();
                showError(e);
            });
        } catch (Exception e) {
            showError(e);
        }
    }

    private void filterAccounts() {
        try {
            cardsContainer.removeAllViews();
            int added = 0;
            if (currentFilter.equals("all") || currentFilter.equals("customers")) {
                for (AccountData acc : customerAccounts) {
                    cardsContainer.addView(createAccountCard(acc.userId, acc.name, acc.email, acc.type, acc.isDisabled, acc.isSeller));
                    added++;
                }
            }
            if (currentFilter.equals("all") || currentFilter.equals("sellers")) {
                for (AccountData acc : sellerAccounts) {
                    cardsContainer.addView(createAccountCard(acc.userId, acc.name, acc.email, acc.type, acc.isDisabled, acc.isSeller));
                    added++;
                }
            }
            if (added == 0) {
                TextView empty = new TextView(requireContext());
                empty.setText("No accounts found.");
                empty.setPadding(32, 64, 32, 32);
                cardsContainer.addView(empty);
            }
            updateCounts();
        } catch (Exception e) {
            showError(e);
        }
    }

    private void updateCounts() {
        try {
            if (countView != null) {
                countView.setText("Customers: " + customerCount + "   Sellers: " + sellerCount);
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    private View createAccountCard(String userId, String name, String email, String type, boolean isDisabled, boolean isSeller) {
        try {
            LinearLayout card = new LinearLayout(requireContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(32, 32, 32, 32);
            card.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
            
            TextView info = new TextView(requireContext());
            info.setText("Name: " + name + "\nEmail: " + email + "\nType: " + type + (isDisabled ? " (On Hold)" : ""));
            
            Button hold = new Button(requireContext());
            hold.setText(isDisabled ? "Unhold" : "Hold");
            hold.setOnClickListener(v -> setAccountHold(userId, isSeller, !isDisabled));
            
            Button delete = new Button(requireContext());
            delete.setText("Delete");
            delete.setOnClickListener(v -> deleteAccount(userId, isSeller));
            
            card.addView(info);
            card.addView(hold);
            card.addView(delete);
            return card;
        } catch (Exception e) {
            showError(e);
            return new TextView(requireContext());
        }
    }

    private void setAccountHold(String userId, boolean isSeller, boolean hold) {
        try {
            String collection = isSeller ? "sellers" : "users";
            db.collection(collection).document(userId)
                .update("isDisabled", hold)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), hold ? "Account put on hold" : "Account unheld", Toast.LENGTH_SHORT).show();
                        loadAccounts();
                    }
                })
                .addOnFailureListener(this::showError);
        } catch (Exception e) {
            showError(e);
        }
    }

    private void deleteAccount(String userId, boolean isSeller) {
        try {
            // First check if current user is a moderator
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                showError(new Exception("You must be logged in to perform this action"));
                return;
            }

            // Check moderator status using accountType
            db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && "moderator".equals(documentSnapshot.getString("accountType"))) {
                        // User is a moderator, proceed with deletion
                        String collection = isSeller ? "sellers" : "users";
                        db.collection(collection).document(userId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show();
                                    loadAccounts();
                                }
                            })
                            .addOnFailureListener(e -> {
                                showError(new Exception("Failed to delete account: " + e.getMessage()));
                            });
                    } else {
                        showError(new Exception("You don't have permission to delete accounts"));
                    }
                })
                .addOnFailureListener(e -> {
                    showError(new Exception("Failed to verify moderator status: " + e.getMessage()));
                });
        } catch (Exception e) {
            showError(e);
        }
    }

    private void showError(Exception e) {
        if (getContext() != null) {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        e.printStackTrace();
    }
} 