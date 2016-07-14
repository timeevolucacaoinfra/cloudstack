package org.apache.cloudstack.framework.config.dao;

//import com.cloud.utils.component.ComponentLifecycle;
import com.cloud.utils.db.GenericDaoBase;
//import com.cloud.utils.db.SearchBuilder;
//import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.framework.config.GloboResourceConfigurationVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
//import java.util.List;
import java.util.Map;

/**
 * Created by sinval.neto on 7/13/16.
 */
@Component
@Local(value = {GloboResourceConfigurationDao.class})
public class GloboResourceConfigurationDaoImpl extends GenericDaoBase<GloboResourceConfigurationVO, String> implements GloboResourceConfigurationDao {
    private static final Logger s_logger = Logger.getLogger(ConfigurationDaoImpl.class);

    //final SearchBuilder<GloboResourceConfigurationVO> ResourceTypeSearch;
    //final SearchBuilder<GloboResourceConfigurationVO> ResourceIdSearch;


    public GloboResourceConfigurationDaoImpl() {
        //ResourceTypeSearch = createSearchBuilder();
        //ResourceTypeSearch.and("instance", ResourceTypeSearch.entity().asdasd(), SearchCriteria.Op.EQ);

        //ResourceIdSearch = createSearchBuilder();
        //ResourceIdSearch.and("name", ResourceIdSearch.entity().getName(), SearchCriteria.Op.EQ);
        //setRunLevel(ComponentLifecycle.RUN_LEVEL_SYSTEM_BOOTSTRAP);
    }

    @Override
    public Map<String, String> getConfiguration(long resourceId) {
        return null;
    }

    @Override
    public Map<String, String> getConfiguration(ResourceType resourceType, long resourceId) {
       // SearchCriteria<GloboResourceConfigurationVO> sc = ResourceTypeSearch.create();
        //sc.setParameters("instance", "DEFAULT");

        //List<GloboResourceConfigurationVO> configurations = listIncludingRemovedBy(sc);
        return null;
    }
}
