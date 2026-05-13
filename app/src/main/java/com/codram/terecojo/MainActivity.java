package com.codram.terecojo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import android.widget.EditText;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.codram.terecojo.databinding.ActivityMainBinding;
import com.codram.terecojo.data.model.ApiResponse;
import com.codram.terecojo.data.model.Province;
import com.codram.terecojo.data.model.Municipality;
import com.codram.terecojo.data.model.MunicipalityResponse;
import com.codram.terecojo.data.model.RideRequest;
import android.widget.ArrayAdapter;
import com.codram.terecojo.data.remote.RetrofitClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.codram.terecojo.data.model.AuthResponse;
import com.codram.terecojo.utils.SessionManager;
import com.codram.terecojo.data.model.SelectionMode;
import com.codram.terecojo.ui.viewmodel.MainViewModel;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.gms.common.api.Status;
import android.content.Intent;

import androidx.lifecycle.ViewModelProvider;

public class MainActivity extends BaseActivity {
    private static final int AUTOCOMPLETE_REQUEST_CODE = 1001;
    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private List<View> stopViews = new ArrayList<>();
    
    // Puntos de ubicación reales
    private LatLng originPoint;
    private LatLng destinationPoint;
    private List<LatLng> stopPoints = new ArrayList<>();

    // Componentes de ubicación para el backend
    private String originProvince;
    private String originMunicipality;
    private String destProvince;
    private String destMunicipality;
    
    // Estado de selección
    private SelectionMode currentSelectionMode = SelectionMode.NONE;
    private int currentlyEditingStopIndex = -1;

    // Marcadores
    private Marker originMarker;
    private Marker destinationMarker;
    private List<Marker> stopMarkers = new ArrayList<>();
    
    private double lastCalculatedDistance = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        setupObservers();

        setupDrawer();
        setupBottomSheet();
        setupStopLogic();
        setupCurrencySpinner();
        setupMapInteractions();
        setupWindowInsets();

        // Configurar clic en el botón de menú flotante
        binding.btnMenu.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(androidx.core.view.GravityCompat.START);
            }
        });

        // Inicializar Google Places
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyBufiSwuBW19JLsbXKDbW86pg_1wL7ifxU");
        }
    }

    private void setupObservers() {
        viewModel.getGeocodeResult().observe(this, result -> {
            applyGeocodedAddress(result.details, result.point, result.mode, result.stopIndex);
        });

        viewModel.getDistanceResult().observe(this, distance -> {
            lastCalculatedDistance = distance;
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            // Se maneja dentro del diálogo ahora
        });

        viewModel.getErrorMessage().observe(this, error -> {
            Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.btnMenuContainer, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.topMargin = (int) (16 * getResources().getDisplayMetrics().density) + systemBars.top;
            v.setLayoutParams(params);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.fabMyLocation, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.rightMargin = (int) (16 * getResources().getDisplayMetrics().density) + systemBars.right;
            // Reseteamos bottomMargin ya que ahora se posiciona respecto al ancla
            params.bottomMargin = (int) (16 * getResources().getDisplayMetrics().density);
            v.setLayoutParams(params);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.fabConfirmLocation, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
            int baseMargin = (int) (172 * getResources().getDisplayMetrics().density);
            params.bottomMargin = baseMargin + systemBars.bottom;
            params.rightMargin = (int) (16 * getResources().getDisplayMetrics().density) + systemBars.right;
            v.setLayoutParams(params);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomSheet, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.searchBarContainer, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.topMargin = (int) (16 * getResources().getDisplayMetrics().density) + systemBars.top;
            v.setLayoutParams(params);
            return insets;
        });
    }

    private void setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        
        binding.btnPublishRequest.setOnClickListener(v -> {
            if (originPoint == null || destinationPoint == null) {
                Toast.makeText(this, "Selecciona origen y destino primero", Toast.LENGTH_SHORT).show();
                return;
            }
            calculateRoadDistance();
            showRideDetailsDialog();
        });

        binding.btnCancelRequest.setOnClickListener(v -> {
            clearFields();
            clearMarkers();
            Toast.makeText(this, "Ruta cancelada", Toast.LENGTH_SHORT).show();
        });
    }

    private void showRideDetailsDialog() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_ride_details, null);
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        builder.setView(dialogView);
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        android.widget.TextView tvDistance = dialogView.findViewById(R.id.tvDialogDistance);
        android.widget.ProgressBar pbDistance = dialogView.findViewById(R.id.pbDialogDistance);

        // Iniciar cálculo automático de distancia
        lastCalculatedDistance = 0;
        tvDistance.setText("Calculando distancia...");
        pbDistance.setVisibility(View.VISIBLE);
        
        viewModel.getDistanceResult().observe(this, new androidx.lifecycle.Observer<Double>() {
            @Override
            public void onChanged(Double distance) {
                viewModel.getDistanceResult().removeObserver(this);
                lastCalculatedDistance = distance;
                tvDistance.setText(getString(R.string.label_distance_format, distance));
                pbDistance.setVisibility(View.GONE);
            }
        });
        calculateRoadDistance();

        android.widget.RadioGroup rgDateTime = dialogView.findViewById(R.id.rgDateTime);
        com.google.android.material.button.MaterialButton btnPickDateTime = dialogView.findViewById(R.id.btnPickDateTime);
        
        final java.util.Calendar scheduledCalendar = java.util.Calendar.getInstance();
        
        rgDateTime.setOnCheckedChangeListener((group, checkedId) -> {
            btnPickDateTime.setVisibility(checkedId == R.id.rbSchedule ? View.VISIBLE : View.GONE);
        });

        btnPickDateTime.setOnClickListener(v -> {
            java.util.Calendar current = java.util.Calendar.getInstance();
            new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                scheduledCalendar.set(java.util.Calendar.YEAR, year);
                scheduledCalendar.set(java.util.Calendar.MONTH, month);
                scheduledCalendar.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth);

                new android.app.TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                    scheduledCalendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay);
                    scheduledCalendar.set(java.util.Calendar.MINUTE, minute);
                    scheduledCalendar.set(java.util.Calendar.SECOND, 0);

                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
                    btnPickDateTime.setText(sdf.format(scheduledCalendar.getTime()));
                }, current.get(java.util.Calendar.HOUR_OF_DAY), current.get(java.util.Calendar.MINUTE), true).show();

            }, current.get(java.util.Calendar.YEAR), current.get(java.util.Calendar.MONTH), current.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

        dialogView.findViewById(R.id.btnFinalPublish).setOnClickListener(v -> {
            EditText etPassengers = dialogView.findViewById(R.id.etPassengers);
            EditText etCargo = dialogView.findViewById(R.id.etCargoType);
            EditText etObs = dialogView.findViewById(R.id.etObservations);
            com.google.android.material.chip.ChipGroup cgCurrencies = dialogView.findViewById(R.id.cgCurrencies);

            String passengers = etPassengers.getText().toString();
            String cargo = etCargo.getText().toString();
            String obs = etObs.getText().toString();
            boolean isInmediato = rgDateTime.getCheckedRadioButtonId() == R.id.rbNow;
            String fechaViaje = null;

            if (!isInmediato) {
                java.text.SimpleDateFormat isoFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US);
                isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                fechaViaje = isoFormat.format(scheduledCalendar.getTime());
            }

            List<String> selectedCurrencies = new ArrayList<>();
            for (int i = 0; i < cgCurrencies.getChildCount(); i++) {
                com.google.android.material.chip.Chip chip = (com.google.android.material.chip.Chip) cgCurrencies.getChildAt(i);
                if (chip.isChecked()) {
                    selectedCurrencies.add(chip.getText().toString().toUpperCase());
                }
            }

            if (selectedCurrencies.isEmpty()) {
                Toast.makeText(this, "Selecciona al menos una moneda", Toast.LENGTH_SHORT).show();
                return;
            }

            executeFinalPublish(passengers, cargo, obs, selectedCurrencies, isInmediato, fechaViaje, dialog);
        });

        dialog.show();
    }

    private void executeFinalPublish(String passengers, String cargo, String obs, List<String> currencies, boolean isInmediato, String fechaViaje, androidx.appcompat.app.AlertDialog dialog) {
        if (lastCalculatedDistance <= 0) {
            Toast.makeText(this, "Calculando ruta final...", Toast.LENGTH_SHORT).show();
            calculateRoadDistance(() -> performApiPublish(passengers, cargo, obs, currencies, isInmediato, fechaViaje, dialog));
        } else {
            performApiPublish(passengers, cargo, obs, currencies, isInmediato, fechaViaje, dialog);
        }
    }

    private void performApiPublish(String passengers, String cargo, String obs, List<String> currencies, boolean isInmediato, String fechaViaje, androidx.appcompat.app.AlertDialog dialog) {
        if (originMunicipality == null || originMunicipality.isEmpty() || originProvince == null || originProvince.isEmpty()) {
            showManualLocationDialog(passengers, cargo, obs, currencies, isInmediato, fechaViaje, dialog);
            return;
        }
        
        sendSolicitudToApi(passengers, cargo, obs, currencies, isInmediato, fechaViaje, dialog);
    }

    private void showManualLocationDialog(String passengers, String cargo, String obs, List<String> currencies, boolean isInmediato, String fechaViaje, androidx.appcompat.app.AlertDialog dialog) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        android.widget.TextView tvTitle = new android.widget.TextView(this);
        tvTitle.setText("No detectamos tu zona");
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 24);
        layout.addView(tvTitle);

        android.widget.TextView tvDesc = new android.widget.TextView(this);
        tvDesc.setText("Por favor, selecciona tu ubicación manualmente para que los choferes cercanos puedan recibir la notificación:");
        tvDesc.setPadding(0, 0, 0, 32);
        layout.addView(tvDesc);

        android.widget.Spinner spinnerProv = new android.widget.Spinner(this);
        android.widget.Spinner spinnerMun = new android.widget.Spinner(this);
        layout.addView(spinnerProv);
        layout.addView(spinnerMun);

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        builder.setView(layout);
        builder.setCancelable(false);
        
        final List<Province>[] provinces = new List[]{new ArrayList<>()};
        final List<Municipality>[] municipalities = new List[]{new ArrayList<>()};

        // Cargar Provincias
        RetrofitClient.getService(this).getProvincias().enqueue(new Callback<ApiResponse<List<Province>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Province>>> call, Response<ApiResponse<List<Province>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    provinces[0] = response.body().getData();
                    List<String> names = new ArrayList<>();
                    names.add("Seleccione Provincia");
                    for (Province p : provinces[0]) names.add(p.getName());
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, names);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerProv.setAdapter(adapter);
                }
            }
            @Override public void onFailure(Call<ApiResponse<List<Province>>> call, Throwable t) {}
        });

        spinnerProv.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    int provId = provinces[0].get(position - 1).getId();
                    RetrofitClient.getService(MainActivity.this).getMunicipios(provId).enqueue(new Callback<ApiResponse<MunicipalityResponse>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<MunicipalityResponse>> call, Response<ApiResponse<MunicipalityResponse>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                municipalities[0] = response.body().getData().getMunicipalities();
                                List<String> mNames = new ArrayList<>();
                                mNames.add("Seleccione Municipio");
                                for (Municipality m : municipalities[0]) mNames.add(m.getName());
                                ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, mNames);
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                spinnerMun.setAdapter(adapter);
                            }
                        }
                        @Override public void onFailure(Call<ApiResponse<MunicipalityResponse>> call, Throwable t) {}
                    });
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        builder.setPositiveButton("Publicar", (d, which) -> {
            int provIdx = spinnerProv.getSelectedItemPosition();
            int munIdx = spinnerMun.getSelectedItemPosition();
            if (provIdx > 0 && munIdx > 0) {
                originProvince = provinces[0].get(provIdx - 1).getName();
                originMunicipality = municipalities[0].get(munIdx - 1).getName();
                // Opcional: también podríamos setear el ID directamente para ahorrar trabajo al backend
                sendSolicitudToApi(passengers, cargo, obs, currencies, isInmediato, fechaViaje, dialog);
            } else {
                Toast.makeText(this, "Debes seleccionar tu ubicación", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (d, which) -> d.dismiss());
        builder.show();
    }

    private void sendSolicitudToApi(String passengers, String cargo, String obs, List<String> currencies, boolean isInmediato, String fechaViaje, androidx.appcompat.app.AlertDialog dialog) {
        String originDesc = binding.etOrigin.getText().toString().trim();
        String destinationDesc = binding.etDestination.getText().toString().trim();

        RideRequest request = new RideRequest();
        request.setOrigenDescripcion(originDesc);
        request.setOrigenLat(originPoint.latitude);
        request.setOrigenLng(originPoint.longitude);
        request.setDestinoDescripcion(destinationDesc);
        request.setDestinoLat(destinationPoint.latitude);
        request.setDestinoLng(destinationPoint.longitude);
        
        try {
            request.setNumPasajeros(Integer.parseInt(passengers));
        } catch (Exception e) {
            request.setNumPasajeros(1);
        }
        request.setTipoCarga(cargo);
        request.setDescripcion(obs);
        request.setMoneda(currencies);
        request.setPrecioOferta(0.0);
        request.setEsInmediato(isInmediato);
        request.setFechaViaje(fechaViaje);
        request.setDistancia(lastCalculatedDistance);

        request.setOrigenProvinciaNombre(originProvince);
        request.setOrigenMunicipioNombre(originMunicipality);
        request.setDestinoProvinciaNombre(destProvince);
        request.setDestinoMunicipioNombre(destMunicipality);

        // Por ahora usamos el municipio del usuario como origen para las notificaciones
        AuthResponse.User currentUser = SessionManager.getInstance(this).getUser();
        if (currentUser != null && currentUser.getMunicipio_id() != null) {
            request.setOrigenMunicipioId(currentUser.getMunicipio_id());
        }

        List<RideRequest.Stop> paradas = new ArrayList<>();
        for (int i = 0; i < stopViews.size(); i++) {
            if (i < stopPoints.size() && stopPoints.get(i) != null) {
                EditText etStop = stopViews.get(i).findViewById(R.id.etStopName);
                String name = etStop.getText().toString().trim();
                LatLng gp = stopPoints.get(i);
                paradas.add(new RideRequest.Stop(name, gp.latitude, gp.longitude));
            }
        }
        request.setParadas(paradas);

        RetrofitClient.getService(this).createSolicitud(request).enqueue(new Callback<ApiResponse<RideRequest>>() {
            @Override
            public void onResponse(Call<ApiResponse<RideRequest>> call, Response<ApiResponse<RideRequest>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "¡Viaje publicado con éxito!", Toast.LENGTH_LONG).show();
                    if (dialog != null) dialog.dismiss();
                    clearFields();
                    clearMarkers();
                } else {
                    Toast.makeText(MainActivity.this, "Error al publicar: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<RideRequest>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupCurrencySpinner() { }

    private void setupMapInteractions() {
        binding.etOrigin.setOnClickListener(v -> startMapSelection(SelectionMode.ORIGIN, -1));
        binding.etDestination.setOnClickListener(v -> startMapSelection(SelectionMode.DESTINATION, -1));

        binding.tilOrigin.setEndIconOnClickListener(v -> 
            Toast.makeText(this, "Favoritos En próximas versiones", Toast.LENGTH_SHORT).show()
        );
        binding.tilDestination.setEndIconOnClickListener(v -> 
            Toast.makeText(this, "Favoritos En próximas versiones", Toast.LENGTH_SHORT).show()
        );

        binding.tvSearchBar.setOnClickListener(v -> {
            List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS);
            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                    .setCountry("CU")
                    .build(this);
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                if (place.getLatLng() != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 17f));
                }
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR && data != null) {
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.e("Places", status.getStatusMessage());
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        super.onMapReady(googleMap);
        
        mMap.setOnCameraMoveStartedListener(reason -> {
            if (currentSelectionMode != SelectionMode.NONE) {
                binding.ivCentralPin.animate().translationY(-20f).setDuration(200).start();
            }
        });

        mMap.setOnCameraIdleListener(() -> {
            if (currentSelectionMode != SelectionMode.NONE) {
                binding.ivCentralPin.animate().translationY(0f).setDuration(200).start();
                LatLng center = mMap.getCameraPosition().target;
                updateAddressField(getString(R.string.msg_geocoding), currentSelectionMode, currentlyEditingStopIndex);
                viewModel.reverseGeocode(center, currentSelectionMode, currentlyEditingStopIndex);
            }
        });
    }

    private void updateAddressField(String text, SelectionMode mode, int stopIndex) {
        switch (mode) {
            case ORIGIN: binding.etOrigin.setText(text); break;
            case DESTINATION: binding.etDestination.setText(text); break;
            case STOP:
                if (stopIndex != -1 && stopIndex < stopViews.size()) {
                    ((EditText) stopViews.get(stopIndex).findViewById(R.id.etStopName)).setText(text);
                }
                break;
        }
    }

    private void applyGeocodedAddress(com.codram.terecojo.data.model.LocationDetails details, LatLng point, SelectionMode mode, int stopIndex) {
        String address = details.getFormattedAddress();
        Log.d("Geocoder", "Aplicando dirección a " + mode + ": " + address);
        switch (mode) {
            case ORIGIN:
                originPoint = point;
                originProvince = details.getProvince();
                originMunicipality = details.getMunicipality();
                binding.etOrigin.setText(address);
                break;
            case DESTINATION:
                destinationPoint = point;
                destProvince = details.getProvince();
                destMunicipality = details.getMunicipality();
                binding.etDestination.setText(address);
                break;
            case STOP:
                if (stopIndex != -1) {
                    while (stopPoints.size() <= stopIndex) stopPoints.add(null);
                    stopPoints.set(stopIndex, point);
                    if (stopIndex < stopViews.size()) {
                        View stopView = stopViews.get(stopIndex);
                        EditText etStop = stopView.findViewById(R.id.etStopName);
                        etStop.setText(address);
                    }
                }
                break;
        }
    }

    private void startMapSelection(SelectionMode mode, int stopIndex) {
        currentSelectionMode = mode;
        currentlyEditingStopIndex = stopIndex;
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        binding.ivCentralPin.setVisibility(View.VISIBLE);
        binding.fabConfirmLocation.setVisibility(View.VISIBLE);
        binding.searchBarContainer.setVisibility(View.VISIBLE);
        binding.fabConfirmLocation.setOnClickListener(v -> confirmLocationSelection());
        binding.containerPublishButtons.setVisibility(View.GONE);
        Toast.makeText(this, R.string.msg_selecting_location, Toast.LENGTH_SHORT).show();
    }

    private void confirmLocationSelection() {
        if (mMap == null) return;
        LatLng center = mMap.getCameraPosition().target;
        SelectionMode finalizedMode = currentSelectionMode;
        int finalizedIndex = currentlyEditingStopIndex;
        updateAddressField(getString(R.string.msg_geocoding), finalizedMode, finalizedIndex);
        viewModel.reverseGeocode(center, finalizedMode, finalizedIndex);
        updateMarker(finalizedMode, center, finalizedIndex);
        binding.ivCentralPin.setVisibility(View.GONE);
        binding.fabConfirmLocation.setVisibility(View.GONE);
        binding.searchBarContainer.setVisibility(View.GONE);
        binding.containerPublishButtons.setVisibility(View.VISIBLE);
        currentSelectionMode = SelectionMode.NONE;
        currentlyEditingStopIndex = -1;
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void calculateRoadDistance() {
        if (originPoint == null || destinationPoint == null) return;
        viewModel.calculateDistance(originPoint, destinationPoint, stopPoints);
    }

    private void calculateRoadDistance(Runnable onFinished) {
        if (originPoint == null || destinationPoint == null) {
            if (onFinished != null) onFinished.run();
            return;
        }
        viewModel.getDistanceResult().observe(this, new androidx.lifecycle.Observer<Double>() {
            @Override
            public void onChanged(Double d) {
                viewModel.getDistanceResult().removeObserver(this);
                if (onFinished != null) onFinished.run();
            }
        });
        viewModel.calculateDistance(originPoint, destinationPoint, stopPoints);
    }

    @Override
    public void onBackPressed() {
        if (currentSelectionMode != SelectionMode.NONE) {
            confirmLocationSelection();
            return;
        }
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            return;
        }
        super.onBackPressed();
    }

    private void updateMarker(SelectionMode mode, LatLng p, int index) {
        if (mMap == null) return;
        switch (mode) {
            case ORIGIN:
                if (originMarker != null) originMarker.remove();
                originMarker = mMap.addMarker(new MarkerOptions().position(p).title("Origen")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                break;
            case DESTINATION:
                if (destinationMarker != null) destinationMarker.remove();
                destinationMarker = mMap.addMarker(new MarkerOptions().position(p).title("Destino")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                break;
            case STOP:
                while (stopMarkers.size() <= index) stopMarkers.add(null);
                if (stopMarkers.get(index) != null) stopMarkers.get(index).remove();
                Marker marker = mMap.addMarker(new MarkerOptions().position(p).title("Parada " + (index + 1))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                stopMarkers.set(index, marker);
                break;
        }
    }

    private void clearFields() {
        binding.etOrigin.setText("Seleccionar punto de recogida");
        binding.etDestination.setText("");
        lastCalculatedDistance = 0;
        binding.containerStops.removeAllViews();
        stopViews.clear();
        updateAddStopButtonVisibility();
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        binding.etOrigin.clearFocus();
        binding.etDestination.clearFocus();
        binding.containerPublishButtons.setVisibility(View.VISIBLE);
        binding.fabConfirmLocation.setVisibility(View.GONE);
    }

    private void clearMarkers() {
        if (originMarker != null) originMarker.remove();
        if (destinationMarker != null) destinationMarker.remove();
        for (Marker m : stopMarkers) if (m != null) m.remove();
        originMarker = null; destinationMarker = null;
        stopMarkers.clear(); stopPoints.clear();
        originPoint = null; destinationPoint = null;
    }

    private void setupStopLogic() {
        binding.btnAddStop.setOnClickListener(v -> addStopField());
    }

    private void addStopField() {
        if (stopViews.size() >= 3) return;
        View stopView = LayoutInflater.from(this).inflate(R.layout.item_stop_input, binding.containerStops, false);
        int index = stopViews.size();
        
        com.google.android.material.textfield.TextInputLayout tilStop = stopView.findViewById(R.id.tilStop);
        EditText etStop = stopView.findViewById(R.id.etStopName);
        
        etStop.setOnClickListener(v -> startMapSelection(SelectionMode.STOP, index));
        
        tilStop.setEndIconOnClickListener(v -> 
            Toast.makeText(this, "Favoritos En próximas versiones", Toast.LENGTH_SHORT).show()
        );

        stopView.findViewById(R.id.btnRemoveStop).setOnClickListener(v -> {
            int pos = stopViews.indexOf(stopView);
            if (pos != -1) {
                binding.containerStops.removeView(stopView);
                if (pos < stopMarkers.size() && stopMarkers.get(pos) != null) stopMarkers.get(pos).remove();
                if (pos < stopPoints.size()) stopPoints.remove(pos);
                if (pos < stopMarkers.size()) stopMarkers.remove(pos);
                stopViews.remove(pos);
            }
            updateAddStopButtonVisibility();
        });
        binding.containerStops.addView(stopView);
        stopViews.add(stopView);
        updateAddStopButtonVisibility();
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void updateAddStopButtonVisibility() {
        binding.btnAddStop.setVisibility(stopViews.size() >= 3 ? View.GONE : View.VISIBLE);
    }
}
