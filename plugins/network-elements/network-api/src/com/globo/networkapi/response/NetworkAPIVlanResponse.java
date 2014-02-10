package com.globo.networkapi.response;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.utils.net.Ip4Address;

public class NetworkAPIVlanResponse extends Answer {

    private Long vlanId;
    
    private String vlanName;
    
    private String vlanDescription;

    private Long vlanNum;

    private Ip4Address mask;

    private Ip4Address networkAddress;
    
    public NetworkAPIVlanResponse(Command command, Long vlanId, String vlanName, String vlanDescription, Long vlanNum, Ip4Address networkAddress, Ip4Address mask) {
    	super(command, true, null);
    	this.vlanId = vlanId;
    	this.vlanName = vlanName;
    	this.vlanDescription = vlanDescription;
    	this.vlanNum = vlanNum;
    	this.mask = mask;
    	this.networkAddress = networkAddress;
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
	
	public String getVlanName() {
		return vlanName;
	}
	
	public void setVlanName(String vlanName) {
		this.vlanName = vlanName;
	}
	
	public String getVlanDescription() {
		return vlanDescription;
	}
	
	public void setVlanDescription(String vlanDescription) {
		this.vlanDescription = vlanDescription;
	}

	public Ip4Address getNetworkAddress() {
		return networkAddress;
	}

	public void setNetworkAddress(Ip4Address networkAddress) {
		this.networkAddress = networkAddress;
	}

}
