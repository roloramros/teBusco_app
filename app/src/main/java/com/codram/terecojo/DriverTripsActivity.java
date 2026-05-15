package com.codram.terecojo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.codram.terecojo.data.model.ApiResponse;
import com.codram.terecojo.data.model.RideRequest;
import com.codram.terecojo.data.remote.RetrofitClient;
import com.codram.terecojo.databinding.ActivityDriverTripsBinding;
import com.codram.terecojo.ui.adapter.DriverTripsAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverTripsActivity extends BaseActivity implements DriverTripsAdapter.OnTripClickListener {

    private ActivityDriverTripsBinding binding;
    private DriverTripsAdapter adapter;
    private List<RideRequest> tripList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDriverTripsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupDrawer();
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
        }

        setupRecyclerView();
        setupSwipeRefresh();
        fetchTrips();
    }

    private void setupRecyclerView() {
        adapter = new DriverTripsAdapter(tripList, this);
        binding.rvDriverTrips.setLayoutManager(new LinearLayoutManager(this));
        binding.rvDriverTrips.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.primary_blue);
        binding.swipeRefresh.setOnRefreshListener(this::fetchTrips);
    }

    private void fetchTrips() {
        binding.swipeRefresh.setRefreshing(true);
        RetrofitClient.getService(this).getMisViajesChofer().enqueue(new Callback<ApiResponse<List<RideRequest>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<RideRequest>>> call, Response<ApiResponse<List<RideRequest>>> response) {
                binding.swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    tripList.clear();
                    tripList.addAll(response.body().getData());
                    adapter.notifyDataSetChanged();
                    updateEmptyView();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<RideRequest>>> call, Throwable t) {
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(DriverTripsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmptyView() {
        if (tripList.isEmpty()) {
            binding.layoutEmpty.getRoot().setVisibility(View.VISIBLE);
            binding.swipeRefresh.setVisibility(View.GONE);
            binding.layoutEmpty.tvEmptyTitle.setText("No tienes viajes");
            binding.layoutEmpty.tvEmptyDescription.setText("Acepta solicitudes en el radar para comenzar a trabajar.");
            binding.layoutEmpty.btnEmptyAction.setText("IR AL RADAR");
            binding.layoutEmpty.btnEmptyAction.setOnClickListener(v -> {
                startActivity(new Intent(this, DriverActivity.class));
                finish();
            });
        } else {
            binding.layoutEmpty.getRoot().setVisibility(View.GONE);
            binding.swipeRefresh.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCancel(RideRequest trip) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cancelar compromiso")
                .setMessage("¿Estás seguro de que deseas cancelar este viaje? El pasajero será notificado y la solicitud volverá al radar.")
                .setPositiveButton("SÍ, CANCELAR", (dialog, which) -> cancelTrip(trip))
                .setNegativeButton("VOLVER", null)
                .show();
    }

    private void cancelTrip(RideRequest trip) {
        RetrofitClient.getService(this).cancelarViajeChofer(trip.getId()).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(DriverTripsActivity.this, "Viaje cancelado", Toast.LENGTH_SHORT).show();
                    fetchTrips();
                } else {
                    Toast.makeText(DriverTripsActivity.this, "Error al cancelar", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(DriverTripsActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onViewRoute(RideRequest trip) {
        Intent intent = new Intent(this, DriverActivity.class);
        intent.putExtra("EXTRA_RIDE_REQUEST", trip);
        // Usar CLEAR_TOP y SINGLE_TOP para asegurar que se reutilice la instancia existente
        // y se llame a onNewIntent si ya está en ejecución.
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        // No cerramos esta actividad para permitir al chofer volver a su lista fácilmente
        // si lo desea (o podemos dejar el finish() si prefieres, pero sin él es más flexible)
    }

    @Override
    public void onCallPassenger(String phoneNumber) {
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
        }
    }

    @Override
    public void onMapReady(@androidx.annotation.NonNull com.google.android.gms.maps.GoogleMap googleMap) {}
}
