package com.pharmap.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * POJO for TomTom Fuzzy Search API response.
 *
 * TomTom endpoint:
 * GET https://api.tomtom.com/search/2/search/pharmacy.json
 *     ?key={API_KEY}&lat={lat}&lon={lon}&radius=5000&limit=10&categorySet=9361
 *
 * categorySet=9361 = Pharmacy / Chemist
 */
public class TomTomSearchResponse {

    @SerializedName("results")
    public List<SearchResult> results;

    public static class SearchResult {
        @SerializedName("id")
        public String id;

        @SerializedName("score")
        public double score;

        @SerializedName("poi")
        public Poi poi;

        @SerializedName("address")
        public Address address;

        @SerializedName("position")
        public Position position;

        @SerializedName("dist")
        public double distanceMeters; // straight-line distance in meters from query point
    }

    public static class Poi {
        @SerializedName("name")
        public String name;

        @SerializedName("phone")
        public String phone;

        @SerializedName("openingHours")
        public OpeningHours openingHours;
    }

    public static class OpeningHours {
        @SerializedName("mode")
        public String mode; // e.g. "nextSevenDays"
    }

    public static class Address {
        @SerializedName("freeformAddress")
        public String freeformAddress;

        @SerializedName("municipality")
        public String municipality;
    }

    public static class Position {
        @SerializedName("lat")
        public double lat;

        @SerializedName("lon")
        public double lon;
    }

    /**
     * Converts TomTom search results to our Pharmacy model list.
     */
    public List<Pharmacy> toPharmacyList() {
        List<Pharmacy> pharmacies = new java.util.ArrayList<>();
        if (results == null) return pharmacies;

        for (SearchResult result : results) {
            if (result.poi == null || result.position == null) continue;

            Pharmacy p = new Pharmacy(
                result.id,
                result.poi.name,
                result.address != null ? result.address.freeformAddress : "",
                result.position.lat,
                result.position.lon
            );
            p.setDistanceKm(result.distanceMeters / 1000.0);

            if (result.poi.phone != null) {
                p.setPhoneNumber(result.poi.phone);
            }

            pharmacies.add(p);
        }
        return pharmacies;
    }
}
