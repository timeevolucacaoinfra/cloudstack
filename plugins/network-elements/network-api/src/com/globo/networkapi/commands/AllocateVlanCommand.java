package com.globo.networkapi.commands;

import com.cloud.agent.api.Command;

public class AllocateVlanCommand extends Command {
	
	private Long environmentId;
	
    private String vlanName;
    
    private String vlanDescription;

	@Override
	public boolean executeInSequence() {
		return false;
	}

	public Long getEnvironmentId() {
		return environmentId;
	}

	public void setEnvironmentId(Long environmentId) {
		this.environmentId = environmentId;
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

}
