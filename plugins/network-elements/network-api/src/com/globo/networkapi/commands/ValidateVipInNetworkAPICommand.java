package com.globo.networkapi.commands;

import com.cloud.agent.api.Command;

public class ValidateVipInNetworkAPICommand extends Command {
	
	private Long vipId;
		
	@Override
	public boolean executeInSequence() {
		return false;
	}

	public Long getVipId() {
		return vipId;
	}

	public void setVipId(Long vipId) {
		this.vipId = vipId;
	}
}
