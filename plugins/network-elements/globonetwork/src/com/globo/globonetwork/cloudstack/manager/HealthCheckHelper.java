package com.globo.globonetwork.cloudstack.manager;

/**
 * Created by lucas.castro on 3/10/16.
 */
public class HealthCheckHelper {

        private String host;
        private String healthCheckType;
        private String healthCheck;
        private String expectedHealthCheck;

        private static final String DEFAULT_EXPECT_FOR_HTTP_HEALTHCHECK = "WORKING";

        private HealthCheckHelper(String host) {
            this.host = host;
        }

        public String getHealthCheckType() {
            return healthCheckType;
        }

        public String getHealthCheck() {
            return healthCheck;
        }

        public String getExpectedHealthCheck() {
            return expectedHealthCheck;
        }

        protected HealthCheckHelper build(String healthcheckType, String healthcheck, String expectedHealthcheck) {
            this.healthCheck = this.buildHealthCheckString(healthcheck, host);

            //old version, client just pass healthcheckpath
            if (healthcheck != null && !healthcheck.isEmpty() &&
                    (expectedHealthcheck == null || !expectedHealthcheck.isEmpty()) &&
                    (healthcheckType == null || healthcheckType.isEmpty())
                    )
            {
                this.healthCheckType = GloboNetworkManager.HealthCheckType.HTTP.name();
                this.expectedHealthCheck = DEFAULT_EXPECT_FOR_HTTP_HEALTHCHECK;
                return this;
            }

            //new version
            if ( healthcheckType != null ){
                this.healthCheckType = GloboNetworkManager.HealthCheckType.valueOf(healthcheckType).name();

                if  (healthCheckType.equals(GloboNetworkManager.HealthCheckType.TCP.name()) || healthCheckType.equals(GloboNetworkManager.HealthCheckType.UDP.name())) {
                    this.expectedHealthCheck = null;
                    this.healthCheck = null;
                } else {
                    this.expectedHealthCheck = expectedHealthcheck;
                }
            } else {
                this.healthCheckType = GloboNetworkManager.HealthCheckType.TCP.name();
                this.expectedHealthCheck = null;
                this.healthCheck = null;
            }

            return this;
        }

        protected String buildHealthCheckString(String path, String host) {
            if (path == null || path.equals("") || host == null) {
                return "";
            }
            if (path.startsWith("GET") || path.startsWith("POST")) {
                return path;
            }
            return "GET " + path + " HTTP/1.0\\r\\nHost: " + host + "\\r\\n\\r\\n";
        }


        public void validate(String host, String healthehckType, String healthcheck, String expectedHealthcheck){


            if ( healthehckType != null && (healthehckType.equals(GloboNetworkManager.HealthCheckType.TCP.name()) || healthehckType.equals(GloboNetworkManager.HealthCheckType.UDP.name()))){
                if (expectedHealthcheck != null && !expectedHealthcheck.isEmpty()) {
                    throw new IllegalArgumentException("When healthCheckType is TCP/UDP expectedHealthCheck should be empty! type: " + healthehckType + ", expectedHealthCheck: " + expectedHealthcheck);
                }

                if (healthcheck != null && !healthcheck.isEmpty()) {
                    throw new IllegalArgumentException("When healthCheckType is TCP healthCheck should be empty! type: " + healthehckType + ",healthCheck: " + healthcheck);
                };
            }



        }

        public static HealthCheckHelper build(String host, String healthehckType, String healthCheck, String expectedHealthCheck) {
            HealthCheckHelper healthCheckHelper = new HealthCheckHelper(host);
            healthCheckHelper.validate(host, healthehckType, healthCheck, expectedHealthCheck);
            return healthCheckHelper.build(healthehckType, healthCheck, expectedHealthCheck);
        }
}

