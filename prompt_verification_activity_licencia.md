# PROMPT — Rediseño de `VerificationActivity`: Estado Real de Licencia
**Proyecto:** Te Busco App (Android Java)  
**Objetivo:** Reemplazar la pantalla estática de "pendiente/verificado" por una pantalla dinámica que consulta la API y muestra el estado real de la licencia del chofer, días restantes, y las instrucciones de pago cuando corresponde.

---

## CONTEXTO

La pantalla actual solo lee el campo `verificado` (booleano) de la sesión local. Con el nuevo sistema de licencias, hay 5 estados posibles que el chofer debe ver claramente:

| Estado API | Qué ve el chofer |
|---|---|
| `TRIAL_ACTIVO` | "Período de prueba activo — X días restantes" |
| `TRIAL_EXPIRADO` | "Tu período de prueba ha terminado — instrucciones de pago" |
| `ACTIVO` | "Licencia activa — X días restantes" |
| `SUSPENDIDO` | "Tu suscripción ha vencido — instrucciones de pago" |
| `BLOQUEADO` | "Cuenta bloqueada — contactar soporte" |

El endpoint que devuelve estos datos ya existe:  
`GET /api/admin/licencias/:chofer_id` — requiere token de admin.

Como el chofer no puede llamar al endpoint de admin, se necesita un **endpoint propio para el chofer** que devuelva su licencia sin privilegios de admin.

---

## PARTE 1 — BACKEND (1 endpoint nuevo)

### Archivo: `tebusco-api/src/routes/solicitud.js` — NO, va en auth

### Archivo: `tebusco-api/src/routes/auth.js`

Agregar al final, antes de `export default router`:

```javascript
router.get('/mi-licencia', authenticate, authController.getMiLicencia)
```

### Archivo: `tebusco-api/src/controllers/authController.js`

Agregar esta función al final del archivo:

```javascript
/**
 * GET /api/auth/mi-licencia
 * El chofer consulta el estado de su propia licencia
 */
export const getMiLicencia = async (req, res) => {
  try {
    const { id: usuarioId } = req.usuario

    // Obtener el chofer_id
    const { rows: choferRows } = await query(
      'SELECT id FROM choferes WHERE usuario_id = $1',
      [usuarioId]
    )

    if (choferRows.length === 0) {
      return response.forbidden(res, 'Solo los choferes pueden consultar su licencia')
    }

    const choferId = choferRows[0].id

    const { rows } = await query(`
      SELECT
        l.estado,
        l.trial_inicio,
        l.trial_fin,
        l.suscripcion_inicio,
        l.suscripcion_fin,
        l.ultimo_pago,
        l.monto_mensual,
        CASE
          WHEN l.estado = 'TRIAL_ACTIVO'
            THEN GREATEST(0, EXTRACT(DAY FROM l.trial_fin - NOW())::int)
          WHEN l.estado = 'ACTIVO'
            THEN GREATEST(0, EXTRACT(DAY FROM l.suscripcion_fin - NOW())::int)
          ELSE 0
        END AS dias_restantes
      FROM licencias_chofer l
      WHERE l.chofer_id = $1
    `, [choferId])

    if (rows.length === 0) {
      // El chofer existe pero aún no tiene licencia (caso legacy)
      return response.success(res, {
        estado: 'SIN_LICENCIA',
        dias_restantes: 0
      })
    }

    return response.success(res, rows[0])
  } catch (err) {
    console.error('❌ Error en /mi-licencia:', err.message)
    throw err
  }
}
```

> Este endpoint no requiere ser admin — solo autenticación normal. El chofer solo ve su propia licencia.

---

## PARTE 2 — ANDROID

### 2.1 Nuevo modelo: `app/src/main/java/com/codram/terecojo/data/model/Licencia.java`

```java
package com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

public class Licencia {

    @SerializedName("estado")
    private String estado;

    @SerializedName("dias_restantes")
    private int diasRestantes;

    @SerializedName("trial_inicio")
    private String trialInicio;

    @SerializedName("trial_fin")
    private String trialFin;

    @SerializedName("suscripcion_fin")
    private String suscripcionFin;

    @SerializedName("ultimo_pago")
    private String ultimoPago;

    @SerializedName("monto_mensual")
    private double montoMensual;

    public String getEstado() { return estado; }
    public int getDiasRestantes() { return diasRestantes; }
    public String getTrialInicio() { return trialInicio; }
    public String getTrialFin() { return trialFin; }
    public String getSuscripcionFin() { return suscripcionFin; }
    public String getUltimoPago() { return ultimoPago; }
    public double getMontoMensual() { return montoMensual; }

    // Helpers de lógica
    public boolean puedeOperar() {
        return "TRIAL_ACTIVO".equals(estado) || "ACTIVO".equals(estado);
    }

    public boolean esTrial() {
        return "TRIAL_ACTIVO".equals(estado) || "TRIAL_EXPIRADO".equals(estado);
    }

    public boolean necesitaPago() {
        return "TRIAL_EXPIRADO".equals(estado) || "SUSPENDIDO".equals(estado);
    }

    public boolean estaBloqueado() {
        return "BLOQUEADO".equals(estado);
    }
}
```

---

### 2.2 Modificar: `app/src/main/java/com/codram/terecojo/data/remote/ApiService.java`

Agregar la declaración del nuevo endpoint junto a los demás de auth:

```java
@GET("api/auth/mi-licencia")
Call<ApiResponse<Licencia>> getMiLicencia();
```

> Agregar también el import del modelo al inicio del archivo:
> ```java
> import com.codram.terecojo.data.model.Licencia;
> ```

---

### 2.3 Reemplazar: `app/src/main/java/com/codram/terecojo/VerificationActivity.java`

Reemplazar el archivo completo:

```java
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
```

---

### 2.4 Reemplazar: `app/src/main/res/layout/activity_verification.xml`

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

        <!-- Progress bar de carga -->
        <ProgressBar
            android:id="@+id/progressBar"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:visibility="visible"
            app:layout_behavior="@string/appbar_scrolling_view_behavior" />

        <!-- Contenido principal (oculto mientras carga) -->
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
                android:padding="20dp"
                android:gravity="center_horizontal">

                <!-- Ícono de estado -->
                <ImageView
                    android:id="@+id/ivStatusIcon"
                    android:layout_width="96dp"
                    android:layout_height="96dp"
                    android:layout_marginTop="32dp"
                    android:src="@android:drawable/ic_menu_recent_history" />

                <!-- Título de estado -->
                <TextView
                    android:id="@+id/tvStatusTitle"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="16dp"
                    android:text="Cargando..."
                    android:textSize="20sp"
                    android:textStyle="bold"
                    android:gravity="center" />

                <!-- Descripción -->
                <TextView
                    android:id="@+id/tvStatusDesc"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="12dp"
                    android:gravity="center"
                    android:textColor="@color/text_secondary"
                    android:textSize="14sp"
                    android:lineSpacingMultiplier="1.4" />

                <!-- Card: Contador de días restantes -->
                <com.google.android.material.card.MaterialCardView
                    android:id="@+id/cardContador"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="24dp"
                    android:visibility="gone"
                    app:cardCornerRadius="16dp"
                    app:cardElevation="2dp"
                    app:cardBackgroundColor="@color/surface_white">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="vertical"
                        android:gravity="center"
                        android:padding="24dp">

                        <TextView
                            android:id="@+id/tvDiasNumero"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="--"
                            android:textSize="64sp"
                            android:textStyle="bold"
                            android:textColor="@color/success_green" />

                        <TextView
                            android:id="@+id/tvDiasLabel"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="días restantes"
                            android:textSize="16sp"
                            android:textColor="@color/text_primary"
                            android:layout_marginTop="4dp" />

                        <TextView
                            android:id="@+id/tvContadorSubtitle"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="de período de prueba"
                            android:textSize="13sp"
                            android:textColor="@color/text_secondary"
                            android:layout_marginTop="4dp" />

                    </LinearLayout>

                </com.google.android.material.card.MaterialCardView>

                <!-- Card: Instrucciones de pago -->
                <com.google.android.material.card.MaterialCardView
                    android:id="@+id/cardPago"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="24dp"
                    android:visibility="gone"
                    app:cardCornerRadius="16dp"
                    app:cardElevation="2dp"
                    app:cardBackgroundColor="@color/surface_white"
                    app:strokeColor="@color/warning_amber"
                    app:strokeWidth="1dp">

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="vertical"
                        android:padding="20dp">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="¿Cómo renovar mi licencia?"
                            android:textSize="16sp"
                            android:textStyle="bold"
                            android:textColor="@color/text_primary" />

                        <TextView
                            android:id="@+id/tvMontoPago"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="12dp"
                            android:textSize="15sp"
                            android:textStyle="bold"
                            android:textColor="@color/primary_blue"
                            android:visibility="gone" />

                        <TextView
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="12dp"
                            android:text="1. Contacta al administrador de Te Busco\n2. Realiza el pago acordado\n3. El administrador activará tu licencia\n4. Recibirás una notificación de confirmación"
                            android:textSize="14sp"
                            android:textColor="@color/text_secondary"
                            android:lineSpacingMultiplier="1.6" />

                        <TextView
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="16dp"
                            android:background="@drawable/bg_rounded_gray"
                            android:padding="12dp"
                            android:text="📞 Contacto: Disponible en la sección de soporte"
                            android:textSize="13sp"
                            android:textColor="@color/text_secondary" />

                    </LinearLayout>

                </com.google.android.material.card.MaterialCardView>

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
                            android:text="Para conocer el motivo del bloqueo y resolver tu situación, contacta al administrador del sistema."
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

                <!-- Spacer inferior -->
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

### 2.5 Nuevo drawable: `app/src/main/res/drawable/bg_rounded_gray.xml`

El `cardPago` tiene un bloque de texto con fondo gris redondeado. Crear este drawable:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#F0F0F0" />
    <corners android:radius="8dp" />
</shape>
```

---

## RESUMEN DE ARCHIVOS

| Archivo | Tipo |
|---|---|
| `tebusco-api/src/controllers/authController.js` | ✏️ Agregar función `getMiLicencia` al final |
| `tebusco-api/src/routes/auth.js` | ✏️ Agregar 1 ruta `GET /mi-licencia` |
| `app/.../data/model/Licencia.java` | ✅ Nuevo |
| `app/.../data/remote/ApiService.java` | ✏️ Agregar 1 llamada + import |
| `app/.../VerificationActivity.java` | ✏️ Reemplazar completo |
| `app/.../res/layout/activity_verification.xml` | ✏️ Reemplazar completo |
| `app/.../res/drawable/bg_rounded_gray.xml` | ✅ Nuevo |

**Sin cambios en:** `BaseActivity`, `SessionManager`, `AuthResponse`, ningún adapter ni ViewModel.

---

## COMPORTAMIENTO ESPERADO

1. El chofer abre "Mi Licencia" desde el menú lateral
2. Aparece un `ProgressBar` mientras se consulta la API
3. La pantalla muestra el estado correcto:
   - **Trial activo:** ícono azul + contador grande en verde/naranja/rojo según urgencia
   - **Activo:** ícono verde + contador de días de suscripción
   - **Trial expirado / Suspendido:** ícono naranja + card con instrucciones de pago
   - **Bloqueado:** ícono rojo + card de contacto con soporte
4. Si no hay conexión: mensaje de error + botón "Reintentar"
