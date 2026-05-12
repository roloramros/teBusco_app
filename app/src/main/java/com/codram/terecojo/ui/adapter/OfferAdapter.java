package com.codram.terecojo.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.codram.terecojo.R;
import com.codram.terecojo.data.model.Offer;
import com.google.android.material.button.MaterialButton;
import java.util.List;

import android.widget.ImageView;
import com.bumptech.glide.Glide;

public class OfferAdapter extends RecyclerView.Adapter<OfferAdapter.ViewHolder> {
    private List<Offer> offers;
    private OnOfferClickListener listener;

    public interface OnOfferClickListener {
        void onAccept(Offer offer);
        void onReject(Offer offer);
        void onImageClick(String imageUrl);
    }

    public OfferAdapter(List<Offer> offers, OnOfferClickListener listener) {
        this.offers = offers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_offer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Offer offer = offers.get(position);
        
        holder.tvDriverName.setText(offer.getChoferNombre());
        holder.tvRating.setText(String.format("⭐ %.1f", offer.getCalificacionPromedio()));
        holder.tvOfferPrice.setText(String.format("$%.0f %s", offer.getPrecioPropuesto(), offer.getMoneda()));
        holder.tvArrivalTime.setText("Llega en " + offer.getTiempoArriboMin() + " min");
        
        holder.btnAcceptOffer.setOnClickListener(v -> {
            if (listener != null) listener.onAccept(offer);
        });

        holder.btnRejectOffer.setOnClickListener(v -> {
            if (listener != null) listener.onReject(offer);
        });

        holder.btnViewVehicle.setOnClickListener(v -> {
            if (listener != null && offer.getVehiculoFoto() != null) {
                listener.onImageClick(offer.getVehiculoFoto());
            }
        });
    }

    @Override
    public int getItemCount() { return offers != null ? offers.size() : 0; }

    public void setListener(OnOfferClickListener listener) {
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDriverName, tvRating, tvOfferPrice, tvArrivalTime;
        MaterialButton btnAcceptOffer, btnRejectOffer, btnViewVehicle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDriverName = itemView.findViewById(R.id.tvDriverName);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvOfferPrice = itemView.findViewById(R.id.tvOfferPrice);
            tvArrivalTime = itemView.findViewById(R.id.tvArrivalTime);
            btnAcceptOffer = itemView.findViewById(R.id.btnAcceptOffer);
            btnRejectOffer = itemView.findViewById(R.id.btnRejectOffer);
            btnViewVehicle = itemView.findViewById(R.id.btnViewVehicle);
        }
    }
}
