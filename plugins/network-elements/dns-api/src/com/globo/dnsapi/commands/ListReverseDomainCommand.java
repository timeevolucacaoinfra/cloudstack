package com.globo.dnsapi.commands;

import com.cloud.agent.api.Command;

public class ListReverseDomainCommand extends Command {
	
	private String query;

	public ListReverseDomainCommand(String query) {
		this.query = query;
	}
	
	@Override
	public boolean executeInSequence() {
		return false;
	}

	public String getQuery() {
		return this.query;
	}
}
