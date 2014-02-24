package com.globo.networkapi.response;

import java.util.List;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.globo.networkapi.model.Environment;

public class NetworkAPIAllEnvironmentResponse extends Answer {

	private List<Environment> environmentList;
	
    public NetworkAPIAllEnvironmentResponse(Command command, List<Environment> environmentList) {
    	super(command, true, null);
    	this.environmentList = environmentList;
    }

	public List<Environment> getEnvironmentList() {
		return environmentList;
	}

}
