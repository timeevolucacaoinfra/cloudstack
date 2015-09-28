package org.apache.cloudstack.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

public class PoolResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the Pool ID")
    private Long id;

    @SerializedName("name")
    @Param(description = "the Pool Load name")
    private String name;

    @SerializedName("lbmethod")
    @Param(description = "the Pool Load balancer Method")
    private String lbMethod;


    @SerializedName("port")
    @Param(description = "the Pool Port")
    private Integer port;

    @SerializedName("vipport")
    @Param(description = "the Vip Port")
    private Integer vipPort;

    @SerializedName("maxconn")
    @Param(description = "the max connections")
    private Integer maxconn;


    @SerializedName("healthchecktype")
    @Param(description = "Healthcheck type")
    private String healthcheckType;

    @SerializedName("healthcheck")
    @Param(description = "Healthcheck")
    private String healthcheck;

    @SerializedName("healthcheckexpect")
    @Param(description = "Expected healthcheck")
    private String expectedHealthcheck;

    public String getHealthcheckType() {
        return healthcheckType;
    }

    public Integer getMaxconn() {
        return maxconn;
    }

    public void setMaxconn(Integer maxconn) {
        this.maxconn = maxconn;
    }

    public void setHealthcheckType(String healthcheckType) {
        this.healthcheckType = healthcheckType;
    }

    public String getHealthcheck() {
        return healthcheck;
    }

    public void setHealthcheck(String healthcheck) {
        this.healthcheck = healthcheck;
    }

    public String getExpectedHealthcheck() {
        return expectedHealthcheck;
    }

    public void setExpectedHealthcheck(String expectedHealthcheck) {
        this.expectedHealthcheck = expectedHealthcheck;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLbMethod() {
        return lbMethod;
    }

    public void setLbMethod(String lbMethod) {
        this.lbMethod = lbMethod;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getVipPort() {
        return vipPort;
    }

    public void setVipPort(Integer vipPort) {
        this.vipPort = vipPort;
    }
}
