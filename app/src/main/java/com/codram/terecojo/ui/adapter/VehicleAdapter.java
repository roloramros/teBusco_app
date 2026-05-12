package com.codram.terecojo.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.codram.terecojo.R;
import com.codram.terecojo.data.model.Vehicle;
import java.util.List;

public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.ViewHolder> {
    private List<Vehicle> vehicles;
    private OnVehicleActionListener actionListener;

    public interface OnVehicleActionListener {
        void onEdit(Vehicle vehicle);
        void onDelete(Vehicle vehicle);
        void onImageClick(String imageUrl);
    }

    public VehicleAdapter(List<Vehicle> vehicles, OnVehicleActionListener listener) {
        this.vehicles = vehicles;
        this.actionListener = listener;
    }

    public void updateList(List<Vehicle> newList) {
        this.vehicles = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vehicle, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Vehicle vehicle = vehicles.get(position);
        holder.tvMarca.setText(vehicle.getMarca());
        holder.tvPlaca.setText(vehicle.getPlaca());
        holder.tvTipo.setText(vehicle.getTipo().toUpperCase());
        holder.tvCapacidad.setText(vehicle.getCapacidadPasajeros() + " pasajeros");

        if (vehicle.getFotoUrl() != null && !vehicle.getFotoUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(vehicle.getFotoUrl())
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_directions)
                    .into(holder.ivVehicle);
        } else {
            holder.ivVehicle.setImageResource(android.R.drawable.ic_menu_directions);
        }

        holder.ivVehicle.setOnClickListener(v -> {
            if (actionListener != null && vehicle.getFotoUrl() != null) {
                actionListener.onImageClick(vehicle.getFotoUrl());
            }
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onEdit(vehicle);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onDelete(vehicle);
        });
    }

    @Override
    public int getItemCount() {
        return vehicles != null ? vehicles.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMarca, tvPlaca, tvTipo, tvCapacidad;
        ImageView ivVehicle;
        View btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMarca = itemView.findViewById(R.id.tvVehicleMarca);
            tvPlaca = itemView.findViewById(R.id.tvVehiclePlaca);
            tvTipo = itemView.findViewById(R.id.tvVehicleTipo);
            tvCapacidad = itemView.findViewById(R.id.tvVehicleCapacidad);
            ivVehicle = itemView.findViewById(R.id.ivVehicleIcon);
            btnEdit = itemView.findViewById(R.id.btnEditVehicle);
            btnDelete = itemView.findViewById(R.id.btnDeleteVehicle);
        }
    }
}
