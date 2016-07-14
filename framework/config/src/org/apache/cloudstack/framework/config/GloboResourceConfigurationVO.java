package org.apache.cloudstack.framework.config;

import org.apache.cloudstack.config.GloboResourceConfiguration;
import org.apache.cloudstack.framework.config.dao.GloboResourceConfigurationDao.ResourceType;

import javax.persistence.GenerationType;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.GeneratedValue;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;


/**
 * Created by sinval.neto on 7/13/16.
 */
@Entity
@Table(name = "globo_resource_configuration")
public class GloboResourceConfigurationVO implements GloboResourceConfiguration {

    public enum Key {
        isDNSRegistered
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "resourceType")
    private ResourceType resourceType;

    @Column(name = "resource")
    private Long resourceId;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "key")
    private Key key;

    @Column(name = "value", length = 8191)
    private String value;

    @Override
    public String getInstance() {
        return null;
    }

    public GloboResourceConfigurationVO(ResourceType resourceType, Long resourceId, Key key, String value){
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.key = key;
        this.value = value;
    }
}
