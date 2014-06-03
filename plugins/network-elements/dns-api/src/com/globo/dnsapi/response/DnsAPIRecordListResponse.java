package com.globo.dnsapi.response;

import java.util.List;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.globo.dnsapi.model.Record;

public class DnsAPIRecordListResponse extends Answer {

	private List<Record> recordList;
	
    public DnsAPIRecordListResponse(Command command, List<Record> recordList) {
    	super(command, true, null);
    	this.recordList = recordList;
    }

	public List<Record> getRecordList() {
		return recordList;
	}

}
