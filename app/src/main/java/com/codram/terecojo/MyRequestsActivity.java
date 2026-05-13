package com.codram.terecojo;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.codram.terecojo.data.model.ApiResponse;
import com.codram.terecojo.data.model.Offer;
import com.codram.terecojo.data.model.RejectOfferRequest;
import com.codram.terecojo.data.model.RideRequest;
import com.codram.terecojo.data.remote.RetrofitClient;
import com.codram.terecojo.databinding.ActivityMyRequestsBinding;
import com.codram.terecojo.ui.adapter.MyRequestsAdapter;
import com.codram.terecojo.ui.adapter.OfferAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.codram.terecojo.utils.ErrorUtils;

import com.codram.terecojo.ui.dialog.RatingDialogFragment;
import com.codram.terecojo.ui.viewmodel.MainViewModel;
import com.google.android.material.snackbar.Snackbar;
import androidx.lifecycle.ViewModelProvider;

public class MyRequestsActivity extends BaseActivity implements MyRequestsAdapter.OnRequestClickListener, RatingDialogFragment.OnRatingSubmitListener {
    private ActivityMyRequestsBinding binding;
    private MyRequestsAdapter adapter;
    private MainViewModel viewModel;
    private List<RideRequest> myRequests = new ArrayList<>();
    private String currentSolicitudId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyRequestsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        setupObservers();

        setupDrawer();
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
        }

        setupRecyclerView();
        setupSwipeRefresh();
        fetchMyRequests();
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeColors(getResources().getColor(R.color.primary_blue));
        binding.swipeRefresh.setOnRefreshListener(this::fetchMyRequests);
    }

    @Override
    public void onMapReady(@androidx.annotation.NonNull com.google.android.gms.maps.GoogleMap googleMap) {
        // No necesario
    }

    private void setupRecyclerView() {
        adapter = new MyRequestsAdapter(myRequests, this);
        binding.rvMyRequests.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMyRequests.setAdapter(adapter);
    }

    private void fetchMyRequests() {
        RetrofitClient.getService(this).getMisSolicitudes().enqueue(new Callback<ApiResponse<List<RideRequest>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<RideRequest>>> call, Response<ApiResponse<List<RideRequest>>> response) {
                binding.swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    myRequests.clear();
                    myRequests.addAll(response.body().getData());
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<RideRequest>>> call, Throwable t) {
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(MyRequestsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onViewOffers(RideRequest request) {
        showOffersDialog(request);
    }

    @Override
    public void onCancel(RideRequest request) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cancelar solicitud")
                .setMessage("¿Estás seguro de que deseas cancelar este viaje? Esta acción no se puede deshacer.")
                .setPositiveButton("SÍ, CANCELAR", (dialog, which) -> {
                    cancelTrip(request);
                })
                .setNegativeButton("VOLVER", null)
                .show();
    }

    private void cancelTrip(RideRequest request) {
        RetrofitClient.getService(this).cancelarSolicitud(request.getId()).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MyRequestsActivity.this, "Solicitud cancelada", Toast.LENGTH_SHORT).show();
                    fetchMyRequests(); // Recargar lista
                } else {
                    String msg = ErrorUtils.parseError(response, "Error al cancelar");
                    Toast.makeText(MyRequestsActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(MyRequestsActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showOffersDialog(RideRequest request) {
        if (request == null) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_view_offers, null);
        androidx.recyclerview.widget.RecyclerView rvOffers = dialogView.findViewById(R.id.rvOffers);
        
        List<Offer> offerList = new ArrayList<>();
        final OfferAdapter[] offerAdapterWrapper = new OfferAdapter[1];
        
        OfferAdapter offerAdapter = new OfferAdapter(offerList, null);
        offerAdapterWrapper[0] = offerAdapter;
        
        if (rvOffers != null) {
            rvOffers.setLayoutManager(new LinearLayoutManager(this));
            rvOffers.setAdapter(offerAdapter);
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Ofertas para este viaje");
        builder.setView(dialogView);
        builder.setNegativeButton("CERRAR", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();

        final AlertDialog finalDialog = dialog;
        
        offerAdapter.setListener(new OfferAdapter.OnOfferClickListener() {
            @Override
            public void onAccept(Offer offer) {
                acceptOffer(offer, finalDialog);
            }

            @Override
            public void onReject(Offer offer) {
                rejectOffer(offer, request, offerList, offerAdapterWrapper[0]);
            }

            @Override
            public void onImageClick(String imageUrl) {
                showImagePreview(imageUrl);
            }
        });

        fetchOffersForDialog(request.getId(), offerList, offerAdapter);
    }

    private void fetchOffersForDialog(String requestId, List<Offer> offerList, OfferAdapter offerAdapter) {
        RetrofitClient.getService(this).getOfertas(requestId).enqueue(new Callback<ApiResponse<List<Offer>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Offer>>> call, Response<ApiResponse<List<Offer>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    offerList.clear();
                    offerList.addAll(response.body().getData());
                    offerAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Offer>>> call, Throwable t) {
                Toast.makeText(MyRequestsActivity.this, "Error al cargar ofertas: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void acceptOffer(Offer offer, AlertDialog dialogToClose) {
        RetrofitClient.getService(this).aceptarOferta(offer.getId()).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    if (dialogToClose != null) dialogToClose.dismiss();
                    Toast.makeText(MyRequestsActivity.this, "¡Viaje confirmado!", Toast.LENGTH_LONG).show();
                    fetchMyRequests(); 
                } else {
                    String msg = ErrorUtils.parseError(response, "Error al aceptar oferta");
                    Toast.makeText(MyRequestsActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(MyRequestsActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void rejectOffer(Offer offer, RideRequest request, List<Offer> offerList, OfferAdapter offerAdapter) {
        EditText etReason = new EditText(this);
        etReason.setHint("Motivo (opcional)");
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(this);
        container.addView(etReason);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) etReason.getLayoutParams();
        params.leftMargin = padding;
        params.rightMargin = padding;
        params.topMargin = (int) (10 * getResources().getDisplayMetrics().density);
        etReason.setLayoutParams(params);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Rechazar oferta")
                .setMessage("¿Deseas indicar un motivo para rechazar la oferta de " + offer.getChoferNombre() + "?")
                .setView(container)
                .setPositiveButton("RECHAZAR", (dialog, which) -> {
                    String reason = etReason.getText().toString().trim();
                    executeRejectOffer(offer, request, reason, offerList, offerAdapter);
                })
                .setNegativeButton("CANCELAR", null)
                .show();
    }

    private void executeRejectOffer(Offer offer, RideRequest request, String reason, List<Offer> offerList, OfferAdapter offerAdapter) {
        RejectOfferRequest rejectRequest = new RejectOfferRequest(reason);
        RetrofitClient.getService(this).rechazarOferta(offer.getId(), rejectRequest).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MyRequestsActivity.this, "Oferta rechazada", Toast.LENGTH_SHORT).show();
                    fetchOffersForDialog(request.getId(), offerList, offerAdapter);
                    // Actualizar contador de la lista principal
                    fetchMyRequests();
                } else {
                    String msg = ErrorUtils.parseError(response, "Error al rechazar oferta");
                    Toast.makeText(MyRequestsActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(MyRequestsActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showImagePreview(String imageUrl) {
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_image_preview);
        android.widget.ImageView ivFull = dialog.findViewById(R.id.ivFullImage);
        
        com.bumptech.glide.Glide.with(this)
                .load(imageUrl)
                .fitCenter()
                .into(ivFull);

        ivFull.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
tivity.this, "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showImagePreview(String imageUrl) {
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_image_preview);
        android.widget.ImageView ivFull = dialog.findViewById(R.id.ivFullImage);
        
        com.bumptech.glide.Glide.with(this)
                .load(imageUrl)
                .fitCenter()
                .into(ivFull);

        ivFull.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
