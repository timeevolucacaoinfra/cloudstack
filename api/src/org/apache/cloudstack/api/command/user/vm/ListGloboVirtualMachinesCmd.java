package org.apache.cloudstack.api.command.user.vm;

import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachine;
import java.util.List;
import javax.inject.Inject;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;

import org.apache.cloudstack.api.BaseListTaggedResourcesCmd;

import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.response.ListResponse;

import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.query.QueryService;

@APICommand(name = "listGloboVirtualMachines", description = "List the virtual machines by usage.", responseObject = UserVmResponse.class, responseView = ResponseObject.ResponseView.Restricted, entityType = {VirtualMachine.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = true)
public class ListGloboVirtualMachinesCmd extends BaseListTaggedResourcesCmd {

    private static final String s_name = "listglobovirtualmachinesresponse";

    @Inject
    public QueryService _queryService;
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.VirtualMachine;
    }

    @Override
    public void execute() {

        ListResponse<UserVmResponse> response = new ListResponse<UserVmResponse>();

        Pair<List<UserVmResponse>, Integer> result = _queryService.listGloboVm(getProjectId(), getTags());

        response.setResponses(result.first(), result.second());
        response.setResponseName(getCommandName());
        response.setObjectName("virtualmachine");
        setResponseObject(response);
    }
}
