package com.globo.networkapi.response;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;

public class NetworkAPIVlanResponse extends Answer {

    private Long vlanNum;

    private Long vlanId;

    public NetworkAPIVlanResponse(Command command, Long vlanNum, Long vlanId) {
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

}
