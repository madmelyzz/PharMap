package com.pharmap.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * POJO classes for parsing TomTom Routing API JSON response.
 *
 * TomTom endpoint:
 * GET https://api.tomtom.com/routing/1/calculateRoute/{origin}:{destination}/json
 *     ?key={API_KEY}&traffic=true&travelMode=car
 *
 * The field we care most about is:
 *   routes[0].summary.travelTimeInSeconds
 */
public class TomTomRouteResponse {

    @SerializedName("routes")
    public List<Route> routes;

    public static class Route {
        @SerializedName("summary")
        public Summary summary;

        @SerializedName("legs")
        public List<Leg> legs;
    }

    public static class Summary {
        /** Total travel time including live traffic delays (seconds). */
        @SerializedName("travelTimeInSeconds")
        public int travelTimeInSeconds;

        /** Road distance in meters. */
        @SerializedName("lengthInMeters")
        public int lengthInMeters;

        /** Traffic delay compared to free-flow (seconds). */
        @SerializedName("trafficDelayInSeconds")
        public int trafficDelayInSeconds;

        /** Departure time (ISO 8601). */
        @SerializedName("departureTime")
        public String departureTime;

        /** Arrival time (ISO 8601). */
        @SerializedName("arrivalTime")
        public String arrivalTime;
    }

    public static class Leg {
        @SerializedName("summary")
        public Summary summary;

        @SerializedName("points")
        public List<Point> points;
    }

    public static class Point {
        @SerializedName("latitude")
        public double latitude;

        @SerializedName("longitude")
        public double longitude;
    }

    // ─── Helper ───────────────────────────────────────────────────────────

    /**
     * Returns the travelTimeInSeconds from the first route, or -1 if unavailable.
     */
    public int getTravelTimeInSeconds() {
        if (routes != null && !routes.isEmpty()
                && routes.get(0).summary != null) {
            return routes.get(0).summary.travelTimeInSeconds;
        }
        return -1;
    }

    /**
     * Derives a traffic status string based on the delay ratio.
     * Green  = delay < 20% of free-flow
     * Yellow = 20–50%
     * Red    = > 50%
     */
    public String getTrafficStatus() {
        if (routes == null || routes.isEmpty()) return Pharmacy.TRAFFIC_GREEN;
        Summary s = routes.get(0).summary;
        if (s.travelTimeInSeconds <= 0) return Pharmacy.TRAFFIC_GREEN;
        double delayRatio = (double) s.trafficDelayInSeconds / s.travelTimeInSeconds;
        if (delayRatio < 0.20) return Pharmacy.TRAFFIC_GREEN;
        if (delayRatio < 0.50) return Pharmacy.TRAFFIC_YELLOW;
        return Pharmacy.TRAFFIC_RED;
    }
}
