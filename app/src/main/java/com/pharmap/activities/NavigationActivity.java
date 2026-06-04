package com.pharmap.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.pharmap.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NavigationActivity extends AppCompatActivity {

    private static final String API_KEY = "a7EqPOlXZO8JGOmbCfP4Qv9VuMwFfc42";

    private WebView webMapView;
    private TextView tvNavigationStatus;
    private LinearLayout llNavigationContainer;

    private OkHttpClient client;
    private FusedLocationProviderClient fusedLocationClient;

    private double userLat = 41.0122;
    private double userLon = 28.9760;
    private double destLat = 0;
    private double destLon = 0;

    private Context langContext;
    private List<String> routeInfo = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Dil ayarı
        String savedLang = getSharedPreferences("pharmap_prefs", MODE_PRIVATE).getString("selected_language", "tr");
        java.util.Locale locale = new java.util.Locale(savedLang);
        java.util.Locale.setDefault(locale);
        android.content.res.Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        langContext = createConfigurationContext(config);
        getResources().updateConfiguration(config, langContext.getResources().getDisplayMetrics());

        setContentView(R.layout.activity_navigation);

        initializeViews();

        client = new OkHttpClient();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Hedef koordinatlarını Intent'ten al
        if (getIntent().hasExtra("dest_lat") && getIntent().hasExtra("dest_lon")) {
            destLat = getIntent().getDoubleExtra("dest_lat", 0);
            destLon = getIntent().getDoubleExtra("dest_lon", 0);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        } else {
            startNavigation();
        }
    }

    private void initializeViews() {
        webMapView = findViewById(R.id.navigationMapContainer);
        tvNavigationStatus = findViewById(R.id.tvNavigationStatus);
        llNavigationContainer = findViewById(R.id.navigationFragmentContainer);

        if (webMapView != null) {
            webMapView.getSettings().setJavaScriptEnabled(true);
            webMapView.setWebViewClient(new WebViewClient());
        }
    }

    private void startNavigation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            tvNavigationStatus.setText(langContext.getString(R.string.location_permission_required));
            return;
        }

        tvNavigationStatus.setText("Rota hesaplanıyor...");

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                userLat = location.getLatitude();
                userLon = location.getLongitude();
                calculateRoute();
            }
        });
    }

    private void calculateRoute() {
        String routeUrl = "https://api.tomtom.com/routing/1/calculateRoute/"
                + userLat + "," + userLon + ":"
                + destLat + "," + destLon
                + "/json?key=" + API_KEY + "&traffic=true";

        Request request = new Request.Builder().url(routeUrl).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> tvNavigationStatus.setText("Rota hesaplama hatası: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    JSONArray routes = json.getJSONArray("routes");

                    if (routes.length() > 0) {
                        JSONObject route = routes.getJSONObject(0);
                        JSONObject summary = route.getJSONObject("summary");

                        long travelTimeSeconds = summary.getLong("travelTimeInSeconds");
                        long distanceMeters = summary.getLong("lengthInMeters");

                        long minutes = travelTimeSeconds / 60;
                        double km = distanceMeters / 1000.0;

                        runOnUiThread(() -> {
                            tvNavigationStatus.setText(String.format(
                                    "Tahmini Süre: %d dakika | Mesafe: %.1f km", minutes, km));
                            loadMapRoute();
                        });
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> tvNavigationStatus.setText("Veri işleme hatası: " + e.getMessage()));
                }
            }
        });
    }

    private void loadMapRoute() {
        if (webMapView != null) {
            String mapUrl = "https://www.openstreetmap.org/export/embed.html?bbox="
                    + (Math.min(userLon, destLon) - 0.02) + "%2C"
                    + (Math.min(userLat, destLat) - 0.01) + "%2C"
                    + (Math.max(userLon, destLon) + 0.02) + "%2C"
                    + (Math.max(userLat, destLat) + 0.01)
                    + "&layer=mapnik&marker=" + userLat + "%2C" + userLon;
            webMapView.loadUrl(mapUrl);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startNavigation();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
