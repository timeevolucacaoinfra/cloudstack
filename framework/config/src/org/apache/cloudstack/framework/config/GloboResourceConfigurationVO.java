package org.apache.cloudstack.framework.config;

import org.apache.cloudstack.config.GloboResourceConfiguration;
import org.apache.cloudstack.framework.config.dao.GloboResourceConfigurationDao;

import javax.persistence.*;

/**
 * Created by sinval.neto on 7/13/16.
 */
@Entity
@Table(name = "globo_resource_configuration")
public class GloboResourceConfigurationVO implements GloboResourceConfiguration {

    @Id
    @Column(name = "id")
    private Long id;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "resourceType")
    private GloboResourceConfigurationDao.ResourceType resourceType;

    @Column(name = "resource")
    private Long resourceId;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "key")
    private GloboResourceConfiguration.key key;

    @Column(name = "value", length = 8191)
    private String value;

    @Override
    public String getInstance() {
        return null;
    }
}
