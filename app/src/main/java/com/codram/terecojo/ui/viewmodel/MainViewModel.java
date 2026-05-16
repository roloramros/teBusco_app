package com.codram.terecojo.ui.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.codram.terecojo.data.model.SelectionMode;
import com.codram.terecojo.data.remote.MapRepository;
import com.codram.terecojo.data.model.ApiResponse;
import com.codram.terecojo.data.model.FinalizeRideRequest;
import com.codram.terecojo.data.remote.RetrofitClient;
import com.google.android.gms.maps.model.LatLng;
import java.util.List;

public class MainViewModel extends AndroidViewModel {
    private final MapRepository mapRepository = new MapRepository();

    private final MutableLiveData<GeocodeResult> geocodeResult = new MutableLiveData<>();
    private final MutableLiveData<Double> distanceResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> viajeFinalizadoExito = new MutableLiveData<>();
    private final MutableLiveData<String> finalizarViajeError = new MutableLiveData<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<GeocodeResult> getGeocodeResult() { return geocodeResult; }
    public LiveData<Double> getDistanceResult() { return distanceResult; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getViajeFinalizadoExito() { return viajeFinalizadoExito; }
    public LiveData<String> getFinalizarViajeError() { return finalizarViajeError; }

    public void finalizarViaje(String solicitudId, int estrellas, String comentario) {
        isLoading.postValue(true);
        FinalizeRideRequest request = new FinalizeRideRequest(estrellas, comentario);
        RetrofitClient.getService().finalizarViaje(solicitudId, request) // MODIFICADO
                .enqueue(new retrofit2.Callback<ApiResponse<Object>>() {
                    @Override
                    public void onResponse(retrofit2.Call<ApiResponse<Object>> call, retrofit2.Response<ApiResponse<Object>> response) {
                        isLoading.postValue(false);
                        if (response.isSuccessful()) {
                            viajeFinalizadoExito.postValue(true);
                        } else {
                            finalizarViajeError.postValue("Error al finalizar el viaje");
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<ApiResponse<Object>> call, Throwable t) {
                        isLoading.postValue(false);
                        finalizarViajeError.postValue("Error de red: " + t.getMessage());
                    }
                });
    }

    public void reverseGeocode(LatLng point, SelectionMode mode, int stopIndex) {
        mapRepository.reverseGeocode(point, new MapRepository.MapCallback<com.codram.terecojo.data.model.LocationDetails>() {
            @Override
            public void onSuccess(com.codram.terecojo.data.model.LocationDetails details) {
                geocodeResult.postValue(new GeocodeResult(details, point, mode, stopIndex));
            }

            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
            }
        });
    }

    public void calculateDistance(LatLng origin, LatLng destination, List<LatLng> waypoints) {
        isLoading.postValue(true);
        mapRepository.calculateDistance(origin, destination, waypoints, new MapRepository.MapCallback<Double>() {
            @Override
            public void onSuccess(Double result) {
                distanceResult.postValue(result);
                isLoading.postValue(false);
            }

            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
                isLoading.postValue(false);
            }
        });
    }

    public static class GeocodeResult {
        public final com.codram.terecojo.data.model.LocationDetails details;
        public final LatLng point;
        public final SelectionMode mode;
        public final int stopIndex;

        public GeocodeResult(com.codram.terecojo.data.model.LocationDetails details, LatLng point, SelectionMode mode, int stopIndex) {
            this.details = details;
            this.point = point;
            this.mode = mode;
            this.stopIndex = stopIndex;
        }
    }
}
