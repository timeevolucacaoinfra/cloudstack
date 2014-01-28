package com.globo.networkapi.element;

public class Network {
	
	private Long id;
	private Long network;
	private Long id_vlan;
	private Long id_network_type;
	private Long id_environment_vip;

	public Long getId_vlan() {
		return id_vlan;
	}

	public void setId_vlan(Long id_vlan) {
		this.id_vlan = id_vlan;
	}

	public Long getId_network_type() {
		return id_network_type;
	}

	public void setId_network_type(Long id_network_type) {
		this.id_network_type = id_network_type;
	}

	public Long getId_environment_vip() {
		return id_environment_vip;
	}

	public void setId_environment_vip(Long id_environment_vip) {
		this.id_environment_vip = id_environment_vip;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getNetwork() {
		return network;
	}

	public void setNetwork(Long network) {
		this.network = network;
	}
}
