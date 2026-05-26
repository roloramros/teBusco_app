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

    @SerializedName("proxima_renovacion")
    private String proximaRenovacion;

    public String getEstado() { return estado; }
    public int getDiasRestantes() { return diasRestantes; }
    public String getTrialInicio() { return trialInicio; }
    public String getTrialFin() { return trialFin; }
    public String getSuscripcionFin() { return suscripcionFin; }
    public String getUltimoPago() { return ultimoPago; }
    public double getMontoMensual() { return montoMensual; }
    public double getSaldoFondo() { return saldoFondo; }
    public String getProximaRenovacion() { return proximaRenovacion; }

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
