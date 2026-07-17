package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.ImageButton;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import android.widget.AutoCompleteTextView;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import android.view.Gravity;
import android.os.Handler;
import android.os.Looper;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Button;
import android.widget.EditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.RadioButton;
import com.google.android.material.card.MaterialCardView;
import android.content.res.ColorStateList;

public class ListNewItemActivity extends AppCompatActivity {
    private static final int MAX_IMAGE_DIMENSION = 800;
    private static final int JPEG_QUALITY = 70;

    private TextInputEditText productNameInput, descriptionInput, priceInput;
    private EditText stockStepperInput, catSearch;
    private Button nextBtn, saveDraftBtn, btnStockMinus, btnStockPlus;
    private RecyclerView stockEntriesRecyclerView, catRecyclerView;
    private TextView totalStockText, stepLabel, categoryTriggerLabel, ringPercent, showMoreBtn, tvPhotoCount, videoBody;
    private ProgressBar progressFill;
    private ImageView imgCoverPreview, videoArrow;
    private androidx.gridlayout.widget.GridLayout gridAdditionalPhotos;
    private View[] photoSlots = new View[8];
    private LinearLayout openCategorySheet, crumbsLayout, variationPanel, priceTierPanel, moreAttrsLayout, videoSummary;
    private View coverPhotoPlaceholder, categoryOverlay, sizeChartTile;
    private MaterialCardView sizeChartCard;

    private int currentPickingVariationIndex = -1;
    private int currentPickingVariationOptionIndex = -1;
    private List<VariationTypeData> variationTypes = new ArrayList<>();

    private static class VariationTypeData {
        String name;
        List<VariationOptionAdapter.VariationOption> options = new ArrayList<>();
        VariationOptionAdapter adapter;
    }

    private ActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest> variationImageLauncher;
    private Switch switchGogo, switchJT;
    private ImageButton backBtn, closeBtn, closeCategorySheet;
    private LinearLayout[] stepContainers;
    private int currentStep = 1;
    private final int totalSteps = 5;

    private ArrayList<Uri> productImageUris = new ArrayList<>();
    private Uri coverPhotoUri;
    private List<CategoryNode> currentPath = new ArrayList<>();
    private CategoryNode selectedCategoryNode = null;
    private CategoryAdapter categoryAdapter;

    private ActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest> coverPhotoLauncher;
    private ActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest> productImagesLauncher;
    private AlertDialog progressDialog;

    private String productId = null;
    private SupabaseManager.ProductModel existingProduct = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_new_item);

        productId = getIntent().getStringExtra("productId");

        initializeViews();
        initializeData();
        setupWizard();
        setupStockStepper();
        setupImagePickers();
        setupCategoryDrillDown();
        setupWholesaleAndVariations();
        setupSpecificationStep();
        setupSizeChart();
        setupVariationImagePicker();

        if (productId != null) {
            loadExistingProductData();
        }
    }

    private void loadExistingProductData() {
        progressDialog.show();
        SupabaseManager.getProduct(productId, new SupabaseManager.SupabaseCallbackWithProduct() {
            @Override
            public void onResult(boolean success, SupabaseManager.ProductModel product, String error) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    if (success && product != null) {
                        existingProduct = product;
                        prefillFields();
                    } else {
                        Toast.makeText(ListNewItemActivity.this, "Error loading product: " + error, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }
        });
    }

    private void prefillFields() {
        productNameInput.setText(existingProduct.getName());
        descriptionInput.setText(existingProduct.getDescription());
        priceInput.setText(String.valueOf(existingProduct.getPrice()));
        stockStepperInput.setText(String.valueOf(existingProduct.getStock()));
        ((TextView)findViewById(R.id.appbarTitle)).setText("Edit Product");

        // Category selection
        // This logic depends on your category data structure
        // Attempting to find the category node
        List<CategoryNode> root = getCategoryData();
        for (CategoryNode node : root) {
            if (node.name.equals(existingProduct.getMain_category())) {
                currentPath.add(node);
                if (node.children != null) {
                    for (CategoryNode sub : node.children) {
                        if (sub.name.equals(existingProduct.getCategory())) {
                            selectCategory(sub);
                            break;
                        }
                    }
                }
                break;
            }
        }

        // Additional photos
        if (existingProduct.getProduct_images() != null) {
            for (String url : existingProduct.getProduct_images()) {
                productImageUris.add(Uri.parse(url));
            }
            updatePhotoGrid();
        }

        // Cover photo
        if (existingProduct.getCover_photo_url() != null) {
            coverPhotoUrl = existingProduct.getCover_photo_url();
            com.bumptech.glide.Glide.with(this).load(coverPhotoUrl).into(imgCoverPreview);
            imgCoverPreview.setVisibility(View.VISIBLE);
            coverPhotoPlaceholder.setVisibility(View.GONE);
            // We set a marker to know it's already there
            coverPhotoUri = Uri.parse(coverPhotoUrl); 
        }

        // Variations
        if (existingProduct.getColors() != null && !existingProduct.getColors().isEmpty()) {
            VariationTypeData colorType = new VariationTypeData();
            colorType.name = "Color";
            for (String color : existingProduct.getColors()) {
                VariationOptionAdapter.VariationOption opt = new VariationOptionAdapter.VariationOption(color);
                if (existingProduct.getVariation_images() != null && existingProduct.getVariation_images().containsKey(color)) {
                    opt.uploadedUrl = existingProduct.getVariation_images().get(color);
                    opt.imageUri = Uri.parse(opt.uploadedUrl);
                }
                colorType.options.add(opt);
            }
            addPrefilledVariationType(colorType);
        }

        if (existingProduct.getSizes() != null && !existingProduct.getSizes().isEmpty()) {
            VariationTypeData sizeType = new VariationTypeData();
            sizeType.name = "Size";
            for (String size : existingProduct.getSizes()) {
                sizeType.options.add(new VariationOptionAdapter.VariationOption(size));
            }
            addPrefilledVariationType(sizeType);
        }
    }

    private void addPrefilledVariationType(VariationTypeData typeData) {
        variationPanel.setVisibility(View.VISIBLE);
        
        variationTypes.add(typeData);

        final View v = getLayoutInflater().inflate(R.layout.panel_variation, variationPanel, false);
        variationPanel.addView(v);

        final TextInputEditText etVariationName = v.findViewById(R.id.etVariationName);
        etVariationName.setText(typeData.name);
        
        etVariationName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                typeData.name = s.toString();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        final EditText optionInput = v.findViewById(R.id.optionInput);
        final Button btnAddOption = v.findViewById(R.id.btnAddOption);
        final RecyclerView rvVariationOptions = v.findViewById(R.id.rvVariationOptions);

        typeData.adapter = new VariationOptionAdapter(typeData.options, new VariationOptionAdapter.OnOptionInteractionListener() {
            @Override
            public void onRemoveOption(int position) {
                typeData.options.remove(position);
                typeData.adapter.notifyItemRemoved(position);
            }

            @Override
            public void onPickImage(int position) {
                int actualVIndex = variationTypes.indexOf(typeData);
                currentPickingVariationIndex = actualVIndex;
                currentPickingVariationOptionIndex = position;
                variationImageLauncher.launch(new androidx.activity.result.PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
            }
        });

        if ("Size".equalsIgnoreCase(typeData.name)) {
            typeData.adapter.setShowImages(false);
        }

        rvVariationOptions.setLayoutManager(new LinearLayoutManager(this));
        rvVariationOptions.setAdapter(typeData.adapter);

        btnAddOption.setOnClickListener(view -> {
            String option = optionInput.getText().toString().trim();
            if (!option.isEmpty()) {
                typeData.options.add(new VariationOptionAdapter.VariationOption(option));
                typeData.adapter.notifyItemInserted(typeData.options.size() - 1);
                optionInput.setText("");
            }
        });

        v.findViewById(R.id.btnDeleteVariation).setOnClickListener(view -> {
            variationPanel.removeView(v);
            variationTypes.remove(typeData);
            updateVariationButtonsVisibility();
        });

        updateVariationButtonsVisibility();
    }

    private String coverPhotoUrl = null;

    private void setupVariationImagePicker() {
        variationImageLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null && currentPickingVariationIndex != -1 && currentPickingVariationOptionIndex != -1) {
                variationTypes.get(currentPickingVariationIndex).options.get(currentPickingVariationOptionIndex).imageUri = uri;
                variationTypes.get(currentPickingVariationIndex).adapter.notifyItemChanged(currentPickingVariationOptionIndex);
            }
        });
    }

    private void initializeViews() {
        productNameInput = findViewById(R.id.productNameInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        priceInput = findViewById(R.id.priceInput);
        stockStepperInput = findViewById(R.id.stockStepperInput);
        btnStockMinus = findViewById(R.id.btnStockMinus);
        btnStockPlus = findViewById(R.id.btnStockPlus);
        catSearch = findViewById(R.id.catSearch);
        imgCoverPreview = findViewById(R.id.imgCoverPreview);
        gridAdditionalPhotos = findViewById(R.id.gridAdditionalPhotos);
        tvPhotoCount = findViewById(R.id.tvPhotoCount);

        for (int i = 0; i < 8; i++) {
            photoSlots[i] = gridAdditionalPhotos.getChildAt(i);
            int index = i;
            photoSlots[i].setOnClickListener(v -> {
                if (index == productImageUris.size()) {
                    productImagesLauncher.launch(new androidx.activity.result.PickVisualMediaRequest.Builder()
                            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                            .build());
                }
            });
            ImageButton btnRemove = photoSlots[i].findViewById(R.id.btnRemovePhoto);
            btnRemove.setOnClickListener(v -> {
                productImageUris.remove(index);
                updatePhotoGrid();
            });
        }

        nextBtn = findViewById(R.id.nextBtn);
        saveDraftBtn = findViewById(R.id.saveDraftBtn);
        backBtn = findViewById(R.id.backBtn);
        closeBtn = findViewById(R.id.closeBtn);
        stepLabel = findViewById(R.id.stepLabel);
        progressFill = findViewById(R.id.progressFill);
        coverPhotoPlaceholder = findViewById(R.id.coverPhotoPlaceholder);

        openCategorySheet = findViewById(R.id.openCategorySheet);
        categoryTriggerLabel = findViewById(R.id.categoryTriggerLabel);
        categoryOverlay = findViewById(R.id.categoryOverlay);
        closeCategorySheet = findViewById(R.id.closeCategorySheet);
        catRecyclerView = findViewById(R.id.catRecyclerView);
        crumbsLayout = findViewById(R.id.crumbsLayout);

        variationPanel = findViewById(R.id.variationPanel);
        priceTierPanel = findViewById(R.id.priceTierPanel);

        showMoreBtn = findViewById(R.id.showMoreBtn);
        moreAttrsLayout = findViewById(R.id.moreAttrsLayout);
        ringPercent = findViewById(R.id.ringPercent);

        sizeChartTile = findViewById(R.id.sizeChartTile);
        sizeChartCard = (MaterialCardView) sizeChartTile;

        switchGogo = findViewById(R.id.switchGogo);
        switchJT = findViewById(R.id.switchJT);

        videoSummary = findViewById(R.id.videoSummary);
        videoBody = findViewById(R.id.videoBody);
        videoArrow = findViewById(R.id.videoArrow);
        setupVideoDropdown();

        stepContainers = new LinearLayout[]{
                findViewById(R.id.step1Container),
                findViewById(R.id.step2Container),
                findViewById(R.id.step3Container),
                findViewById(R.id.step4Container),
                findViewById(R.id.step5Container)
        };

        progressDialog = new AlertDialog.Builder(this)
                .setView(R.layout.progress_dialog)
                .setCancelable(false)
                .create();
    }

    private void setupVideoDropdown() {
        videoSummary.setOnClickListener(v -> {
            boolean isVisible = videoBody.getVisibility() == View.VISIBLE;
            videoBody.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            videoArrow.setRotation(isVisible ? 90 : 270);
        });
    }

    private void setupWizard() {
        updateWizardUI();
        nextBtn.setOnClickListener(v -> {
            if (currentStep < totalSteps) {
                if (validateStep(currentStep)) {
                    currentStep++;
                    updateWizardUI();
                }
            } else {
                if (validateInputs()) saveProduct();
                else Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            }
        });
        backBtn.setOnClickListener(v -> {
            if (currentStep > 1) {
                currentStep--;
                updateWizardUI();
            } else finish();
        });
        closeBtn.setOnClickListener(v -> finish());
        saveDraftBtn.setOnClickListener(v -> Toast.makeText(this, "Draft saved", Toast.LENGTH_SHORT).show());
    }

    private void updateWizardUI() {
        for (int i = 0; i < stepContainers.length; i++) {
            stepContainers[i].setVisibility(i == currentStep - 1 ? View.VISIBLE : View.GONE);
            if (i == currentStep - 1) {
                stepContainers[i].setAlpha(0f);
                stepContainers[i].animate().alpha(1f).setDuration(250).start();
            }
        }
        String[] stepNames = {"Category", "Basic information", "Specification", "Sales information", "Shipping"};
        stepLabel.setText("Step " + currentStep + " of " + totalSteps + " · " + stepNames[currentStep - 1]);
        progressFill.setProgress(currentStep * 100 / totalSteps);
        nextBtn.setText(currentStep == totalSteps ? "Save & publish" : "Next");
        backBtn.setVisibility(currentStep == 1 ? View.INVISIBLE : View.VISIBLE);
        findViewById(R.id.contentScrollView).scrollTo(0, 0);
    }

    private boolean validateStep(int step) {
        switch (step) {
            case 1:
                if (TextUtils.isEmpty(productNameInput.getText())) {
                    Toast.makeText(this, "Enter product name", Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (selectedCategoryNode == null) {
                    Toast.makeText(this, "Select a category", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            case 2:
                if (coverPhotoUri == null) {
                    Toast.makeText(this, "Add a cover photo", Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (TextUtils.isEmpty(descriptionInput.getText())) {
                    Toast.makeText(this, "Enter description", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            case 4:
                if (TextUtils.isEmpty(priceInput.getText())) {
                    Toast.makeText(this, "Enter price", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            default:
                return true;
        }
    }

    private void setupCategoryDrillDown() {
        final List<CategoryNode> rootCategories = getCategoryData();
        categoryAdapter = new CategoryAdapter(rootCategories, node -> {
            if (node.children != null && !node.children.isEmpty()) {
                currentPath.add(node);
                updateCategoryList(node.children);
                updateCrumbs();
            } else {
                selectCategory(node);
            }
        });
        catRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        catRecyclerView.setAdapter(categoryAdapter);

        openCategorySheet.setOnClickListener(v -> {
            categoryOverlay.setVisibility(View.VISIBLE);
            currentPath.clear();
            updateCategoryList(rootCategories);
            updateCrumbs();
        });

        closeCategorySheet.setOnClickListener(v -> categoryOverlay.setVisibility(View.GONE));

        catSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                categoryAdapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void updateCategoryList(List<CategoryNode> nodes) {
        categoryAdapter.updateNodes(nodes);
        catRecyclerView.scrollToPosition(0);
    }

    private void updateCrumbs() {
        crumbsLayout.removeAllViews();
        addCrumb("All categories", -1);
        for (int i = 0; i < currentPath.size(); i++) {
            addCrumb(currentPath.get(i).name, i);
        }
    }

    private void addCrumb(String name, final int index) {
        TextView tv = new TextView(this);
        tv.setText(name);
        tv.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
        tv.setBackgroundResource(R.drawable.rounded_button_outline);
        tv.setTextSize(12.5f);
        tv.setTextColor(getResources().getColor(R.color.wizard_slate));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, dpToPx(8), 0);
        tv.setLayoutParams(lp);

        if (index == currentPath.size() - 1 && index != -1) {
            tv.setTextColor(getResources().getColor(R.color.wizard_accent));
            tv.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.wizard_accent_tint)));
        }

        tv.setOnClickListener(v -> {
            if (index == -1) {
                currentPath.clear();
                updateCategoryList(getCategoryData());
            } else {
                currentPath = new ArrayList<>(currentPath.subList(0, index + 1));
                updateCategoryList(currentPath.get(index).children);
            }
            updateCrumbs();
        });
        crumbsLayout.addView(tv);
    }

    private void selectCategory(CategoryNode node) {
        selectedCategoryNode = node;
        StringBuilder sb = new StringBuilder();
        for (CategoryNode p : currentPath) sb.append(p.name).append(" > ");
        sb.append(node.name);
        categoryTriggerLabel.setText(sb.toString());
        categoryTriggerLabel.setTextColor(getResources().getColor(R.color.wizard_ink));
        categoryOverlay.setVisibility(View.GONE);
    }

    private void setupWholesaleAndVariations() {
        findViewById(R.id.btnAddColorVariation).setOnClickListener(v -> {
            variationPanel.setVisibility(View.VISIBLE);
            addVariationType("Color");
            updateVariationButtonsVisibility();
        });

        findViewById(R.id.btnAddSizeVariation).setOnClickListener(v -> {
            variationPanel.setVisibility(View.VISIBLE);
            addVariationType("Size");
            updateVariationButtonsVisibility();
        });

        findViewById(R.id.addPriceTierBtn).setOnClickListener(v -> {
            v.setVisibility(View.GONE);
            priceTierPanel.setVisibility(View.VISIBLE);
            addPriceTier();
        });
    }

    private void updateVariationButtonsVisibility() {
        boolean hasColor = false;
        boolean hasSize = false;
        for (VariationTypeData type : variationTypes) {
            if ("Color".equalsIgnoreCase(type.name)) hasColor = true;
            if ("Size".equalsIgnoreCase(type.name)) hasSize = true;
        }

        findViewById(R.id.btnAddColorVariation).setVisibility(hasColor ? View.GONE : View.VISIBLE);
        findViewById(R.id.btnAddSizeVariation).setVisibility(hasSize ? View.GONE : View.VISIBLE);
        
        if (variationTypes.size() >= 2) {
            findViewById(R.id.layoutVariationButtons).setVisibility(View.GONE);
        } else {
            findViewById(R.id.layoutVariationButtons).setVisibility(View.VISIBLE);
        }
    }

    private void addVariationType(String defaultName) {
        VariationTypeData typeData = new VariationTypeData();
        typeData.name = defaultName;
        
        if ("Size".equalsIgnoreCase(defaultName)) {
            String[] defaultSizes = {"M", "L", "XL", "2XL", "3XL"};
            for (String s : defaultSizes) {
                typeData.options.add(new VariationOptionAdapter.VariationOption(s));
            }
        }
        
        variationTypes.add(typeData);

        final View v = getLayoutInflater().inflate(R.layout.panel_variation, variationPanel, false);
        variationPanel.addView(v);

        final TextInputEditText etVariationName = v.findViewById(R.id.etVariationName);
        if (defaultName != null) {
            etVariationName.setText(defaultName);
        }

        etVariationName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                typeData.name = s.toString();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        
        final EditText optionInput = v.findViewById(R.id.optionInput);
        final Button btnAddOption = v.findViewById(R.id.btnAddOption);
        final RecyclerView rvVariationOptions = v.findViewById(R.id.rvVariationOptions);

        typeData.adapter = new VariationOptionAdapter(typeData.options, new VariationOptionAdapter.OnOptionInteractionListener() {
            @Override
            public void onRemoveOption(int position) {
                typeData.options.remove(position);
                typeData.adapter.notifyItemRemoved(position);
            }

            @Override
            public void onPickImage(int position) {
                // Find the correct index in variationTypes list since it might have shifted
                int actualVIndex = variationTypes.indexOf(typeData);
                currentPickingVariationIndex = actualVIndex;
                currentPickingVariationOptionIndex = position;
                variationImageLauncher.launch(new androidx.activity.result.PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
            }
        });

        if ("Size".equalsIgnoreCase(defaultName)) {
            typeData.adapter.setShowImages(false);
        }

        rvVariationOptions.setLayoutManager(new LinearLayoutManager(this));
        rvVariationOptions.setAdapter(typeData.adapter);

        etVariationName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                typeData.name = s.toString();
                if (typeData.adapter != null) {
                    typeData.adapter.setShowImages(!"Size".equalsIgnoreCase(typeData.name));
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnAddOption.setOnClickListener(view -> {
            String option = optionInput.getText().toString().trim();
            if (!option.isEmpty()) {
                typeData.options.add(new VariationOptionAdapter.VariationOption(option));
                typeData.adapter.notifyItemInserted(typeData.options.size() - 1);
                optionInput.setText("");
            }
        });

        v.findViewById(R.id.btnDeleteVariation).setOnClickListener(view -> {
            variationPanel.removeView(v);
            variationTypes.remove(typeData);
            updateVariationButtonsVisibility();
        });
    }

    private void addPriceTier() {
        View v = getLayoutInflater().inflate(R.layout.item_price_tier, priceTierPanel, false);
        priceTierPanel.addView(v);
        v.findViewById(R.id.btnDeleteTier).setOnClickListener(view -> {
            priceTierPanel.removeView(v);
            if (priceTierPanel.getChildCount() == 0) {
                priceTierPanel.setVisibility(View.GONE);
                findViewById(R.id.addPriceTierBtn).setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupStockStepper() {
        btnStockMinus.setOnClickListener(v -> {
            int val = Integer.parseInt(stockStepperInput.getText().toString());
            stockStepperInput.setText(String.valueOf(Math.max(0, val - 1)));
        });
        btnStockPlus.setOnClickListener(v -> {
            int val = Integer.parseInt(stockStepperInput.getText().toString());
            stockStepperInput.setText(String.valueOf(val + 1));
        });
    }

    private void setupSpecificationStep() {
        showMoreBtn.setOnClickListener(v -> {
            boolean isVisible = moreAttrsLayout.getVisibility() == View.VISIBLE;
            moreAttrsLayout.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            showMoreBtn.setText(isVisible ? "Show 5 more attributes" : "Show less");
        });
    }

    private void setupSizeChart() {
        sizeChartTile.setOnClickListener(v -> {
            boolean isFilled = sizeChartCard.getTag() != null && (boolean) sizeChartCard.getTag();
            if (!isFilled) {
                sizeChartCard.setTag(true);
                sizeChartCard.setStrokeColor(ColorStateList.valueOf(getResources().getColor(R.color.wizard_mint)));
                sizeChartCard.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.wizard_mint_tint)));
                findViewById(R.id.sizeChartEmpty).setVisibility(View.GONE);
                findViewById(R.id.sizeChartFilled).setVisibility(View.VISIBLE);
            } else {
                sizeChartCard.setTag(false);
                sizeChartCard.setStrokeColor(ColorStateList.valueOf(getResources().getColor(R.color.wizard_line)));
                sizeChartCard.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.wizard_paper)));
                findViewById(R.id.sizeChartEmpty).setVisibility(View.VISIBLE);
                findViewById(R.id.sizeChartFilled).setVisibility(View.GONE);
            }
        });

        findViewById(R.id.sizeChartRemove).setOnClickListener(v -> {
            sizeChartCard.setTag(false);
            sizeChartCard.setStrokeColor(ColorStateList.valueOf(getResources().getColor(R.color.wizard_line)));
            sizeChartCard.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.wizard_paper)));
            findViewById(R.id.sizeChartEmpty).setVisibility(View.VISIBLE);
            findViewById(R.id.sizeChartFilled).setVisibility(View.GONE);
        });
    }

    // Helper classes for Category drill-down
    private static class CategoryNode {
        String name; List<CategoryNode> children;
        CategoryNode(String n) { this.name = n; }
        CategoryNode(String n, List<CategoryNode> c) { this.name = n; this.children = c; }
    }

    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
        private List<CategoryNode> allNodes;
        private List<CategoryNode> filteredNodes;
        private OnCategoryClickListener listener;

        CategoryAdapter(List<CategoryNode> nodes, OnCategoryClickListener l) {
            this.allNodes = nodes;
            this.filteredNodes = new ArrayList<>(nodes);
            this.listener = l;
        }

        void updateNodes(List<CategoryNode> nodes) {
            this.allNodes = nodes;
            this.filteredNodes = new ArrayList<>(nodes);
            notifyDataSetChanged();
        }

        void filter(String query) {
            filteredNodes.clear();
            if (query.isEmpty()) {
                filteredNodes.addAll(allNodes);
            } else {
                for (CategoryNode node : allNodes) {
                    if (node.name.toLowerCase().contains(query.toLowerCase())) {
                        filteredNodes.add(node);
                    }
                }
            }
            notifyDataSetChanged();
        }

        @Override public ViewHolder onCreateViewHolder(ViewGroup p, int t) {
            return new ViewHolder(getLayoutInflater().inflate(R.layout.item_category_row, p, false));
        }

        @Override public void onBindViewHolder(ViewHolder h, int p) {
            final CategoryNode node = filteredNodes.get(p);
            h.name.setText(node.name);
            boolean hasChildren = node.children != null && !node.children.isEmpty();
            h.arrow.setVisibility(hasChildren ? View.VISIBLE : View.GONE);
            h.radio.setVisibility(hasChildren ? View.GONE : View.VISIBLE);
            h.radio.setChecked(selectedCategoryNode == node);
            h.itemView.setOnClickListener(v -> listener.onClick(node));
        }

        @Override public int getItemCount() { return filteredNodes.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name; ImageView arrow; RadioButton radio;
            ViewHolder(View v) { super(v); name = v.findViewById(R.id.categoryName); arrow = v.findViewById(R.id.categoryArrow); radio = v.findViewById(R.id.categoryRadio); }
        }
    }

    interface OnCategoryClickListener { void onClick(CategoryNode node); }

    private List<CategoryNode> getCategoryData() {
        List<CategoryNode> data = new ArrayList<>();
        Map<String, List<CategoryConstants.Group>> hierarchy = CategoryConstants.getCategoryHierarchy();

        for (Map.Entry<String, List<CategoryConstants.Group>> entry : hierarchy.entrySet()) {
            String mainCat = entry.getKey();
            List<CategoryConstants.Group> groups = entry.getValue();
            
            List<CategoryNode> groupNodes = new ArrayList<>();
            for (CategoryConstants.Group g : groups) {
                List<CategoryNode> subNodes = new ArrayList<>();
                for (String s : g.subs) {
                    subNodes.add(new CategoryNode(s));
                }
                groupNodes.add(new CategoryNode(g.name, subNodes));
            }
            data.add(new CategoryNode(mainCat, groupNodes));
        }

        return data;
    }

    private void initializeData() {
    }

    private void setupImagePickers() {
        coverPhotoLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                coverPhotoUri = uri;
                com.bumptech.glide.Glide.with(this).load(uri).into(imgCoverPreview);
                imgCoverPreview.setVisibility(View.VISIBLE);
                coverPhotoPlaceholder.setVisibility(View.GONE);
            }
        });

        productImagesLauncher = registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(8), uris -> {
            if (uris != null && !uris.isEmpty()) {
                for (Uri uri : uris) {
                    if (productImageUris.size() < 8) {
                        productImageUris.add(uri);
                    }
                }
                updatePhotoGrid();
            }
        });

        findViewById(R.id.addCoverPhotoButton).setOnClickListener(v -> {
            coverPhotoLauncher.launch(new androidx.activity.result.PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });
    }

    private void updatePhotoGrid() {
        for (int i = 0; i < 8; i++) {
            View slot = photoSlots[i];
            TextView placeholder = slot.findViewById(R.id.photoPlaceholder);
            ImageView preview = slot.findViewById(R.id.photoPreview);
            ImageButton btnRemove = slot.findViewById(R.id.btnRemovePhoto);

            if (i < productImageUris.size()) {
                com.bumptech.glide.Glide.with(this).load(productImageUris.get(i)).into(preview);
                preview.setVisibility(View.VISIBLE);
                placeholder.setVisibility(View.GONE);
                btnRemove.setVisibility(View.VISIBLE);
            } else if (i == productImageUris.size()) {
                preview.setVisibility(View.GONE);
                placeholder.setVisibility(View.VISIBLE);
                btnRemove.setVisibility(View.GONE);
                slot.setEnabled(true);
            } else {
                preview.setVisibility(View.GONE);
                placeholder.setVisibility(View.GONE);
                btnRemove.setVisibility(View.GONE);
                slot.setEnabled(false);
            }
        }
        tvPhotoCount.setText(productImageUris.size() + "/8");
    }

    private Bitmap compressImage(Uri imageUri) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri), null, options);
        int sampleSize = 1;
        if (options.outHeight > MAX_IMAGE_DIMENSION || options.outWidth > MAX_IMAGE_DIMENSION) {
            sampleSize = Math.max(Math.round((float) options.outHeight / MAX_IMAGE_DIMENSION), Math.round((float) options.outWidth / MAX_IMAGE_DIMENSION));
        }
        options.inJustDecodeBounds = false;
        options.inSampleSize = sampleSize;
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri), null, options);
    }

    private Uri getCompressedImageUri(Uri originalUri) throws IOException {
        Bitmap compressedBitmap = compressImage(originalUri);
        File tempFile = File.createTempFile("compressed_", ".jpg", getCacheDir());
        FileOutputStream out = new FileOutputStream(tempFile);
        compressedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
        out.close();
        compressedBitmap.recycle();
        return Uri.fromFile(tempFile.getAbsoluteFile());
    }

    private int dpToPx(int dp) { return Math.round(dp * getResources().getDisplayMetrics().density); }

    private boolean validateInputs() {
        if (TextUtils.isEmpty(productNameInput.getText())) return false;
        if (TextUtils.isEmpty(priceInput.getText())) return false;
        if (selectedCategoryNode == null) return false;
        if (coverPhotoUri == null) return false;
        return true;
    }

    private void saveProduct() {
        progressDialog.show();
        List<Uri> images = new ArrayList<>();
        
        // Logic for cover photo
        if (coverPhotoUri != null && !coverPhotoUri.toString().startsWith("http")) {
            images.add(coverPhotoUri);
        }
        
        // Product images (if added later)
        for (Uri uri : productImageUris) {
            if (!uri.toString().startsWith("http")) {
                images.add(uri);
            }
        }

        // Variation images
        for (VariationTypeData type : variationTypes) {
            for (VariationOptionAdapter.VariationOption opt : type.options) {
                if (opt.imageUri != null && !opt.imageUri.toString().startsWith("http")) {
                    images.add(opt.imageUri);
                }
            }
        }
        
        if (images.isEmpty()) {
            // If no NEW images to upload, just save data
            saveProductData(new ArrayList<>());
            return;
        }

        uploadImages(images, (imageUrls, error) -> {
            if (imageUrls != null) {
                saveProductData(imageUrls);
            } else {
                progressDialog.dismiss();
                String errorMsg = error != null ? error : "Upload failed";
                showErrorDialog("Upload Error", errorMsg);
            }
        });
    }

    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void uploadImages(List<Uri> images, OnImagesUploadedListener listener) {
        String userId = SupabaseManager.getCurrentUserId();
        if (userId == null) {
            listener.onImagesUploaded(null, "User not logged in");
            return;
        }
        
        List<String> uploadedPaths = Collections.synchronizedList(new ArrayList<>(Collections.nCopies(images.size(), (String)null)));
        AtomicInteger processedCount = new AtomicInteger(0);
        java.util.concurrent.atomic.AtomicReference<String> lastError = new java.util.concurrent.atomic.AtomicReference<>(null);

        for (int i = 0; i < images.size(); i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    Bitmap bitmap = compressImage(images.get(index));
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);
                    SupabaseManager.uploadImage("thriftshop_db", userId + "/" + UUID.randomUUID() + ".jpg", baos.toByteArray(), new SupabaseManager.SupabaseCallbackWithUrl() {
                        @Override public void onResult(boolean success, String url, String error) {
                            if (success) {
                                uploadedPaths.set(index, url);
                            } else {
                                lastError.set(error);
                                Log.e("ListNewItem", "Upload error at index " + index + ": " + error);
                            }

                            if (processedCount.incrementAndGet() == images.size()) {
                                runOnUiThread(() -> {
                                    List<String> resultPaths = new ArrayList<>();
                                    for (String path : uploadedPaths) if (path != null) resultPaths.add(path);
                                    
                                    if (resultPaths.isEmpty() && !images.isEmpty()) {
                                        listener.onImagesUploaded(null, lastError.get());
                                    } else {
                                        listener.onImagesUploaded(resultPaths, null);
                                    }
                                });
                            }
                        }
                    });
                } catch (Exception e) {
                    lastError.set(e.getMessage());
                    if (processedCount.incrementAndGet() == images.size()) {
                        runOnUiThread(() -> listener.onImagesUploaded(null, lastError.get()));
                    }
                }
            }).start();
        }
    }

    private void saveProductData(List<String> uploadedUrls) {
        String userId = SupabaseManager.getCurrentUserId();
        if (userId == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String category = "Unknown";
        String subCategory = "Unknown";
        if (selectedCategoryNode != null) {
            subCategory = selectedCategoryNode.name;
            if (!currentPath.isEmpty()) {
                category = currentPath.get(0).name;
            } else {
                category = subCategory;
            }
        }

        // Cover URL: Use new one if uploaded, else existing
        String finalCoverUrl = coverPhotoUrl;
        int urlPtr = 0;
        if (coverPhotoUri != null && !coverPhotoUri.toString().startsWith("http")) {
            finalCoverUrl = uploadedUrls.get(urlPtr++);
        }
        
        int stock = 0;
        try {
            stock = Integer.parseInt(stockStepperInput.getText().toString().trim());
        } catch (Exception ignored) {}

        List<String> colors = new ArrayList<>();
        List<String> sizes = new ArrayList<>();
        // Product images
        List<String> finalProductImages = new ArrayList<>();
        if (existingProduct != null && existingProduct.getProduct_images() != null) {
            for (String url : existingProduct.getProduct_images()) {
                if (productImageUris.contains(Uri.parse(url))) {
                    finalProductImages.add(url);
                }
            }
        }
        
        // Variation images
        Map<String, String> finalVariationImages = new HashMap<>();

        for (VariationTypeData type : variationTypes) {
            List<String> options = new ArrayList<>();
            for (VariationOptionAdapter.VariationOption opt : type.options) {
                options.add(opt.name);
                String finalOptUrl = opt.uploadedUrl;
                if (opt.imageUri != null && !opt.imageUri.toString().startsWith("http")) {
                    finalOptUrl = uploadedUrls.get(urlPtr++);
                }
                if (finalOptUrl != null) {
                    finalVariationImages.put(opt.name, finalOptUrl);
                }
            }
            if (type.name != null && type.name.equalsIgnoreCase("Color")) {
                colors.addAll(options);
            } else if (type.name != null && type.name.equalsIgnoreCase("Size")) {
                sizes.addAll(options);
            } else if (colors.isEmpty()) {
                colors.addAll(options);
            } else if (sizes.isEmpty()) {
                sizes.addAll(options);
            }
        }

        // Additional product images (newly uploaded)
        for (Uri uri : productImageUris) {
            if (!uri.toString().startsWith("http")) {
                finalProductImages.add(uploadedUrls.get(urlPtr++));
            }
        }

        SupabaseManager.ProductModel product = new SupabaseManager.ProductModel(
                productId, userId, productNameInput.getText().toString().trim(), descriptionInput.getText().toString().trim(),
                Double.parseDouble(priceInput.getText().toString().trim()), category, subCategory,
                finalCoverUrl, 1.0, "1", stock, colors, sizes, finalProductImages, finalVariationImages, true, null
        );

        if (productId == null) {
            SupabaseManager.saveProduct(product, new SupabaseManager.SupabaseCallback() {
                @Override public void onResult(boolean success, String error) {
                    handleSaveResult(success, error);
                }
            });
        } else {
            // Build update map
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", product.getName());
            updates.put("description", product.getDescription());
            updates.put("price", product.getPrice());
            updates.put("stock", product.getStock());
            updates.put("main_category", product.getMain_category());
            updates.put("category", product.getCategory());
            updates.put("cover_photo_url", product.getCover_photo_url());
            // updates.put("product_images", product.getProduct_images());
            updates.put("colors", product.getColors());
            updates.put("sizes", product.getSizes());
            // updates.put("variation_images", product.getVariation_images());

            SupabaseManager.updateProduct(productId, updates, new SupabaseManager.SupabaseCallback() {
                @Override public void onResult(boolean success, String error) {
                    handleSaveResult(success, error);
                }
            });
        }
    }

    private void handleSaveResult(boolean success, String error) {
        runOnUiThread(() -> {
            progressDialog.dismiss();
            if (success) {
                Toast.makeText(this, productId == null ? "Product published!" : "Product updated!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                showErrorDialog("Save Error", error);
            }
        });
    }

    private interface OnImagesUploadedListener { void onImagesUploaded(List<String> urls, String error); }
}