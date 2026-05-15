package com.codram.terecojo;

import android.content.Intent;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.codram.terecojo.data.model.RideRequest;
import com.codram.terecojo.data.model.AuthResponse;
import com.codram.terecojo.databinding.ActivityDriverProfileBinding;
import com.codram.terecojo.ui.adapter.RideRequestAdapter;
import com.codram.terecojo.utils.SessionManager;
import java.util.ArrayList;
import java.util.List;

import com.codram.terecojo.data.model.ApiResponse;
import com.codram.terecojo.data.model.OfferRequest;
import com.codram.terecojo.data.model.Vehicle;
import com.codram.terecojo.data.remote.RetrofitClient;
import com.codram.terecojo.ui.viewmodel.DriverViewModel;
import androidx.lifecycle.ViewModelProvider;
import android.widget.ArrayAdapter;
import java.util.stream.Collectors;
import android.view.View;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.widget.Toast;

public class DriverProfileActivity extends BaseActivity implements RideRequestAdapter.OnRideActionListener {
    private ActivityDriverProfileBinding profileBinding;
    private RideRequestAdapter adapter;
    private List<RideRequest> requests = new ArrayList<>();
    private DriverViewModel viewModel;
    private List<Vehicle> myVehicles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profileBinding = ActivityDriverProfileBinding.inflate(getLayoutInflater());
        setContentView(profileBinding.getRoot());

        viewModel = new ViewModelProvider(this).get(DriverViewModel.class);
        setupObservers();

        setupDrawer();
        
        setupRequestsList();
        setupSwipeRefresh();
        
        if (findViewById(R.id.fabMyLocation) != null) {
            findViewById(R.id.fabMyLocation).setOnClickListener(v -> {
                startActivity(new Intent(this, DriverActivity.class));
            });
        }

        viewModel.fetchMyVehicles();
    }

    private void setupObservers() {
        viewModel.getErrorMessage().observe(this, error -> {
            Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
        });

        viewModel.getVehicles().observe(this, vehicles -> {
            this.myVehicles = vehicles;
        });

        viewModel.getOfferSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(this, "¡Oferta enviada con éxito!", Toast.LENGTH_LONG).show();
                loadRequests(); // Recargar para ver si hay cambios
            }
        });
    }

    private void setupRequestsList() {
        AuthResponse.User user = SessionManager.getInstance(this).getUser();
        boolean verified = user != null && user.isVerificado();
        adapter = new RideRequestAdapter(requests, verified, this);
        profileBinding.rvRequests.setLayoutManager(new LinearLayoutManager(this));
        profileBinding.rvRequests.setAdapter(adapter);
    }

    @Override
    public void onAccept(RideRequest request) {
        AuthResponse.User user = SessionManager.getInstance(this).getUser();
        if (user != null && !user.isVerificado()) {
            Toast.makeText(this, "Tu cuenta está pendiente de verificación", Toast.LENGTH_SHORT).show();
            return;
        }
        showMakeOfferDialog(request);
    }

    private void showMakeOfferDialog(RideRequest rideRequest) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_make_offer, null);
        android.widget.AutoCompleteTextView spinnerVehicles = dialogView.findViewById(R.id.spinnerVehicles);
        android.widget.AutoCompleteTextView spinnerCurrency = dialogView.findViewById(R.id.spinnerCurrency);
        com.google.android.material.textfield.TextInputEditText etPrice = dialogView.findViewById(R.id.etOfferPrice);
        com.google.android.material.textfield.TextInputEditText etArrival = dialogView.findViewById(R.id.etArrivalTime);
        com.google.android.material.textfield.TextInputEditText etMessage = dialogView.findViewById(R.id.etOfferMessage);

        // Configurar Vehículos
        List<String> vehicleNames = myVehicles.stream()
                .map(v -> v.getMarca() + " (" + v.getPlaca() + ")")
                .collect(Collectors.toList());
        ArrayAdapter<String> vehicleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, vehicleNames);
        spinnerVehicles.setAdapter(vehicleAdapter);

        // Configurar Monedas
        String[] currencies = {"CUP", "MLC", "USD"};
        ArrayAdapter<String> currencyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, currencies);
        spinnerCurrency.setAdapter(currencyAdapter);
        spinnerCurrency.setText("CUP", false);

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        builder.setView(dialogView);
        builder.setPositiveButton("ENVIAR OFERTA", (dialog, which) -> {
            String selectedVehicleName = spinnerVehicles.getText().toString();
            String priceStr = etPrice.getText().toString();
            String arrivalStr = etArrival.getText().toString();
            String currency = spinnerCurrency.getText().toString();
            String message = etMessage.getText().toString();

            if (selectedVehicleName.isEmpty() || priceStr.isEmpty() || arrivalStr.isEmpty()) {
                Toast.makeText(this, "Por favor completa los campos obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            // Encontrar ID del vehículo seleccionado
            String selectedId = "";
            for (Vehicle v : myVehicles) {
                if (selectedVehicleName.contains(v.getPlaca())) {
                    selectedId = v.getId();
                    break;
                }
            }

            OfferRequest offerRequest = new OfferRequest(
                    selectedId,
                    message,
                    Double.parseDouble(priceStr),
                    currency,
                    Integer.parseInt(arrivalStr)
            );

            viewModel.sendOffer(rideRequest.getId(), offerRequest);
        });
        builder.setNegativeButton("CANCELAR", null);
        builder.show();
    }

    @Override
    public void onViewMap(RideRequest request) {
        // NUEVO
        Intent intent = new Intent(this, DriverActivity.class);
        intent.putExtra("AUTO_ROUTE_JSON", new com.google.gson.Gson().toJson(request));
        intent.putExtra("AUTO_OPEN_ROUTE", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            profileBinding.layoutEmpty.getRoot().setVisibility(View.VISIBLE);
            profileBinding.swipeRefreshRadar.setVisibility(View.GONE);

            profileBinding.layoutEmpty.ivEmptyIcon.setImageResource(android.R.drawable.ic_menu_compass);
            profileBinding.layoutEmpty.tvEmptyTitle.setText(R.string.empty_radar_title);
            profileBinding.layoutEmpty.tvEmptyDescription.setText(R.string.empty_radar_desc);
            profileBinding.layoutEmpty.btnEmptyAction.setText(R.string.empty_radar_action);
            profileBinding.layoutEmpty.btnEmptyAction.setOnClickListener(v -> {
                profileBinding.swipeRefreshRadar.setRefreshing(true);
                loadRequests();
            });
        } else {
            profileBinding.layoutEmpty.getRoot().setVisibility(View.GONE);
            profileBinding.swipeRefreshRadar.setVisibility(View.VISIBLE);
        }
    }

    private void setupSwipeRefresh() {
        profileBinding.swipeRefreshRadar.setOnRefreshListener(this::loadRequests);
        profileBinding.swipeRefreshRadar.setColorSchemeResources(R.color.primary_dark_blue);
    }

    private void loadRequests() {
        RetrofitClient.getService(this).getRadarSolicitudes().enqueue(new Callback<ApiResponse<List<RideRequest>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<RideRequest>>> call, Response<ApiResponse<List<RideRequest>>> response) {
                profileBinding.swipeRefreshRadar.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    requests.clear();
                    requests.addAll(response.body().getData());
                    adapter.notifyDataSetChanged();
                    updateEmptyState(requests.isEmpty());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<RideRequest>>> call, Throwable t) {
                profileBinding.swipeRefreshRadar.setRefreshing(false);
                updateEmptyState(requests.isEmpty());
                Toast.makeText(DriverProfileActivity.this, "Error al cargar radar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRequests();
    }

    @Override
    public void onMapReady(@androidx.annotation.NonNull com.google.android.gms.maps.GoogleMap googleMap) {}
}
