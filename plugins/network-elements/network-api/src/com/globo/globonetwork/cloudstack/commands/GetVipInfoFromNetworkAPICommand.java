package com.globo.globonetwork.cloudstack.commands;

import com.cloud.agent.api.Command;

public class GetVipInfoFromNetworkAPICommand extends Command {
	
	private long vipId;
	
	@Override
	public boolean executeInSequence() {
		return false;
	}
	
	public long getVipId() {
		return vipId;
	}
	
	public void setVipId(long vipId) {
		this.vipId = vipId;
	}
}
