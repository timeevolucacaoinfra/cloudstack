package com.globo.dnsapi.commands;

import com.cloud.agent.api.Command;

public class RemoveDomainCommand extends Command {
	
	private Long domainId;

	public RemoveDomainCommand(Long domainId) {
		this.domainId = domainId;
	}
	
	@Override
	public boolean executeInSequence() {
		return false;
	}
	
	public Long getDomainId() {
		return this.domainId;
	}
}
