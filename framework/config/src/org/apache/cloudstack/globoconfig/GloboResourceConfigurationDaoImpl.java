package org.apache.cloudstack.globoconfig;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import java.util.List;
import java.util.Map;

/**
 * Created by sinval.neto on 7/15/16.
 */
@Component
@Local(value = {GloboResourceConfigurationDao.class})
public class GloboResourceConfigurationDaoImpl extends GenericDaoBase<GloboResourceConfigurationVO, String> implements GloboResourceConfigurationDao {
    private static final Logger s_logger = Logger.getLogger(GloboResourceConfigurationDaoImpl.class);

    private final SearchBuilder<GloboResourceConfigurationVO> ListByResourceId;



    public GloboResourceConfigurationDaoImpl() {
        ListByResourceId = createSearchBuilder();
        ListByResourceId.and("resourceId", ListByResourceId.entity().getResourceId(), SearchCriteria.Op.EQ);
        ListByResourceId.and("resourceType", ListByResourceId.entity().getResourceType(), SearchCriteria.Op.EQ);
        ListByResourceId.and("key", ListByResourceId.entity().getKey(), SearchCriteria.Op.EQ);

        ListByResourceId.done();
    }

    @Override
    public Map<String, String> getConfiguration(long resourceId) {
        return null;
    }

    @Override
    public List<GloboResourceConfigurationVO> getConfiguration(GloboResourceType resourceType, long resourceId) {
        SearchCriteria<GloboResourceConfigurationVO> sc = ListByResourceId.create();
        sc.setParameters("resourceType", resourceType);
        sc.setParameters("resourceId", resourceId);
        return listBy(sc);
    }

    @Override
    public List<GloboResourceConfigurationVO> getConfiguration(GloboResourceType resourceType, long resourceId, GloboResourceKey key) {
        SearchCriteria<GloboResourceConfigurationVO> sc = ListByResourceId.create();
        sc.setParameters("resourceType", resourceType);
        sc.setParameters("resourceId", resourceId);
        sc.setParameters("key", key);
        return listBy(sc);
    }
}