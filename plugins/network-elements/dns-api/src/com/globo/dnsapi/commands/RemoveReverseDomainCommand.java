package com.globo.dnsapi.commands;

import com.cloud.agent.api.Command;

public class RemoveReverseDomainCommand extends Command {
	
	private Long reverseDomainId;

	public RemoveReverseDomainCommand(Long reverseDomainId) {
		this.reverseDomainId = reverseDomainId;
	}
	
	@Override
	public boolean executeInSequence() {
		return false;
	}
	
	public Long getReverseDomainId() {
		return this.reverseDomainId;
	}
}
