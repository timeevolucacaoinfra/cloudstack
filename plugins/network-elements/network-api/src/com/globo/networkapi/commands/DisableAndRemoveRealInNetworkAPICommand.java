package com.globo.networkapi.commands;

import com.cloud.agent.api.Command;

public class DisableAndRemoveRealInNetworkAPICommand extends Command {
	
	private String ip;
	
	private Long napiEnvironmentId;
	
	private Long vipId;
	
	private Long equipId;
	
	private Integer vipPort;
	
	private Integer realPort;
		
	@Override
	public boolean executeInSequence() {
		return false;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}
	
	public Long getNapiEnvironmentId() {
		return napiEnvironmentId;
	}
	
	public void setNapiEnvironmentId(Long napiEnvironmentId) {
		this.napiEnvironmentId = napiEnvironmentId;
	}

	public Long getVipId() {
		return vipId;
	}

	public void setVipId(Long vipId) {
		this.vipId = vipId;
	}

	public Long getEquipId() {
		return equipId;
	}

	public void setEquipId(Long equipId) {
		this.equipId = equipId;
	}

	public Integer getVipPort() {
		return vipPort;
	}

	public void setVipPort(Integer vipPort) {
		this.vipPort = vipPort;
	}

	public Integer getRealPort() {
		return realPort;
	}

	public void setRealPort(Integer realPort) {
		this.realPort = realPort;
	}
}
