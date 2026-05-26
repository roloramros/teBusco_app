package com.codram.terecojo;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;

import com.codram.terecojo.data.model.ApiResponse;
import com.codram.terecojo.data.model.Licencia;
import com.codram.terecojo.data.remote.RetrofitClient;
import com.codram.terecojo.databinding.ActivityVerificationBinding;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerificationActivity extends BaseActivity {

    private ActivityVerificationBinding binding;

    // Recarga dialog
    private Uri capturaUri = null;
    private android.app.Dialog dialogRecarga = null;

    // Launcher moderno para selección de imagen
    private final androidx.activity.result.ActivityResultLauncher<Intent> pickImageLauncher =
        registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK
                        && result.getData() != null
                        && result.getData().getData() != null) {
                    capturaUri = result.getData().getData();
                    // Actualizar preview en el dialog si sigue abierto
                    actualizarPreviewCaptura();
                }
            }
        );

    // Período total según el estado: trial = 45 días, suscripción = 30 días
    private static final int DIAS_TRIAL       = 45;
    private static final int DIAS_SUSCRIPCION = 30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupDrawer();
        cargarLicencia();
    }

    // — Carga de datos —

    private void cargarLicencia() {
        mostrarCargando(true);
        RetrofitClient.getService().getMiLicencia().enqueue(new Callback<ApiResponse<Licencia>>() {
            @Override
            public void onResponse(Call<ApiResponse<Licencia>> call,
                                   Response<ApiResponse<Licencia>> response) {
                mostrarCargando(false);
                if (response.isSuccessful() && response.body() != null && response.body().isOk()) {
                    renderLicencia(response.body().getData());
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

    // — Renderizado principal —

    private void renderLicencia(Licencia licencia) {
        if (licencia == null) { mostrarError(); return; }

        // Ocultar todo primero
        binding.frameCirculo.setVisibility(View.GONE);
        binding.tvMensajeEstado.setVisibility(View.GONE);
        binding.cardFondo.setVisibility(View.GONE);
        binding.cardBloqueado.setVisibility(View.GONE);
        binding.btnComoRecargar.setVisibility(View.GONE);
        binding.btnReintentar.setVisibility(View.GONE);

        switch (licencia.getEstado()) {

            case "TRIAL_ACTIVO":
                setBadge("Trial Activo", "#1565C0", "#E3F2FD");
                mostrarCirculo(licencia.getDiasRestantes(), DIAS_TRIAL);
                mostrarCardFondo(licencia);
                binding.btnComoRecargar.setVisibility(View.VISIBLE);
                break;

            case "ACTIVO":
                setBadge("Licencia Activa", "#2E7D32", "#E8F5E9");
                mostrarCirculo(licencia.getDiasRestantes(), DIAS_SUSCRIPCION);
                mostrarCardFondo(licencia);
                binding.btnComoRecargar.setVisibility(View.VISIBLE);
                break;

            case "SUSPENDIDO":
                setBadge("Suspendida", "#E65100", "#FFF3E0");
                mostrarMensaje("Tu suscripción ha vencido y no tienes saldo suficiente en tu fondo para la renovación automática.\n\nRecarga tu fondo para volver a operar.");
                mostrarCardFondo(licencia);
                binding.btnComoRecargar.setVisibility(View.VISIBLE);
                break;

            case "TRIAL_EXPIRADO":
                setBadge("Trial Expirado", "#E65100", "#FFF3E0");
                mostrarMensaje("Tu período de prueba de 45 días ha finalizado.\n\nRecarga tu fondo para activar tu licencia mensual y seguir operando en Te Busco.");
                mostrarCardFondo(licencia);
                binding.btnComoRecargar.setVisibility(View.VISIBLE);
                break;

            case "BLOQUEADO":
                setBadge("Bloqueada", "#B71C1C", "#FFEBEE");
                mostrarMensaje("Tu cuenta ha sido bloqueada por el administrador.");
                binding.cardBloqueado.setVisibility(View.VISIBLE);
                break;

            default:
                setBadge("Sin Licencia", "#616161", "#F5F5F5");
                mostrarMensaje("Tu licencia aún no ha sido asignada. El administrador la activará próximamente.");
                break;
        }

        // Configurar botón de recarga
        binding.btnComoRecargar.setOnClickListener(v -> mostrarDialogRecarga());
    }

    // — Componentes visuales —

    /**
     * Configura el badge de estado con texto y colores personalizados.
     * textColor: color del texto en hex (ej: "#1565C0")
     * bgColor:   color del fondo en hex (ej: "#E3F2FD")
     */
    private void setBadge(String texto, String textColor, String bgColor) {
        binding.tvEstadoBadge.setText(texto);
        binding.tvEstadoBadge.setTextColor(Color.parseColor(textColor));
        binding.tvEstadoBadge.getBackground().mutate()
            .setColorFilter(Color.parseColor(bgColor),
                android.graphics.PorterDuff.Mode.SRC_IN);
    }

    /**
     * Muestra el círculo de progreso con los días restantes.
     * diasRestantes: días que quedan
     * diasTotales:   período total (45 para trial, 30 para suscripción)
     */
    private void mostrarCirculo(int diasRestantes, int diasTotales) {
        binding.frameCirculo.setVisibility(View.VISIBLE);

        // Calcular progreso como porcentaje de días restantes sobre el total
        int progreso = diasTotales > 0
            ? Math.min(100, (diasRestantes * 100) / diasTotales)
            : 0;

        // Animar el progreso
        binding.progressCircle.setProgress(progreso);

        // Color del arco según urgencia
        String colorHex;
        if (diasRestantes <= 5) {
            colorHex = "#F44336"; // Rojo — crítico
        } else if (diasRestantes <= 10) {
            colorHex = "#FF9800"; // Naranja — urgente
        } else if (diasRestantes <= 15) {
            colorHex = "#2196F3"; // Azul — atención
        } else {
            colorHex = "#4CAF50"; // Verde — bien
        }

        // Aplicar color al drawable del arco
        binding.progressCircle.getProgressDrawable().mutate()
            .setColorFilter(Color.parseColor(colorHex),
                android.graphics.PorterDuff.Mode.SRC_IN);

        // Texto central
        if (diasRestantes == 0) {
            binding.tvDiasNumero.setText("Hoy");
            binding.tvDiasLabel.setVisibility(View.GONE);
            binding.tvDiasSubtitulo.setText("vence");
        } else {
            binding.tvDiasNumero.setText(String.valueOf(diasRestantes));
            binding.tvDiasLabel.setText(diasRestantes == 1 ? "día" : "días");
            binding.tvDiasSubtitulo.setText("restantes");
        }
        binding.tvDiasNumero.setTextColor(Color.parseColor(colorHex));
    }

    /**
     * Muestra el card con saldo, fecha de renovación y cuota mensual.
     */
    private void mostrarCardFondo(Licencia licencia) {
        binding.cardFondo.setVisibility(View.VISIBLE);

        // Saldo en fondo
        double saldo = licencia.getSaldoFondo();
        binding.tvSaldoFondo.setText(String.format(Locale.getDefault(), "$%.2f", saldo));

        // Indicador si el saldo alcanza para renovar
        double cuota = licencia.getMontoMensual();
        if (cuota > 0) {
            binding.tvSaldoIndicador.setVisibility(View.VISIBLE);
            if (saldo >= cuota) {
                binding.tvSaldoIndicador.setText("✓ Alcanza para renovar");
                binding.tvSaldoIndicador.setTextColor(Color.parseColor("#2E7D32"));
                binding.tvSaldoIndicador.setBackgroundColor(Color.parseColor("#E8F5E9"));
            } else {
                binding.tvSaldoIndicador.setText("✗ Saldo insuficiente");
                binding.tvSaldoIndicador.setTextColor(Color.parseColor("#B71C1C"));
                binding.tvSaldoIndicador.setBackgroundColor(Color.parseColor("#FFEBEE"));
            }
            // Redondear el background del indicador
            binding.tvSaldoIndicador.setPadding(
                dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
        } else {
            binding.tvSaldoIndicador.setVisibility(View.GONE);
        }

        // Próxima renovación
        String fechaRenovacion = formatearFecha(licencia.getProximaRenovacion());
        binding.tvProximaRenovacion.setText(
            fechaRenovacion != null ? fechaRenovacion : "No disponible");

        // Cuota mensual
        if (cuota > 0) {
            binding.tvCuotaMensual.setText(
                String.format(Locale.getDefault(), "$%.2f / mes", cuota));
        } else {
            binding.tvCuotaMensual.setText("Sin cargo");
        }
    }

    private void mostrarMensaje(String texto) {
        binding.tvMensajeEstado.setVisibility(View.VISIBLE);
        binding.tvMensajeEstado.setText(texto);
    }

    // — Dialog de recarga —

    private void mostrarDialogRecarga() {
        // Número de tarjeta del administrador
        final String NUMERO_TARJETA = "9226 3000 XXXX XXXX";
        final String WHATSAPP_NUMBER = "5350140609"; // Cuba +53, sin el +

        // Inflar el layout del dialog
        android.view.View dialogView = getLayoutInflater()
            .inflate(R.layout.dialog_recarga, null);

        // Referencias a las vistas del dialog
        android.widget.TextView tvNumeroTarjeta = dialogView.findViewById(R.id.tvNumeroTarjeta);
        com.google.android.material.button.MaterialButton btnCopiarTarjeta = dialogView.findViewById(R.id.btnCopiarTarjeta);
        com.google.android.material.card.MaterialCardView cardPreview = dialogView.findViewById(R.id.cardPreviewImagen);
        android.widget.ImageView ivPreview = dialogView.findViewById(R.id.ivPreviewCaptura);
        com.google.android.material.button.MaterialButton btnQuitarImagen = dialogView.findViewById(R.id.btnQuitarImagen);
        com.google.android.material.button.MaterialButton btnCargarCaptura = dialogView.findViewById(R.id.btnCargarCaptura);
        com.google.android.material.button.MaterialButton btnEnviarWhatsApp = dialogView.findViewById(R.id.btnEnviarWhatsApp);

        // Número de tarjeta
        tvNumeroTarjeta.setText(NUMERO_TARJETA);

        // Botón copiar tarjeta
        btnCopiarTarjeta.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Número de tarjeta", NUMERO_TARJETA.replace(" ", ""));
            clipboard.setPrimaryClip(clip);
            android.widget.Toast.makeText(this, "Número copiado al portapapeles", android.widget.Toast.LENGTH_SHORT).show();
        });

        // Botón cargar captura — abre la galería
        btnCargarCaptura.setOnClickListener(v -> {
            // Verificar permiso en Android 13+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, 200);
                    return;
                }
            }
            Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickIntent.setType("image/*");
            pickImageLauncher.launch(pickIntent);
        });

        // Botón quitar imagen
        btnQuitarImagen.setOnClickListener(v -> {
            capturaUri = null;
            cardPreview.setVisibility(View.GONE);
            btnEnviarWhatsApp.setEnabled(false);
            btnCargarCaptura.setText("📷  Cargar captura de transferencia");
        });

        // Botón enviar por WhatsApp
        btnEnviarWhatsApp.setOnClickListener(v -> {
            if (capturaUri == null) {
                android.widget.Toast.makeText(this, "Primero carga la captura de la transferencia", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            enviarPorWhatsApp(WHATSAPP_NUMBER, capturaUri);
        });

        // Construir y mostrar el dialog
        dialogRecarga = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setNegativeButton("Cerrar", (d, w) -> dialogRecarga = null)
            .create();

        dialogRecarga.show();
    }

    private void actualizarPreviewCaptura() {
        if (dialogRecarga == null || !dialogRecarga.isShowing() || capturaUri == null) return;

        com.google.android.material.card.MaterialCardView cardPreview = dialogRecarga.findViewById(R.id.cardPreviewImagen);
        android.widget.ImageView ivPreview = dialogRecarga.findViewById(R.id.ivPreviewCaptura);
        com.google.android.material.button.MaterialButton btnEnviar = dialogRecarga.findViewById(R.id.btnEnviarWhatsApp);
        com.google.android.material.button.MaterialButton btnCargar = dialogRecarga.findViewById(R.id.btnCargarCaptura);

        if (cardPreview == null || ivPreview == null) return;

        cardPreview.setVisibility(View.VISIBLE);
        com.bumptech.glide.Glide.with(this).load(capturaUri).centerCrop().into(ivPreview);
        if (btnEnviar != null) btnEnviar.setEnabled(true);
        if (btnCargar != null) btnCargar.setText("📷  Cambiar captura");
    }

    private void enviarPorWhatsApp(String numeroWhatsApp, Uri imagenUri) {
        com.codram.terecojo.data.model.AuthResponse.User usuario =
            com.codram.terecojo.utils.SessionManager.getInstance(this).getUser();

        String nombre   = usuario != null ? usuario.getNombre()   : "Chofer";
        String username = usuario != null ? "@" + usuario.getUsername() : "";

        String mensaje = "📋 *Solicitud de Recarga de Fondo — Te Busco*\n\n"
            + "👤 Nombre: " + nombre + "\n"
            + "🆔 Usuario: " + username + "\n\n"
            + "Adjunto captura de la transferencia para su revisión.\n"
            + "Por favor confirmar la acreditación del saldo. ¡Gracias!";

        Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
        whatsappIntent.setType("image/*");
        whatsappIntent.setPackage("com.whatsapp");
        whatsappIntent.putExtra("jid", numeroWhatsApp + "@s.whatsapp.net");
        whatsappIntent.putExtra(Intent.EXTRA_TEXT, mensaje);
        whatsappIntent.putExtra(Intent.EXTRA_STREAM, imagenUri);
        whatsappIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (whatsappIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(whatsappIntent);
        } else {
            try {
                String mensajeCodificado = Uri.encode(mensaje);
                Uri uri = Uri.parse("https://wa.me/" + numeroWhatsApp + "?text=" + mensajeCodificado);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(browserIntent);
                android.widget.Toast.makeText(this, "La imagen deberá adjuntarse manualmente en WhatsApp Web.", android.widget.Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                android.widget.Toast.makeText(this, "No se pudo abrir WhatsApp. Instala la aplicación e intenta de nuevo.", android.widget.Toast.LENGTH_LONG).show();
            }
        }
    }

    // — Estado de error —

    private void mostrarError() {
        binding.frameCirculo.setVisibility(View.GONE);
        binding.cardFondo.setVisibility(View.GONE);
        binding.cardBloqueado.setVisibility(View.GONE);
        binding.btnComoRecargar.setVisibility(View.GONE);
        binding.tvMensajeEstado.setVisibility(View.VISIBLE);
        binding.tvMensajeEstado.setText(
            "No pudimos obtener el estado de tu licencia.\nVerifica tu conexión e intenta de nuevo.");
        binding.btnReintentar.setVisibility(View.VISIBLE);
        binding.btnReintentar.setOnClickListener(v -> {
            binding.btnReintentar.setVisibility(View.GONE);
            cargarLicencia();
        });
    }

    // — Utilidades —

    /**
     * Convierte una fecha ISO 8601 del servidor a formato legible "dd MMM yyyy".
     * Ejemplo: "2026-06-30T04:00:00.000Z" → "30 jun. 2026"
     */
    private String formatearFecha(String fechaIso) {
        if (fechaIso == null || fechaIso.isEmpty()) return null;
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = isoFormat.parse(fechaIso);
            SimpleDateFormat displayFormat = new SimpleDateFormat(
                "dd MMM yyyy", new Locale("es", "ES"));
            return displayFormat.format(date);
        } catch (ParseException e) {
            // Intentar formato alternativo sin milisegundos
            try {
                SimpleDateFormat alt = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ssX", Locale.getDefault());
                Date date = alt.parse(fechaIso);
                SimpleDateFormat displayFormat = new SimpleDateFormat(
                    "dd MMM yyyy", new Locale("es", "ES"));
                return displayFormat.format(date);
            } catch (ParseException e2) {
                return fechaIso.substring(0, Math.min(10, fechaIso.length()));
            }
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {}
}
