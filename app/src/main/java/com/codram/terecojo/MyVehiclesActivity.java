package com.codram.terecojo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.codram.terecojo.data.model.ApiResponse;
import com.codram.terecojo.data.model.Vehicle;
import com.codram.terecojo.data.remote.ApiService;
import com.codram.terecojo.data.remote.RetrofitClient;
import com.codram.terecojo.databinding.ActivityMyVehiclesBinding;
import com.codram.terecojo.ui.adapter.VehicleAdapter;
import com.codram.terecojo.ui.dialog.AddVehicleDialogFragment;
import com.codram.terecojo.utils.SessionManager;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyVehiclesActivity extends BaseActivity {
    private ActivityMyVehiclesBinding binding;
    private VehicleAdapter adapter;
    private List<Vehicle> vehicleList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyVehiclesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupDrawer();

        setupVehiclesList();
        loadVehicles();

        binding.fabAddVehicle.setOnClickListener(v -> {
            AddVehicleDialogFragment dialog = new AddVehicleDialogFragment();
            dialog.setOnVehicleAddedListener(this::loadVehicles);
            dialog.show(getSupportFragmentManager(), "AddVehicle");
        });

        // Aplicar insets para el FAB
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.fabAddVehicle, (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.bottomMargin = (int) (16 * getResources().getDisplayMetrics().density) + systemBars.bottom;
            params.rightMargin = (int) (16 * getResources().getDisplayMetrics().density) + systemBars.right;
            v.setLayoutParams(params);
            return insets;
        });
    }

    private void setupVehiclesList() {
        adapter = new VehicleAdapter(vehicleList, new VehicleAdapter.OnVehicleActionListener() {
            @Override
            public void onEdit(Vehicle vehicle) {
                AddVehicleDialogFragment dialog = AddVehicleDialogFragment.newInstance(vehicle);
                dialog.setOnVehicleAddedListener(MyVehiclesActivity.this::loadVehicles);
                dialog.show(getSupportFragmentManager(), "EditVehicle");
            }

            @Override
            public void onDelete(Vehicle vehicle) {
                showDeleteConfirmation(vehicle);
            }

            @Override
            public void onImageClick(String imageUrl) {
                showImagePreview(imageUrl);
            }
        });
        binding.rvVehicles.setLayoutManager(new LinearLayoutManager(this));
        binding.rvVehicles.setAdapter(adapter);
    }

    private void showImagePreview(String imageUrl) {
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_image_preview);
        android.widget.ImageView ivFull = dialog.findViewById(R.id.ivFullImage);
        
        com.bumptech.glide.Glide.with(this)
                .load(imageUrl)
                .fitCenter()
                .into(ivFull);

        ivFull.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showDeleteConfirmation(Vehicle vehicle) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Eliminar Vehículo")
                .setMessage("¿Estás seguro de que deseas eliminar el vehículo " + vehicle.getMarca() + " (" + vehicle.getPlaca() + ")?")
                .setPositiveButton("Eliminar", (dialog, which) -> deleteVehicle(vehicle))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deleteVehicle(Vehicle vehicle) {
        ApiService apiService = RetrofitClient.getService(this);
        apiService.deleteVehicle(vehicle.getId()).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MyVehiclesActivity.this, "Vehículo eliminado", Toast.LENGTH_SHORT).show();
                    loadVehicles();
                } else {
                    Toast.makeText(MyVehiclesActivity.this, "Error al eliminar", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(MyVehiclesActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadVehicles() {
        ApiService apiService = RetrofitClient.getService(this);
        apiService.getVehicles().enqueue(new Callback<ApiResponse<List<Vehicle>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Vehicle>>> call, Response<ApiResponse<List<Vehicle>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    vehicleList = response.body().getData();
                    adapter.updateList(vehicleList);
                    
                    if (vehicleList.isEmpty()) {
                        Toast.makeText(MyVehiclesActivity.this, "No tienes vehículos registrados", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MyVehiclesActivity.this, "Error al cargar vehículos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Vehicle>>> call, Throwable t) {
                Toast.makeText(MyVehiclesActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadVehicles(); // Refrescar al volver (por ejemplo, tras añadir uno)
    }
}