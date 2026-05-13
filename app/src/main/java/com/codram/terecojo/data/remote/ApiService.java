package com.codram.terecojo.data.remote;

import com.codram.terecojo.data.model.ApiResponse;
import com.codram.terecojo.data.model.AuthResponse;
import com.codram.terecojo.data.model.FcmTokenRequest;
import com.codram.terecojo.data.model.LoginRequest;
import com.codram.terecojo.data.model.MunicipalityResponse;
import com.codram.terecojo.data.model.Notification;
import com.codram.terecojo.data.model.Province;
import com.codram.terecojo.data.model.RegisterRequest;
import com.codram.terecojo.data.model.RideRequest;
import com.codram.terecojo.data.model.Offer;
import com.codram.terecojo.data.model.OfferRequest;
import com.codram.terecojo.data.model.StatsResponse;
import com.codram.terecojo.data.model.Vehicle;
import com.codram.terecojo.data.model.VehicleRequest;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ApiService {

    @GET("api/geo/provincias")
    Call<ApiResponse<List<Province>>> getProvincias();

    @GET("api/geo/stats")
    Call<ApiResponse<StatsResponse>> getStats();

    @GET("api/geo/provincias/{id}/municipios")
    Call<ApiResponse<MunicipalityResponse>> getMunicipios(@Path("id") int provinciaId);

    @POST("api/auth/registro")
    Call<ApiResponse<AuthResponse>> registro(@Body RegisterRequest request);

    @POST("api/auth/login")
    Call<ApiResponse<AuthResponse>> login(@Body LoginRequest request);

    @POST("api/auth/logout")
    Call<ApiResponse<Void>> logout();

    @POST("api/auth/update-fcm-token")
    Call<ApiResponse<Void>> updateFcmToken(@Body FcmTokenRequest request);

    @GET("api/auth/me")
    Call<ApiResponse<AuthResponse.User>> getMe();

    @GET("api/geo/vehicle-types")
    Call<ApiResponse<List<String>>> getVehicleTypes();

    @GET("api/vehicles")
    Call<ApiResponse<List<Vehicle>>> getVehicles();

    @Multipart
    @POST("api/vehicles")
    Call<ApiResponse<Vehicle>> addVehicle(
            @Part("marca") RequestBody marca,
            @Part("placa") RequestBody placa,
            @Part("tipo") RequestBody tipo,
            @Part("capacidad_pasajeros") RequestBody capacidad,
            @Part MultipartBody.Part foto
    );

    @Multipart
    @PUT("api/vehicles/{id}")
    Call<ApiResponse<Vehicle>> updateVehicle(
            @Path("id") String id,
            @Part("marca") RequestBody marca,
            @Part("placa") RequestBody placa,
            @Part("tipo") RequestBody tipo,
            @Part("capacidad_pasajeros") RequestBody capacidad,
            @Part MultipartBody.Part foto
    );

    @DELETE("api/vehicles/{id}")
    Call<ApiResponse<Void>> deleteVehicle(@Path("id") String id);

    // SOLICITUDES
    @POST("api/solicitudes")
    Call<ApiResponse<RideRequest>> createSolicitud(@Body RideRequest request);

    @GET("api/solicitudes/radar")
    Call<ApiResponse<List<RideRequest>>> getRadarSolicitudes();

    @GET("api/solicitudes/mis-solicitudes")
    Call<ApiResponse<List<RideRequest>>> getMisSolicitudes();

    @POST("api/solicitudes/{id}/cancelar")
    Call<ApiResponse<Void>> cancelarSolicitud(@Path("id") String id);

    @POST("api/solicitudes/{id}/finalizar")
    Call<ApiResponse<Object>> finalizarViaje(
            @Path("id") String solicitudId,
            @Body FinalizeRideRequest body
    );

    @GET("api/solicitudes/{id}")
    Call<ApiResponse<RideRequest>> getSolicitudById(@Path("id") String id);

    // SISTEMA DE OFERTAS
    @POST("api/solicitudes/{id}/responder")
    Call<ApiResponse<Offer>> responderSolicitud(@Path("id") String solicitudId, @Body OfferRequest request);

    @GET("api/solicitudes/{id}/ofertas")
    Call<ApiResponse<List<Offer>>> getOfertas(@Path("id") String solicitudId);

    @POST("api/solicitudes/ofertas/{id}/aceptar")
    Call<ApiResponse<Void>> aceptarOferta(@Path("id") String respuestaId);

    @POST("api/solicitudes/ofertas/{id}/rechazar")
    Call<ApiResponse<Void>> rechazarOferta(@Path("id") String respuestaId, @Body com.codram.terecojo.data.model.RejectOfferRequest request);

    // NOTIFICACIONES
    @GET("api/notificaciones")
    Call<ApiResponse<List<Notification>>> getNotifications();

    @PATCH("api/notificaciones/{id}/read")
    Call<ApiResponse<Void>> markNotificationAsRead(@Path("id") String id);

    @POST("api/notificaciones/read-all")
    Call<ApiResponse<Void>> markAllAsRead();

    @DELETE("api/notificaciones/{id}")
    Call<ApiResponse<Void>> deleteNotification(@Path("id") String id);

    @DELETE("api/notificaciones")
    Call<ApiResponse<Void>> deleteAllNotifications();
}
