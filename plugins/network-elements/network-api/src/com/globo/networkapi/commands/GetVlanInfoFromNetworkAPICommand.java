package com.globo.networkapi.commands;

import com.cloud.agent.api.Command;

public class GetVlanInfoFromNetworkAPICommand extends Command {
	
	private Long vlanId;
	
	@Override
	public boolean executeInSequence() {
		return false;
	}

	public Long getVlanId() {
		return vlanId;
	}

	public void setVlanId(Long vlanId) {
		this.vlanId = vlanId;
	}

}
