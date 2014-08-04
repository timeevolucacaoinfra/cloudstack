package com.globo.globonetwork.cloudstack.api;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.NicResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.Nic;
import com.globo.globonetwork.cloudstack.manager.GloboNetworkService;

@APICommand(name = "associateNetworkApiRealToVip", responseObject=SuccessResponse.class, description="Associates a nic (real) to one specific VIP")
public class AddGloboNetworkRealToVipCmd extends BaseCmd {

    public static final Logger s_logger = Logger.getLogger(AddGloboNetworkRealToVipCmd.class.getName());
    private static final String s_name = "associatenetworkapirealtovipresponse";
    
    @Inject
    GloboNetworkService _globoNetworkService;

    @Parameter(name=ApiConstants.VIP_ID, type=CommandType.LONG, required=true, description="GloboNetwork Vip Id")
    private Long vipId;

    @Parameter(name=ApiConstants.NIC_ID, type=CommandType.UUID, entityType=NicResponse.class,
            required=true, description="NIC ID")
    private Long nicId;
    
    public Nic getNic() {
        Nic nic = _entityMgr.findById(Nic.class, nicId);
        if (nic == null) {
            throw new InvalidParameterValueException("Can't find specified nic");
        }
        return nic;
    }

    /* Implementation */
    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        try {
        	s_logger.debug("associateGloboNetworkRealToVip command with VipId=" + this.vipId + " nicId=" + this.nicId);
        	_globoNetworkService.associateNicToVip(this.vipId, this.getNic());
        	
        	SuccessResponse response = new SuccessResponse(getCommandName());
        	response.setSuccess(true);
        	this.setResponseObject(response);
        	
        }  catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (CloudRuntimeException runtimeExcp) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, runtimeExcp.getMessage());
        }
    }
 
    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
    	return CallContext.current().getCallingAccountId();
    }
}
