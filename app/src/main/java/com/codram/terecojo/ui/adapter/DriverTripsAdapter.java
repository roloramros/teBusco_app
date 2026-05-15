package com.codram.terecojo.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.codram.terecojo.R;
import com.codram.terecojo.data.model.RideRequest;
import com.codram.terecojo.databinding.ItemDriverTripBinding;
import java.util.List;

public class DriverTripsAdapter extends RecyclerView.Adapter<DriverTripsAdapter.ViewHolder> {

    private final List<RideRequest> trips;
    private final OnTripClickListener listener;

    public interface OnTripClickListener {
        void onCancel(RideRequest trip);
        void onViewRoute(RideRequest trip);
        void onCallPassenger(String phoneNumber);
    }

    public DriverTripsAdapter(List<RideRequest> trips, OnTripClickListener listener) {
        this.trips = trips;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDriverTripBinding binding = ItemDriverTripBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(trips.get(position));
    }

    @Override
    public int getItemCount() {
        return trips.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemDriverTripBinding binding;

        public ViewHolder(ItemDriverTripBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(RideRequest trip) {
            binding.tvPassengerName.setText(trip.getPasajeroNombre());
            binding.tvOrigin.setText("Origen: " + trip.getOrigenDescripcion());
            binding.tvDestination.setText("Destino: " + trip.getDestinoDescripcion());
            
            String estado = trip.getEstado().toUpperCase();
            binding.tvStatus.setText(estado);
            
            int colorRes = R.color.primary_blue;
            switch (trip.getEstado()) {
                case "en_proceso":
                    colorRes = R.color.offer_gold;
                    binding.btnViewRoute.setVisibility(View.VISIBLE);
                    binding.btnCancel.setVisibility(View.VISIBLE);
                    binding.btnCallPassenger.setVisibility(View.VISIBLE);
                    break;
                case "completada":
                    colorRes = R.color.success_green;
                    binding.btnViewRoute.setVisibility(View.GONE);
                    binding.btnCancel.setVisibility(View.GONE);
                    binding.btnCallPassenger.setVisibility(View.GONE);
                    break;
                case "cancelada":
                    colorRes = R.color.error_red;
                    binding.btnViewRoute.setVisibility(View.GONE);
                    binding.btnCancel.setVisibility(View.GONE);
                    binding.btnCallPassenger.setVisibility(View.GONE);
                    break;
                default:
                    binding.btnViewRoute.setVisibility(View.GONE);
                    binding.btnCancel.setVisibility(View.VISIBLE);
                    binding.btnCallPassenger.setVisibility(View.GONE);
                    break;
            }
            
            binding.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.getContext(), colorRes)));

            binding.btnCancel.setOnClickListener(v -> listener.onCancel(trip));
            binding.btnViewRoute.setOnClickListener(v -> listener.onViewRoute(trip));
            binding.btnCallPassenger.setOnClickListener(v -> {
                if (trip.getPasajeroTelefono() != null && !trip.getPasajeroTelefono().isEmpty()) {
                    listener.onCallPassenger(trip.getPasajeroTelefono());
                } else {
                    android.widget.Toast.makeText(itemView.getContext(), "Número no disponible", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
