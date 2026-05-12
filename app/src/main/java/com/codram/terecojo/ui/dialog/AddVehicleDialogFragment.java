package com.codram.terecojo.ui.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.bumptech.glide.Glide;
import com.codram.terecojo.R;
import com.codram.terecojo.data.model.ApiResponse;
import com.codram.terecojo.data.model.Vehicle;
import com.codram.terecojo.data.remote.ApiService;
import com.codram.terecojo.data.remote.RetrofitClient;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddVehicleDialogFragment extends DialogFragment {
    private Spinner spinnerVehicleType;
    private EditText etBrand, etPlate, etCapacity;
    private ImageView ivVehiclePhoto;
    private List<String> vehicleTypes = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private Uri selectedImageUri;
    private Vehicle vehicleToEdit;
    private boolean isEditMode = false;

    public interface OnVehicleAddedListener {
        void onVehicleAdded();
    }

    private OnVehicleAddedListener listener;

    public void setOnVehicleAddedListener(OnVehicleAddedListener listener) {
        this.listener = listener;
    }

    public static AddVehicleDialogFragment newInstance(Vehicle vehicle) {
        AddVehicleDialogFragment fragment = new AddVehicleDialogFragment();
        fragment.vehicleToEdit = vehicle;
        fragment.isEditMode = vehicle != null;
        return fragment;
    }

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    ivVehiclePhoto.setPadding(0, 0, 0, 0);
                    ivVehiclePhoto.setImageTintList(null);
                    Glide.with(this).load(selectedImageUri).centerCrop().into(ivVehiclePhoto);
                }
            }
    );

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_vehicle, null);
        
        TextView tvTitle = view.findViewById(R.id.tvTitle); // Assuming we added an ID to the title TextView
        etBrand = view.findViewById(R.id.etBrand);
        etPlate = view.findViewById(R.id.etPlate);
        etCapacity = view.findViewById(R.id.etCapacity);
        ivVehiclePhoto = view.findViewById(R.id.ivVehiclePhoto);
        spinnerVehicleType = view.findViewById(R.id.spinnerVehicleType);

        if (tvTitle == null) {
             // If we didn't have ID, let's find it by position or similar, but better add ID to layout
        }
        
        setupSpinner();
        loadVehicleTypes();

        if (isEditMode && vehicleToEdit != null) {
            if (tvTitle != null) tvTitle.setText("Editar Automóvil");
            etBrand.setText(vehicleToEdit.getMarca());
            etPlate.setText(vehicleToEdit.getPlaca());
            etCapacity.setText(String.valueOf(vehicleToEdit.getCapacidadPasajeros()));
            
            if (vehicleToEdit.getFotoUrl() != null) {
                ivVehiclePhoto.setPadding(0, 0, 0, 0);
                ivVehiclePhoto.setImageTintList(null);
                Glide.with(this).load(vehicleToEdit.getFotoUrl()).centerCrop().into(ivVehiclePhoto);
            }
        }

        view.findViewById(R.id.cardVehiclePhoto).setOnClickListener(v -> pickImage());

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(view);

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btnSave).setOnClickListener(v -> validateAndSave());

        return builder.create();
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void setupSpinner() {
        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, vehicleTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVehicleType.setAdapter(adapter);
    }

    private void loadVehicleTypes() {
        ApiService apiService = RetrofitClient.getService(requireContext());
        apiService.getVehicleTypes().enqueue(new Callback<ApiResponse<List<String>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<String>>> call, Response<ApiResponse<List<String>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    vehicleTypes.clear();
                    vehicleTypes.addAll(response.body().getData());
                    adapter.notifyDataSetChanged();
                    
                    if (isEditMode && vehicleToEdit != null) {
                        int pos = vehicleTypes.indexOf(vehicleToEdit.getTipo());
                        if (pos >= 0) spinnerVehicleType.setSelection(pos);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<String>>> call, Throwable t) {
                Toast.makeText(getContext(), "Error de red al cargar tipos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void validateAndSave() {
        String marca = etBrand.getText().toString().trim();
        String placa = etPlate.getText().toString().trim();
        String capacidadStr = etCapacity.getText().toString().trim();
        String tipo = (spinnerVehicleType.getSelectedItem() != null) ? spinnerVehicleType.getSelectedItem().toString() : "";

        if (marca.isEmpty() || placa.isEmpty() || capacidadStr.isEmpty() || tipo.isEmpty()) {
            Toast.makeText(getContext(), "Por favor rellena todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isEditMode && selectedImageUri == null) {
            Toast.makeText(getContext(), "Por favor selecciona una foto", Toast.LENGTH_SHORT).show();
            return;
        }

        saveVehicle(marca, placa, tipo, capacidadStr);
    }

    private void saveVehicle(String marca, String placa, String tipo, String capacidad) {
        MultipartBody.Part body = null;
        if (selectedImageUri != null) {
            File file = createTmpFileFromUri(selectedImageUri);
            if (file != null) {
                RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
                body = MultipartBody.Part.createFormData("foto", file.getName(), requestFile);
            }
        }

        RequestBody rbMarca = RequestBody.create(MediaType.parse("text/plain"), marca);
        RequestBody rbPlaca = RequestBody.create(MediaType.parse("text/plain"), placa);
        RequestBody rbTipo = RequestBody.create(MediaType.parse("text/plain"), tipo);
        RequestBody rbCapacidad = RequestBody.create(MediaType.parse("text/plain"), capacidad);

        ApiService apiService = RetrofitClient.getService(requireContext());
        Call<ApiResponse<Vehicle>> call;
        
        if (isEditMode) {
            call = apiService.updateVehicle(vehicleToEdit.getId(), rbMarca, rbPlaca, rbTipo, rbCapacidad, body);
        } else {
            call = apiService.addVehicle(rbMarca, rbPlaca, rbTipo, rbCapacidad, body);
        }

        call.enqueue(new Callback<ApiResponse<Vehicle>>() {
            @Override
            public void onResponse(Call<ApiResponse<Vehicle>> call, Response<ApiResponse<Vehicle>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), isEditMode ? "Vehículo actualizado" : "Vehículo guardado", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onVehicleAdded();
                    }
                    dismiss();
                } else {
                    Toast.makeText(getContext(), "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Vehicle>> call, Throwable t) {
                Toast.makeText(getContext(), "Error de red: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private File createTmpFileFromUri(Uri uri) {
        try {
            String fileName = "upload_" + System.currentTimeMillis() + ".jpg";
            File tempFile = new File(requireContext().getCacheDir(), fileName);
            
            // Cargar el Bitmap desde el Uri
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(
                    requireContext().getContentResolver().openInputStream(uri));
            
            // Redimensionar si es muy grande (ej: max 1200px) para ahorrar más espacio
            int maxWidth = 1200;
            int maxHeight = 1200;
            float ratio = Math.min((float) maxWidth / bitmap.getWidth(), (float) maxHeight / bitmap.getHeight());
            if (ratio < 1.0f) {
                int newWidth = Math.round(bitmap.getWidth() * ratio);
                int newHeight = Math.round(bitmap.getHeight() * ratio);
                bitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            }

            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile);
            
            // Comprimir: Calidad 70 suele ser el punto dulce entre tamaño y visibilidad
            // Esto bajará una foto de 3MB a unos 200KB-400KB aproximadamente
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream);
            
            outputStream.flush();
            outputStream.close();
            bitmap.recycle(); // Liberar memoria
            
            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}