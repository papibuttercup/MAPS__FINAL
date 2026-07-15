package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SelectLanguageActivity extends AppCompatActivity {
    private String selectedLanguage = "English";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_language);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        ImageButton btnDone = findViewById(R.id.btnDone);
        btnDone.setOnClickListener(v -> finish());

        setupLanguages();
    }

    private void setupLanguages() {
        setupLanguageItem(R.id.itemEnglish, "English");
        setupLanguageItem(R.id.itemChinese, "简体中文");
        setupLanguageItem(R.id.itemFilipino, "Filipino");

        updateCheckmarks();
    }

    private void setupLanguageItem(int viewId, String name) {
        View view = findViewById(viewId);
        if (view != null) {
            TextView txtName = view.findViewById(R.id.txtLanguageName);
            if (txtName != null) txtName.setText(name);
            view.setOnClickListener(v -> {
                selectedLanguage = name;
                updateCheckmarks();
            });
        }
    }

    private void updateCheckmarks() {
        updateItemCheckmark(R.id.itemEnglish, "English");
        updateItemCheckmark(R.id.itemChinese, "简体中文");
        updateItemCheckmark(R.id.itemFilipino, "Filipino");
    }

    private void updateItemCheckmark(int viewId, String name) {
        View view = findViewById(viewId);
        if (view != null) {
            View check = view.findViewById(R.id.imgCheck);
            if (check != null) {
                check.setVisibility(selectedLanguage.equals(name) ? View.VISIBLE : View.INVISIBLE);
            }
        }
    }
}
