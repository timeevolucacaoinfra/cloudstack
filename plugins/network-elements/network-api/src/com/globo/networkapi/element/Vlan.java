package com.globo.networkapi.element;

public class Vlan {
	
	private Long id;
	private Integer rede_oct2;
	private Integer rede_oct3;
	private Integer rede_oct1;
	private boolean ativada;
	private boolean acl_valida;
	private String nome;
	private String broadcast;
	private Integer num_vlan;
	private Integer ambiente;
	
	
	public Integer getRede_oct2() {
		return rede_oct2;
	}
	public void setRede_oct2(Integer rede_oct2) {
		this.rede_oct2 = rede_oct2;
	}
	public Integer getRede_oct3() {
		return rede_oct3;
	}
	public void setRede_oct3(Integer rede_oct3) {
		this.rede_oct3 = rede_oct3;
	}
	public Integer getRede_oct1() {
		return rede_oct1;
	}
	public void setRede_oct1(Integer rede_oct1) {
		this.rede_oct1 = rede_oct1;
	}
	public boolean isAtivada() {
		return ativada;
	}
	public void setAtivada(boolean ativada) {
		this.ativada = ativada;
	}
	public boolean isAcl_valida() {
		return acl_valida;
	}
	public void setAcl_valida(boolean acl_valida) {
		this.acl_valida = acl_valida;
	}
	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getBroadcast() {
		return broadcast;
	}
	public void setBroadcast(String broadcast) {
		this.broadcast = broadcast;
	}
	public Integer getNum_vlan() {
		return num_vlan;
	}
	public void setNum_vlan(Integer num_vlan) {
		this.num_vlan = num_vlan;
	}
	public Integer getAmbiente() {
		return ambiente;
	}
	public void setAmbiente(Integer ambiente) {
		this.ambiente = ambiente;
	}

}
