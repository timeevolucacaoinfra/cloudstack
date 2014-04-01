package com.globo.dnsapi.response;

import java.util.List;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.globo.dnsapi.model.Domain;

public class DnsAPIDomainListResponse extends Answer {

	private List<Domain> domainList;
	
    public DnsAPIDomainListResponse(Command command, List<Domain> domainList) {
    	super(command, true, null);
    	this.domainList = domainList;
    }

	public List<Domain> getDomainList() {
		return domainList;
	}

}
