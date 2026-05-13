package com.codram.terecojo.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.codram.terecojo.R;
import com.codram.terecojo.data.model.RideRequest;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class MyRequestsAdapter extends RecyclerView.Adapter<MyRequestsAdapter.ViewHolder> {
    private List<RideRequest> requests;
    private OnRequestClickListener listener;

    public interface OnRequestClickListener {
        void onViewOffers(RideRequest request);
        void onCancel(RideRequest request);
        void onFinish(RideRequest request);
    }

    public MyRequestsAdapter(List<RideRequest> requests, OnRequestClickListener listener) {
        this.requests = requests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RideRequest request = requests.get(position);
        
        holder.tvStatus.setText(request.getEstado().toUpperCase());
        holder.tvOrigin.setText("De: " + request.getOrigenDescripcion());
        holder.tvDestination.setText("A: " + request.getDestinoDescripcion());
        
        if (request.getCreadaEn() != null) {
            try {
                // El API devuelve formato ISO 8601 en UTC (ej: 2026-05-09T23:38:42.034Z)
                java.text.SimpleDateFormat isoFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
                isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                java.util.Date date = isoFormat.parse(request.getCreadaEn());
                
                java.text.SimpleDateFormat localFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
                localFormat.setTimeZone(java.util.TimeZone.getDefault());
                holder.tvDate.setText(localFormat.format(date));
            } catch (Exception e) {
                holder.tvDate.setText(request.getCreadaEn().replace("T", " ").substring(0, 16));
            }
        }

        int offers = request.getNumOfertas();
        holder.btnViewOffers.setText("VER " + offers + (offers == 1 ? " OFERTA" : " OFERTAS"));
        
        // Control de visibilidad según estado
        if ("en_proceso".equals(request.getEstado())) {
            holder.btnViewOffers.setVisibility(View.GONE);
            holder.layoutActiveActions.setVisibility(View.GONE);
            holder.layoutInProgressActions.setVisibility(View.VISIBLE);
        } else if ("completada".equals(request.getEstado()) || "cancelada".equals(request.getEstado())) {
            holder.btnViewOffers.setVisibility(View.GONE);
            holder.layoutActiveActions.setVisibility(View.GONE);
            holder.layoutInProgressActions.setVisibility(View.GONE);
        } else {
            // Estado ACTIVA
            holder.btnViewOffers.setVisibility(View.VISIBLE);
            holder.layoutActiveActions.setVisibility(View.VISIBLE);
            holder.layoutInProgressActions.setVisibility(View.GONE);
            holder.btnViewOffers.setEnabled(offers > 0);
        }
        
        holder.btnViewOffers.setOnClickListener(v -> {
            if (listener != null) listener.onViewOffers(request);
        });

        holder.btnCancelActive.setOnClickListener(v -> {
            if (listener != null) listener.onCancel(request);
        });

        holder.btnFinishTrip.setOnClickListener(v -> {
            if (listener != null) listener.onFinish(request);
        });
    }

    @Override
    public int getItemCount() { return requests != null ? requests.size() : 0; }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStatus, tvDate, tvOrigin, tvDestination;
        MaterialButton btnViewOffers, btnCancelTrip, btnFinishTrip, btnCancelActive;
        View layoutInProgressActions, layoutActiveActions;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvOrigin = itemView.findViewById(R.id.tvOrigin);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            btnViewOffers = itemView.findViewById(R.id.btnViewOffers);
            btnCancelTrip = itemView.findViewById(R.id.btnCancelTrip);
            btnFinishTrip = itemView.findViewById(R.id.btnFinishTrip);
            btnCancelActive = itemView.findViewById(R.id.btnCancelActive);
            layoutInProgressActions = itemView.findViewById(R.id.layoutInProgressActions);
            layoutActiveActions = itemView.findViewById(R.id.layoutActiveActions);
        }
    }
}
