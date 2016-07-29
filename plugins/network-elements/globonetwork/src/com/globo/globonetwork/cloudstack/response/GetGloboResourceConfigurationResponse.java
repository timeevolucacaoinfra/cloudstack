package com.globo.globonetwork.cloudstack.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.BaseResponse;

/**
 * Created by sinval.neto on 7/27/16.
 */
public class GetGloboResourceConfigurationResponse extends BaseResponse {

    @SerializedName("uuid")
    @Param(
            description = "the id of the resource"
    )
    private String uuid;

    @SerializedName("resourcetype")
    @Param(
            description = "the resourcetype of the resource"
    )
    private String resourceType;

    @SerializedName("configurationkey")
    @Param(
            description = "the configuration key"
    )
    private String configurationKey;

    @SerializedName("configurationvalue")
    @Param(
            description = "the configuration value"
    )
    private String configurationValue;

    public GetGloboResourceConfigurationResponse() {
    }

    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getResourceType() {
        return this.resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public void setConfigurationKey(String configurationKey){
        this.configurationKey = configurationKey;
    }

    public void setConfigurationValue(String configurationValue){
        this.configurationValue = configurationValue;
    }
}
