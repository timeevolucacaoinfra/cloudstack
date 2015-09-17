package com.globo.globonetwork.cloudstack.response;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import java.util.List;

public class GloboNetworkListPoolResponse extends Answer{

    public GloboNetworkListPoolResponse(Command command, boolean success, String details, List<Pool> pools) {
        super(command, success, details);
        this.pools = pools;
    }

    public GloboNetworkListPoolResponse(List<Pool> pools) {
        this.pools = pools;
    }

    private List<Pool> pools;

    public List<Pool> getPools() {
        return pools;
    }

    public void setPools(List<Pool> pools) {
        this.pools = pools;
    }

    public static class Pool {
        private Long id;
        private String identifier;
        private String lbMethod;
        private Integer port;
        private String healthcheckType;
        private String expectedHealthcheck;
        private String healthcheck;
        private Integer maxconn;

        public String getHealthcheckType() {
            return healthcheckType;
        }

        public void setHealthcheckType(String healthcheckType) {
            this.healthcheckType = healthcheckType;
        }

        public String getExpectedHealthcheck() {
            return expectedHealthcheck;
        }

        public void setExpectedHealthcheck(String expectedHealthcheck) {
            this.expectedHealthcheck = expectedHealthcheck;
        }

        public String getHealthcheck() {
            return healthcheck;
        }

        public void setHealthcheck(String healthcheck) {
            this.healthcheck = healthcheck;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
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


        public Integer getMaxconn() {
            return maxconn;
        }
        public void setMaxconn(Integer maxconn) {
            this.maxconn = maxconn;
        }
    }
}
