package com.globo.globonetwork.cloudstack.manager;

/**
 * Created by lucas.castro on 3/10/16.
 */
public class HealthcheckHelper {

        private String host;
        private String healthcheckType;
        private String healthcheck;
        private String expectedHealthcheck;

        private static final String DEFAULT_EXPECT_FOR_HTTP_HEALTHCHECK = "WORKING";

        private HealthcheckHelper(String host) {
            this.host = host;
        }

        public String getHealthcheckType() {
            return healthcheckType;
        }

        public String getHealthcheck() {
            return healthcheck;
        }

        public String getExpectedHealthcheck() {
            return expectedHealthcheck;
        }

        protected HealthcheckHelper build(String healthcheckType, String healthcheck, String expectedHealthcheck) {
            this.healthcheck = this.buildHealthCheckString(healthcheck, host);

            //old version, client just pass healthcheckpath
            if (healthcheck != null && !healthcheck.isEmpty() &&
                    (expectedHealthcheck == null || !expectedHealthcheck.isEmpty()) &&
                    (healthcheckType == null || healthcheckType.isEmpty())
                    )
            {
                this.healthcheckType = GloboNetworkManager.HealthCheckType.HTTP.name();
                this.expectedHealthcheck = DEFAULT_EXPECT_FOR_HTTP_HEALTHCHECK;
                return this;
            }

            //new version
            if  (healthcheck == null || healthcheck.isEmpty()) {
                this.healthcheckType = GloboNetworkManager.HealthCheckType.TCP.name();
                this.expectedHealthcheck = null;
            } else {
                this.healthcheckType = GloboNetworkManager.HealthCheckType.HTTP.name();
                this.expectedHealthcheck = expectedHealthcheck;
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


            if ( healthehckType != null && healthehckType.equals(GloboNetworkManager.HealthCheckType.TCP.name())){
                if (expectedHealthcheck != null && !expectedHealthcheck.isEmpty()) {
                    throw new IllegalArgumentException("When healthcheckType is TCP expectedHealthcheck should be empty! expectedHealthcheck: " + expectedHealthcheck);
                }

                if (healthcheck != null && !healthcheck.isEmpty()) {
                    throw new IllegalArgumentException("When healthcheckType is TCP healthcheck should be empty! healthcheck: " + healthcheck);
                };
            }



        }

        public static HealthcheckHelper build(String host, String healthehckType, String healthcheck, String expectedHealthcheck) {
            HealthcheckHelper healthcheckHelper = new HealthcheckHelper(host);
            healthcheckHelper.validate(host, healthehckType, healthcheck, expectedHealthcheck);
            return healthcheckHelper.build(healthehckType, healthcheck, expectedHealthcheck);
        }
}

