package com.globo.globonetwork.cloudstack.response;

import java.util.List;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;

public class NetworkAPIAllEnvironmentResponse extends Answer {

	private List<Environment> environmentList;
	
    public NetworkAPIAllEnvironmentResponse(Command command, List<Environment> environmentList) {
    	super(command, true, null);
    	this.environmentList = environmentList;
    }

	public List<Environment> getEnvironmentList() {
		return environmentList;
	}
	
	public static class Environment {
		private Long id;
		private String l3GroupName;
		private String logicalEnvironmentName;
		private String dcDivisionName;

		public Long getId() {
			return id;
		}
		public void setId(Long id) {
			this.id = id;
		}
		public String getL3GroupName() {
			return l3GroupName;
		}
		public void setL3GroupName(String l3GroupName) {
			this.l3GroupName = l3GroupName;
		}
		public String getLogicalEnvironmentName() {
			return logicalEnvironmentName;
		}
		public void setLogicalEnvironmentName(String logicalEnvironmentName) {
			this.logicalEnvironmentName = logicalEnvironmentName;
		}
		public String getDcDivisionName() {
			return dcDivisionName;
		}
		public void setDcDivisionName(String dcDivisionName) {
			this.dcDivisionName = dcDivisionName;
		}
	}

}
