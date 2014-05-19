package com.globo.networkapi.commands;

import java.util.List;

import com.cloud.agent.api.Command;

public class ListVipsFromNetworkAPICommand extends Command {
	
	private List<Long> napiVipIds;
	
	@Override
	public boolean executeInSequence() {
		return false;
	}
	
	public List<Long> getNapiVipIds() {
		return napiVipIds;
	}
	
	public void setNapiVipIds(List<Long> napiVipIds) {
		this.napiVipIds = napiVipIds;
	}
}
