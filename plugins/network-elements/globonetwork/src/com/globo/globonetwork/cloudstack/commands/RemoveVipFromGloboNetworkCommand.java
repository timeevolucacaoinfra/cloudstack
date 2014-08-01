package com.globo.globonetwork.cloudstack.commands;

import com.cloud.agent.api.Command;

/**
 * Remove VIP from NetworkAPI
 *
 */
public class RemoveVipFromGloboNetworkCommand extends Command {
	
	private Long vipId;
	
	@Override
	public boolean executeInSequence() {
		return true;
	}

	public Long getVipId() {
		return vipId;
	}

	public void setVipId(Long vipId) {
		this.vipId = vipId;
	}

}
