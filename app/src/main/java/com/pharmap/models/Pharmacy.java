package com.pharmap.models;

/**
 * Represents a pharmacy with location, distance, and TomTom-calculated travel time.
 * The core insight: travelTimeInSeconds (from TomTom) is the real routing metric,
 * not raw distance. A closer pharmacy with heavy traffic may be slower than a
 * farther one with clear roads.
 */
public class Pharmacy {

    private String id;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private double distanceKm;           // straight-line or road distance in km
    private int travelTimeInSeconds;     // TomTom Routing API result (the key metric)
    private String trafficStatus;        // "green", "yellow", "red"
    private boolean isOpen24Hours;
    private String phoneNumber;
    private boolean isSelected;          // true = fastest option recommended

    // Traffic status constants
    public static final String TRAFFIC_GREEN  = "green";   // Akıcı (Flowing)
    public static final String TRAFFIC_YELLOW = "yellow";  // Orta (Moderate)
    public static final String TRAFFIC_RED    = "red";     // Yoğun (Heavy)

    public Pharmacy() {}

    public Pharmacy(String id, String name, String address,
                    double latitude, double longitude) {
        this.id        = id;
        this.name      = name;
        this.address   = address;
        this.latitude  = latitude;
        this.longitude = longitude;
        this.travelTimeInSeconds = Integer.MAX_VALUE; // not yet fetched
    }

    // ─── Getters & Setters ────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public int getTravelTimeInSeconds() { return travelTimeInSeconds; }
    public void setTravelTimeInSeconds(int travelTimeInSeconds) {
        this.travelTimeInSeconds = travelTimeInSeconds;
    }

    public String getTrafficStatus() { return trafficStatus; }
    public void setTrafficStatus(String trafficStatus) {
        this.trafficStatus = trafficStatus;
    }

    public boolean isOpen24Hours() { return isOpen24Hours; }
    public void setOpen24Hours(boolean open24Hours) { isOpen24Hours = open24Hours; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }

    // ─── Helper: human-readable travel time ───────────────────────────────

    /**
     * Returns a display string e.g. "7 dk" or "1 sa 5 dk"
     */
    public String getFormattedTravelTime() {
        if (travelTimeInSeconds == Integer.MAX_VALUE) return "–";
        int minutes = travelTimeInSeconds / 60;
        if (minutes < 60) {
            return minutes + " dk";
        } else {
            int hours = minutes / 60;
            int mins  = minutes % 60;
            return hours + " sa " + (mins > 0 ? mins + " dk" : "");
        }
    }

    /**
     * Returns a display string e.g. "1.2 km"
     */
    public String getFormattedDistance() {
        return String.format("%.1f km", distanceKm);
    }
}
