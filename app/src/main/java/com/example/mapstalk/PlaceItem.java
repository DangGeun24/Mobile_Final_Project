package com.example.mapstalk;

public class PlaceItem {
    private String placeId;
    private String name;
    private String address;
    private double latitude;
    private double longitude;

    public PlaceItem(String placeId, String name, String address,
                     double latitude, double longitude) {
        this.placeId = placeId;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getPlaceId() { return placeId; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}
