package com.globo.dnsapi.commands;

import com.cloud.agent.api.Command;

public class GetDomainInfoCommand extends Command {
	
	private Long domainId;

	public GetDomainInfoCommand(Long domainId) {
		this.domainId = domainId;
	}
	
	@Override
	public boolean executeInSequence() {
		return false;
	}

	public Long getDomainId() {
		return domainId;
	}
}
