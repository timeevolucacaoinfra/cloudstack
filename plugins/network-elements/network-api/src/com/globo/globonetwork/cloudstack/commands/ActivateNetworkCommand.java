package com.globo.globonetwork.cloudstack.commands;

import com.cloud.agent.api.Command;

public class ActivateNetworkCommand extends Command {
	
	private long networkId;
	
	private long vlanId;

	public ActivateNetworkCommand(long vlanId, long networkId) {
		this.vlanId = vlanId;
		this.networkId = networkId;
	}
	
	@Override
	public boolean executeInSequence() {
		return false;
	}

	public long getNetworkId() {
		return networkId;
	}
	
	public long getVlanId() {
		return vlanId;
	}

}
