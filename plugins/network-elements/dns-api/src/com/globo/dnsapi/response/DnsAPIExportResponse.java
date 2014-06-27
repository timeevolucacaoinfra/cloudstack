package com.globo.dnsapi.response;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.globo.dnsapi.model.Export;

public class DnsAPIExportResponse extends Answer {

	private Export export;
	
    public DnsAPIExportResponse(Command command, Export export) {
    	super(command, true, null);
    	this.export = export;
    }

	public Export getExport() {
		return export;
	}

}
