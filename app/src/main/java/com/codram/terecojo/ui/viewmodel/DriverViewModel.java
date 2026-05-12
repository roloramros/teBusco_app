package com.codram.terecojo.ui.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.codram.terecojo.data.remote.MapRepository;
import com.google.android.gms.maps.model.LatLng;
import java.util.List;

import com.codram.terecojo.data.model.ApiResponse;
import com.codram.terecojo.data.model.Offer;
import com.codram.terecojo.data.model.OfferRequest;
import com.codram.terecojo.data.model.Vehicle;
import com.codram.terecojo.data.remote.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverViewModel extends AndroidViewModel {
    private final MapRepository mapRepository = new MapRepository();
    private final MutableLiveData<String> polylineResult = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    private final MutableLiveData<List<Vehicle>> vehicles = new MutableLiveData<>();
    private final MutableLiveData<Boolean> offerSuccess = new MutableLiveData<>();

    public DriverViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<String> getPolylineResult() { return polylineResult; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<List<Vehicle>> getVehicles() { return vehicles; }
    public LiveData<Boolean> getOfferSuccess() { return offerSuccess; }

    public void fetchMyVehicles() {
        RetrofitClient.getService(getApplication()).getVehicles().enqueue(new Callback<ApiResponse<List<Vehicle>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Vehicle>>> call, Response<ApiResponse<List<Vehicle>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    vehicles.postValue(response.body().getData());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Vehicle>>> call, Throwable t) {
                errorMessage.postValue(t.getMessage());
            }
        });
    }

    public void sendOffer(String solicitudId, OfferRequest request) {
        RetrofitClient.getService(getApplication()).responderSolicitud(solicitudId, request).enqueue(new Callback<ApiResponse<Offer>>() {
            @Override
            public void onResponse(Call<ApiResponse<Offer>> call, Response<ApiResponse<Offer>> response) {
                if (response.isSuccessful()) {
                    offerSuccess.postValue(true);
                } else {
                    errorMessage.postValue("Error al enviar oferta");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Offer>> call, Throwable t) {
                errorMessage.postValue(t.getMessage());
            }
        });
    }

    public void fetchDirections(LatLng origin, LatLng destination, List<LatLng> waypoints) {
        mapRepository.getDirections(origin, destination, waypoints, new MapRepository.MapCallback<String>() {
            @Override
            public void onSuccess(String result) {
                polylineResult.postValue(result);
            }

            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
            }
        });
    }
}
