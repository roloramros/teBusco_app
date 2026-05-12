package com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MunicipalityResponse {
    @SerializedName("provincia")
    private Province province;
    
    @SerializedName("municipios")
    private List<Municipality> municipalities;

    public Province getProvince() { return province; }
    public List<Municipality> getMunicipalities() { return municipalities; }
}
