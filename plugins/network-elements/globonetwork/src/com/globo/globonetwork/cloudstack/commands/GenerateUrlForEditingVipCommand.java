package com.globo.globonetwork.cloudstack.commands;

import com.cloud.agent.api.Command;

public class GenerateUrlForEditingVipCommand extends Command {
	
	private Long vipId;
	
	private String vipServerUrl;
	
	public GenerateUrlForEditingVipCommand(Long vipId, String vipServerUrl) {
		this.vipId = vipId;
		this.vipServerUrl = vipServerUrl;
	}

	@Override
	public boolean executeInSequence() {
		return false;
	}
	
	public Long getVipId() {
		return vipId;
	}
	
	public String getVipServerUrl() {
		return vipServerUrl;
	}

}
