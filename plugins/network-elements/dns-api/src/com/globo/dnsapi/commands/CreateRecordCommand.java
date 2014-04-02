package com.globo.dnsapi.commands;

import com.cloud.agent.api.Command;

public class CreateRecordCommand extends Command {
	
	private Long domainId;
	
	private String name;
	
	private String content;
	
	private String type;

	public CreateRecordCommand(Long domainId, String name, String content, String type) {
		this.domainId = domainId;
		this.name = name;
		this.content = content;
		this.type = type;
	}
	
	@Override
	public boolean executeInSequence() {
		return false;
	}

	public Long getDomainId() {
		return this.domainId;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getContent() {
		return this.content;
	}
	
	public String getType() {
		return this.type;
	}
}
