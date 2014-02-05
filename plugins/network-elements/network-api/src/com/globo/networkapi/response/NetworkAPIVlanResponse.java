package com.globo.networkapi.response;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.network.IpAddress;
import com.cloud.utils.net.Ip4Address;

public class NetworkAPIVlanResponse extends Answer {

    private Long vlanNum;

    private Long vlanId;
    
    private Ip4Address mask;
    
    private Ip4Address gateway;

    public NetworkAPIVlanResponse(Command command, Long vlanNum, Long vlanId, Ip4Address gateway, Ip4Address mask) {
    	super(command, true, null);
    	this.vlanId = vlanId;
    	this.vlanNum = vlanNum;
    }
	public Long getVlanNum() {
		return vlanNum;
	}

	public void setVlanNum(Long vlanNum) {
		this.vlanNum = vlanNum;
	}

	public Long getVlanId() {
		return vlanId;
	}

	public void setVlanId(Long vlanId) {
		this.vlanId = vlanId;
	}
	public Ip4Address getMask() {
		return mask;
	}
	public void setMask(Ip4Address mask) {
		this.mask = mask;
	}
	public Ip4Address getGateway() {
		return gateway;
	}
	public void setGateway(Ip4Address gateway) {
		this.gateway = gateway;
	}

}
