package com.codram.terecojo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.codram.terecojo.databinding.ActivityDriverBinding;
import com.codram.terecojo.utils.SessionManager;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.codram.terecojo.data.model.ApiResponse;
import com.codram.terecojo.data.model.AuthResponse;
import com.codram.terecojo.data.model.RideRequest;
import com.codram.terecojo.data.remote.RetrofitClient;
import com.codram.terecojo.ui.viewmodel.DriverViewModel;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import com.codram.terecojo.ui.adapter.RideRequestAdapter;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import java.util.ArrayList;

import com.codram.terecojo.data.model.OfferRequest;
import com.codram.terecojo.data.model.Vehicle;
import android.widget.ArrayAdapter;
import java.util.stream.Collectors;

public class DriverActivity extends BaseActivity implements RideRequestAdapter.OnRideActionListener {
    private ActivityDriverBinding binding;
    private DriverViewModel viewModel;
    private RideRequestAdapter adapter;
    private List<RideRequest> radarRequests = new ArrayList<>();
    private List<Polyline> activePolylines = new ArrayList<>();
    private List<com.google.android.gms.maps.model.Marker> routeMarkers = new ArrayList<>();
    private List<com.google.android.gms.maps.model.Marker> radarMarkers = new ArrayList<>();
    private List<Vehicle> myVehicles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDriverBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(DriverViewModel.class);
        setupObservers();

        setupDrawer();
        
        if (binding.btnMenu != null) {
            binding.btnMenu.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(androidx.core.view.GravityCompat.START);
                }
            });
        }
        
        setupRecyclerView();
        setupClearRouteButton();
        setupWindowInsets();

        viewModel.fetchMyVehicles();
    }

    private void setupWindowInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.btnMenuContainer, (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.topMargin = (int) (16 * getResources().getDisplayMetrics().density) + systemBars.top;
            v.setLayoutParams(params);
            return insets;
        });
    }

    private void setupObservers() {
        viewModel.getPolylineResult().observe(this, encodedPoints -> {
            List<LatLng> path = decodePoly(encodedPoints);
            Polyline polyline = mMap.addPolyline(new PolylineOptions()
                    .addAll(path)
                    .width(12f)
                    .color(getResources().getColor(R.color.primary_dark_blue))
                    .geodesic(true));
            activePolylines.add(polyline);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
        });

        viewModel.getVehicles().observe(this, vehicles -> {
            this.myVehicles = vehicles;
        });

        viewModel.getOfferSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(this, "¡Oferta enviada con éxito!", Toast.LENGTH_LONG).show();
            }
        });
    }
    private void setupClearRouteButton() {
        binding.fabClearRoute.setOnClickListener(v -> {
            clearRouteMarkersAndPolylines();
            binding.fabClearRoute.setVisibility(View.GONE);
        });
    }

    private void clearRouteMarkersAndPolylines() {
        for (Polyline p : activePolylines) p.remove();
        activePolylines.clear();
        
        for (com.google.android.gms.maps.model.Marker m : routeMarkers) m.remove();
        routeMarkers.clear();

        // Volver a mostrar todos los marcadores del radar
        for (com.google.android.gms.maps.model.Marker m : radarMarkers) {
            m.setVisible(true);
        }
    }

    private void setupRecyclerView() {
        AuthResponse.User user = SessionManager.getInstance(this).getUser();
        boolean verified = user != null && user.isVerificado();
        adapter = new RideRequestAdapter(radarRequests, verified, this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        super.onMapReady(googleMap);
        
        mMap.setOnMarkerClickListener(marker -> {
            RideRequest req = (RideRequest) marker.getTag();
            if (req != null) {
                onViewMap(req);
                marker.showInfoWindow();
                return true; // Consumimos el evento para manejarlo nosotros
            }
            return false;
        });

        mMap.setOnInfoWindowClickListener(marker -> {
            RideRequest req = (RideRequest) marker.getTag();
            if (req != null) showRequestDetailsDialog(req);
        });

        fetchRadarData();
    }

    private void showRequestDetailsDialog(RideRequest req) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        builder.setTitle("Detalles de la Solicitud");
        
        StringBuilder msg = new StringBuilder();
        msg.append("👤 Pasajero: ").append(req.getPasajeroNombre()).append("\n");
        msg.append("📏 Distancia: ").append(String.format("%.1f km", req.getDistancia())).append("\n");
        msg.append("👥 Pasajeros: ").append(req.getNumPasajeros()).append("\n");
        
        int stops = (req.getParadas() != null) ? req.getParadas().size() : 0;
        msg.append("📍 Paradas: ").append(stops).append("\n");
        
        if (req.getDescripcion() != null && !req.getDescripcion().isEmpty()) {
            msg.append("\n📝 Notas: ").append(req.getDescripcion()).append("\n");
        }
        
        msg.append("\n💰 Oferta Pasajero: $").append(req.getPrecioOferta());
        
        if (req.isHaRespondido()) {
            msg.append("\n\n✅ Ya has enviado una oferta para este viaje.");
        }

        builder.setMessage(msg.toString());
        builder.setPositiveButton("OFERTAR", (dialog, which) -> onAccept(req));
        builder.setNegativeButton("CERRAR", null);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
        
        if (req.isHaRespondido()) {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        }
    }

    private void fetchRadarData() {
        RetrofitClient.getService(this).getRadarSolicitudes().enqueue(new Callback<ApiResponse<List<RideRequest>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<RideRequest>>> call, Response<ApiResponse<List<RideRequest>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    radarRequests.clear();
                    radarRequests.addAll(response.body().getData());
                    displayMarkers(radarRequests);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<RideRequest>>> call, Throwable t) { }
        });
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
        if (mMap == null) return;
        clearRouteMarkersAndPolylines();

        // Ocultar todos los marcadores del radar excepto el seleccionado
        for (com.google.android.gms.maps.model.Marker m : radarMarkers) {
            RideRequest tag = (RideRequest) m.getTag();
            if (tag != null && !tag.getId().equals(request.getId())) {
                m.setVisible(false);
            }
        }

        LatLng origin = new LatLng(request.getOrigenLat(), request.getOrigenLng());
        LatLng dest = new LatLng(request.getDestinoLat(), request.getDestinoLng());

        com.google.android.gms.maps.model.Marker destMarker = mMap.addMarker(new MarkerOptions()
                .position(dest).title("Destino").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        routeMarkers.add(destMarker);

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boundsBuilder.include(origin);
        boundsBuilder.include(dest);

        List<LatLng> waypoints = new ArrayList<>();
        if (request.getParadas() != null) {
            for (int i = 0; i < request.getParadas().size(); i++) {
                RideRequest.Stop stop = request.getParadas().get(i);
                LatLng stopPos = new LatLng(stop.getLat(), stop.getLng());
                waypoints.add(stopPos);
                com.google.android.gms.maps.model.Marker sMarker = mMap.addMarker(new MarkerOptions()
                        .position(stopPos).title("Parada " + (i + 1)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                routeMarkers.add(sMarker);
                boundsBuilder.include(stopPos);
            }
        }

        viewModel.fetchDirections(origin, dest, waypoints);

        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 150));
        binding.fabClearRoute.setVisibility(View.VISIBLE);
    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0; result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            poly.add(new LatLng((((double) lat / 1E5)), (((double) lng / 1E5))));
        }
        return poly;
    }

    private void displayMarkers(List<RideRequest> requests) {
        if (mMap == null || requests == null) return;
        
        // Limpiar marcadores previos del radar
        for (com.google.android.gms.maps.model.Marker m : radarMarkers) m.remove();
        radarMarkers.clear();

        for (RideRequest req : requests) {
            LatLng pos = new LatLng(req.getOrigenLat(), req.getOrigenLng());
            float color = req.isEsInmediato() ? BitmapDescriptorFactory.HUE_GREEN : BitmapDescriptorFactory.HUE_AZURE;
            
            String snippet = String.format(java.util.Locale.getDefault(), "%.2f Km ...ver más", req.getDistancia());
            
            com.google.android.gms.maps.model.Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(req.getPasajeroNombre())
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(color)));
            
            if (marker != null) {
                marker.setTag(req);
                radarMarkers.add(marker);
            }
        }
    }

    private void setupDriverNavigation() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                Intent intent = new Intent(this, DriverProfileActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                finish();
            } else if (id == R.id.nav_my_vehicles) {
                startActivity(new Intent(this, MyVehiclesActivity.class));
            } else if (id == R.id.nav_logout) {
                SessionManager.getInstance(this).logout(this);
            }
            drawerLayout.closeDrawers();
            return true;
        });
    }
}
