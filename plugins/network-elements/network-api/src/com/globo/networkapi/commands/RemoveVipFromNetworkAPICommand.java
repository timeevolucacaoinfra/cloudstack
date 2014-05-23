package com.globo.networkapi.commands;

import com.cloud.agent.api.Command;

/**
 * Remove VIP from NetworkAPI
 *
 */
public class RemoveVipFromNetworkAPICommand extends Command {
	
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
