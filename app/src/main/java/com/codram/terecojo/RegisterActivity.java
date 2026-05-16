package com.codram.terecojo;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.codram.terecojo.data.model.ApiResponse;
import com.codram.terecojo.data.model.AuthResponse;
import com.codram.terecojo.data.model.Municipality;
import com.codram.terecojo.data.model.MunicipalityResponse;
import com.codram.terecojo.data.model.Province;
import com.codram.terecojo.data.model.RegisterRequest;
import com.codram.terecojo.data.remote.RetrofitClient;
import com.codram.terecojo.databinding.ActivityRegisterBinding;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.codram.terecojo.utils.ErrorUtils;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private List<Province> provincesList;
    private List<Municipality> municipalitiesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loadProvinces();

        binding.spinnerProvince.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position > 0) {
                    loadMunicipios(provincesList.get(position - 1).getId());
                } else {
                    municipalitiesList = new ArrayList<>();
                    updateMunicipalitySpinner();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        binding.btnRegister.setOnClickListener(v -> onRegisterClicked());
    }

    private void loadProvinces() {
        RetrofitClient.getService().getProvincias().enqueue(new Callback<ApiResponse<List<Province>>>() { // MODIFICADO
            @Override
            public void onResponse(Call<ApiResponse<List<Province>>> call, Response<ApiResponse<List<Province>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isOk()) {
                    provincesList = response.body().getData();
                    updateProvinceSpinner();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Province>>> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateProvinceSpinner() {
        List<String> names = new ArrayList<>();
        names.add("Seleccione una provincia");
        if (provincesList != null) {
            for (Province p : provincesList) {
                names.add(p.getName());
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerProvince.setAdapter(adapter);
    }

    private void loadMunicipios(int provinciaId) {
        RetrofitClient.getService().getMunicipios(provinciaId).enqueue(new Callback<ApiResponse<MunicipalityResponse>>() { // MODIFICADO
            @Override
            public void onResponse(Call<ApiResponse<MunicipalityResponse>> call, Response<ApiResponse<MunicipalityResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isOk()) {
                    municipalitiesList = response.body().getData().getMunicipalities();
                    updateMunicipalitySpinner();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MunicipalityResponse>> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateMunicipalitySpinner() {
        List<String> names = new ArrayList<>();
        names.add("Seleccione un municipio");
        if (municipalitiesList != null) {
            for (Municipality m : municipalitiesList) {
                names.add(m.getName());
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerMunicipality.setAdapter(adapter);
    }

    private void onRegisterClicked() {
        String nombre = binding.etFullName.getText().toString().trim();
        String username = binding.etUsername.getText().toString().trim();
        String telefono = binding.etPhone.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        
        int provIndex = binding.spinnerProvince.getSelectedItemPosition();
        int muniIndex = binding.spinnerMunicipality.getSelectedItemPosition();

        if (nombre.isEmpty() || username.isEmpty() || telefono.isEmpty() || password.isEmpty() || provIndex <= 0 || muniIndex <= 0) {
            Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer provinciaId = provincesList.get(provIndex - 1).getId();
        Integer municipioId = municipalitiesList.get(muniIndex - 1).getId();
        String tipo = binding.btnDriver.isChecked() ? "chofer" : "pasajero";

        binding.btnRegister.setEnabled(false);

        // Obtener Token de FCM antes de registrar
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            String fcmToken = task.isSuccessful() ? task.getResult() : null;
            executeRegister(nombre, username, telefono, password, tipo, provinciaId, municipioId, fcmToken);
        });
    }

    private void executeRegister(String nombre, String username, String telefono, String password, 
                                 String tipo, Integer provinciaId, Integer municipioId, String fcmToken) {
        
        RegisterRequest request = new RegisterRequest(nombre, username, telefono, null, 
                                                      password, tipo, provinciaId, municipioId, fcmToken);

        RetrofitClient.getService().registro(request).enqueue(new Callback<ApiResponse<AuthResponse>>() { // MODIFICADO
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call, Response<ApiResponse<AuthResponse>> response) {
                binding.btnRegister.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isOk()) {
                    Toast.makeText(RegisterActivity.this, "¡Registro exitoso! Ya puedes iniciar sesión.", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    String msg = ErrorUtils.parseError(response, "Error en el registro");
                    Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                binding.btnRegister.setEnabled(true);
                Toast.makeText(RegisterActivity.this, "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
