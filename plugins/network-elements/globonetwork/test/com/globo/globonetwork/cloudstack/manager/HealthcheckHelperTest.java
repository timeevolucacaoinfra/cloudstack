package com.globo.globonetwork.cloudstack.manager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by lucas.castro on 3/10/16.
 */
public class HealthcheckHelperTest {



    @Test
    public void testBuild() {
        HealthcheckHelper healthcheck = HealthcheckHelper.build("lb.test.com", "HTTP", "/health.html", "200 OK");
        assertEquals("HTTP", healthcheck.getHealthcheckType());
        assertEquals("200 OK", healthcheck.getExpectedHealthcheck());
        assertEquals("GET /health.html HTTP/1.0\\r\\nHost: lb.test.com\\r\\n\\r\\n", healthcheck.getHealthcheck());

        healthcheck = HealthcheckHelper.build("lb.test.com", "TCP", null, null);
        assertEquals("TCP", healthcheck.getHealthcheckType());
        assertEquals("", healthcheck.getHealthcheck());
        assertNull(healthcheck.getExpectedHealthcheck());


    }

    @Test(expected=IllegalArgumentException.class)
    public void testBuild_validate() {
        HealthcheckHelper healthcheck = HealthcheckHelper.build("lb.test.com", "TCP", null, "WORKING");
        assertEquals("TCP", healthcheck.getHealthcheckType());
        assertEquals("", healthcheck.getHealthcheck());
        assertNull(healthcheck.getExpectedHealthcheck());
    }

    @Test
    public void testBuild_http_old_version() {
        HealthcheckHelper healthcheck = HealthcheckHelper.build("lb.test.com", null, "/health.html", null);
        assertEquals("HTTP", healthcheck.getHealthcheckType());
        assertEquals("WORKING", healthcheck.getExpectedHealthcheck());
        assertEquals("GET /health.html HTTP/1.0\\r\\nHost: lb.test.com\\r\\n\\r\\n", healthcheck.getHealthcheck());
    }


    @Test
    public void testBuild_tcp_old_version() {
        HealthcheckHelper healthcheck = HealthcheckHelper.build("lb.test.com", null, null, null);
        assertEquals("TCP", healthcheck.getHealthcheckType());
        assertEquals("", healthcheck.getHealthcheck());
        assertNull(healthcheck.getExpectedHealthcheck());
    }




    @Test
    public void testBuildHealthCheckStringGivenPathAndHostNull(){
        HealthcheckHelper healthcheck = HealthcheckHelper.build(null, "HTTP", null, "200 OK");
        assertEquals("", healthcheck.buildHealthCheckString(null, null));
    }

    @Test
    public void testBuildHealthCheckStringGivenPathNullAndHostFilled(){
        HealthcheckHelper healthCheck = HealthcheckHelper.build("host", "HTTP", null, "200 OK");
        assertEquals("", healthCheck.buildHealthCheckString(null, "host"));
    }

    @Test
    public void testBuildHealthCheckStringGiveFullHTTPPath(){
        HealthcheckHelper healthCheck = HealthcheckHelper.build("lb.test.com", "HTTP", "/healthcheck.html", "200 OK");
        assertEquals("GET /healtcheck.html", healthCheck.buildHealthCheckString("GET /healtcheck.html", "lb.test.com"));
    }

    @Test
    public void testBuildHealthCheckStringGivenURIandHost(){
        HealthcheckHelper healthCheck = HealthcheckHelper.build("lb.test.com", "HTTP", "/healthcheck.html", "200 OK");
        assertEquals("GET /healtcheck.html HTTP/1.0\\r\\nHost: lb.test.com\\r\\n\\r\\n", healthCheck.buildHealthCheckString("/healtcheck.html", "lb.test.com"));
    }

}