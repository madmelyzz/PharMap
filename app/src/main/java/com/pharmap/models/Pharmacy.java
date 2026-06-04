package com.pharmap.models;

public class Pharmacy {

    private String id;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private double distanceKm;
    private int travelTimeInSeconds;
    private String trafficStatus;
    private boolean isOpen24Hours;
    private String phoneNumber;
    private boolean isSelected;

    // 🌟 YENİ: Saat ve Nöbetçi Alanları
    private String openingTime = "09:00";
    private String closingTime = "19:00";
    private boolean isDuty = false;

    public static final String TRAFFIC_GREEN  = "green";
    public static final String TRAFFIC_YELLOW = "yellow";
    public static final String TRAFFIC_RED    = "red";

    public Pharmacy() {}

    public Pharmacy(String id, String name, String address,
                    double latitude, double longitude) {
        this.id        = id;
        this.name      = name;
        this.address   = address;
        this.latitude  = latitude;
        this.longitude = longitude;
        this.travelTimeInSeconds = Integer.MAX_VALUE;
    }

    // 🌟 Dinamik Saat Kontrol Algoritması
    public boolean isCurrentlyOpen() {
        if (isOpen24Hours || isDuty) {
            return true; // 24 saat açıksa veya nöbetçiyse daima açık kabul et
        }
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            String currentTimeStr = sdf.format(new java.util.Date());

            java.util.Date now = sdf.parse(currentTimeStr);
            java.util.Date open = sdf.parse(openingTime);
            java.util.Date close = sdf.parse(closingTime);

            if (now.after(close) || now.before(open)) {
                return false; // Saat 19:00 - 09:00 arasındaysa ve nöbetçi değilse kapalıdır
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    // Getters & Setters
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
    public void setTravelTimeInSeconds(int travelTimeInSeconds) { this.travelTimeInSeconds = travelTimeInSeconds; }
    public String getTrafficStatus() { return trafficStatus; }
    public void setTrafficStatus(String trafficStatus) { this.trafficStatus = trafficStatus; }
    public boolean isOpen24Hours() { return isOpen24Hours; }
    public void setOpen24Hours(boolean open24Hours) { this.isOpen24Hours = open24Hours; this.isDuty = open24Hours; }
    public String getPhoneNumber() { return phoneNumber != null ? phoneNumber : "İletişim numarası yok"; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
    public String getOpeningTime() { return openingTime; }
    public void setOpeningTime(String openingTime) { this.openingTime = openingTime; }
    public String getClosingTime() { return closingTime; }
    public void setClosingTime(String closingTime) { this.closingTime = closingTime; }
    public boolean isDuty() { return isDuty; }
    public void setDuty(boolean duty) { isDuty = duty; }

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

    public String getFormattedDistance() {
        return String.format("%.1f km", distanceKm);
    }
}