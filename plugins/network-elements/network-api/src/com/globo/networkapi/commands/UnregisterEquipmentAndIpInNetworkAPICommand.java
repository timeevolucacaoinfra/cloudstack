package com.globo.networkapi.commands;

import com.cloud.agent.api.Command;

public class UnregisterEquipmentAndIpInNetworkAPICommand extends Command {
	
	private String nicIp;
	private String nicDescription;
	private String vmName;
	private Long vlanId;
	private Long environmentId;
	private Long equipmentGroupId;

	@Override
	public boolean executeInSequence() {
		return true;
	}

	public String getNicIp() {
		return nicIp;
	}

	public void setNicIp(String nicIp) {
		this.nicIp = nicIp;
	}

	public String getNicDescription() {
		return nicDescription;
	}

	public void setNicDescription(String nicDescription) {
		this.nicDescription = nicDescription;
	}

	public String getVmName() {
		return vmName;
	}

	public void setVmName(String vmName) {
		this.vmName = vmName;
	}

	public Long getVlanId() {
		return vlanId;
	}

	public void setVlanId(Long vlanId) {
		this.vlanId = vlanId;
	}

	public Long getEnvironmentId() {
		return environmentId;
	}

	public void setEnvironmentId(Long environmentId) {
		this.environmentId = environmentId;
	}

	public Long getEquipmentGroupId() {
		return equipmentGroupId;
	}

	public void setEquipmentGroupId(Long equipmentGroupId) {
		this.equipmentGroupId = equipmentGroupId;
	}

}
