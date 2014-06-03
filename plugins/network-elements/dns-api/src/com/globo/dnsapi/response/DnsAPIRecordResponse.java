package com.globo.dnsapi.response;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.globo.dnsapi.model.Record;

public class DnsAPIRecordResponse extends Answer {

	private Record record;
	
    public DnsAPIRecordResponse(Command command, Record record) {
    	super(command, true, null);
    	this.record = record;
    }

	public Record getRecord() {
		return record;
	}

}
