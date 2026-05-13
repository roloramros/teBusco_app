package com.codram.terecojo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.codram.terecojo.data.model.ApiResponse;
import com.codram.terecojo.data.model.AuthResponse;
import com.codram.terecojo.data.model.Notification;
import com.codram.terecojo.data.remote.RetrofitClient;
import com.codram.terecojo.utils.SessionManager;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;

public abstract class BaseActivity extends AppCompatActivity implements OnMapReadyCallback {

    protected DrawerLayout drawerLayout;
    protected Toolbar toolbar;
    protected NavigationView navigationView;
    protected GoogleMap mMap;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        // Verificar permisos en tiempo de ejecución
        ArrayList<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        requestPermissionsIfNecessary(permissions.toArray(new String[0]));
    }

    protected void setupDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.toolbar);
        navigationView = findViewById(R.id.nav_view);

        setSupportActionBar(toolbar);

        setupMap();

        // Botón de centrar ubicación
        android.view.View fabMyLocation = findViewById(R.id.fabMyLocation);
        if (fabMyLocation != null) {
            fabMyLocation.setOnClickListener(v -> {
                if (mMap != null && mMap.getMyLocation() != null) {
                    LatLng myLocation = new LatLng(mMap.getMyLocation().getLatitude(), mMap.getMyLocation().getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 17.0f));
                }
            });

            // Ajustar FAB para que no quede detrás de la barra de navegación
            ViewCompat.setOnApplyWindowInsetsListener(fabMyLocation, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
                params.bottomMargin = (int) (16 * getResources().getDisplayMetrics().density) + systemBars.bottom;
                params.rightMargin = (int) (16 * getResources().getDisplayMetrics().density) + systemBars.right;
                v.setLayoutParams(params);
                return insets;
            });
        }

        // Ajustar Toolbar para que no se recorte con la barra de estado
        if (toolbar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(0, systemBars.top, 0, 0);
                return insets;
            });
        }

        // Ajustar contenido principal para que no quede debajo de la barra de navegación
        android.view.View mainContent = findViewById(R.id.main_content);
        if (mainContent != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainContent, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
                return insets;
            });
        }

        // Ajustar NavigationView para que el contenido no quede detrás de las barras
        if (navigationView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(navigationView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(0, systemBars.top, 0, 0); // Padding superior para la barra de estado
                
                // También ajustar el header específicamente si existe
                android.view.View header = navigationView.getHeaderView(0);
                if (header != null) {
                    header.setPadding(header.getPaddingLeft(), systemBars.top, header.getPaddingRight(), header.getPaddingBottom());
                }
                return insets;
            });
        }

        // Ajustar footer del menú lateral para la barra de navegación
        android.view.View navFooter = findViewById(R.id.nav_footer_container);
        if (navFooter != null) {
            ViewCompat.setOnApplyWindowInsetsListener(navFooter, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
                return insets;
            });
        }

        if (drawerLayout != null && toolbar != null) {
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
                @Override
                public void onDrawerOpened(android.view.View drawerView) {
                    super.onDrawerOpened(drawerView);
                    updateNotificationBadge();
                }
            };
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
        }

        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(androidx.core.view.GravityCompat.START);
                }
            });
        }

        // Configurar clics del menú
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                Intent intent = null;

                if (id == R.id.nav_request_service) {
                    if (!(this instanceof MainActivity)) {
                        intent = new Intent(this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    }
                } else if (id == R.id.nav_trips) {
                    if (!(this instanceof MyRequestsActivity)) {
                        intent = new Intent(this, MyRequestsActivity.class);
                    }
                } else if (id == R.id.nav_frequent_sites) {
                    if (!(this instanceof FrequentSitesActivity)) {
                        intent = new Intent(this, FrequentSitesActivity.class);
                    }
                } else if (id == R.id.nav_driver_radar) {
                    if (!(this instanceof DriverActivity)) {
                        intent = new Intent(this, DriverActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    }
                } else if (id == R.id.nav_driver_oferts) {
                    if (!(this instanceof DriverOffersActivity)) {
                        intent = new Intent(this, DriverOffersActivity.class);
                    }
                } else if (id == R.id.nav_my_vehicles) {
                    if (!(this instanceof MyVehiclesActivity)) {
                        intent = new Intent(this, MyVehiclesActivity.class);
                    }
                } else if (id == R.id.nav_profile) {
                    if (!(this instanceof DriverProfileActivity)) {
                        intent = new Intent(this, DriverProfileActivity.class);
                    }
                }

                if (intent != null) {
                    startActivity(intent);
                }
                
                if (drawerLayout != null) drawerLayout.closeDrawers();
                return true;
            });

            // Configurar visibilidad según el tipo de usuario
            AuthResponse.User user = SessionManager.getInstance(this).getUser();
            if (user != null) {
                Menu menu = navigationView.getMenu();
                boolean isChofer = "chofer".equalsIgnoreCase(user.getTipo());
                boolean isAdmin = "admin".equalsIgnoreCase(user.getTipo());
                boolean isPasajero = "pasajero".equalsIgnoreCase(user.getTipo());
                
                // Items de Pasajero
                MenuItem navRequest = menu.findItem(R.id.nav_request_service);
                if (navRequest != null) navRequest.setVisible(isPasajero);
                
                MenuItem navTrips = menu.findItem(R.id.nav_trips);
                if (navTrips != null) navTrips.setVisible(isPasajero);
                
                MenuItem navSites = menu.findItem(R.id.nav_frequent_sites);
                if (navSites != null) navSites.setVisible(isPasajero);
                
                // Items de Chofer
                MenuItem navRadar = menu.findItem(R.id.nav_driver_radar);
                if (navRadar != null) navRadar.setVisible(isChofer);
                
                MenuItem navVehicles = menu.findItem(R.id.nav_my_vehicles);
                if (navVehicles != null) navVehicles.setVisible(isChofer);
                
                MenuItem navProfile = menu.findItem(R.id.nav_profile);
                if (navProfile != null) navProfile.setVisible(isChofer);

                // Items de Admin
                MenuItem navAdmin = menu.findItem(R.id.nav_admin_panel);
                if (navAdmin != null) navAdmin.setVisible(isAdmin);

                // Configurar Nombre en footer
                TextView tvName = navigationView.findViewById(R.id.tvUserFullName);
                if (tvName != null) {
                    tvName.setText(user.getNombre());
                }
            }

            // Configurar clic en ícono de configuraciones en el header
            android.view.View headerView = navigationView.getHeaderView(0);
            if (headerView != null) {
                android.view.View ivSettings = headerView.findViewById(R.id.ivSettings);
                if (ivSettings != null) {
                    ivSettings.setOnClickListener(v -> {
                        Toast.makeText(this, "Próximamente: Configuraciones", Toast.LENGTH_SHORT).show();
                        if (drawerLayout != null) drawerLayout.closeDrawers();
                    });
                }

                android.view.View ivNotifications = headerView.findViewById(R.id.ivNotifications);
                if (ivNotifications != null) {
                    ivNotifications.setOnClickListener(v -> {
                        Intent intent = new Intent(this, NotificationsActivity.class);
                        startActivity(intent);
                        if (drawerLayout != null) drawerLayout.closeDrawers();
                    });
                }
            }

            android.view.View btnLogout = navigationView.findViewById(R.id.btnLogoutDrawer);
            if (btnLogout != null) {
                btnLogout.setOnClickListener(v -> {
                    SessionManager.getInstance(this).logout(this);
                });
            }
        }
    }

    private void setupMap() {
        android.view.View mapView = findViewById(R.id.mapView);
        if (mapView == null) return;
        
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapView);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        
        // Configuración inicial del mapa
        mMap.getUiSettings().setMyLocationButtonEnabled(false); // Usamos nuestro propio FAB
        
        // Ubicación por defecto (La Habana) solo si no hay GPS
        LatLng habana = new LatLng(23.1136, -82.3666);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(habana, 15.0f));

        enableMyLocation();

        // Intentar centrar en mi ubicación actual al inicio
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setOnMyLocationChangeListener(location -> {
                if (location != null) {
                    LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 16.0f));
                    // Solo centrar la primera vez al abrir
                    mMap.setOnMyLocationChangeListener(null);
                }
            });
        }
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                enableMyLocation();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_filter) {
            Toast.makeText(this, "Próximamente: Filtros", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void updateNotificationBadge() {
        if (navigationView == null) return;
        android.view.View headerView = navigationView.getHeaderView(0);
        if (headerView == null) return;

        android.view.View badge = headerView.findViewById(R.id.notificationBadge);
        if (badge == null) return;

        RetrofitClient.getService(this).getNotifications().enqueue(new Callback<ApiResponse<List<Notification>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Notification>>> call, Response<ApiResponse<List<Notification>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Notification> notifications = response.body().getData();
                    boolean hasUnread = false;
                    for (Notification n : notifications) {
                        if (!n.isLeida()) {
                            hasUnread = true;
                            break;
                        }
                    }
                    badge.setVisibility(hasUnread ? android.view.View.VISIBLE : android.view.View.GONE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Notification>>> call, Throwable t) {
                // Silencioso en caso de error
            }
        });
    }
}
