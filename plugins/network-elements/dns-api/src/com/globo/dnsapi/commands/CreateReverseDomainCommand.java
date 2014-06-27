package com.globo.dnsapi.commands;

import com.cloud.agent.api.Command;

public class CreateReverseDomainCommand extends Command {
	
	private String name;
	
	private Long templateId;
	
	private String authorityType;

	public CreateReverseDomainCommand(String name, Long templateId, String authorityType) {
		this.name = name;
		this.templateId = templateId;
		this.authorityType = authorityType;
	}
	
	@Override
	public boolean executeInSequence() {
		return false;
	}

	public String getName() {
		return this.name;
	}
	
	public Long getTemplateId() {
		return this.templateId;
	}
	
	public String getAuthorityType() {
		return this.authorityType;
	}
}
