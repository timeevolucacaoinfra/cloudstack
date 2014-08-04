package com.globo.globonetwork.cloudstack.commands;

import com.cloud.agent.api.Command;

public class CreateNewVlanInGloboNetworkCommand extends Command {
	
	private String vlanName;
	
	private String vlanDescription;

	private Long globoNetworkEnvironmentId;

	@Override
	public boolean executeInSequence() {
		return false;
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

	public Long getGloboNetworkEnvironmentId() {
		return globoNetworkEnvironmentId;
	}

	public void setGloboNetworkEnvironmentId(Long globoNetworkEnvironmentId) {
		this.globoNetworkEnvironmentId = globoNetworkEnvironmentId;
	}

}
