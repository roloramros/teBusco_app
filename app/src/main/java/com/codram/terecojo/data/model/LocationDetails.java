package com.codram.terecojo.data.model;

public class LocationDetails {
    private String formattedAddress;
    private String province;
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
