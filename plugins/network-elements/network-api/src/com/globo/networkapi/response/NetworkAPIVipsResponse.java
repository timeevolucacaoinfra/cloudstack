package com.globo.networkapi.response;

import java.util.List;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;

public class NetworkAPIVipsResponse extends Answer {

	private List<Vip> vipsList;
	
    public NetworkAPIVipsResponse(Command command, List<Vip> vipsList) {
    	super(command, true, null);
    	this.vipsList = vipsList;
    }

	public List<Vip> getVipsList() {
		return vipsList;
	}
	
	public static class Vip {
		private Long id;
		private String name;
		private String ip;

		public Long getId() {
			return id;
		}
		public void setId(Long id) {
			this.id = id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getIp() {
			return ip;
		}
		public void setIp(String ip) {
			this.ip = ip;
		}
	}

}
