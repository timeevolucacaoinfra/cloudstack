package com.globo.networkapi.commands;

import com.cloud.agent.api.Command;

public class GenerateUrlForEditingVipCommand extends Command {
	
	private Long vipId;
	
	public GenerateUrlForEditingVipCommand(Long vipId) {
		this.vipId = vipId;
	}

	@Override
	public boolean executeInSequence() {
		return false;
	}
	
	public Long getVipId() {
		return vipId;
	}

}
