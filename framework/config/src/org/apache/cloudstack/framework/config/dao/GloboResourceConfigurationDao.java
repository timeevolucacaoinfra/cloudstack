package org.apache.cloudstack.framework.config.dao;


import java.util.Map;

/**
 * Created by sinval.neto on 7/13/16.
 */
public interface GloboResourceConfigurationDao {

    public Map<String, String> getConfiguration(long resourceId);

    public Map<String, String> getConfiguration(ResourceType resourceType, long resourceId);

    public enum ResourceType {
        LOAD_BALANCER
    }
}