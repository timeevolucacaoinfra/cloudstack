package com.globo.globonetwork.cloudstack.commands;

import com.cloud.agent.api.Command;

public class UnregisterEquipmentAndIpInGloboNetworkCommand extends Command {
	
	private String nicIp;
	private String vmName;
	private Long environmentId;

	@Override
	public boolean executeInSequence() {
		return true;
	}

	public String getNicIp() {
		return nicIp;
	}

	public void setNicIp(String nicIp) {
		this.nicIp = nicIp;
	}

	public String getVmName() {
		return vmName;
	}

	public void setVmName(String vmName) {
		this.vmName = vmName;
	}

	public Long getEnvironmentId() {
		return environmentId;
	}

	public void setEnvironmentId(Long environmentId) {
		this.environmentId = environmentId;
	}
}
