package com.codram.terecojo.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.codram.terecojo.R;
import com.codram.terecojo.data.model.RideRequest;
import java.util.List;

public class RideRequestAdapter extends RecyclerView.Adapter<RideRequestAdapter.ViewHolder> {
    private List<RideRequest> requests;
    private OnRideActionListener listener;
    private boolean isUserVerified;

    public interface OnRideActionListener {
        void onAccept(RideRequest request);
        void onViewMap(RideRequest request);
    }

    public RideRequestAdapter(List<RideRequest> requests, boolean isUserVerified, OnRideActionListener listener) {   
        this.requests = requests;
        this.isUserVerified = isUserVerified;
        this.listener = listener;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ride_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RideRequest request = requests.get(position);
        holder.tvPassengerName.setText(request.getPasajeroNombre());
        
        // Mostrar fecha/hora del viaje
        if (request.isEsInmediato()) {
            holder.tvDateTime.setText("Viaje: Ahora");
        } else if (request.getFechaViaje() != null) {
            try {
                // Formato ISO esperado: yyyy-MM-dd HH:mm:ss o similar
                java.text.SimpleDateFormat isoFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US);
                java.util.Date date = isoFormat.parse(request.getFechaViaje());
                
                java.text.SimpleDateFormat legibleFormat = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault());
                holder.tvDateTime.setText("Viaje: " + legibleFormat.format(date));
            } catch (Exception e) {
                // Fallback si el parseo falla (limpiar T si existe y formatear manualmente)
                String rawDate = request.getFechaViaje().replace("T", " ");
                if (rawDate.length() >= 16) {
                    holder.tvDateTime.setText("Viaje: " + rawDate.substring(0, 16).replace("-", "/"));
                } else {
                    holder.tvDateTime.setText("Viaje: " + rawDate);
                }
            }
        }

        // Monedas
        List<String> currencies = request.getMoneda();
        if (currencies != null && !currencies.isEmpty()) {
            holder.tvPrice.setText(android.text.TextUtils.join(", ", currencies));
        }

        // Detalles Nuevos
        holder.tvDistanceApprox.setText(String.format(java.util.Locale.getDefault(), "Distancia aprox: %.2f km", request.getDistancia()));
        
        int stopsCount = (request.getParadas() != null) ? request.getParadas().size() : 0;
        holder.tvStopsDetail.setText("Paradas: " + stopsCount);
        
        holder.tvPassengersDetail.setText("Pasajeros: " + request.getNumPasajeros());
        
        // Descripción
        if (request.getDescripcion() != null && !request.getDescripcion().isEmpty()) {
            holder.tvDescription.setVisibility(View.VISIBLE);
            holder.tvDescription.setText(request.getDescripcion());
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }

        // Precio de Oferta (Se muestra siempre)
        holder.tvOfferPrice.setVisibility(View.VISIBLE);
        holder.tvOfferPrice.setText("Oferta Pasajero: $" + request.getPrecioOferta());

        // Fecha de creación (MM/DD HH:MM) con conversión a zona horaria local
        if (request.getCreadaEn() != null) {
            try {
                // El API devuelve formato ISO 8601 en UTC (ej: 2026-05-09T23:38:42.034Z)
                java.text.SimpleDateFormat isoFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
                isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                java.util.Date date = isoFormat.parse(request.getCreadaEn());
                
                java.text.SimpleDateFormat localFormat = new java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault());
                localFormat.setTimeZone(java.util.TimeZone.getDefault());
                holder.tvCreatedDate.setText("Creado: " + localFormat.format(date));
            } catch (Exception e) {
                holder.tvCreatedDate.setText("Creado: " + request.getCreadaEn().substring(5, 16).replace("T", " "));
            }
        }

        // Botones
        holder.btnViewMap.setOnClickListener(v -> {
            if (listener != null) listener.onViewMap(request);
        });

        if (request.isHaRespondido()) {
            holder.btnAccept.setEnabled(false);
            holder.btnAccept.setAlpha(0.5f);
            if (holder.btnAccept instanceof android.widget.TextView) {
                ((android.widget.TextView) holder.btnAccept).setText("OFERTADO");
            } else if (holder.btnAccept instanceof android.widget.Button) {
                ((android.widget.Button) holder.btnAccept).setText("OFERTADO");
            }
        } else if (!isUserVerified) {
            holder.btnAccept.setEnabled(false);
            holder.btnAccept.setAlpha(0.5f);
            if (holder.btnAccept instanceof android.widget.TextView) {
                ((android.widget.TextView) holder.btnAccept).setText("PENDIENTE");
            } else if (holder.btnAccept instanceof android.widget.Button) {
                ((android.widget.Button) holder.btnAccept).setText("PENDIENTE");
            }
        } else {
            holder.btnAccept.setEnabled(true);
            holder.btnAccept.setAlpha(1.0f);
            if (holder.btnAccept instanceof android.widget.TextView) {
                ((android.widget.TextView) holder.btnAccept).setText("OFERTAR");
            } else if (holder.btnAccept instanceof android.widget.Button) {
                ((android.widget.Button) holder.btnAccept).setText("OFERTAR");
            }
        }

        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) listener.onAccept(request);
        });
    }

    @Override
    public int getItemCount() { return requests != null ? requests.size() : 0; }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPassengerName, tvPrice, tvDateTime;
        TextView tvDistanceApprox, tvStopsDetail, tvPassengersDetail, tvCreatedDate, tvDescription, tvOfferPrice;
        View btnAccept, btnViewMap;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPassengerName = itemView.findViewById(R.id.tvPassengerName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            
            tvDistanceApprox = itemView.findViewById(R.id.tvDistanceApprox);
            tvStopsDetail = itemView.findViewById(R.id.tvStopsDetail);
            tvPassengersDetail = itemView.findViewById(R.id.tvPassengersDetail);
            tvCreatedDate = itemView.findViewById(R.id.tvCreatedDate);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvOfferPrice = itemView.findViewById(R.id.tvOfferPrice);
            
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnViewMap = itemView.findViewById(R.id.btnViewMap);
        }
    }
}
