package com.pharmap.utils;

public class LocationHelper {
    public LocationHelper(Object context) {}
    public boolean hasLocationPermission() { return true; }
    public void stopLocationUpdates() {}
    public interface LocationCallback2 {
        void onLocationReceived(double latitude, double longitude);
        void onLocationError(String message);
    }
    public void getLastKnownLocation(LocationCallback2 callback) {}
}