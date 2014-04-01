package com.globo.dnsapi.commands;

import com.cloud.agent.api.Command;

public class ListDomainCommand extends Command {
	
	private String query;

	public ListDomainCommand(String query) {
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
