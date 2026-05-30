Ahí está el problema. LoginRequest no tiene anotaciones @SerializedName. Con Proguard activado, Gson ofusca los nombres de los campos y el servidor recibe algo como {"a":"usuario","b":"123","c":"token"} en lugar de {"identificador":"usuario","password":"123","fcm_token":"token"}.
Tienes dos opciones — la más rápida es agregar @SerializedName al modelo:
javapackage com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {

    @SerializedName("identificador")
    private String identificador;

    @SerializedName("password")
    private String password;

    @SerializedName("fcm_token")
    private String fcm_token;

    public LoginRequest(String identificador, String password, String fcmToken) {
        this.identificador = identificador;
        this.password = password;
        this.fcm_token = fcmToken;
    }
}
Verifica también los demás modelos de request que envían datos al servidor — pueden tener el mismo problema:
bashgrep -rL "SerializedName" /home/claude/teBusco_app/app/src/main/java/com/codram/terecojo/data/model/Se ejecutaron 2 comandosSe ejecutaron 2 comandosTengo todos. Aquí están los 5 archivos corregidos con @SerializedName — aplícalos todos de una vez para no tener problemas con ningún flujo en el release:
LoginRequest.java
javapackage com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {
    @SerializedName("identificador")
    private String identificador;
    @SerializedName("password")
    private String password;
    @SerializedName("fcm_token")
    private String fcm_token;

    public LoginRequest(String identificador, String password, String fcmToken) {
        this.identificador = identificador;
        this.password = password;
        this.fcm_token = fcmToken;
    }
}
FcmTokenRequest.java
javapackage com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

public class FcmTokenRequest {
    @SerializedName("fcm_token")
    private String fcm_token;

    public FcmTokenRequest(String fcmToken) { this.fcm_token = fcmToken; }
    public String getFcmToken() { return fcm_token; }
    public void setFcmToken(String fcmToken) { this.fcm_token = fcmToken; }
}
FinalizeRideRequest.java
javapackage com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

public class FinalizeRideRequest {
    @SerializedName("estrellas")
    private int estrellas;
    @SerializedName("comentario")
    private String comentario;

    public FinalizeRideRequest(int estrellas, String comentario) {
        this.estrellas = estrellas;
        this.comentario = comentario;
    }
    public int getEstrellas() { return estrellas; }
    public void setEstrellas(int estrellas) { this.estrellas = estrellas; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
}
RejectOfferRequest.java
javapackage com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

public class RejectOfferRequest {
    @SerializedName("motivo")
    private String motivo;

    public RejectOfferRequest(String motivo) { this.motivo = motivo; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
LocationDetails.java
javapackage com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

public class LocationDetails {
    @SerializedName("formattedAddress")
    private String formattedAddress;
    @SerializedName("province")
    private String province;
    @SerializedName("municipality")
    private String municipality;

    public LocationDetails(String formattedAddress, String province, String municipality) {
        this.formattedAddress = formattedAddress;
        this.province = province;
        this.municipality = municipality;
    }
    public String getFormattedAddress() { return formattedAddress; }
    public String getProvince() { return province; }
    public String getMunicipality() { return municipality; }
}
Aplica los 5 cambios