package com.codram.terecojo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
    private boolean isViewingRoute = false;
    private RideRequest pendingAutoRoute = null; // NUEVO
    private String licenciaEstado = "PENDIENTE";

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
        setupRefreshRadarButton();
        setupWindowInsets();

        processAutoRouteIntent(getIntent()); // NUEVO
        viewModel.fetchMyVehicles();
        cargarEstadoLicencia(); // NUEVO

        // Nueva lógica de suscripción proactiva
        AuthResponse.User user = SessionManager.getInstance(this).getUser();
        if (user != null && "chofer".equalsIgnoreCase(user.getTipo()) && user.getProvincia_id() != null) {
            String topic = "provincia_" + user.getProvincia_id();
            com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic(topic);
            Log.d("FCM", "Suscripción proactiva al tema: " + topic);
        }
    }

    private void processAutoRouteIntent(Intent intent) { // NUEVO
        if (intent != null && intent.getBooleanExtra("AUTO_OPEN_ROUTE", false)) {
            String json = intent.getStringExtra("AUTO_ROUTE_JSON");
            if (json != null) {
                pendingAutoRoute = new com.google.gson.Gson().fromJson(json, RideRequest.class);
            }
        }
    }

    private void triggerAutoRoute(RideRequest request) { // NUEVO
        onViewMap(request);
        for (com.google.android.gms.maps.model.Marker marker : radarMarkers) {
            RideRequest tag = (RideRequest) marker.getTag();
            if (tag != null && tag.getId().equals(request.getId())) {
                marker.showInfoWindow();
                break;
            }
        }
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

        viewModel.getDescartarSuccess().observe(this, solicitudId -> {
            if (solicitudId != null) {
                Toast.makeText(this, "Solicitud descartada", Toast.LENGTH_SHORT).show();
                // 1. Eliminar el marcador del mapa
                com.google.android.gms.maps.model.Marker toRemove = null;
                for (com.google.android.gms.maps.model.Marker m : radarMarkers) {
                    RideRequest tag = (RideRequest) m.getTag();
                    if (tag != null && tag.getId().equals(solicitudId)) {
                        toRemove = m;
                        break;
                    }
                }
                if (toRemove != null) {
                    toRemove.remove();
                    radarMarkers.remove(toRemove);
                }
                // 2. Eliminar de la lista del adapter
                radarRequests.removeIf(r -> r.getId().equals(solicitudId));
                if (adapter != null) adapter.notifyDataSetChanged();
                // 3. Limpiar la ruta si se estaba viendo la ruta de esta solicitud
                clearRouteMarkersAndPolylines();
                binding.fabClearRoute.setVisibility(View.GONE);
            }
        });
    }
    private void setupRefreshRadarButton() {
        binding.fabRefreshRadar.setOnClickListener(v -> {
            fetchRadarData();
            Toast.makeText(this, "Actualizando solicitudes...", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupVisibilidadToggle() {
        // El FAB de visibilidad — se agrega al layout en el Paso 6F
        if (binding.fabVisibilidad == null) return;

        binding.fabVisibilidad.setOnClickListener(v -> {
            if (estaVisible) {
                // Desactivar
                desactivarVisibilidad();
            } else {
                // Activar — pedir ubicación y mostrar dialog de vehículo si aplica
                activarVisibilidad();
            }
        });
    }

    private void activarVisibilidad() {
        // Verificar permiso de ubicación
        if (androidx.core.content.ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Se necesita permiso de ubicación para activarte en el mapa", Toast.LENGTH_LONG).show();
            return;
        }

        // Obtener la última ubicación conocida para enviarla al activarse
        com.google.android.gms.location.FusedLocationProviderClient client =
            com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this);

        try {
            client.getLastLocation().addOnSuccessListener(location -> {
                if (location == null) {
                    Toast.makeText(this, "No pudimos obtener tu ubicación. Asegúrate de tener el GPS activo.", Toast.LENGTH_LONG).show();
                    return;
                }

                double lat = location.getLatitude();
                double lng = location.getLongitude();

                if (myVehicles.isEmpty()) {
                    // Sin vehículos → activar sin vehiculo_id
                    confirmarActivacion(lat, lng, null);
                } else if (myVehicles.size() == 1) {
                    // Un solo vehículo → activar directamente
                    confirmarActivacion(lat, lng, myVehicles.get(0).getId());
                } else {
                    // Varios vehículos → mostrar dialog de selección
                    mostrarDialogSeleccionVehiculo(lat, lng);
                }
            });
        } catch (SecurityException e) {
            Toast.makeText(this, "Error de permisos de ubicación", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarDialogSeleccionVehiculo(double lat, double lng) {
        String[] nombres = myVehicles.stream()
            .map(v -> v.getMarca() + " · " + v.getPlaca()
                + (v.getCapacidadPasajeros() != null ? " · " + v.getCapacidadPasajeros() + " pax" : ""))
            .toArray(String[]::new);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("¿Con qué vehículo sales hoy?")
            .setItems(nombres, (dialog, which) -> {
                String vehiculoId = myVehicles.get(which).getId();
                confirmarActivacion(lat, lng, vehiculoId);
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void confirmarActivacion(double lat, double lng, String vehiculoId) {
        com.codram.terecojo.data.model.ToggleVisibilidadRequest request = new com.codram.terecojo.data.model.ToggleVisibilidadRequest(true, lat, lng, vehiculoId);

        RetrofitClient.getService().toggleVisibilidad(request).enqueue(
            new retrofit2.Callback<ApiResponse<Void>>() {
                @Override
                public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                    if (response.isSuccessful()) {
                        estaVisible = true;
                        actualizarBotonVisibilidad();
                        // Iniciar el ForegroundService
                        Intent serviceIntent = new Intent(DriverActivity.this,
                            com.codram.terecojo.utils.LocationForegroundService.class);
                        startForegroundService(serviceIntent);
                        Toast.makeText(DriverActivity.this,
                            "¡Estás visible! Los pasajeros pueden encontrarte.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(DriverActivity.this,
                            "No se pudo activar la visibilidad. Verifica tu licencia.", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                    Toast.makeText(DriverActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
                }
            }
        );
    }

    private void desactivarVisibilidad() {
        com.codram.terecojo.data.model.ToggleVisibilidadRequest request = new com.codram.terecojo.data.model.ToggleVisibilidadRequest(false);

        RetrofitClient.getService().toggleVisibilidad(request).enqueue(
            new retrofit2.Callback<ApiResponse<Void>>() {
                @Override
                public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                    // Detener el service independientemente de la respuesta
                    Intent serviceIntent = new Intent(DriverActivity.this,
                        com.codram.terecojo.utils.LocationForegroundService.class);
                    stopService(serviceIntent);

                    estaVisible = false;
                    actualizarBotonVisibilidad();
                    Toast.makeText(DriverActivity.this,
                        "Ya no eres visible en el mapa.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                    // Aunque falle la red, detener el service localmente
                    Intent serviceIntent = new Intent(DriverActivity.this,
                        com.codram.terecojo.utils.LocationForegroundService.class);
                    stopService(serviceIntent);
                    estaVisible = false;
                    actualizarBotonVisibilidad();
                }
            }
        );
    }

    private void actualizarBotonVisibilidad() {
        if (binding.fabVisibilidad == null) return;
        if (estaVisible) {
            binding.fabVisibilidad.setImageResource(android.R.drawable.presence_online);
            binding.fabVisibilidad.setBackgroundTintList(
                androidx.core.content.ContextCompat.getColorStateList(this, R.color.success_green));
            binding.fabVisibilidad.setContentDescription("Desactivar visibilidad en el mapa");
        } else {
            binding.fabVisibilidad.setImageResource(android.R.drawable.presence_invisible);
            binding.fabVisibilidad.setBackgroundTintList(
                androidx.core.content.ContextCompat.getColorStateList(this, R.color.gray_dark));
            binding.fabVisibilidad.setContentDescription("Activar visibilidad en el mapa");
        }
    }

    private void registrarVisibilidadReceiver() {
        visibilidadReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, Intent intent) {
                // El service fue detenido remotamente
                estaVisible = false;
                actualizarBotonVisibilidad();
                Toast.makeText(DriverActivity.this,
                    "Tu visibilidad fue desactivada automáticamente.", Toast.LENGTH_LONG).show();
            }
        };

        androidx.localbroadcastmanager.content.LocalBroadcastManager
            .getInstance(this)
            .registerReceiver(visibilidadReceiver,
                new android.content.IntentFilter("com.codram.terecojo.VISIBILIDAD_DESACTIVADA"));
    }

    private void setupClearRouteButton() {
        binding.fabClearRoute.setOnClickListener(v -> {
            clearRouteMarkersAndPolylines();
            binding.fabClearRoute.setVisibility(View.GONE);
        });
    }

    private void clearRouteMarkersAndPolylines() {
        isViewingRoute = false;
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
        adapter = new RideRequestAdapter(radarRequests, verified, licenciaEstado, this);
    }

    private void cargarEstadoLicencia() {
        RetrofitClient.getService().getMiLicencia().enqueue(new Callback<ApiResponse<com.codram.terecojo.data.model.Licencia>>() {
            @Override
            public void onResponse(Call<ApiResponse<com.codram.terecojo.data.model.Licencia>> call, Response<ApiResponse<com.codram.terecojo.data.model.Licencia>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    licenciaEstado = response.body().getData().getEstado();
                    setupRecyclerView(); // Refrescar adapter con el nuevo estado
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<com.codram.terecojo.data.model.Licencia>> call, Throwable t) {
                // Fallback a estado previo
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        super.onMapReady(googleMap);
        
        mMap.setOnMarkerClickListener(marker -> {
            RideRequest req = (RideRequest) marker.getTag();
            if (req != null) {
                // Dibujamos la ruta y ocultamos los demás
                onViewMap(req);
                
                // IMPORTANTE: Después de dibujar la ruta, forzamos que se muestre el InfoWindow 
                // del marcador que acabamos de tocar.
                marker.showInfoWindow();
                return true; 
            }
            return false;
        });

        mMap.setOnInfoWindowClickListener(marker -> {
            RideRequest req = (RideRequest) marker.getTag();
            if (req != null) showRequestDetailsDialog(req);
        });

        fetchRadarData();

        // Verificar si venimos de "Ver Ruta" desde la otra actividad
        checkIncomingRequest(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap != null) fetchRadarData();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        
        // Si es un clic desde el menú (sin extras de ruta), limpiamos la vista de ruta
        if (!intent.hasExtra("EXTRA_RIDE_REQUEST") && !intent.getBooleanExtra("AUTO_OPEN_ROUTE", false)) {
            clearRouteMarkersAndPolylines();
            binding.fabClearRoute.setVisibility(View.GONE);
        }

        checkIncomingRequest(intent);
        processAutoRouteIntent(intent); // NUEVO
        if (mMap != null) fetchRadarData();
    }

    private void checkIncomingRequest(Intent intent) {
        if (intent == null) return;
        
        if (intent.hasExtra("EXTRA_RIDE_REQUEST")) {
            RideRequest request = (RideRequest) intent.getSerializableExtra("EXTRA_RIDE_REQUEST");
            if (request != null) {
                if (mMap != null) {
                    processIncomingRoute(request);
                } else {
                    // Reintentar brevemente si el mapa aún no está listo
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        if (mMap != null) processIncomingRoute(request);
                        else Toast.makeText(this, "Error: Mapa no listo para dibujar ruta", Toast.LENGTH_LONG).show();
                    }, 1200);
                }
            } else {
                Toast.makeText(this, "Error: Datos de ruta corruptos", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void processIncomingRoute(RideRequest request) {
        // 1. Dibujamos la ruta (esto oculta los demás marcadores y activa el botón X)
        onViewMap(request);

        // 2. Buscamos el marcador del radar para este viaje para mostrar su InfoWindow
        com.google.android.gms.maps.model.Marker targetMarker = null;
        for (com.google.android.gms.maps.model.Marker m : radarMarkers) {
            RideRequest tag = (RideRequest) m.getTag();
            if (tag != null && tag.getId().equals(request.getId())) {
                targetMarker = m;
                break;
            }
        }

        // 3. Si no existe (porque el radar no ha cargado), creamos uno temporal para mostrar la info
        if (targetMarker == null) {
            LatLng pos = new LatLng(request.getOrigenLat(), request.getOrigenLng());
            targetMarker = mMap.addMarker(new com.google.android.gms.maps.model.MarkerOptions()
                    .position(pos)
                    .title(request.getPasajeroNombre())
                    .snippet(String.format(java.util.Locale.getDefault(), "%.2f Km ...ver más", request.getDistancia()))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            if (targetMarker != null) {
                targetMarker.setTag(request);
                radarMarkers.add(targetMarker);
            }
        }

        // 4. Forzamos mostrar el panel de información
        if (targetMarker != null) {
            targetMarker.setVisible(true);
            targetMarker.showInfoWindow();
        }
    }

    // Nuevo método — implementa la acción de descartar desde la lista
    public void onDiscard(RideRequest request) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Descartar solicitud")
            .setMessage("No volverás a ver esta solicitud en el radar. ¿Confirmas?")
            .setPositiveButton("DESCARTAR", (dialog, which) -> {
                viewModel.descartarSolicitud(request.getId());
            })
            .setNegativeButton("CANCELAR", null)
            .show();
    }

    private void showRequestDetailsDialog(RideRequest req) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
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

        // Lógica de habilitación según licencia
        boolean tieneLicenciaActiva = "TRIAL_ACTIVO".equals(licenciaEstado) || "ACTIVO".equals(licenciaEstado);

        if (!tieneLicenciaActiva) {
            msg.append("\n\n⚠️ No tienes una licencia activa para realizar ofertas.");
            builder.setMessage(msg.toString()); // Actualizar mensaje con la advertencia
            builder.setPositiveButton("SIN LICENCIA", null);
        } else {
            // Botón principal: Ofertar
            builder.setPositiveButton("OFERTAR", (dialog, which) -> onAccept(req));
        }

        // Botón neutral: No me interesa (solo si no ha ofertado ya y tiene licencia activa)
        if (!req.isHaRespondido() && tieneLicenciaActiva) {
            builder.setNeutralButton("NO ME INTERESA", (dialog, which) -> onDiscard(req));
        }

        // Botón negativo: Cerrar
        builder.setNegativeButton("CERRAR", null);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();

        // Deshabilitar Ofertar si ya respondió o si no tiene licencia
        if (req.isHaRespondido() || !tieneLicenciaActiva) {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        }

        // Colorear el botón "No me interesa" en gris para diferenciarlo visualmente
        android.widget.Button btnNeutral = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL);
        if (btnNeutral != null) {
            btnNeutral.setTextColor(
                androidx.core.content.ContextCompat.getColor(this, R.color.gray_dark)
            );
        }
    }

    private void fetchRadarData() {
        RetrofitClient.getService().getRadarSolicitudes().enqueue(new Callback<ApiResponse<List<RideRequest>>>() { // MODIFICADO
            @Override
            public void onResponse(Call<ApiResponse<List<RideRequest>>> call, Response<ApiResponse<List<RideRequest>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    radarRequests.clear();
                    radarRequests.addAll(response.body().getData());
                    displayMarkers(radarRequests);
                    
                    if (adapter != null) adapter.notifyDataSetChanged();

                    if (pendingAutoRoute != null) { // NUEVO
                        triggerAutoRoute(pendingAutoRoute);
                        pendingAutoRoute = null;
                    }
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

        // Lógica de licencia
        boolean tieneLicenciaActiva = "TRIAL_ACTIVO".equals(licenciaEstado) || "ACTIVO".equals(licenciaEstado);
        if (!tieneLicenciaActiva) {
            Toast.makeText(this, "No tienes una licencia activa para operar", Toast.LENGTH_LONG).show();
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
    protected boolean shouldAutoCenterAtStart() {
        Intent intent = getIntent();
        if (intent == null) return true;
        
        // No centrar si venimos a ver una ruta específica
        boolean hasIncomingRequest = intent.hasExtra("EXTRA_RIDE_REQUEST");
        boolean hasAutoRoute = intent.getBooleanExtra("AUTO_OPEN_ROUTE", false);
        
        return !hasIncomingRequest && !hasAutoRoute;
    }

    public void onViewMap(RideRequest request) {
        if (mMap == null) return;
        
        isViewingRoute = true;
        clearRouteMarkersAndPolylines();

        // Ocultar marcadores del radar (menos el que se seleccionó si se llamó desde el clic)
        for (com.google.android.gms.maps.model.Marker m : radarMarkers) {
            RideRequest tag = (RideRequest) m.getTag();
            if (tag != null && tag.getId().equals(request.getId())) {
                m.setVisible(true); // Mantener visible el seleccionado
            } else {
                m.setVisible(false);
            }
        }

        LatLng origin = new LatLng(request.getOrigenLat(), request.getOrigenLng());
        LatLng dest = new LatLng(request.getDestinoLat(), request.getDestinoLng());

        // Marcadores de la ruta (Origen/Destino/Paradas)
        // Usamos azul para origen y rojo para destino
        com.google.android.gms.maps.model.Marker originMarker = mMap.addMarker(new MarkerOptions()
                .position(origin).title("Origen").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        routeMarkers.add(originMarker);

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

        try {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 200));
        } catch (Exception e) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(origin, 14f));
        }
        
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
                    .visible(!isViewingRoute) // Solo visible si no estamos viendo una ruta específica
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
