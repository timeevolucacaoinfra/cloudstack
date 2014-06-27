package com.globo.dnsapi.response;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.globo.dnsapi.model.Domain;

public class DnsAPIDomainResponse extends Answer {

	private Domain domain;
	
    public DnsAPIDomainResponse(Command command, Domain domain) {
    	super(command, true, null);
    	this.domain = domain;
    }

	public Domain getDomain() {
		return domain;
	}

}
