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
        int id = item.getItemId();
        if (id == R.id.nav_mi_licencia) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            handleCommonNavigation(id);
        }
        return true;
    }
}
