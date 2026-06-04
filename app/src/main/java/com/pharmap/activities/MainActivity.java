package com.pharmap.activities;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TOMTOM_API_KEY = "a7EqPOlXZO8JGOmbCfP4Qv9VuMwFfc42";
    private static final String NOSY_API_KEY   = "e2x99ouO0lAgOmZL7xsTYco3OTtmQx2hByRba2VIOjumcC6bKEYSHZqxH0G3";

    private TextView      tvStatus;
    private LinearLayout  llResults;
    private LinearLayout  llInfo;
    private TextView      tvInfoTitle;
    private TextView      tvInfoDetails;
    private ScrollView    scrollView;
    private LinearLayout  llMainRoot;
    private Button        btnThemeToggle;
    private WebView       webMapPreview;
    private LinearLayout  llQuickLocationsContainer;
    private LinearLayout  llPrescriptionContainer;
    private TextView      tvListBackButton;

    private OkHttpClient                client;
    private FusedLocationProviderClient fusedLocationClient;
    private PharMapDatabase             dbHelper;

    private double userLat = 41.0122;
    private double userLon = 28.9760;

    private boolean isDarkMode = false;

    private LinearLayout selectedCard = null;

    private final List<long[]>  lastResults   = new ArrayList<>();
    private final List<String>  lastNames     = new ArrayList<>();
    private final List<Double>  lastLats      = new ArrayList<>();
    private final List<Double>  lastLons      = new ArrayList<>();
    private final List<String>  lastPhones    = new ArrayList<>();
    private final List<String>  lastAddresses = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Kaydedilen dili uygula (setContentView'dan ÖNCE)
        String savedLang = getSharedPreferences("pharmap_prefs", MODE_PRIVATE)
                .getString("selected_language", "tr");
        java.util.Locale locale = new java.util.Locale(savedLang);
        java.util.Locale.setDefault(locale);
        android.content.res.Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        Context langContext = createConfigurationContext(config);
        getResources().updateConfiguration(config, langContext.getResources().getDisplayMetrics());

        setContentView(com.pharmap.R.layout.activity_main);

        tvStatus      = findViewById(com.pharmap.R.id.tvStatus);
        llResults     = findViewById(com.pharmap.R.id.llResults);
        llInfo        = findViewById(com.pharmap.R.id.llInfo);
        tvInfoTitle   = findViewById(com.pharmap.R.id.tvInfoTitle);
        tvInfoDetails = findViewById(com.pharmap.R.id.tvInfoDetails);
        scrollView    = findViewById(com.pharmap.R.id.scrollView);
        llMainRoot    = (LinearLayout) tvStatus.getParent();

        client              = new OkHttpClient();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        dbHelper            = new PharMapDatabase(this);

        dbHelper.checkAndInsertDefaultLocations();

        // ✅ Dil butonlarını bağla
        TextView btnLangTR = findViewById(com.pharmap.R.id.btnLangTR);
        TextView btnLangEN = findViewById(com.pharmap.R.id.btnLangEN);

        if (btnLangTR != null && btnLangEN != null) {
            if (savedLang.equals("en")) {
                btnLangEN.setBackgroundResource(com.pharmap.R.drawable.lang_selected_bg);
                btnLangEN.setTextColor(Color.WHITE);
                btnLangTR.setBackground(null);
                btnLangTR.setTextColor(Color.parseColor("#0D5C8A"));
            } else {
                btnLangTR.setBackgroundResource(com.pharmap.R.drawable.lang_selected_bg);
                btnLangTR.setTextColor(Color.WHITE);
                btnLangEN.setBackground(null);
                btnLangEN.setTextColor(Color.parseColor("#0D5C8A"));
            }

            btnLangTR.setOnClickListener(v -> changeLanguage("tr"));
            btnLangEN.setOnClickListener(v -> changeLanguage("en"));
        }

        initListBackButton();
        showWelcomeFeaturesOnCard();
        initThemeButton();

        findViewById(com.pharmap.R.id.btnFind).setOnClickListener(v -> {
            if (tvListBackButton != null) tvListBackButton.setVisibility(View.VISIBLE);
            llResults.removeAllViews();
            llInfo.setVisibility(View.GONE);
            selectedCard = null;
            tvStatus.setText("📍 Konum alınıyor...");
            getUserLocationThenSearch();
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        } else {
            updateMapToUserLocation();
        }
    }

    // ✅ Dil değiştirici
    public void changeLanguage(String languageCode) {
        java.util.Locale newLocale = new java.util.Locale(languageCode);
        java.util.Locale.setDefault(newLocale);

        android.content.res.Configuration newConfig = getResources().getConfiguration();
        newConfig.setLocale(newLocale);
        Context context = createConfigurationContext(newConfig);
        getResources().updateConfiguration(newConfig, context.getResources().getDisplayMetrics());

        // SharedPreferences'a kaydet
        getSharedPreferences("pharmap_prefs", MODE_PRIVATE)
                .edit()
                .putString("selected_language", languageCode)
                .apply();

        // Activity'yi yeniden başlat
        Intent intent = getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        finish();
        startActivity(intent);
    }

    private void showWelcomeFeaturesOnCard() {
        llResults.removeAllViews();

        webMapPreview = new WebView(this);
        LinearLayout.LayoutParams mapParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 500);
        mapParams.setMargins(16, 24, 16, 16);
        webMapPreview.setLayoutParams(mapParams);
        GradientDrawable mapShape = new GradientDrawable();
        mapShape.setCornerRadius(24f);
        webMapPreview.setBackground(mapShape);
        webMapPreview.setClipToOutline(true);
        webMapPreview.getSettings().setJavaScriptEnabled(true);
        webMapPreview.setWebViewClient(new WebViewClient());
        loadMapUrl(userLat, userLon);
        llResults.addView(webMapPreview);

        llQuickLocationsContainer = new LinearLayout(this);
        llQuickLocationsContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams qParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        qParams.setMargins(16, 20, 16, 16);
        llQuickLocationsContainer.setLayoutParams(qParams);

        LinearLayout llLocationHeader = new LinearLayout(this);
        llLocationHeader.setOrientation(LinearLayout.HORIZONTAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("📌 Sık Kullanılan Konumlarım");
        tvTitle.setTextSize(14);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(isDarkMode ? Color.WHITE : Color.parseColor("#212121"));
        llLocationHeader.addView(tvTitle,
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView btnAddNewLocation = new TextView(this);
        btnAddNewLocation.setText("➕ Yeni Konum Ekle");
        btnAddNewLocation.setTextColor(Color.parseColor("#4CAF50"));
        btnAddNewLocation.setTypeface(null, Typeface.BOLD);
        btnAddNewLocation.setTextSize(14);
        btnAddNewLocation.setPadding(10, 10, 10, 10);
        btnAddNewLocation.setOnClickListener(v -> showAddLocationDialog());
        llLocationHeader.addView(btnAddNewLocation);
        llQuickLocationsContainer.addView(llLocationHeader);

        LinearLayout llButtonsRow = new LinearLayout(this);
        llButtonsRow.setOrientation(LinearLayout.VERTICAL);
        llButtonsRow.setPadding(0, 12, 0, 0);
        llQuickLocationsContainer.addView(llButtonsRow);
        llResults.addView(llQuickLocationsContainer);

        loadLocationsFromDatabase();

        llPrescriptionContainer = new LinearLayout(this);
        llPrescriptionContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams pParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        pParams.setMargins(16, 20, 16, 20);
        llPrescriptionContainer.setLayoutParams(pParams);

        LinearLayout llHeaderRow = new LinearLayout(this);
        llHeaderRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView tvPTitle = new TextView(this);
        tvPTitle.setText("🧾 Kayıtlı Reçetelerim");
        tvPTitle.setTextSize(14);
        tvPTitle.setTypeface(null, Typeface.BOLD);
        tvPTitle.setTextColor(isDarkMode ? Color.WHITE : Color.parseColor("#212121"));
        llHeaderRow.addView(tvPTitle,
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView btnAddPrescription = new TextView(this);
        btnAddPrescription.setText("➕ Yeni Ekle");
        btnAddPrescription.setTextColor(Color.parseColor("#4CAF50"));
        btnAddPrescription.setTypeface(null, Typeface.BOLD);
        btnAddPrescription.setTextSize(14);
        btnAddPrescription.setPadding(10, 10, 10, 10);
        btnAddPrescription.setOnClickListener(v -> showAddPrescriptionDialog());
        llHeaderRow.addView(btnAddPrescription);
        llPrescriptionContainer.addView(llHeaderRow);

        LinearLayout llListArea = new LinearLayout(this);
        llListArea.setOrientation(LinearLayout.VERTICAL);
        llPrescriptionContainer.addView(llListArea);

        llResults.addView(llPrescriptionContainer);
        loadPrescriptionsFromDatabase();
    }

    private void loadLocationsFromDatabase() {
        if (llQuickLocationsContainer == null || llQuickLocationsContainer.getChildCount() < 2) return;
        LinearLayout buttonsArea = (LinearLayout) llQuickLocationsContainer.getChildAt(1);
        buttonsArea.removeAllViews();

        SQLiteDatabase db     = dbHelper.getReadableDatabase();
        Cursor         cursor = db.rawQuery("SELECT * FROM konumlar", null);

        if (cursor.moveToFirst()) {
            int indexBaslik = cursor.getColumnIndexOrThrow("baslik");
            int indexAdres  = cursor.getColumnIndexOrThrow("adres");
            do {
                String label   = cursor.getString(indexBaslik);
                String address = cursor.getString(indexAdres);

                LinearLayout itemRow = new LinearLayout(this);
                itemRow.setOrientation(LinearLayout.HORIZONTAL);
                itemRow.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.setMargins(0, 8, 0, 8);
                itemRow.setLayoutParams(rowParams);

                TextView tvLocButton = new TextView(this);
                tvLocButton.setText(label + " (" + address + ")");
                tvLocButton.setTextSize(13);
                tvLocButton.setTypeface(null, Typeface.BOLD);
                tvLocButton.setPadding(24, 24, 24, 24);
                LinearLayout.LayoutParams btnParams =
                        new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                tvLocButton.setLayoutParams(btnParams);

                GradientDrawable cardBg = new GradientDrawable();
                cardBg.setCornerRadius(20f);
                cardBg.setColor(isDarkMode ? Color.parseColor("#2D2D2D") : Color.parseColor("#F5F5F5"));
                tvLocButton.setBackground(cardBg);
                tvLocButton.setTextColor(isDarkMode ? Color.WHITE : Color.parseColor("#333333"));
                tvLocButton.setOnClickListener(v -> searchPharmaciesByAddress(label, address));

                itemRow.addView(tvLocButton);
                buttonsArea.addView(itemRow);
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    private void showAddLocationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Yeni Sık Kullanılan Konum Ekle");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText etLabel = new EditText(this);
        etLabel.setHint("Konum Başlığı (Örn: Okulum, Annem vb.)");
        layout.addView(etLabel);

        final EditText etAddress = new EditText(this);
        etAddress.setHint("Açık Adres (Eczane bulmak için tam girin)");
        layout.addView(etAddress);

        builder.setView(layout);
        builder.setPositiveButton("Konumu Kaydet", (dialog, which) -> {
            String label   = etLabel.getText().toString().trim();
            String address = etAddress.getText().toString().trim();
            if (!label.isEmpty() && !address.isEmpty()) {
                SQLiteDatabase db     = dbHelper.getWritableDatabase();
                ContentValues  values = new ContentValues();
                values.put("baslik", label);
                values.put("adres", address);
                db.insert("konumlar", null, values);
                Toast.makeText(this, "Yeni konum başarıyla kaydedildi!", Toast.LENGTH_SHORT).show();
                loadLocationsFromDatabase();
            } else {
                Toast.makeText(this, "Lütfen tüm alanları doldurun!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("İptal", null);
        builder.show();
    }

    private void loadPrescriptionsFromDatabase() {
        if (llPrescriptionContainer == null || llPrescriptionContainer.getChildCount() < 2) return;
        LinearLayout listArea = (LinearLayout) llPrescriptionContainer.getChildAt(1);
        listArea.removeAllViews();

        SQLiteDatabase db     = dbHelper.getReadableDatabase();
        Cursor         cursor = db.rawQuery("SELECT * FROM receteler ORDER BY id DESC", null);

        if (cursor.moveToFirst()) {
            int indexId      = cursor.getColumnIndexOrThrow("id");
            int indexKod     = cursor.getColumnIndexOrThrow("recete_kod");
            int indexEczane  = cursor.getColumnIndexOrThrow("eczane_adi");
            int indexIlaclar = cursor.getColumnIndexOrThrow("ilac_listesi");
            do {
                int    id      = cursor.getInt(indexId);
                String kod     = cursor.getString(indexKod);
                String eczane  = cursor.getString(indexEczane);
                String ilaclar = cursor.getString(indexIlaclar);

                LinearLayout cardRow = new LinearLayout(this);
                cardRow.setOrientation(LinearLayout.HORIZONTAL);
                cardRow.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.setMargins(0, 12, 0, 0);
                cardRow.setLayoutParams(rowParams);

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(24, 20, 24, 20);
                card.setLayoutParams(
                        new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

                GradientDrawable cardBg = new GradientDrawable();
                cardBg.setCornerRadius(16f);
                cardBg.setColor(isDarkMode ? Color.parseColor("#2D2D2D") : Color.parseColor("#F9F9F9"));
                card.setBackground(cardBg);

                TextView tvKod = new TextView(this);
                tvKod.setText("🔑 Reçete No: " + kod);
                tvKod.setTypeface(null, Typeface.BOLD);
                tvKod.setTextColor(isDarkMode ? Color.WHITE : Color.BLACK);
                card.addView(tvKod);

                TextView tvEczane = new TextView(this);
                tvEczane.setText("🏥 Alındığı Eczane: " + eczane);
                tvEczane.setTextSize(13);
                tvEczane.setTextColor(isDarkMode ? Color.parseColor("#CCCCCC") : Color.parseColor("#666666"));
                card.addView(tvEczane);

                TextView tvIlac = new TextView(this);
                tvIlac.setText("💊 İlaçlar: " + ilaclar);
                tvIlac.setTextSize(13);
                tvIlac.setTextColor(isDarkMode ? Color.parseColor("#CCCCCC") : Color.parseColor("#666666"));
                card.addView(tvIlac);

                cardRow.addView(card);

                TextView btnDelete = new TextView(this);
                btnDelete.setText("❌ Sil");
                btnDelete.setTextColor(Color.parseColor("#D32F2F"));
                btnDelete.setTypeface(null, Typeface.BOLD);
                btnDelete.setTextSize(13);
                btnDelete.setPadding(20, 20, 20, 20);
                btnDelete.setGravity(Gravity.CENTER);
                btnDelete.setOnClickListener(v ->
                        new AlertDialog.Builder(this)
                                .setTitle("Reçeteyi Sil")
                                .setMessage("Bu reçete kaydını kalıcı olarak silmek istediğinize emin misiniz?")
                                .setPositiveButton("Evet, Sil", (dialog, which) -> {
                                    dbHelper.getWritableDatabase()
                                            .delete("receteler", "id = ?", new String[]{String.valueOf(id)});
                                    Toast.makeText(this, "Reçete silindi.", Toast.LENGTH_SHORT).show();
                                    loadPrescriptionsFromDatabase();
                                })
                                .setNegativeButton("İptal", null)
                                .show());

                cardRow.addView(btnDelete);
                listArea.addView(cardRow);
            } while (cursor.moveToNext());
        } else {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("Henüz kaydedilmiş reçeteniz bulunmuyor.");
            tvEmpty.setTextSize(12);
            tvEmpty.setTextColor(Color.GRAY);
            tvEmpty.setPadding(8, 16, 0, 0);
            listArea.addView(tvEmpty);
        }
        cursor.close();
    }

    private void showAddPrescriptionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reçete Bilgilerini Kaydet");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText etKod     = new EditText(this);
        etKod.setHint("Reçete Kodu veya Numarası");
        layout.addView(etKod);

        final EditText etEczane  = new EditText(this);
        etEczane.setHint("Eczane Adı");
        layout.addView(etEczane);

        final EditText etIlaclar = new EditText(this);
        etIlaclar.setHint("İlaç isimleri (Virgülle ayırın)");
        layout.addView(etIlaclar);

        builder.setView(layout);
        builder.setPositiveButton("Sisteme Kaydet", (dialog, which) -> {
            String kod     = etKod.getText().toString().trim();
            String eczane  = etEczane.getText().toString().trim();
            String ilaclar = etIlaclar.getText().toString().trim();
            if (!kod.isEmpty() && !eczane.isEmpty()) {
                ContentValues values = new ContentValues();
                values.put("recete_kod", kod);
                values.put("eczane_adi", eczane);
                values.put("ilac_listesi", ilaclar);
                dbHelper.getWritableDatabase().insert("receteler", null, values);
                Toast.makeText(this, "Reçete başarıyla kaydedildi!", Toast.LENGTH_SHORT).show();
                loadPrescriptionsFromDatabase();
            } else {
                Toast.makeText(this, "Lütfen gerekli alanları doldurun!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("İptal", null);
        builder.show();
    }

    private void initListBackButton() {
        tvListBackButton = new TextView(this);
        tvListBackButton.setText("⬅ Ana Sayfaya Dön");
        tvListBackButton.setTextSize(15);
        tvListBackButton.setTypeface(null, Typeface.BOLD);
        tvListBackButton.setPadding(24, 16, 16, 16);
        tvListBackButton.setVisibility(View.GONE);
        tvListBackButton.setOnClickListener(v -> {
            tvListBackButton.setVisibility(View.GONE);
            llInfo.setVisibility(View.GONE);
            selectedCard = null;
            lastResults.clear();
            tvStatus.setText("Butona bas, nöbetçi eczaneler hesaplanıyor...");
            showWelcomeFeaturesOnCard();
            scrollView.smoothScrollTo(0, 0);
        });
        llMainRoot.addView(tvListBackButton, llMainRoot.indexOfChild(tvStatus));
    }

    private void loadMapUrl(double lat, double lon) {
        if (webMapPreview != null) {
            String mapUrl = "https://www.openstreetmap.org/export/embed.html?bbox="
                    + (lon - 0.01) + "%2C" + (lat - 0.005) + "%2C"
                    + (lon + 0.01) + "%2C" + (lat + 0.005)
                    + "&layer=mapnik&marker=" + lat + "%2C" + lon;
            webMapPreview.loadUrl(mapUrl);
        }
    }

    private void updateMapToUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    userLat = location.getLatitude();
                    userLon = location.getLongitude();
                    loadMapUrl(userLat, userLon);
                }
            });
        }
    }

    private void initThemeButton() {
        LinearLayout themeLayout = new LinearLayout(this);
        themeLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.END;
        layoutParams.setMargins(16, 16, 16, 16);
        themeLayout.setLayoutParams(layoutParams);
        themeLayout.setPadding(8, 8, 8, 8);



        // 🌙 Ay butonu
        btnThemeToggle = new Button(this);
        btnThemeToggle.setText("🌙");
        btnThemeToggle.setTextSize(14);
        btnThemeToggle.setPadding(8, 2, 8, 2);
        btnThemeToggle.setBackground(null);
        btnThemeToggle.setOnClickListener(v -> {
            if (!isDarkMode) {
                isDarkMode = true;
                applyTheme();
            }
        });

        // ☀️ Güneş butonu
        Button btnLight = new Button(this);
        btnLight.setText("☀️");
        btnLight.setTextSize(14);
        btnLight.setPadding(8, 2, 8, 2);
        btnLight.setBackground(null);
        btnLight.setOnClickListener(v -> {
            if (isDarkMode) {
                isDarkMode = false;
                applyTheme();
            }
        });

        themeLayout.addView(btnThemeToggle);
        themeLayout.addView(btnLight);
        llMainRoot.addView(themeLayout, 0);
        applyTheme();
    }

    private void applyTheme() {
        if (tvListBackButton != null)
            tvListBackButton.setTextColor(isDarkMode ? Color.WHITE : Color.parseColor("#3498db"));

        // Tema container background


        if (isDarkMode) {
            llMainRoot.setBackgroundColor(Color.parseColor("#121212"));
            tvStatus.setTextColor(Color.WHITE);
            llInfo.setBackgroundColor(Color.parseColor("#1E1E1E"));
            tvInfoTitle.setTextColor(Color.WHITE);
            tvInfoDetails.setTextColor(Color.parseColor("#BBBBBB"));
        } else {
            llMainRoot.setBackgroundColor(Color.WHITE);
            tvStatus.setTextColor(Color.parseColor("#212121"));
            llInfo.setBackgroundColor(Color.WHITE);
            tvInfoTitle.setTextColor(Color.parseColor("#212121"));
            tvInfoDetails.setTextColor(Color.parseColor("#212121"));
        }

        if (llQuickLocationsContainer != null && llPrescriptionContainer != null) {
            showWelcomeFeaturesOnCard();
        }

        if (!lastResults.isEmpty()) {
            showResults(lastResults, lastNames, lastLats, lastLons);
        }
    }

    private void searchPharmaciesByAddress(String label, String address) {
        if (tvListBackButton != null) tvListBackButton.setVisibility(View.VISIBLE);
        llResults.removeAllViews();
        llInfo.setVisibility(View.GONE);
        selectedCard = null;
        tvStatus.setText("📍 " + label + " adresi sorgulanıyor...");

        String geocodeUrl = "https://api.tomtom.com/search/2/geocode/"
                + Uri.encode(address) + ".json?key=" + TOMTOM_API_KEY + "&limit=1";

        Request request = new Request.Builder().url(geocodeUrl).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> tvStatus.setText("Adres bulunamadı: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONObject json    = new JSONObject(response.body().string());
                    JSONArray  results = json.getJSONArray("results");
                    if (results.length() > 0) {
                        JSONObject position = results.getJSONObject(0).getJSONObject("position");
                        userLat = position.getDouble("lat");
                        userLon = position.getDouble("lon");
                        runOnUiThread(() -> {
                            tvStatus.setText("✅ " + label + " konumu doğrulandı. Nöbetçi eczaneler aranıyor...");
                            searchNearbyDutyPharmacies();
                        });
                    } else {
                        runOnUiThread(() -> tvStatus.setText("⚠️ Girdiğiniz adres haritada eşleşmedi!"));
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> tvStatus.setText("Adres çözümleme hatası!"));
                } finally {
                    response.close();
                }
            }
        });
    }

    private void getUserLocationThenSearch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            tvStatus.setText("⚠️ Konum izni gerekli!");
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                userLat = location.getLatitude();
                userLon = location.getLongitude();
            }
            tvStatus.setText("🏥 Nöbetçi eczaneler aranıyor...");
            searchNearbyDutyPharmacies();
        });
    }

    private void searchNearbyDutyPharmacies() {
        String nosyUrl = "https://www.nosyapi.com/apiv2/service/pharmacies-on-duty/locations"
                + "?latitude=" + userLat
                + "&longitude=" + userLon
                + "&apiKey=" + NOSY_API_KEY;

        Request request = new Request.Builder().url(nosyUrl).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        tvStatus.setText("🔴 NosyAPI bağlantı hatası: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String     body = response.body().string();
                    JSONObject json = new JSONObject(body);

                    if (!"success".equals(json.optString("status"))) {
                        String msg = json.optString("messageTR", "Bilinmeyen hata");
                        runOnUiThread(() -> tvStatus.setText("🔴 NosyAPI: " + msg));
                        return;
                    }

                    JSONArray data = json.getJSONArray("data");

                    lastNames.clear();
                    lastLats.clear();
                    lastLons.clear();
                    lastPhones.clear();
                    lastAddresses.clear();

                    int limit = Math.min(data.length(), 5);
                    for (int i = 0; i < limit; i++) {
                        JSONObject item = data.getJSONObject(i);
                        lastNames.add(item.getString("pharmacyName"));
                        lastLats.add(item.getDouble("latitude"));
                        lastLons.add(item.getDouble("longitude"));
                        lastAddresses.add(item.optString("address", "Adres bilgisi yok"));

                        String phone = item.optString("phone", "").trim();
                        if (phone.isEmpty()) phone = item.optString("phone2", "").trim();
                        if (phone.isEmpty()) phone = "İletişim numarası bulunamadı";
                        lastPhones.add(phone);
                    }

                    if (limit == 0) {
                        runOnUiThread(() ->
                                tvStatus.setText("ℹ️ Bu konumda nöbetçi eczane bulunamadı."));
                        return;
                    }

                    final int found = limit;
                    runOnUiThread(() ->
                            tvStatus.setText("🏥 " + found + " nöbetçi eczane bulundu, trafik hesaplanıyor..."));

                    calculateTravelTimes(lastNames, lastLats, lastLons);

                } catch (Exception e) {
                    runOnUiThread(() ->
                            tvStatus.setText("🔴 Veri ayrıştırma hatası: " + e.getMessage()));
                } finally {
                    response.close();
                }
            }
        });
    }

    private void calculateTravelTimes(List<String> names, List<Double> lats, List<Double> lons) {
        lastResults.clear();
        for (int i = 0; i < names.size(); i++) {
            final int index = i;
            String url = "https://api.tomtom.com/routing/1/calculateRoute/"
                    + userLat + "," + userLon + ":"
                    + lats.get(i) + "," + lons.get(i)
                    + "/json?key=" + TOMTOM_API_KEY + "&traffic=true";

            Request request = new Request.Builder().url(url).build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> tvStatus.setText("Rota hatası: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        JSONObject json   = new JSONObject(response.body().string());
                        JSONArray  routes = json.getJSONArray("routes");
                        JSONObject sum    = routes.getJSONObject(0).getJSONObject("summary");
                        long travelTime   = sum.getLong("travelTimeInSeconds");
                        long roadMetres   = sum.getLong("lengthInMeters");

                        double theta = userLon - lons.get(index);
                        double dist  = Math.sin(Math.toRadians(userLat))
                                * Math.sin(Math.toRadians(lats.get(index)))
                                + Math.cos(Math.toRadians(userLat))
                                * Math.cos(Math.toRadians(lats.get(index)))
                                * Math.cos(Math.toRadians(theta));
                        dist = Math.acos(Math.max(-1.0, Math.min(1.0, dist)));
                        dist = Math.toDegrees(dist) * 60 * 1.1515 * 1.609344;
                        long straightKm100 = Math.round(dist * 100);

                        synchronized (lastResults) {
                            lastResults.add(new long[]{index, travelTime, roadMetres, straightKm100});
                            if (lastResults.size() == names.size()) {
                                showResults(lastResults, lastNames, lastLats, lastLons);
                            }
                        }
                    } catch (Exception e) {
                        runOnUiThread(() -> tvStatus.setText("Rota hesaplama hatası: " + e.getMessage()));
                    } finally {
                        response.close();
                    }
                }
            });
        }
    }

    private void showResults(List<long[]> results, List<String> names,
                             List<Double> lats, List<Double> lons) {

        results.sort((a, b) -> Long.compare(a[1], b[1]));

        int  nearestIdx = 0;
        long minDist    = Long.MAX_VALUE;
        for (int i = 0; i < results.size(); i++) {
            if (results.get(i)[3] < minDist) {
                minDist    = results.get(i)[3];
                nearestIdx = i;
            }
        }
        final int nearest = nearestIdx;

        runOnUiThread(() -> {
            tvStatus.setText("✅ En hızlı nöbetçi eczane bulundu!");
            llResults.removeAllViews();

            for (int i = 0; i < results.size(); i++) {
                long[]  r         = results.get(i);
                int     origIdx   = (int) r[0];
                long    minutes   = r[1] / 60;
                double  roadKm    = r[2] / 1000.0;
                double  distKm    = r[3] / 100.0;
                boolean isFastest = (i == 0);
                boolean isNearest = (i == nearest);

                String trafficMsg;
                String trafficEmoji;

                if (isFastest && !isNearest) {
                    double nearestMin = results.get(nearest)[1] / 60.0;
                    double nearestKm  = results.get(nearest)[3] / 100.0;
                    long   diff       = Math.round(nearestMin - minutes);
                    trafficMsg = String.format(
                            "Daha yakın bir nöbetçi eczane var (%.1f km) ama trafik yoğun, %d dk daha uzun sürer.\n"
                                    + "Bu eczane %.1f km uzakta ama trafik akıcı — sadece %d dk!",
                            nearestKm, diff, distKm, minutes);
                    trafficEmoji = "🟢";
                } else if (isNearest && !isFastest) {
                    double fastestMin = results.get(0)[1] / 60.0;
                    long   diff       = Math.round(minutes - fastestMin);
                    trafficMsg = String.format(
                            "En yakın nöbetçi eczane (%.1f km) ama trafik çok yoğun!\n"
                                    + "%d dk sürer. %d dk kazanmak için diğer eczaneyi tercih et.",
                            distKm, minutes, diff);
                    trafficEmoji = "🔴";
                } else if (isFastest) {
                    trafficMsg   = String.format("Hem en yakın hem en hızlı! %.1f km, %d dakika.", distKm, minutes);
                    trafficEmoji = "🟢";
                } else {
                    double ratio = (distKm > 0) ? roadKm / distKm : 1.0;
                    if (ratio > 1.5) {
                        trafficEmoji = "🔴";
                        trafficMsg   = String.format("%.1f km uzakta, trafik yoğun — %d dk sürer.", distKm, minutes);
                    } else if (ratio > 1.2) {
                        trafficEmoji = "🟡";
                        trafficMsg   = String.format("%.1f km uzakta, trafik orta — %d dk sürer.", distKm, minutes);
                    } else {
                        trafficEmoji = "🟢";
                        trafficMsg   = String.format("%.1f km uzakta, trafik akıcı — %d dk sürer.", distKm, minutes);
                    }
                }

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(32, 28, 32, 28);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(16, 16, 16, 0);
                card.setLayoutParams(params);

                int defaultBgColor = isFastest
                        ? Color.parseColor("#3498db")
                        : (isDarkMode
                        ? (i % 2 == 0 ? Color.parseColor("#1F1E1E") : Color.parseColor("#2D2D2D"))
                        : (i % 2 == 0 ? Color.parseColor("#EEEEEE") : Color.WHITE));

                GradientDrawable defaultDrawable = new GradientDrawable();
                defaultDrawable.setColor(defaultBgColor);
                defaultDrawable.setCornerRadius(24f);
                card.setBackground(defaultDrawable);
                card.setTag(defaultBgColor);

                if (isFastest) selectedCard = card;

                int textColor = isFastest ? Color.WHITE
                        : (isDarkMode ? Color.WHITE : Color.parseColor("#212121"));

                TextView tvName = new TextView(this);
                tvName.setText((isFastest ? "⭐ EN HIZLI NÖBETÇİ — " : (i + 1) + ". ") + names.get(origIdx));
                tvName.setTextSize(16);
                tvName.setTextColor(textColor);
                tvName.setTypeface(null, Typeface.BOLD);
                card.addView(tvName);

                TextView tvTime = new TextView(this);
                tvTime.setText(trafficEmoji + " " + minutes + " dk   📍 "
                        + String.format("%.1f", roadKm) + " km (yol)");
                tvTime.setTextSize(14);
                tvTime.setTextColor(textColor);
                tvTime.setPadding(0, 6, 0, 0);
                card.addView(tvTime);

                final String currentPhone = (origIdx < lastPhones.size())
                        ? lastPhones.get(origIdx).trim() : "İletişim numarası yok";
                TextView tvPhoneNum = new TextView(this);
                tvPhoneNum.setText("📞 " + currentPhone);
                tvPhoneNum.setTextSize(13);
                tvPhoneNum.setTextColor(textColor);
                tvPhoneNum.setPadding(0, 4, 0, 0);

                if (!currentPhone.contains("bulunamadı") && !currentPhone.contains("yok") && !currentPhone.isEmpty()) {
                    tvPhoneNum.setOnClickListener(v -> {
                        String cleanNumber = tvPhoneNum.getText().toString()
                                .replace("📞", "")
                                .replaceAll("\\s+", "");
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.parse("tel:" + cleanNumber));
                        startActivity(intent);
                    });
                }
                card.addView(tvPhoneNum);

                TextView tvMsg = new TextView(this);
                tvMsg.setText(trafficMsg);
                tvMsg.setTextSize(13);
                tvMsg.setTextColor(isDarkMode ? Color.parseColor("#AAAAAA") : Color.parseColor("#555555"));
                tvMsg.setPadding(0, 8, 0, 0);
                card.addView(tvMsg);

                final LinearLayout thisCard    = card;
                final String       finalName   = names.get(origIdx);
                final long         finalMin    = minutes;
                final double       finalRoadKm = roadKm;
                final double       finalDistKm = distKm;
                final String       finalEmoji  = trafficEmoji;
                final double       destLat     = lats.get(origIdx);
                final double       destLon     = lons.get(origIdx);
                final String       finalAddress = (origIdx < lastAddresses.size())
                        ? lastAddresses.get(origIdx) : "Adres bilgisi yok";

                card.setOnClickListener(v -> {
                    if (selectedCard != null && selectedCard != thisCard) {
                        int prevBg = (int) selectedCard.getTag();
                        GradientDrawable reset = new GradientDrawable();
                        reset.setColor(prevBg);
                        reset.setCornerRadius(24f);
                        selectedCard.setBackground(reset);
                        int prevText = (prevBg == Color.parseColor("#3498db")) ? Color.WHITE
                                : (isDarkMode ? Color.WHITE : Color.parseColor("#212121"));
                        for (int j = 0; j < selectedCard.getChildCount(); j++) {
                            if (selectedCard.getChildAt(j) instanceof TextView)
                                ((TextView) selectedCard.getChildAt(j)).setTextColor(prevText);
                        }
                    }

                    GradientDrawable sel = new GradientDrawable();
                    sel.setColor(isDarkMode ? Color.parseColor("#252525") : Color.parseColor("#EEEEEE"));
                    sel.setCornerRadius(24f);
                    sel.setStroke(4, Color.parseColor("#3498db"));
                    thisCard.setBackground(sel);
                    for (int j = 0; j < thisCard.getChildCount(); j++) {
                        if (thisCard.getChildAt(j) instanceof TextView)
                            ((TextView) thisCard.getChildAt(j))
                                    .setTextColor(isDarkMode ? Color.WHITE : Color.parseColor("#212121"));
                    }
                    selectedCard = thisCard;

                    llInfo.setVisibility(View.VISIBLE);
                    tvInfoTitle.setText("⬅ Geri dön            🏥 " + finalName);
                    tvInfoTitle.setTextSize(15);
                    tvInfoTitle.setTypeface(null, Typeface.BOLD);
                    tvInfoTitle.setOnClickListener(titleView -> {
                        llInfo.setVisibility(View.GONE);
                        if (selectedCard != null) {
                            int prevBg = (int) selectedCard.getTag();
                            GradientDrawable resetD = new GradientDrawable();
                            resetD.setColor(prevBg);
                            resetD.setCornerRadius(24f);
                            selectedCard.setBackground(resetD);
                            int prevText = (prevBg == Color.parseColor("#3498db")) ? Color.WHITE
                                    : (isDarkMode ? Color.WHITE : Color.parseColor("#212121"));
                            for (int j = 0; j < selectedCard.getChildCount(); j++) {
                                if (selectedCard.getChildAt(j) instanceof TextView)
                                    ((TextView) selectedCard.getChildAt(j)).setTextColor(prevText);
                            }
                            selectedCard = null;
                        }
                        scrollView.smoothScrollTo(0, 0);
                    });

                    String cleanInfoPhone = tvPhoneNum.getText().toString().replace("📞", "").trim();

                    tvInfoDetails.setGravity(Gravity.CENTER_VERTICAL);
                    tvInfoDetails.setLineSpacing(12f, 1.2f);
                    tvInfoDetails.setText(
                            "🏥 Eczane: " + finalName + "\n" +
                                    "📍 Adres: " + finalAddress + "\n" +
                                    "📞 İletişim: " + cleanInfoPhone + "\n" +
                                    "🕐 Tahmini süre: " + finalMin + " dakika\n" +
                                    "🛣️ Yol mesafesi: " + String.format("%.1f", finalRoadKm) + " km\n" +
                                    "📏 Düz mesafe: " + String.format("%.1f", finalDistKm) + " km\n" +
                                    "🚦 Trafik durumu: " + finalEmoji + "\n" +
                                    "⏰ Durum: Bugün nöbetçi ✅"
                    );

                    if (llInfo.getChildCount() > 2) llInfo.removeViewAt(2);

                    TextView btnNav = new TextView(MainActivity.this);
                    btnNav.setText("➔ CANLI NAVİGASYONU BAŞLAT");
                    btnNav.setTextColor(Color.WHITE);
                    GradientDrawable btnShape = new GradientDrawable();
                    btnShape.setColor(Color.parseColor("#4CAF50"));
                    btnShape.setCornerRadius(20f);
                    btnNav.setBackground(btnShape);
                    btnNav.setTextSize(15);
                    btnNav.setTypeface(null, Typeface.BOLD);
                    btnNav.setGravity(Gravity.CENTER);
                    btnNav.setPadding(0, 35, 0, 35);
                    LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    navParams.setMargins(20, 30, 20, 20);
                    btnNav.setLayoutParams(navParams);
                    btnNav.setOnClickListener(view -> {
                        // google.navigation şeması doğrudan navigasyon modunu tetikler
                        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + destLat + "," + destLon);
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);

                        // Google Haritalar uygulamasını hedef alıyoruz
                        mapIntent.setPackage("com.google.android.apps.maps");

                        // 🌟 KESİN ÇÖZÜM: Arka plandaki eski harita oturumunu ve kilitlenen konumları temizleyen bayraklar
                        mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                        // Güvenli çalıştırma kontrolü
                        if (mapIntent.resolveActivity(getPackageManager()) != null) {
                            startActivity(mapIntent);
                        } else {
                            try {
                                startActivity(mapIntent);
                            } catch (android.content.ActivityNotFoundException e) {
                                Toast.makeText(MainActivity.this, "Google Haritalar uygulaması yüklü değil!", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                    llInfo.addView(btnNav);
                    llInfo.post(() -> scrollView.smoothScrollTo(0, llInfo.getTop()));
                });

                llResults.addView(card);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            updateMapToUserLocation();
        }
    }

    private static class PharMapDatabase extends SQLiteOpenHelper {
        private static final String DB_NAME    = "pharmap_data.db";
        private static final int    DB_VERSION = 2;

        public PharMapDatabase(AppCompatActivity activity) {
            super(activity, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE receteler ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "recete_kod TEXT,"
                    + "eczane_adi TEXT,"
                    + "ilac_listesi TEXT)");
            db.execSQL("CREATE TABLE konumlar ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "baslik TEXT,"
                    + "adres TEXT)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                db.execSQL("CREATE TABLE IF NOT EXISTS konumlar ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "baslik TEXT,"
                        + "adres TEXT)");
            }
        }

        public void checkAndInsertDefaultLocations() {
            SQLiteDatabase db     = this.getWritableDatabase();
            Cursor         cursor = db.rawQuery("SELECT COUNT(*) FROM konumlar", null);
            cursor.moveToFirst();
            int count = cursor.getInt(0);
            cursor.close();

            if (count == 0) {
                ContentValues ev = new ContentValues();
                ev.put("baslik", "🏠 Evim");
                ev.put("adres", "Fatih, İstanbul");
                db.insert("konumlar", null, ev);

                ContentValues is = new ContentValues();
                is.put("baslik", "💼 İş Yerim");
                is.put("adres", "Taksim, Beyoğlu, İstanbul");
                db.insert("konumlar", null, is);
            }
        }
    }
}