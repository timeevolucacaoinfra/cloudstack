package com.globo.networkapi.commands;

import com.cloud.agent.api.Command;

/**
 * Removes network from a specific VLAN in NetworkAPI
 * @author Daniel Vega
 *
 */
public class RemoveNetworkInNetworkAPICommand extends Command {
	
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
