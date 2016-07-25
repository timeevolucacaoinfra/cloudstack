package org.apache.cloudstack.globoconfig;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;




/**
 * Created by sinval.neto on 7/15/16.
 */
@Entity
@Table(name = "globo_resource_configuration")
public class GloboResourceConfigurationVO implements GloboResourceConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "resource_type")
    private GloboResourceType resourceType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "key")
    private GloboResourceKey key;

    @Column(name = "value", length = 255)
    private String value;

    public GloboResourceConfigurationVO(){}

    public GloboResourceConfigurationVO(GloboResourceType resourceType, Long resourceId, GloboResourceKey key, String value){
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.key = key;
        this.value = value;
    }

    @Override
    public String getInstance() {
        return null;
    }

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public GloboResourceType getResourceType() {
        return this.resourceType;
    }

    @Override
    public Long getResourceId() {
        return this.resourceId;
    }

    @Override
    public GloboResourceKey getKey() {
        return this.key;
    }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public void setValue(String value) {this.value = value; }
}
