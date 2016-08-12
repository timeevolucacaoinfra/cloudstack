package org.apache.cloudstack.globoconfig;

import com.cloud.utils.db.GenericDao;

import java.util.List;
import java.util.Map;



/**
 * Created by sinval.neto on 7/15/16.
 */
public interface GloboResourceConfigurationDao extends GenericDao<GloboResourceConfigurationVO, String> {
    Map<String, String> getConfiguration(long resourceId);

    List<GloboResourceConfigurationVO> getConfiguration(GloboResourceType resourceType, String resourceUuid);

    List<GloboResourceConfigurationVO> getConfiguration(GloboResourceType resourceType, String resourceUuid, GloboResourceKey key);


}
