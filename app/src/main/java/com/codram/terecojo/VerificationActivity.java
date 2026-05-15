package com.codram.terecojo;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.codram.terecojo.data.model.AuthResponse;
import com.codram.terecojo.databinding.ActivityVerificationBinding;
import com.codram.terecojo.utils.SessionManager;

public class VerificationActivity extends BaseActivity {

    private ActivityVerificationBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupDrawer();
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
        }

        updateStatus();
    }

    private void updateStatus() {
        AuthResponse.User user = SessionManager.getInstance(this).getUser();
        if (user == null) return;

        boolean verificado = user.isVerificado();
        
        binding.tvStatusTitle.setText(verificado ? "CUENTA VERIFICADA" : "PENDIENTE DE VERIFICACIÓN");
        binding.tvStatusDesc.setText(verificado 
            ? "¡Felicidades! Tu cuenta y licencia han sido aprobadas. Puedes operar en todo el municipio."
            : "Estamos revisando tus documentos. Este proceso suele tardar entre 24 y 48 horas hábiles.");
        
        binding.ivStatusIcon.setImageResource(verificado 
            ? android.R.drawable.checkbox_on_background 
            : android.R.drawable.ic_menu_recent_history);
        
        binding.btnUploadLicense.setVisibility(verificado ? View.GONE : View.VISIBLE);
        binding.btnUploadLicense.setOnClickListener(v -> {
            Toast.makeText(this, "Funcionalidad próximamente disponible", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onMapReady(@androidx.annotation.NonNull com.google.android.gms.maps.GoogleMap googleMap) {}
}
