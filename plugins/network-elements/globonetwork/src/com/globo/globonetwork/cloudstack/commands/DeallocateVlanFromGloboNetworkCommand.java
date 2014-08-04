package com.globo.globonetwork.cloudstack.commands;

import com.cloud.agent.api.Command;

/**
 * Deallocates an inactive VLAN in GloboNetwork
 * @author Daniel Vega
 *
 */
public class DeallocateVlanFromGloboNetworkCommand extends Command {
	
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
