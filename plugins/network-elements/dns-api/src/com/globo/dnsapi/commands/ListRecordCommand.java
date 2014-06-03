package com.globo.dnsapi.commands;

import com.cloud.agent.api.Command;

public class ListRecordCommand extends Command {
	
	private Long domainId;
	
	private String query;

	public ListRecordCommand(Long domainId, String query) {
		this.domainId = domainId;
		this.query = query;
	}
	
	@Override
	public boolean executeInSequence() {
		return false;
	}

	public Long getDomainId() {
		return this.domainId;
	}
	
	public String getQuery() {
		return this.query;
	}
}
