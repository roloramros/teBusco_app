package com.codram.terecojo.data.model;

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
