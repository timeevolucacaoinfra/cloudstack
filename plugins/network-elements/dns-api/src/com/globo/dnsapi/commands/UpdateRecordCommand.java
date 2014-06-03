package com.globo.dnsapi.commands;

import com.cloud.agent.api.Command;

public class UpdateRecordCommand extends Command {
	
	private Long domainId;
	
	private Long recordId;
	
	private String name;
	
	private String content;
	
	private String type;

	public UpdateRecordCommand(Long domainId, Long recordId, String name, String content, String type) {
		this.domainId = domainId;
		this.recordId = recordId;
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
	
	public Long getRecordId() {
		return this.recordId;
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
