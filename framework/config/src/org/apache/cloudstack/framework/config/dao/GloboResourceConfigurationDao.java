package org.apache.cloudstack.framework.config.dao;


import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.framework.config.GloboResourceConfigurationVO;

import java.util.Map;

/**
 * Created by sinval.neto on 7/13/16.
 */
public interface GloboResourceConfigurationDao extends GenericDao<GloboResourceConfigurationVO, String> {

    public Map<String, String> getConfiguration(long resourceId);

    public Map<String, String> getConfiguration(ResourceType resourceType, long resourceId);

    public enum ResourceType {
        LOAD_BALANCER
    }
}