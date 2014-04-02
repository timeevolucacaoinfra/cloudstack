package com.globo.dnsapi.commands;

import com.cloud.agent.api.Command;

public class RemoveRecordCommand extends Command {
	
	private Long recordId;

	public RemoveRecordCommand(Long recordId) {
		this.recordId = recordId;
	}
	
	@Override
	public boolean executeInSequence() {
		return false;
	}
	
	public Long getRecordId() {
		return this.recordId;
	}
}
