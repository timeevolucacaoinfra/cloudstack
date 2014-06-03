package com.globo.dnsapi.commands;

import com.cloud.agent.api.Command;

public class GetRecordInfoCommand extends Command {
	
	private Long recordId;

	public GetRecordInfoCommand(Long recordId) {
		this.recordId = recordId;
	}
	
	@Override
	public boolean executeInSequence() {
		return false;
	}

	public Long getRecordId() {
		return recordId;
	}
}
