package com.globo.dnsapi.commands;

import com.cloud.agent.api.Command;

public class ScheduleExportCommand extends Command {

	public ScheduleExportCommand() {
	}
	
	@Override
	public boolean executeInSequence() {
		return false;
	}

}
