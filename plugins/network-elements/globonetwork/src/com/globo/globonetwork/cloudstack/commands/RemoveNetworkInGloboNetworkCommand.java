package com.globo.globonetwork.cloudstack.commands;

import com.cloud.agent.api.Command;

/**
 * Removes network from a specific VLAN in GloboNetwork
 * @author Daniel Vega
 *
 */
public class RemoveNetworkInGloboNetworkCommand extends Command {
	
	private Long vlanId;
	
	@Override
	public boolean executeInSequence() {
		return true;
	}

	public Long getVlanId() {
		return vlanId;
	}

	public void setVlanId(Long vlanId) {
		this.vlanId = vlanId;
	}

}
