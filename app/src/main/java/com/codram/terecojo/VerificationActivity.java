package com.codram.terecojo;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

import com.codram.terecojo.data.model.ApiResponse;
import com.codram.terecojo.data.model.Licencia;
import com.codram.terecojo.data.remote.RetrofitClient;
import com.codram.terecojo.databinding.ActivityVerificationBinding;
import com.google.android.gms.maps.GoogleMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerificationActivity extends BaseActivity {

    private ActivityVerificationBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupDrawer();
        cargarLicencia();
    }

    private void cargarLicencia() {
        mostrarCargando(true);

        RetrofitClient.getService().getMiLicencia().enqueue(new Callback<ApiResponse<Licencia>>() {
            @Override
            public void onResponse(Call<ApiResponse<Licencia>> call, Response<ApiResponse<Licencia>> response) {
                mostrarCargando(false);
                if (response.isSuccessful() && response.body() != null && response.body().isOk()) {
                    mostrarEstado(response.body().getData());
                } else {
                    mostrarError();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Licencia>> call, Throwable t) {
                mostrarCargando(false);
                mostrarError();
            }
        });
    }

    private void mostrarCargando(boolean cargando) {
        binding.progressBar.setVisibility(cargando ? View.VISIBLE : View.GONE);
        binding.layoutContenido.setVisibility(cargando ? View.GONE : View.VISIBLE);
    }

    private void mostrarEstado(Licencia licencia) {
        if (licencia == null) {
            mostrarError();
            return;
        }

        // Ocultar todos los bloques primero y mostrar solo los que aplican
        binding.cardContador.setVisibility(View.GONE);
        binding.cardPago.setVisibility(View.GONE);
        binding.cardBloqueado.setVisibility(View.GONE);

        switch (licencia.getEstado()) {

            case "TRIAL_ACTIVO":
                binding.ivStatusIcon.setImageResource(R.drawable.ic_notifications); // ícono temporal
                binding.ivStatusIcon.setColorFilter(
                    getResources().getColor(R.color.primary_blue, getTheme()));
                binding.tvStatusTitle.setText("Período de Prueba Activo");
                binding.tvStatusTitle.setTextColor(
                    getResources().getColor(R.color.primary_blue, getTheme()));
                binding.tvStatusDesc.setText(
                    "Estás disfrutando tu período de prueba gratuito de 45 días. " +
                    "Durante este tiempo tienes acceso completo a todas las funciones de Te Busco.");
                mostrarContador(licencia.getDiasRestantes(), false);
                break;

            case "ACTIVO":
                binding.ivStatusIcon.setImageResource(android.R.drawable.checkbox_on_background);
                binding.ivStatusIcon.setColorFilter(
                    getResources().getColor(R.color.success_green, getTheme()));
                binding.tvStatusTitle.setText("Licencia Activa");
                binding.tvStatusTitle.setTextColor(
                    getResources().getColor(R.color.success_green, getTheme()));
                binding.tvStatusDesc.setText(
                    "Tu suscripción está al día. Tienes acceso completo para operar en Te Busco.");
                mostrarContador(licencia.getDiasRestantes(), true);
                break;

            case "TRIAL_EXPIRADO":
                binding.ivStatusIcon.setImageResource(android.R.drawable.ic_dialog_alert);
                binding.ivStatusIcon.setColorFilter(
                    getResources().getColor(R.color.warning_amber, getTheme()));
                binding.tvStatusTitle.setText("Período de Prueba Finalizado");
                binding.tvStatusTitle.setTextColor(
                    getResources().getColor(R.color.warning_amber, getTheme()));
                binding.tvStatusDesc.setText(
                    "Tu período de prueba gratuito de 45 días ha concluido. " +
                    "Para continuar operando en Te Busco, activa tu licencia mensual.");
                mostrarInstruccionesPago(licencia);
                break;

            case "SUSPENDIDO":
                binding.ivStatusIcon.setImageResource(android.R.drawable.ic_dialog_alert);
                binding.ivStatusIcon.setColorFilter(
                    getResources().getColor(R.color.warning_amber, getTheme()));
                binding.tvStatusTitle.setText("Suscripción Vencida");
                binding.tvStatusTitle.setTextColor(
                    getResources().getColor(R.color.warning_amber, getTheme()));
                binding.tvStatusDesc.setText(
                    "Tu suscripción mensual ha vencido. Renueva tu licencia para volver " +
                    "a recibir solicitudes de viaje.");
                mostrarInstruccionesPago(licencia);
                break;

            case "BLOQUEADO":
                binding.ivStatusIcon.setImageResource(android.R.drawable.ic_delete);
                binding.ivStatusIcon.setColorFilter(
                    getResources().getColor(R.color.error_red, getTheme()));
                binding.tvStatusTitle.setText("Cuenta Bloqueada");
                binding.tvStatusTitle.setTextColor(
                    getResources().getColor(R.color.error_red, getTheme()));
                binding.tvStatusDesc.setText(
                    "Tu cuenta ha sido bloqueada por el administrador del sistema. " +
                    "Contacta al soporte para obtener más información.");
                binding.cardBloqueado.setVisibility(View.VISIBLE);
                break;

            default: // SIN_LICENCIA u otro
                binding.ivStatusIcon.setImageResource(android.R.drawable.ic_menu_recent_history);
                binding.ivStatusIcon.setColorFilter(
                    getResources().getColor(R.color.gray_dark, getTheme()));
                binding.tvStatusTitle.setText("Licencia Pendiente");
                binding.tvStatusTitle.setTextColor(
                    getResources().getColor(R.color.gray_dark, getTheme()));
                binding.tvStatusDesc.setText(
                    "Tu cuenta aún no tiene una licencia asignada. " +
                    "El administrador la activará próximamente.");
                break;
        }
    }

    private void mostrarContador(int diasRestantes, boolean esSuscripcion) {
        binding.cardContador.setVisibility(View.VISIBLE);

        if (diasRestantes == 0) {
            binding.tvDiasNumero.setText("Hoy");
            binding.tvDiasLabel.setText("vence la licencia");
            binding.tvDiasNumero.setTextColor(
                getResources().getColor(R.color.error_red, getTheme()));
        } else {
            binding.tvDiasNumero.setText(String.valueOf(diasRestantes));
            binding.tvDiasLabel.setText(diasRestantes == 1 ? "día restante" : "días restantes");

            // Colorear el contador según urgencia
            int color;
            if (diasRestantes <= 7) {
                color = R.color.warning_amber;   // Menos de 7 días → naranja
            } else if (diasRestantes <= 15) {
                color = R.color.primary_blue;    // Menos de 15 días → azul
            } else {
                color = R.color.success_green;   // Más de 15 días → verde
            }
            binding.tvDiasNumero.setTextColor(getResources().getColor(color, getTheme()));
        }

        binding.tvContadorSubtitle.setText(esSuscripcion
            ? "de suscripción activa"
            : "de período de prueba");
    }

    private void mostrarInstruccionesPago(Licencia licencia) {
        binding.cardPago.setVisibility(View.VISIBLE);
        // El monto se muestra si el admin lo configuró, si es 0 se oculta
        if (licencia.getMontoMensual() > 0) {
            binding.tvMontoPago.setVisibility(View.VISIBLE);
            binding.tvMontoPago.setText(
                String.format("Costo mensual: $%.2f CUP", licencia.getMontoMensual()));
        } else {
            binding.tvMontoPago.setVisibility(View.GONE);
        }
    }

    private void mostrarError() {
        binding.ivStatusIcon.setImageResource(android.R.drawable.ic_dialog_alert);
        binding.tvStatusTitle.setText("Sin conexión");
        binding.tvStatusDesc.setText(
            "No pudimos obtener el estado de tu licencia. " +
            "Verifica tu conexión e intenta de nuevo.");
        binding.cardContador.setVisibility(View.GONE);
        binding.cardPago.setVisibility(View.GONE);
        binding.cardBloqueado.setVisibility(View.GONE);
        binding.btnReintentar.setVisibility(View.VISIBLE);
        binding.btnReintentar.setOnClickListener(v -> {
            binding.btnReintentar.setVisibility(View.GONE);
            cargarLicencia();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {}
}
