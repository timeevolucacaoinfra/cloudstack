package com.globo.globonetwork.cloudstack.commands;

import com.cloud.agent.api.Command;

/**
 * Validate if an ip and vlan are valid to specific network vlanId.
 * @author snbuback
 *
 */
public class ValidateNicInVlanCommand extends Command {
	
	private String nicIp;
	
	private Long vlanNum;
	
	private Long vlanId;
	
	@Override
	public boolean executeInSequence() {
		return false;
	}

	public String getNicIp() {
		return nicIp;
	}

	public void setNicIp(String nicIp) {
		this.nicIp = nicIp;
	}

	public Long getVlanNum() {
		return vlanNum;
	}

	public void setVlanNum(Long vlanNum) {
		this.vlanNum = vlanNum;
	}

	public Long getVlanId() {
		return vlanId;
	}

	public void setVlanId(Long vlanId) {
		this.vlanId = vlanId;
	}

}
