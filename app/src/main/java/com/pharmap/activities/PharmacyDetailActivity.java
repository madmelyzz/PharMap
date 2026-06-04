package com.pharmap.activities;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.pharmap.R;

public class PharmacyDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pharmacy_detail);

        // Gelen verileri al
        Intent intent = getIntent();
        String name     = intent.getStringExtra("name");
        String phone    = intent.getStringExtra("phone");
        long   minutes  = intent.getLongExtra("minutes", 0);
        double roadKm   = intent.getDoubleExtra("roadKm", 0);
        double distKm   = intent.getDoubleExtra("distKm", 0);
        double destLat  = intent.getDoubleExtra("lat", 0);
        double destLon  = intent.getDoubleExtra("lon", 0);
        String traffic  = intent.getStringExtra("traffic"); // "green", "yellow", "red"

        // View'ları bağla
        TextView tvName          = findViewById(R.id.tvPharmacyName);
        TextView tvAddress       = findViewById(R.id.tvPharmacyAddress);
        TextView tvTravelTime    = findViewById(R.id.tvTravelTime);
        TextView tvDistance      = findViewById(R.id.tvDistance);
        TextView tvTrafficStatus = findViewById(R.id.tvTrafficStatus);
        View     viewTrafficDot  = findViewById(R.id.viewTrafficDot);
        Button   btnNavigate     = findViewById(R.id.btnNavigate);
        Button   btnCall         = findViewById(R.id.btnCall);

        // Verileri göster
        tvName.setText(name);
        tvAddress.setText("📍 " + name);
        tvTravelTime.setText("🕐 " + minutes + " dakika");
        tvDistance.setText("🛣️ Yol: " + String.format("%.1f", roadKm) + " km   |   📏 Düz: " + String.format("%.1f", distKm) + " km");

        // Trafik durumu
        if ("red".equals(traffic)) {
            tvTrafficStatus.setText("Trafik Yoğun");
            viewTrafficDot.setBackgroundColor(Color.RED);
        } else if ("yellow".equals(traffic)) {
            tvTrafficStatus.setText("Trafik Orta");
            viewTrafficDot.setBackgroundColor(Color.parseColor("#FFA500"));
        } else {
            tvTrafficStatus.setText("Trafik Akıcı");
            viewTrafficDot.setBackgroundColor(Color.parseColor("#4CAF50"));
        }

        // Telefon butonu
        if (phone != null && !phone.equals("İletişim numarası bulunamadı")) {
            btnCall.setVisibility(View.VISIBLE);
            btnCall.setOnClickListener(v -> {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + phone.replace(" ", "")));
                startActivity(callIntent);
            });
        }

        // Navigasyon butonu
        btnNavigate.setOnClickListener(v -> {
            Intent navIntent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("google.navigation:q=" + destLat + "," + destLon)
            );
            navIntent.setPackage("com.google.android.apps.maps");
            if (navIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(navIntent);
            } else {
                Intent webIntent = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("http://maps.google.com/maps?daddr=" + destLat + "," + destLon)
                );
                startActivity(webIntent);
            }
        });
    }
}