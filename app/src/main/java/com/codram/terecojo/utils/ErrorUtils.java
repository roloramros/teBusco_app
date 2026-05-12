package com.codram.terecojo.utils;

import android.util.Log;
import com.codram.terecojo.data.model.ApiResponse;
import com.google.gson.Gson;
import retrofit2.Response;

public class ErrorUtils {

    /**
     * Extrae el mensaje de error de una respuesta de Retrofit que no fue exitosa.
     * @param response La respuesta de Retrofit
     * @param defaultMessage Mensaje por defecto si no se puede parsear la respuesta
     * @return El mensaje de error del servidor o el mensaje por defecto
     */
    public static String parseError(Response<?> response, String defaultMessage) {
        if (response.isSuccessful()) return null;

        try {
            if (response.errorBody() != null) {
                String errorJson = response.errorBody().string();
                ApiResponse<?> errorResponse = new Gson().fromJson(errorJson, ApiResponse.class);
                if (errorResponse != null && errorResponse.getMessage() != null) {
                    return errorResponse.getMessage();
                }
            } else if (response.body() != null) {
                // Caso raro donde isSuccessful es false pero hay body
                ApiResponse<?> body = (ApiResponse<?>) response.body();
                if (body.getMessage() != null) return body.getMessage();
            }
        } catch (Exception e) {
            Log.e("ErrorUtils", "Error parsing server error response", e);
        }

        return defaultMessage;
    }

    public static String parseError(Response<?> response) {
        return parseError(response, "Ha ocurrido un error en el servidor");
    }
}
