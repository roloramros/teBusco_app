package com.codram.terecojo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.codram.terecojo.data.model.ApiResponse;
import com.codram.terecojo.data.model.Notification;
import com.codram.terecojo.data.remote.RetrofitClient;
import com.codram.terecojo.databinding.ActivityNotificationsBinding;
import com.codram.terecojo.ui.adapter.NotificationAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsActivity extends BaseActivity implements NotificationAdapter.OnNotificationClickListener {

    private ActivityNotificationsBinding binding;
    private NotificationAdapter adapter;
    private List<Notification> notificationList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupDrawer();
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
        }

        setupRecyclerView();
        setupSwipeRefresh();
        fetchNotifications();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.notifications_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(android.view.Menu menu) {
        android.view.MenuItem markReadItem = menu.findItem(R.id.action_mark_all_read);
        android.view.MenuItem deleteAllItem = menu.findItem(R.id.action_delete_all);
        
        if (notificationList.isEmpty()) {
            if (markReadItem != null) markReadItem.setVisible(false);
            if (deleteAllItem != null) deleteAllItem.setVisible(false);
        } else {
            if (deleteAllItem != null) deleteAllItem.setVisible(true);
            
            boolean hasUnread = false;
            for (Notification n : notificationList) {
                if (!n.isLeida()) {
                    hasUnread = true;
                    break;
                }
            }
            if (markReadItem != null) markReadItem.setVisible(hasUnread);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_mark_all_read) {
            markAllAsRead();
            return true;
        } else if (id == R.id.action_delete_all) {
            confirmDeleteAll();
            return true;
        } else if (id == android.R.id.home) {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(androidx.core.view.GravityCompat.START);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(notificationList, this);
        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotifications.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.primary_blue);
        binding.swipeRefresh.setOnRefreshListener(this::fetchNotifications);
    }

    private void fetchNotifications() {
        binding.swipeRefresh.setRefreshing(true);
        RetrofitClient.getService(this).getNotifications().enqueue(new Callback<ApiResponse<List<Notification>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Notification>>> call, @NonNull Response<ApiResponse<List<Notification>>> response) {
                binding.swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    notificationList.clear();
                    notificationList.addAll(response.body().getData());
                    adapter.notifyDataSetChanged();
                    updateEmptyView();
                    
                    updateActionButtonsVisibility();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Notification>>> call, @NonNull Throwable t) {
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(NotificationsActivity.this, "Error al cargar: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateActionButtonsVisibility() {
        invalidateOptionsMenu(); 
    }

    private void updateEmptyView() {
        binding.emptyView.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvNotifications.setVisibility(notificationList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void markAllAsRead() {
        RetrofitClient.getService(this).markAllAsRead().enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    for (Notification n : notificationList) n.setLeida(true);
                    adapter.notifyDataSetChanged();
                    updateActionButtonsVisibility();
                    updateNotificationBadge();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                Toast.makeText(NotificationsActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeleteAll() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_delete_all_title)
                .setMessage(R.string.dialog_delete_all_msg)
                .setPositiveButton(R.string.dialog_delete_all_positive, (dialog, which) -> deleteAll())
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void deleteAll() {
        RetrofitClient.getService(this).deleteAllNotifications().enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    notificationList.clear();
                    adapter.notifyDataSetChanged();
                    updateEmptyView();
                    updateActionButtonsVisibility();
                    updateNotificationBadge();
                    Toast.makeText(NotificationsActivity.this, R.string.msg_notifications_cleared, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                Toast.makeText(NotificationsActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onNotificationClick(Notification notification) {
        if (!notification.isLeida()) {
            markAsRead(notification);
        }
        navigateBasedOnNotification(notification);
    }

    @Override
    public void onNotificationLongClick(Notification notification) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Eliminar notificación")
                .setMessage("¿Deseas eliminar esta notificación?")
                .setPositiveButton("ELIMINAR", (dialog, which) -> deleteSingle(notification))
                .setNegativeButton("CANCELAR", null)
                .show();
    }

    private void deleteSingle(Notification notification) {
        RetrofitClient.getService(this).deleteNotification(notification.getId()).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    notificationList.remove(notification);
                    adapter.notifyDataSetChanged();
                    updateEmptyView();
                    updateActionButtonsVisibility();
                    updateNotificationBadge();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                Toast.makeText(NotificationsActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markAsRead(Notification notification) {
        RetrofitClient.getService(this).markNotificationAsRead(notification.getId()).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    notification.setLeida(true);
                    adapter.notifyDataSetChanged();
                    updateActionButtonsVisibility();
                    updateNotificationBadge();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {}
        });
    }

    private void navigateBasedOnNotification(Notification notification) {
        Intent intent = null;
        switch (notification.getTipo()) {
            case "nueva_oferta":
                intent = new Intent(this, MyRequestsActivity.class);
                break;
            case "nueva_solicitud":
                intent = new Intent(this, DriverOffersActivity.class);
                break;
            case "oferta_aceptada":
            case "viaje_confirmado":
                intent = new Intent(this, MyRequestsActivity.class);
                break;
        }

        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
    }

    @Override
    public void onMapReady(@NonNull com.google.android.gms.maps.GoogleMap googleMap) {}
}
