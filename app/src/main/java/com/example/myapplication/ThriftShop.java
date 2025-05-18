package com.example.myapplication;

public class ThriftShop {
    private String id;
    private String name;
    private String type;
    private String location;
    private String coverPhotoUri;
    private double latitude;
    private double longitude;
    private String description;

    public ThriftShop() {
        // Required empty constructor for Firestore
    }

    public ThriftShop(String id, String name, String type, String location, String coverPhotoUri, 
                     double latitude, double longitude, String description) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.location = location;
        this.coverPhotoUri = coverPhotoUri;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getCoverPhotoUri() { return coverPhotoUri; }
    public void setCoverPhotoUri(String coverPhotoUri) { this.coverPhotoUri = coverPhotoUri; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
} 