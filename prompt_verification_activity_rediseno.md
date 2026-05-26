# PROMPT — Rediseño Visual de `VerificationActivity`: Círculo de Días + Fondo + Recarga
**Proyecto:** Te Busco App (Android Java)  
**Rama:** Desarrollo

---

## CONTEXTO

La `VerificationActivity` actual muestra el estado de la licencia con texto e ícono. Se rediseña completamente para mostrar:

1. **Badge de estado** arriba (pill con color según estado)
2. **Círculo de progreso** con días restantes hasta la próxima renovación
3. **Card de fondo** con saldo, fecha de próxima renovación y cuota mensual
4. **Botón "¿Cómo Recargar?"** que abre un dialog informativo

---

## PARTE 1 — BACKEND: agregar `saldo_fondo` a `getMiLicencia`

### Archivo: `tebusco-api/src/controllers/authController.js`

Localizar la función `getMiLicencia` (línea ~461). En el `SELECT`, agregar `l.saldo_fondo` y `l.suscripcion_fin`:

```javascript
// ANTES
const { rows } = await query(`
  SELECT
    l.estado,
    l.trial_inicio,
    l.trial_fin,
    l.suscripcion_inicio,
    l.suscripcion_fin,
    l.ultimo_pago,
    l.monto_mensual,
    CASE ... END AS dias_restantes
  FROM licencias_chofer l
  WHERE l.chofer_id = $1
`, [choferId])

// DESPUÉS — agregar l.saldo_fondo
const { rows } = await query(`
  SELECT
    l.estado,
    l.trial_inicio,
    l.trial_fin,
    l.suscripcion_inicio,
    l.suscripcion_fin,
    l.ultimo_pago,
    l.monto_mensual,
    l.saldo_fondo,
    CASE
      WHEN l.estado = 'TRIAL_ACTIVO'
        THEN GREATEST(0, EXTRACT(DAY FROM l.trial_fin - NOW())::int)
      WHEN l.estado = 'ACTIVO'
        THEN GREATEST(0, EXTRACT(DAY FROM l.suscripcion_fin - NOW())::int)
      ELSE 0
    END AS dias_restantes,
    -- Fecha exacta de próxima renovación para mostrar en la app
    CASE
      WHEN l.estado = 'TRIAL_ACTIVO' THEN l.trial_fin
      WHEN l.estado = 'ACTIVO'       THEN l.suscripcion_fin
      ELSE NULL
    END AS proxima_renovacion
  FROM licencias_chofer l
  WHERE l.chofer_id = $1
`, [choferId])
```

---

## PARTE 2 — MODELO ANDROID: actualizar `Licencia.java`

### Archivo: `app/src/main/java/com/codram/terecojo/data/model/Licencia.java`

Agregar los dos campos nuevos que ahora devuelve la API:

```java
// Agregar estos dos campos con los demás @SerializedName:

@SerializedName("saldo_fondo")
private double saldoFondo;

@SerializedName("proxima_renovacion")
private String proximaRenovacion;

// Agregar sus getters:
public double getSaldoFondo() { return saldoFondo; }
public String getProximaRenovacion() { return proximaRenovacion; }
```

---

## PARTE 3 — NUEVO DRAWABLE: `app/src/main/res/drawable/circle_progress_bg.xml`

Fondo del círculo (track gris):

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="oval">
    <solid android:color="#F0F0F0" />
</shape>
```

---

## PARTE 4 — REEMPLAZAR LAYOUT: `app/src/main/res/layout/activity_verification.xml`

Reemplazar el archivo completo:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.drawerlayout.widget.DrawerLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/drawer_layout"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.coordinatorlayout.widget.CoordinatorLayout
        android:id="@+id/main_content"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@color/background_light">

        <com.google.android.material.appbar.AppBarLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@color/primary_blue"
            app:elevation="0dp">

            <com.google.android.material.appbar.MaterialToolbar
                android:id="@+id/toolbar"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:minHeight="?attr/actionBarSize"
                app:title="Mi Licencia"
                app:titleTextColor="@color/white"
                app:navigationIcon="@drawable/ic_menu"
                app:navigationIconTint="@color/white" />

        </com.google.android.material.appbar.AppBarLayout>

        <!-- ProgressBar de carga inicial -->
        <ProgressBar
            android:id="@+id/progressBar"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:visibility="visible"
            app:layout_behavior="@string/appbar_scrolling_view_behavior" />

        <!-- Contenido principal -->
        <androidx.core.widget.NestedScrollView
            android:id="@+id/layoutContenido"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:visibility="gone"
            app:layout_behavior="@string/appbar_scrolling_view_behavior">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="24dp"
                android:gravity="center_horizontal">

                <!-- 1. BADGE DE ESTADO -->
                <TextView
                    android:id="@+id/tvEstadoBadge"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="16dp"
                    android:paddingStart="20dp"
                    android:paddingEnd="20dp"
                    android:paddingTop="6dp"
                    android:paddingBottom="6dp"
                    android:text="TRIAL ACTIVO"
                    android:textSize="12sp"
                    android:textStyle="bold"
                    android:textAllCaps="true"
                    android:letterSpacing="0.08"
                    android:background="@drawable/bg_badge_estado" />

                <!-- 2. CÍRCULO DE PROGRESO -->
                <!-- Usamos un FrameLayout para superponer el ProgressBar circular y el texto -->
                <FrameLayout
                    android:id="@+id/frameCirculo"
                    android:layout_width="220dp"
                    android:layout_height="220dp"
                    android:layout_marginTop="32dp"
                    android:visibility="gone">

                    <!-- Track de fondo (círculo gris) -->
                    <ProgressBar
                        android:id="@+id/progressCircleTrack"
                        style="?android:attr/progressBarStyleHorizontal"
                        android:layout_width="220dp"
                        android:layout_height="220dp"
                        android:layout_gravity="center"
                        android:indeterminate="false"
                        android:progress="100"
                        android:progressDrawable="@drawable/circle_track"
                        android:rotation="-90" />

                    <!-- Progreso real (círculo de color) -->
                    <ProgressBar
                        android:id="@+id/progressCircle"
                        style="?android:attr/progressBarStyleHorizontal"
                        android:layout_width="220dp"
                        android:layout_height="220dp"
                        android:layout_gravity="center"
                        android:indeterminate="false"
                        android:progress="100"
                        android:max="100"
                        android:progressDrawable="@drawable/circle_progress"
                        android:rotation="-90" />

                    <!-- Texto central dentro del círculo -->
                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="match_parent"
                        android:orientation="vertical"
                        android:gravity="center">

                        <TextView
                            android:id="@+id/tvDiasNumero"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="--"
                            android:textSize="56sp"
                            android:textStyle="bold"
                            android:textColor="@color/text_primary" />

                        <TextView
                            android:id="@+id/tvDiasLabel"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="días"
                            android:textSize="14sp"
                            android:textColor="@color/text_secondary" />

                        <TextView
                            android:id="@+id/tvDiasSubtitulo"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="2dp"
                            android:text="restantes"
                            android:textSize="12sp"
                            android:textColor="@color/text_secondary" />

                    </LinearLayout>

                </FrameLayout>

                <!-- Mensaje para estados sin círculo (SUSPENDIDO, BLOQUEADO, etc.) -->
                <TextView
                    android:id="@+id/tvMensajeEstado"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="24dp"
                    android:gravity="center"
                    android:textSize="15sp"
                    android:textColor="@color/text_secondary"
                    android:lineSpacingMultiplier="1.4"
                    android:visibility="gone" />

                <!-- 3. CARD: Fondo, próxima renovación y cuota -->
                <com.google.android.material.card.MaterialCardView
                    android:id="@+id/cardFondo"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="28dp"
                    android:visibility="gone"
                    app:cardCornerRadius="20dp"
                    app:cardElevation="3dp"
                    app:cardBackgroundColor="@color/surface_white">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="vertical"
                        android:padding="20dp">

                        <!-- Saldo en fondo -->
                        <LinearLayout
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:orientation="horizontal"
                            android:gravity="center_vertical">

                            <LinearLayout
                                android:layout_width="0dp"
                                android:layout_height="wrap_content"
                                android:layout_weight="1"
                                android:orientation="vertical">

                                <TextView
                                    android:layout_width="wrap_content"
                                    android:layout_height="wrap_content"
                                    android:text="SALDO EN FONDO"
                                    android:textSize="10sp"
                                    android:textStyle="bold"
                                    android:textAllCaps="true"
                                    android:letterSpacing="0.08"
                                    android:textColor="@color/text_secondary" />

                                <TextView
                                    android:id="@+id/tvSaldoFondo"
                                    android:layout_width="wrap_content"
                                    android:layout_height="wrap_content"
                                    android:layout_marginTop="4dp"
                                    android:text="$0.00"
                                    android:textSize="28sp"
                                    android:textStyle="bold"
                                    android:textColor="@color/primary_blue" />

                            </LinearLayout>

                            <!-- Indicador visual de si el saldo alcanza para renovar -->
                            <TextView
                                android:id="@+id/tvSaldoIndicador"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:paddingStart="12dp"
                                android:paddingEnd="12dp"
                                android:paddingTop="6dp"
                                android:paddingBottom="6dp"
                                android:textSize="11sp"
                                android:textStyle="bold"
                                android:visibility="gone" />

                        </LinearLayout>

                        <!-- Divider -->
                        <View
                            android:layout_width="match_parent"
                            android:layout_height="1dp"
                            android:layout_marginTop="16dp"
                            android:layout_marginBottom="16dp"
                            android:background="#F0F0F0" />

                        <!-- Próxima renovación -->
                        <LinearLayout
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:orientation="horizontal"
                            android:gravity="center_vertical">

                            <TextView
                                android:layout_width="0dp"
                                android:layout_height="wrap_content"
                                android:layout_weight="1"
                                android:text="Próxima renovación"
                                android:textSize="13sp"
                                android:textColor="@color/text_secondary" />

                            <TextView
                                android:id="@+id/tvProximaRenovacion"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="--"
                                android:textSize="13sp"
                                android:textStyle="bold"
                                android:textColor="@color/text_primary" />

                        </LinearLayout>

                        <!-- Cuota mensual -->
                        <LinearLayout
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="10dp"
                            android:orientation="horizontal"
                            android:gravity="center_vertical">

                            <TextView
                                android:layout_width="0dp"
                                android:layout_height="wrap_content"
                                android:layout_weight="1"
                                android:text="Cuota mensual"
                                android:textSize="13sp"
                                android:textColor="@color/text_secondary" />

                            <TextView
                                android:id="@+id/tvCuotaMensual"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="--"
                                android:textSize="13sp"
                                android:textStyle="bold"
                                android:textColor="@color/text_primary" />

                        </LinearLayout>

                    </LinearLayout>

                </com.google.android.material.card.MaterialCardView>

                <!-- 4. BOTÓN: ¿Cómo Recargar? -->
                <com.google.android.material.button.MaterialButton
                    android:id="@+id/btnComoRecargar"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="20dp"
                    android:text="¿Cómo Recargar?"
                    android:textSize="15sp"
                    app:cornerRadius="14dp"
                    app:icon="@android:drawable/ic_menu_help"
                    app:iconGravity="textStart"
                    app:iconSize="18dp"
                    android:visibility="gone" />

                <!-- Card: Cuenta bloqueada -->
                <com.google.android.material.card.MaterialCardView
                    android:id="@+id/cardBloqueado"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="24dp"
                    android:visibility="gone"
                    app:cardCornerRadius="16dp"
                    app:cardElevation="2dp"
                    app:cardBackgroundColor="@color/surface_white"
                    app:strokeColor="@color/error_red"
                    app:strokeWidth="1dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="vertical"
                        android:padding="20dp">

                        <TextView
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:text="Tu cuenta ha sido bloqueada por el administrador del sistema. Contacta al soporte para obtener más información."
                            android:textSize="14sp"
                            android:textColor="@color/text_secondary"
                            android:lineSpacingMultiplier="1.4" />

                    </LinearLayout>

                </com.google.android.material.card.MaterialCardView>

                <!-- Botón reintentar (solo en error de red) -->
                <com.google.android.material.button.MaterialButton
                    android:id="@+id/btnReintentar"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="32dp"
                    android:text="Reintentar"
                    android:visibility="gone"
                    app:cornerRadius="12dp" />

                <View
                    android:layout_width="match_parent"
                    android:layout_height="32dp" />

            </LinearLayout>

        </androidx.core.widget.NestedScrollView>

    </androidx.coordinatorlayout.widget.CoordinatorLayout>

    <com.google.android.material.navigation.NavigationView
        android:id="@+id/nav_view"
        android:layout_width="wrap_content"
        android:layout_height="match_parent"
        android:layout_gravity="start"
        app:headerLayout="@layout/nav_drawer_header"
        app:menu="@menu/nav_drawer_menu"
        app:itemIconTint="@color/primary_blue"
        app:itemTextColor="@color/text_primary"
        app:itemIconPadding="20dp"
        app:itemHorizontalPadding="24dp">
        <include layout="@layout/nav_drawer_footer" />
    </com.google.android.material.navigation.NavigationView>

</androidx.drawerlayout.widget.DrawerLayout>
```

---

## PARTE 5 — DRAWABLES para el círculo de progreso

### `app/src/main/res/drawable/circle_track.xml` (fondo gris)

```xml
<?xml version="1.0" encoding="utf-8"?>
<rotate xmlns:android="http://schemas.android.com/apk/res/android"
    android:fromDegrees="270"
    android:toDegrees="270">
    <shape
        android:innerRadiusRatio="2.8"
        android:shape="ring"
        android:thicknessRatio="14"
        android:useLevel="true">
        <solid android:color="#E8E8E8" />
    </shape>
</rotate>
```

### `app/src/main/res/drawable/circle_progress.xml` (arco de color)

```xml
<?xml version="1.0" encoding="utf-8"?>
<rotate xmlns:android="http://schemas.android.com/apk/res/android"
    android:fromDegrees="270"
    android:toDegrees="270">
    <shape
        android:innerRadiusRatio="2.8"
        android:shape="ring"
        android:thicknessRatio="14"
        android:useLevel="true">
        <solid android:color="#4CAF50" />
    </shape>
</rotate>
```

> El color del arco se cambia en código según la urgencia. El drawable define el color base.

### `app/src/main/res/drawable/bg_badge_estado.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#E3F2FD" />
    <corners android:radius="50dp" />
</shape>
```

> El color de fondo y texto del badge se cambia en código según el estado.

---

## PARTE 6 — REEMPLAZAR `VerificationActivity.java`

Reemplazar el archivo completo:

```java
package com.codram.terecojo;

import android.graphics.Color;
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
        new MaterialAlertDialogBuilder(this)
            .setTitle("¿Cómo Recargar tu Fondo?")
            .setMessage(
                "Próximamente encontrarás aquí las instrucciones detalladas para " +
                "recargar tu fondo en Te Busco.\n\n" +
                "Por ahora, comunícate con el administrador para realizar tu recarga.\n\n" +
                "Una vez recibido el pago, el administrador actualizará tu saldo " +
                "y recibirás una notificación de confirmación."
            )
            .setPositiveButton("Entendido", null)
            .show();
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
```

---

## RESUMEN DE ARCHIVOS

| Archivo | Tipo |
|---|---|
| `tebusco-api/src/controllers/authController.js` | ✏️ Agregar `saldo_fondo` y `proxima_renovacion` al SELECT de `getMiLicencia` |
| `app/.../data/model/Licencia.java` | ✏️ Agregar `saldoFondo` y `proximaRenovacion` + getters |
| `app/.../res/drawable/circle_track.xml` | ✅ Nuevo |
| `app/.../res/drawable/circle_progress.xml` | ✅ Nuevo |
| `app/.../res/drawable/bg_badge_estado.xml` | ✅ Nuevo |
| `app/.../res/layout/activity_verification.xml` | ✏️ Reemplazar completo |
| `app/.../VerificationActivity.java` | ✏️ Reemplazar completo |

---

## FLUJO VISUAL ESPERADO

```
┌─────────────────────────────┐
│        Mi Licencia          │  ← Toolbar
├─────────────────────────────┤
│                             │
│    [ TRIAL ACTIVO ]         │  ← Badge pill azul
│                             │
│         ╭─────╮             │
│        ╱  28   ╲            │  ← Círculo con arco verde
│       │  días   │           │     va de 45 a 0 días
│       │restantes│           │
│        ╲       ╱            │
│         ╰─────╯             │
│                             │
│  ┌───────────────────────┐  │
│  │ SALDO EN FONDO        │  │  ← Card
│  │ $1,500.00  ✓ Alcanza  │  │
│  │ ─────────────────── │  │
│  │ Próx. renovación  30 jun 2026│
│  │ Cuota mensual    $500.00/mes│
│  └───────────────────────┘  │
│                             │
│   [ ¿Cómo Recargar? ]       │  ← Botón primario
│                             │
└─────────────────────────────┘
```
