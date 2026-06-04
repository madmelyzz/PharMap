package com.pharmap.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.pharmap.R;
import com.pharmap.models.Pharmacy;
import java.util.List;

public class PharmacyAdapter extends RecyclerView.Adapter<PharmacyAdapter.PharmacyViewHolder> {

    private List<Pharmacy> pharmacyList;
    private OnPharmacyClickListener listener;

    public interface OnPharmacyClickListener {
        void onPharmacyClick(Pharmacy pharmacy);
    }

    public PharmacyAdapter(List<Pharmacy> pharmacyList, OnPharmacyClickListener listener) {
        this.pharmacyList = pharmacyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PharmacyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 🌟 Eğer listenin tasarımı farklı bir XML ise R.layout.item_pharmacy kısmını o dosya adıyla değiştir Melahat:
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pharmacy, parent, false);
        return new PharmacyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PharmacyViewHolder holder, int position) {
        Pharmacy pharmacy = pharmacyList.get(position);

        // Senin ekran görüntündeki tam arayüz düzeni
        if (holder.tvName != null) holder.tvName.setText((position + 1) + ". " + pharmacy.getName());
        if (holder.tvDistance != null) holder.tvDistance.setText("📍 " + pharmacy.getFormattedDistance() + " (yol)");
        if (holder.tvTravelTime != null) holder.tvTravelTime.setText("🟡 " + pharmacy.getFormattedTravelTime());
        if (holder.tvPhone != null) holder.tvPhone.setText("📞 " + pharmacy.getPhoneNumber());

        String trafficText = pharmacy.getTrafficStatus() != null ? pharmacy.getTrafficStatus() : "normal";
        if (holder.tvDescription != null) {
            holder.tvDescription.setText(pharmacy.getFormattedDistance() + " uzakta, trafik " + trafficText + " — " + pharmacy.getFormattedTravelTime() + " sürer.");
        }

        // 🌟 ÇÖZÜM: Açılış ve Kapanış Saatlerini Dinamik Olarak Buraya Yazdırıyoruz
        if (holder.tvClosingTime != null) {
            if (pharmacy.isCurrentlyOpen()) {
                holder.tvClosingTime.setText("🕒 Çalışma Saatleri: " + pharmacy.getOpeningTime() + " - " + pharmacy.getClosingTime() + " [AÇIK 🟢]");
                holder.tvClosingTime.setTextColor(Color.parseColor("#2ECC71"));
            } else {
                holder.tvClosingTime.setText("🕒 Mesai Bitti: " + pharmacy.getClosingTime() + " [KAPALI 🔴 Nöbetçiye Yönlendirir]");
                holder.tvClosingTime.setTextColor(Color.parseColor("#E74C3C"));
            }
        }

        // TomTom Trafik Durumu Renklendirmesi
        if (pharmacy.getTrafficStatus() != null && holder.tvTravelTime != null) {
            switch (pharmacy.getTrafficStatus()) {
                case Pharmacy.TRAFFIC_GREEN: holder.tvTravelTime.setTextColor(Color.parseColor("#2ECC71")); break;
                case Pharmacy.TRAFFIC_YELLOW: holder.tvTravelTime.setTextColor(Color.parseColor("#F1C40F")); break;
                case Pharmacy.TRAFFIC_RED: holder.tvTravelTime.setTextColor(Color.parseColor("#E74C3C")); break;
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPharmacyClick(pharmacy);
            }
        });
    }

    @Override
    public int getItemCount() {
        return pharmacyList != null ? pharmacyList.size() : 0;
    }

    public static class PharmacyViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDistance, tvTravelTime, tvPhone, tvDescription, tvClosingTime;

        public PharmacyViewHolder(@NonNull View itemView) {
            super(itemView);

            // 🌟 Dinamik Eşleştirme: XML içindeki ID'ler ne olursa olsun projenin çökmesini engeller
            int idName = itemView.getResources().getIdentifier("tvPharmacyName", "id", itemView.getContext().getPackageName());
            int idDistance = itemView.getResources().getIdentifier("tvPharmacyDistance", "id", itemView.getContext().getPackageName());
            int idTime = itemView.getResources().getIdentifier("tvPharmacyTravelTime", "id", itemView.getContext().getPackageName());
            int idPhone = itemView.getResources().getIdentifier("tvPharmacyPhone", "id", itemView.getContext().getPackageName());
            int idDesc = itemView.getResources().getIdentifier("tvPharmacyDescription", "id", itemView.getContext().getPackageName());
            int idClose = itemView.getResources().getIdentifier("tvClosingTime", "id", itemView.getContext().getPackageName());

            if (idName != 0) tvName = itemView.findViewById(idName);
            if (idDistance != 0) tvDistance = itemView.findViewById(idDistance);
            if (idTime != 0) tvTravelTime = itemView.findViewById(idTime);
            if (idPhone != 0) tvPhone = itemView.findViewById(idPhone);
            if (idDesc != 0) tvDescription = itemView.findViewById(idDesc);

            // Eğer tvClosingTime bulunamazsa, projedeki tvPharmacyDescription alanının üzerine yazması için yedek bağlantı kuruyoruz
            if (idClose != 0) {
                tvClosingTime = itemView.findViewById(idClose);
            } else if (idDesc != 0) {
                tvClosingTime = itemView.findViewById(idDesc);
            }
        }
    }
}