# Mi Licencia View Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the "Mi Licencia" view in the driver app, allowing drivers to see their license status, remaining days, balance, and monthly quota.

**Architecture:** Use a dedicated `MiLicenciaActivity` with a Material Design layout. Fetch data from the existing `GET /api/auth/mi-licencia` endpoint using Retrofit. Implement dynamic UI updates based on the license state (Trial, Active, Suspended).

**Tech Stack:** Android (Java), XML, Retrofit, Material Components.

---

### Task 1: Update Licencia Model

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/data/model/Licencia.java`

- [ ] **Step 1: Add saldoFondo field and update helpers**

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

    @SerializedName("saldo_fondo")
    private double saldoFondo;

    public String getEstado() { return estado; }
    public int getDiasRestantes() { return diasRestantes; }
    public String getTrialInicio() { return trialInicio; }
    public String getTrialFin() { return trialFin; }
    public String getSuscripcionFin() { return suscripcionFin; }
    public String getUltimoPago() { return ultimoPago; }
    public double getMontoMensual() { return montoMensual; }
    public double getSaldoFondo() { return saldoFondo; }

    // Helpers de lógica
    public boolean puedeOperar() {
        return "TRIAL_ACTIVO".equals(estado) || "ACTIVO".equals(estado);
    }

    public boolean esTrial() {
        return "TRIAL_ACTIVO".equals(estado);
    }

    public boolean necesitaPago() {
        return "SUSPENDIDO".equals(estado);
    }

    public boolean estaBloqueado() {
        return "BLOQUEADO".equals(estado);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/codram/terecojo/data/model/Licencia.java
git commit -m "feat(model): update Licencia model with saldoFondo and refined helpers"
```

---

### Task 2: Update Navigation Menu

**Files:**
- Modify: `app/src/main/res/menu/nav_drawer_menu.xml`

- [ ] **Step 1: Add Mi Licencia item to the menu**

```xml
    <!-- Add this item in the driver section of the menu -->
    <item
        android:id="@+id/nav_mi_licencia"
        android:icon="🪪"
        android:title="Mi Licencia" />
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/menu/nav_drawer_menu.xml
git commit -m "feat(ui): add Mi Licencia to navigation drawer menu"
```

---

### Task 3: Create Layout for Mi Licencia

**Files:**
- Create: `app/src/main/res/layout/activity_mi_licencia.xml`

- [ ] **Step 1: Implement the dashboard layout**

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.drawerlayout.widget.DrawerLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/drawer_layout"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@color/background_light">

        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@color/primary_blue"
            android:elevation="4dp"
            android:theme="@style/ThemeOverlay.Material3.Dark.ActionBar"
            app:layout_constraintTop_toTopOf="parent"
            app:title="Mi Licencia"
            app:titleTextColor="@color/white"
            app:navigationIcon="@drawable/ic_menu" />

        <androidx.swiperefreshlayout.widget.SwipeRefreshLayout
            android:id="@+id/swipeRefresh"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintTop_toBottomOf="@id/toolbar">

            <androidx.core.widget.NestedScrollView
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:fillViewport="true">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="20dp">

                    <!-- Status Card -->
                    <com.google.android.material.card.MaterialCardView
                        android:id="@+id/cardStatus"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        app:cardCornerRadius="16dp"
                        app:cardElevation="2dp"
                        app:strokeWidth="0dp">

                        <LinearLayout
                            android:id="@+id/layoutStatusHeader"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:background="@color/primary_blue"
                            android:gravity="center"
                            android:orientation="vertical"
                            android:padding="24dp">

                            <TextView
                                android:id="@+id/tvStatusLabel"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="Trial Activo"
                                android:textColor="@color/white"
                                android:textSize="22sp"
                                android:fontFamily="sans-serif-black" />

                        </LinearLayout>
                    </com.google.android.material.card.MaterialCardView>

                    <!-- Circular Progress Container -->
                    <androidx.constraintlayout.widget.ConstraintLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="32dp">

                        <ProgressBar
                            android:id="@+id/circularProgress"
                            style="?android:attr/progressBarStyleHorizontal"
                            android:layout_width="220dp"
                            android:layout_height="220dp"
                            android:indeterminate="false"
                            android:max="100"
                            android:progress="75"
                            android:progressDrawable="@drawable/circular_progress_bar"
                            app:layout_constraintBottom_toBottomOf="parent"
                            app:layout_constraintEnd_toEndOf="parent"
                            app:layout_constraintStart_toStartOf="parent"
                            app:layout_constraintTop_toTopOf="parent" />

                        <LinearLayout
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:gravity="center"
                            android:orientation="vertical"
                            app:layout_constraintBottom_toBottomOf="@id/circularProgress"
                            app:layout_constraintEnd_toEndOf="@id/circularProgress"
                            app:layout_constraintStart_toStartOf="@id/circularProgress"
                            app:layout_constraintTop_toTopOf="@id/circularProgress">

                            <TextView
                                android:id="@+id/tvDaysCount"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="15"
                                android:textColor="@color/text_primary"
                                android:textSize="48sp"
                                android:fontFamily="sans-serif-black" />

                            <TextView
                                android:id="@+id/tvDaysLabel"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="días restantes"
                                android:textColor="@color/text_secondary"
                                android:textSize="14sp" />
                        </LinearLayout>
                    </androidx.constraintlayout.widget.ConstraintLayout>

                    <!-- Balance Card -->
                    <com.google.android.material.card.MaterialCardView
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="32dp"
                        app:cardCornerRadius="16dp"
                        app:cardElevation="4dp"
                        app:cardBackgroundColor="@color/surface_white"
                        app:strokeColor="#EEEEEE"
                        app:strokeWidth="1dp">

                        <LinearLayout
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:orientation="vertical"
                            android:padding="20dp">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="Tu Fondo"
                                android:textColor="@color/text_secondary"
                                android:textSize="14sp"
                                android:textAllCaps="true"
                                android:letterSpacing="0.1" />

                            <TextView
                                android:id="@+id/tvSaldo"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:layout_marginTop="4dp"
                                android:text="$0.00"
                                android:textColor="@color/primary_blue"
                                android:textSize="32sp"
                                android:fontFamily="sans-serif-medium" />

                            <View
                                android:layout_width="match_parent"
                                android:layout_height="1dp"
                                android:layout_marginVertical="16dp"
                                android:background="#EEEEEE" />

                            <LinearLayout
                                android:layout_width="match_parent"
                                android:layout_height="wrap_content"
                                android:orientation="horizontal"
                                android:gravity="center_vertical">

                                <TextView
                                    android:layout_width="0dp"
                                    android:layout_height="wrap_content"
                                    android:layout_weight="1"
                                    android:text="Cuota Mensual"
                                    android:textColor="@color/text_secondary" />

                                <TextView
                                    android:id="@+id/tvCuota"
                                    android:layout_width="wrap_content"
                                    android:layout_height="wrap_content"
                                    android:text="$0.00 / mes"
                                    android:textColor="@color/text_primary"
                                    android:fontFamily="sans-serif-medium" />
                            </LinearLayout>

                        </LinearLayout>
                    </com.google.android.material.card.MaterialCardView>

                    <!-- Recharge Help -->
                    <com.google.android.material.button.MaterialButton
                        android:id="@+id/btnHelp"
                        style="@style/Widget.Material3.Button.OutlinedButton"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="24dp"
                        android:text="¿Cómo recargar?"
                        android:textColor="@color/primary_blue"
                        app:strokeColor="@color/primary_blue"
                        app:cornerRadius="12dp"
                        android:padding="12dp" />

                    <TextView
                        android:id="@+id/tvNextRenewal"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="24dp"
                        android:gravity="center"
                        android:text="Próxima renovación: --/--/----"
                        android:textColor="@color/text_secondary"
                        android:textSize="12sp" />

                </LinearLayout>
            </androidx.core.widget.NestedScrollView>
        </androidx.swiperefreshlayout.widget.SwipeRefreshLayout>

    </androidx.constraintlayout.widget.ConstraintLayout>

    <com.google.android.material.navigation.NavigationView
        android:id="@+id/nav_view"
        android:layout_width="wrap_content"
        android:layout_height="match_parent"
        android:layout_gravity="start"
        app:headerLayout="@layout/nav_drawer_header"
        app:menu="@menu/nav_drawer_menu"
        app:itemIconTint="@color/primary_blue"
        app:itemTextColor="@color/text_primary">
        <include layout="@layout/nav_drawer_footer" />
    </com.google.android.material.navigation.NavigationView>

</androidx.drawerlayout.widget.DrawerLayout>
```

- [ ] **Step 2: Create the circular progress drawable**

Create `app/src/main/res/drawable/circular_progress_bar.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:id="@android:id/background">
        <shape
            android:innerRadiusRatio="3"
            android:shape="ring"
            android:thicknessRatio="15.0"
            android:useLevel="false">
            <solid android:color="#E0E0E0" />
        </shape>
    </item>
    <item android:id="@android:id/progress">
        <rotate
            android:fromDegrees="270"
            android:pivotX="50%"
            android:pivotY="50%"
            android:toDegrees="270">
            <shape
                android:innerRadiusRatio="3"
                android:shape="ring"
                android:thicknessRatio="15.0"
                android:useLevel="true">
                <solid android:color="@color/primary_blue" />
            </shape>
        </rotate>
    </item>
</layer-list>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/activity_mi_licencia.xml app/src/main/res/drawable/circular_progress_bar.xml
git commit -m "feat(ui): create layout and progress drawable for Mi Licencia view"
```

---

### Task 4: Implement MiLicenciaActivity

**Files:**
- Create: `app/src/main/java/com/codram/terecojo/MiLicenciaActivity.java`

- [ ] **Step 1: Implement activity logic**

```java
package com.codram.terecojo;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.codram.terecojo.data.model.ApiResponse;
import com.codram.terecojo.data.model.Licencia;
import com.codram.terecojo.data.remote.ApiService;
import com.codram.terecojo.data.remote.RetrofitClient;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MiLicenciaActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvStatusLabel, tvDaysCount, tvSaldo, tvCuota, tvNextRenewal, tvDaysLabel;
    private android.widget.ProgressBar circularProgress;
    private android.view.View layoutStatusHeader;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mi_licencia);

        initViews();
        setupNavigation();
        
        apiService = RetrofitClient.getInstance().getApi();
        loadData();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        tvStatusLabel = findViewById(R.id.tvStatusLabel);
        tvDaysCount = findViewById(R.id.tvDaysCount);
        tvDaysLabel = findViewById(R.id.tvDaysLabel);
        tvSaldo = findViewById(R.id.tvSaldo);
        tvCuota = findViewById(R.id.tvCuota);
        tvNextRenewal = findViewById(R.id.tvNextRenewal);
        circularProgress = findViewById(R.id.circularProgress);
        layoutStatusHeader = findViewById(R.id.layoutStatusHeader);

        swipeRefresh.setOnRefreshListener(this::loadData);

        findViewById(R.id.btnHelp).setOnClickListener(v -> showRechargeHelp());
    }

    private void setupNavigation() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_mi_licencia);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void loadData() {
        swipeRefresh.setRefreshing(true);
        apiService.getMiLicencia().enqueue(new Callback<ApiResponse<Licencia>>() {
            @Override
            public void onResponse(Call<ApiResponse<Licencia>> call, Response<ApiResponse<Licencia>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    updateUI(response.body().getData());
                } else {
                    Toast.makeText(MiLicenciaActivity.this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Licencia>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(MiLicenciaActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(Licencia licencia) {
        if (licencia == null) return;

        // Balance & Quota
        tvSaldo.setText(String.format(Locale.getDefault(), "$%.2f", licencia.getSaldoFondo()));
        tvCuota.setText(String.format(Locale.getDefault(), "$%.2f / mes", licencia.getMontoMensual()));

        // Status Colors & Labels
        int color;
        String label;
        int maxDays = 30; // Default period

        switch (licencia.getEstado()) {
            case "TRIAL_ACTIVO":
                color = getResources().getColor(R.color.primary_blue);
                label = "Trial Activo";
                maxDays = 45;
                break;
            case "ACTIVO":
                color = getResources().getColor(R.color.success_green);
                label = "Licencia Activa";
                break;
            case "SUSPENDIDO":
                color = getResources().getColor(R.color.error_red);
                label = "Licencia Suspendida";
                break;
            case "BLOQUEADO":
                color = getResources().getColor(R.color.gray_dark);
                label = "Licencia Bloqueada";
                break;
            default:
                color = getResources().getColor(R.color.gray_light);
                label = licencia.getEstado();
        }

        layoutStatusHeader.setBackgroundColor(color);
        tvStatusLabel.setText(label);
        
        // Circular Progress
        int days = licencia.getDiasRestantes();
        tvDaysCount.setText(String.valueOf(days));
        tvDaysCount.setTextColor(color);
        
        int progress = (int) (((float) days / maxDays) * 100);
        circularProgress.setProgress(Math.min(100, Math.max(0, progress)));
        
        // Next renewal date
        String expiry = licencia.getSuscripcionFin() != null ? licencia.getSuscripcionFin() : licencia.getTrialFin();
        if (expiry != null) {
            tvNextRenewal.setText("Próxima renovación: " + expiry);
        } else {
            tvNextRenewal.setText("Próxima renovación: --/--/----");
        }
    }

    private void showRechargeHelp() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("¿Cómo recargar tu fondo?")
                .setMessage("Próximamente aquí verás las instrucciones detalladas para recargar tu monedero.")
                .setPositiveButton("Entendido", null)
                .show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Use logic from DriverActivity or common helper
        int id = item.getItemId();
        if (id == R.id.nav_mi_licencia) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            // Handle other items (Open corresponding activities)
            handleCommonNavigation(id);
        }
        return true;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/codram/terecojo/MiLicenciaActivity.java
git commit -m "feat(logic): implement MiLicenciaActivity with data binding"
```

---

### Task 5: Register Activity and Handle Navigation

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/codram/terecojo/DriverActivity.java`

- [ ] **Step 1: Register activity in manifest**

```xml
        <activity
            android:name=".MiLicenciaActivity"
            android:exported="false"
            android:label="Mi Licencia"
            android:theme="@style/Theme.TeRecojo" />
```

- [ ] **Step 2: Add navigation click listener in DriverActivity**

```java
    // Inside onNavigationItemSelected in DriverActivity.java
    else if (id == R.id.nav_mi_licencia) {
        startActivity(new Intent(this, MiLicenciaActivity.class));
    }
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/com/codram/terecojo/DriverActivity.java
git commit -m "feat(nav): register MiLicenciaActivity and update DriverActivity navigation"
```
