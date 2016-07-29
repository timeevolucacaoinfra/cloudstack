package com.globo.globonetwork.cloudstack.api;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.ResourceAllocationException;
import com.globo.globonetwork.cloudstack.manager.GloboNetworkService;
import com.globo.globonetwork.cloudstack.response.GetGloboResourceConfigurationResponse;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.globoconfig.GloboResourceConfigurationVO;
import org.apache.cloudstack.globoconfig.GloboResourceType;
import org.apache.log4j.Logger;

import javax.inject.Inject;

/**
 * Created by sinval.neto on 7/27/16.
 */
@APICommand(name = "getGloboResourceConfiguration", description = "Get a GlovoResourceConfiguration", responseObject = GetGloboResourceConfigurationResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class GetGloboResourceConfigurationCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(GetGloboResourceConfigurationCmd.class.getName());

    private static final String s_name = "getgloboresourceconfiguration";

    @Inject
    GloboNetworkService _globoNetworkService;

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.UUID, type = CommandType.STRING, entityType = GetGloboResourceConfigurationResponse.class, description = "the ID of the RESOURCE")
    private String uuid;

    @Parameter(name = ApiConstants.RESOURCE_TYPE, type = CommandType.STRING, entityType = GetGloboResourceConfigurationResponse.class, description = "the type of the resource")
    private String resourceType;

    public String getUuid() {
        return uuid;
    }

    public String getResourceType() {
        return this.resourceType;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        GetGloboResourceConfigurationResponse response = new GetGloboResourceConfigurationResponse();
        response.setUuid(this.getUuid());
        try {
            GloboResourceConfigurationVO globoResourceConfigurationVO = _globoNetworkService.getGloboResourceConfiguration(this.getUuid(), GloboResourceType.LOAD_BALANCER);
            response.setResourceType(String.valueOf(globoResourceConfigurationVO.getResourceType()));
            response.setConfigurationValue(String.valueOf(globoResourceConfigurationVO.getValue()));
            response.setConfigurationKey(String.valueOf(globoResourceConfigurationVO.getKey()));
        } catch (RuntimeException ex) {
            s_logger.debug("Failed trying to retrieve the value of a GloboResourceConfiguration." + ex.getMessage());
            response.setConfigurationValue(null);
        }

        this.setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return 0;
    }
}
