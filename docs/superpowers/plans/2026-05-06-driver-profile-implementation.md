# Driver Profile Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a dedicated profile activity for drivers with statistics, pending requests list, FAB for map navigation, and a "Add Vehicle" dialog.

**Architecture:** Use a separate Activity (`DriverProfileActivity`) as the main entry point for drivers. Data will be displayed using a grid for stats and a `RecyclerView` for requests. Interaction will be handled via a Navigation Drawer and a DialogFragment.

**Tech Stack:** Java, Android SDK, View Binding, Material Components, Retrofit (for potential future API integration).

---

### Task 1: Create Data Model and Adapter for Requests

**Files:**
- Create: `app/src/main/java/com/codram/terecojo/data/model/RideRequest.java`
- Create: `app/src/main/java/com/codram/terecojo/ui/adapter/RideRequestAdapter.java`
- Create: `app/src/main/res/layout/item_ride_request.xml`

- [ ] **Step 1: Create the RideRequest model**
```java
package com.codram.terecojo.data.model;

public class RideRequest {
    private String id;
    private String passengerName;
    private String origin;
    private String destination;

    public RideRequest(String id, String passengerName, String origin, String destination) {
        this.id = id;
        this.passengerName = passengerName;
        this.origin = origin;
        this.destination = destination;
    }

    // Getters
    public String getPassengerName() { return passengerName; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
}
```

- [ ] **Step 2: Create the layout for a single request item**
```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="8dp"
    app:cardElevation="2dp"
    app:cardCornerRadius="8dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <TextView
            android:id="@+id/tvPassengerName"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Nombre del Pasajero"
            android:textAppearance="@style/TextAppearance.Material3.TitleMedium" />

        <TextView
            android:id="@+id/tvOrigin"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:text="De: Calle A"
            android:textAppearance="@style/TextAppearance.Material3.BodyMedium" />

        <TextView
            android:id="@+id/tvDestination"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="2dp"
            android:text="A: Calle B"
            android:textAppearance="@style/TextAppearance.Material3.BodyMedium" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnAccept"
            style="@style/Widget.Material3.Button.TonalButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="end"
            android:layout_marginTop="8dp"
            android:text="Aceptar" />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

- [ ] **Step 3: Create the Adapter**
```java
package com.codram.terecojo.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.codram.terecojo.R;
import com.codram.terecojo.data.model.RideRequest;
import java.util.List;

public class RideRequestAdapter extends RecyclerView.Adapter<RideRequestAdapter.ViewHolder> {
    private List<RideRequest> requests;

    public RideRequestAdapter(List<RideRequest> requests) {
        this.requests = requests;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ride_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RideRequest request = requests.get(position);
        holder.tvPassengerName.setText(request.getPassengerName());
        holder.tvOrigin.setText("De: " + request.getOrigin());
        holder.tvDestination.setText("A: " + request.getDestination());
    }

    @Override
    public int getItemCount() { return requests.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPassengerName, tvOrigin, tvDestination;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPassengerName = itemView.findViewById(R.id.tvPassengerName);
            tvOrigin = itemView.findViewById(R.id.tvOrigin);
            tvDestination = itemView.findViewById(R.id.tvDestination);
        }
    }
}
```

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/codram/terecojo/data/model/RideRequest.java app/src/main/java/com/codram/terecojo/ui/adapter/RideRequestAdapter.java app/src/main/res/layout/item_ride_request.xml
git commit -m "feat: add RideRequest model and adapter"
```

---

### Task 2: Create AddVehicleDialogFragment

**Files:**
- Create: `app/src/main/res/layout/dialog_add_vehicle.xml`
- Create: `app/src/main/java/com/codram/terecojo/ui/dialog/AddVehicleDialogFragment.java`

- [ ] **Step 1: Create the dialog layout**
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="24dp">

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Añadir Automóvil"
        android:textAppearance="@style/TextAppearance.Material3.HeadlineSmall"
        android:layout_marginBottom="16dp"/>

    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Marca"
        style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
        android:layout_marginBottom="8dp">
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/etBrand"
            android:layout_width="match_parent"
            android:layout_height="wrap_content" />
    </com.google.android.material.textfield.TextInputLayout>

    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Modelo"
        style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
        android:layout_marginBottom="8dp">
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/etModel"
            android:layout_width="match_parent"
            android:layout_height="wrap_content" />
    </com.google.android.material.textfield.TextInputLayout>

    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Placa"
        style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
        android:layout_marginBottom="16dp">
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/etPlate"
            android:layout_width="match_parent"
            android:layout_height="wrap_content" />
    </com.google.android.material.textfield.TextInputLayout>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="end">
        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnCancel"
            style="@style/Widget.Material3.Button.TextButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Cancelar" />
        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnSave"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Guardar"
            android:layout_marginStart="8dp" />
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 2: Create the DialogFragment class**
```java
package com.codram.terecojo.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.codram.terecojo.R;

public class AddVehicleDialogFragment extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_vehicle, null);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(view);

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btnSave).setOnClickListener(v -> {
            // Future logic for saving
            dismiss();
        });

        return builder.create();
    }
}
```

- [ ] **Step 3: Commit**
```bash
git add app/src/main/res/layout/dialog_add_vehicle.xml app/src/main/java/com/codram/terecojo/ui/dialog/AddVehicleDialogFragment.java
git commit -m "feat: add AddVehicleDialogFragment"
```

---

### Task 3: Create DriverProfileActivity

**Files:**
- Create: `app/src/main/res/layout/activity_driver_profile.xml`
- Create: `app/src/main/java/com/codram/terecojo/DriverProfileActivity.java`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create the activity layout**
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.drawerlayout.widget.DrawerLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/drawer_layout"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.coordinatorlayout.widget.CoordinatorLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <com.google.android.material.appbar.AppBarLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content">
            <com.google.android.material.appbar.MaterialToolbar
                android:id="@+id/toolbar"
                android:layout_width="match_parent"
                android:layout_height="?attr/actionBarSize"
                app:title="Mi Perfil" />
        </com.google.android.material.appbar.AppBarLayout>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:orientation="vertical"
            android:padding="16dp"
            app:layout_behavior="@string/appbar_scrolling_view_behavior">

            <!-- Stats Grid -->
            <GridLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:columnCount="3"
                android:rowCount="1">

                <com.google.android.material.card.MaterialCardView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_columnWeight="1"
                    android:layout_margin="4dp">
                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:gravity="center"
                        android:orientation="vertical"
                        android:padding="8dp">
                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Viajes"
                            android:textAppearance="@style/TextAppearance.Material3.LabelSmall" />
                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="24"
                            android:textAppearance="@style/TextAppearance.Material3.TitleLarge" />
                    </LinearLayout>
                </com.google.android.material.card.MaterialCardView>

                <com.google.android.material.card.MaterialCardView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_columnWeight="1"
                    android:layout_margin="4dp">
                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:gravity="center"
                        android:orientation="vertical"
                        android:padding="8dp">
                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Rating"
                            android:textAppearance="@style/TextAppearance.Material3.LabelSmall" />
                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="4.8★"
                            android:textAppearance="@style/TextAppearance.Material3.TitleLarge" />
                    </LinearLayout>
                </com.google.android.material.card.MaterialCardView>

                <com.google.android.material.card.MaterialCardView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_columnWeight="1"
                    android:layout_margin="4dp">
                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:gravity="center"
                        android:orientation="vertical"
                        android:padding="8dp">
                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Ganancias"
                            android:textAppearance="@style/TextAppearance.Material3.LabelSmall" />
                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="$150"
                            android:textAppearance="@style/TextAppearance.Material3.TitleLarge" />
                    </LinearLayout>
                </com.google.android.material.card.MaterialCardView>
            </GridLayout>

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="16dp"
                android:text="Solicitudes Pendientes"
                android:textAppearance="@style/TextAppearance.Material3.TitleLarge" />

            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/rvRequests"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:layout_marginTop="8dp" />
        </LinearLayout>

        <com.google.android.material.floatingactionbutton.FloatingActionButton
            android:id="@+id/fabMap"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="bottom|end"
            android:layout_margin="16dp"
            android:src="@drawable/ic_my_location"
            app:tint="white" />

    </androidx.coordinatorlayout.widget.CoordinatorLayout>

    <com.google.android.material.navigation.NavigationView
        android:id="@+id/nav_view"
        android:layout_width="wrap_content"
        android:layout_height="match_parent"
        android:layout_gravity="start"
        app:menu="@menu/driver_profile_menu"
        app:headerLayout="@layout/nav_header_main" /> <!-- Assuming existing header exists -->

</androidx.drawerlayout.widget.DrawerLayout>
```

- [ ] **Step 2: Create the navigation menu**
Create `app/src/main/res/menu/driver_profile_menu.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:id="@+id/nav_profile"
        android:title="Mi Perfil" />
    <item
        android:id="@+id/nav_add_vehicle"
        android:title="Añadir Vehículo" />
    <item
        android:id="@+id/nav_logout"
        android:title="Cerrar Sesión" />
</menu>
```

- [ ] **Step 3: Create the Activity class**
```java
package com.codram.terecojo;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.codram.terecojo.data.model.RideRequest;
import com.codram.terecojo.databinding.ActivityDriverProfileBinding;
import com.codram.terecojo.ui.adapter.RideRequestAdapter;
import com.codram.terecojo.ui.dialog.AddVehicleDialogFragment;
import com.codram.terecojo.utils.SessionManager;
import java.util.ArrayList;
import java.util.List;

public class DriverProfileActivity extends AppCompatActivity {
    private ActivityDriverProfileBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDriverProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        setupRequestsList();
        setupNavigation();
        
        binding.fabMap.setOnClickListener(v -> {
            startActivity(new Intent(this, DriverActivity.class));
        });
    }

    private void setupRequestsList() {
        List<RideRequest> mockRequests = new ArrayList<>();
        mockRequests.add(new RideRequest("1", "Juan Perez", "Calle 23", "Vedado"));
        mockRequests.add(new RideRequest("2", "Maria Garcia", "Plaza", "Miramar"));
        
        binding.rvRequests.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRequests.setAdapter(new RideRequestAdapter(mockRequests));
    }

    private void setupNavigation() {
        binding.navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_add_vehicle) {
                new AddVehicleDialogFragment().show(getSupportFragmentManager(), "AddVehicle");
            } else if (id == R.id.nav_logout) {
                SessionManager.getInstance(this).logout(this);
            }
            binding.drawerLayout.closeDrawers();
            return true;
        });
    }
}
```

- [ ] **Step 4: Register Activity in AndroidManifest.xml**
```xml
<activity
    android:name=".DriverProfileActivity"
    android:exported="false"
    android:theme="@style/Theme.TeRecojo" />
```

- [ ] **Step 5: Commit**
```bash
git add app/src/main/res/layout/activity_driver_profile.xml app/src/main/res/menu/driver_profile_menu.xml app/src/main/java/com/codram/terecojo/DriverProfileActivity.java app/src/main/AndroidManifest.xml
git commit -m "feat: implement DriverProfileActivity"
```

---

### Task 4: Update Login Redirection

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/LoginActivity.java`

- [ ] **Step 1: Update redirection logic**
In `LoginActivity.java`, inside the success callback of the login process:
```java
// Inside handleLoginSuccess(AuthResponse response)
String role = response.getUser().getRol();
Intent intent;
if ("driver".equalsIgnoreCase(role)) {
    intent = new Intent(LoginActivity.this, DriverProfileActivity.class);
} else {
    intent = new Intent(LoginActivity.this, MainActivity.class);
}
startActivity(intent);
finish();
```

- [ ] **Step 2: Commit**
```bash
git add app/src/main/java/com/codram/terecojo/LoginActivity.java
git commit -m "feat: redirect drivers to profile activity"
```
