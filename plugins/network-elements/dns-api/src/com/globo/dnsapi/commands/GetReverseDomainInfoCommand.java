package com.globo.dnsapi.commands;

import com.cloud.agent.api.Command;

public class GetReverseDomainInfoCommand extends Command {
	
	private Long reverseDomainId;

	public GetReverseDomainInfoCommand(Long reverseDomainId) {
		this.reverseDomainId = reverseDomainId;
	}
	
	@Override
	public boolean executeInSequence() {
		return false;
	}

	public Long getReverseDomainId() {
		return reverseDomainId;
	}
}
