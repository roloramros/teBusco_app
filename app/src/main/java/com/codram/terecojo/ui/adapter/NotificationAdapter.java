package com.codram.terecojo.ui.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.codram.terecojo.R;
import com.codram.terecojo.data.model.Notification;
import com.codram.terecojo.databinding.ItemNotificationBinding;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<Notification> notifications;
    private final OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
        void onNotificationLongClick(Notification notification);
        void onCallClick(String phoneNumber);
    }

    public NotificationAdapter(List<Notification> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNotificationBinding binding = ItemNotificationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(notifications.get(position));
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemNotificationBinding binding;

        public ViewHolder(ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Notification notification) {
            binding.tvTitle.setText(notification.getTitulo());
            binding.tvBody.setText(notification.getCuerpo());
            
            // Formatear fecha legible y local
            if (notification.getCreadaEn() != null) {
                try {
                    // El API devuelve ISO 8601 en UTC (ej: 2026-05-10T15:30:00.000Z)
                    java.text.SimpleDateFormat isoFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
                    isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    java.util.Date date = isoFormat.parse(notification.getCreadaEn());

                    java.text.SimpleDateFormat localFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
                    localFormat.setTimeZone(java.util.TimeZone.getDefault());
                    binding.tvDate.setText(localFormat.format(date));
                } catch (Exception e) {
                    binding.tvDate.setText(notification.getCreadaEn());
                }
            }
            
            binding.unreadIndicator.setVisibility(notification.isLeida() ? View.GONE : View.VISIBLE);

            // Personalización según el tipo
            String icon = "🔔";
            int colorRes = R.color.gray_light;

            switch (notification.getTipo()) {
                case "nueva_oferta":
                    icon = "💰";
                    colorRes = R.color.offer_gold;
                    break;
                case "oferta_aceptada":
                case "viaje_confirmado":
                case "viaje_completado":
                    icon = "✅";
                    colorRes = R.color.success_green;
                    break;
                case "oferta_rechazada":
                case "viaje_cancelado":
                    icon = "❌";
                    colorRes = R.color.error_red;
                    break;
                case "nueva_solicitud":
                    icon = "🚕";
                    colorRes = R.color.primary_blue;
                    break;
            }

            binding.tvIcon.setText(icon);
            binding.tvIcon.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.getContext(), colorRes)));

            // Botón de llamada para ofertas aceptadas
            if ("oferta_aceptada".equals(notification.getTipo()) && 
                notification.getDatosExtra() != null && 
                notification.getDatosExtra().containsKey("pasajero_telefono")) {
                
                String phone = String.valueOf(notification.getDatosExtra().get("pasajero_telefono"));
                binding.btnCall.setVisibility(View.VISIBLE);
                binding.btnCall.setOnClickListener(v -> listener.onCallClick(phone));
            } else {
                binding.btnCall.setVisibility(View.GONE);
            }

            binding.getRoot().setOnClickListener(v -> listener.onNotificationClick(notification));
            binding.getRoot().setOnLongClickListener(v -> {
                listener.onNotificationLongClick(notification);
                return true;
            });
        }
    }
}
