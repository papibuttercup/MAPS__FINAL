package com.example.myapplication;

import android.app.Application;
import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.database.FirebaseDatabase;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        
        // Initialize Firebase App Check
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        
        // Use debug provider for development
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Using debug App Check provider");
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            );
        } else {
            // Use Play Integrity for production
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            );
        }
        
        // Set up persistence for Firebase Database
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
} 