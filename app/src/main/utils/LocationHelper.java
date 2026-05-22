package com.pharmap.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * Utility class for obtaining the user's current GPS location
 * using the Fused Location Provider.
 *
 * PHARMAP updates data every 30 seconds (as per the blueprint spec)
 * to reflect changing traffic conditions and the user moving.
 */
public class LocationHelper {

    private static final String TAG = "LocationHelper";

    // 30 seconds update interval to match blueprint's data refresh frequency
    private static final long UPDATE_INTERVAL_MS      = 30_000L;
    private static final long FASTEST_UPDATE_INTERVAL = 10_000L;

    public interface LocationCallback2 {
        void onLocationReceived(double latitude, double longitude);
        void onLocationError(String message);
    }

    private final Context context;
    private final FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;

    public LocationHelper(Context context) {
        this.context     = context;
        this.fusedClient = LocationServices.getFusedLocationProviderClient(context);
    }

    /**
     * Checks if location permissions have been granted.
     */
    public boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(context,
            Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Gets the last known location quickly (used for fast initial load).
     */
    public void getLastKnownLocation(LocationCallback2 callback) {
        if (!hasLocationPermission()) {
            callback.onLocationError("Konum izni gerekli");
            return;
        }

        fusedClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                Log.d(TAG, "Last location: " + location.getLatitude()
                    + ", " + location.getLongitude());
                callback.onLocationReceived(location.getLatitude(),
                    location.getLongitude());
            } else {
                // No cached location; start updates
                startLocationUpdates(callback);
            }
        }).addOnFailureListener(e -> {
            callback.onLocationError("Konum alınamadı: " + e.getMessage());
        });
    }

    /**
     * Starts continuous location updates (every 30s to match traffic refresh).
     */
    public void startLocationUpdates(LocationCallback2 callback) {
        if (!hasLocationPermission()) {
            callback.onLocationError("Konum izni gerekli");
            return;
        }

        LocationRequest request = new LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL)
            .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null) return;
                Location loc = result.getLastLocation();
                if (loc != null) {
                    callback.onLocationReceived(loc.getLatitude(), loc.getLongitude());
                }
            }
        };

        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    }

    /**
     * Stops location updates — call in onPause/onDestroy to save battery.
     */
    public void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedClient.removeLocationUpdates(locationCallback);
        }
    }

    /**
     * Calculates the straight-line (Haversine) distance between two coordinates.
     * Used as a fallback if TomTom routing is unavailable.
     *
     * @return distance in kilometers
     */
    public static double haversineDistanceKm(double lat1, double lon1,
                                             double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1))
            * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
