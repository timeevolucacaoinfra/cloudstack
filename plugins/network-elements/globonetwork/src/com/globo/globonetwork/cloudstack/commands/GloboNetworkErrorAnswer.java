package com.globo.globonetwork.cloudstack.commands;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;

public class GloboNetworkErrorAnswer extends Answer {

	protected int napiCode;
	protected String napiDescription;
	
	public GloboNetworkErrorAnswer(Command command, int napiCode, String napiDescription) {
		super(command, false, napiCode + " - " + napiDescription);
		this.napiCode = napiCode;
		this.napiDescription = napiDescription;
	}
	
	public int getNapiCode() {
		return this.napiCode;
	}
	
	public String getNapiDescription() {
		return this.napiDescription;
	}
}
