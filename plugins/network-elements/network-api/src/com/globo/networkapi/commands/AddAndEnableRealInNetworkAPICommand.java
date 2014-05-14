package com.globo.networkapi.commands;

import com.cloud.agent.api.Command;

public class AddAndEnableRealInNetworkAPICommand extends Command {
	
	private String ip;
	
	private Long vipId;
	
	private String equipName;
		
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
	
	public Long getVipId() {
		return vipId;
	}

	public void setVipId(Long vipId) {
		this.vipId = vipId;
	}

	public String getEquipName() {
		return equipName;
	}

	public void setEquipId(String equipName) {
		this.equipName = equipName;
	}
}
