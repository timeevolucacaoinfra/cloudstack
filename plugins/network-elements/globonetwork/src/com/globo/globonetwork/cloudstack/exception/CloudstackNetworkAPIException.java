package com.globo.globonetwork.cloudstack.exception;

import com.cloud.utils.exception.CloudRuntimeException;

public class CloudstackNetworkAPIException extends CloudRuntimeException {

	private static final long serialVersionUID = 678159764759471937L;

	private int napiCode;
	private String napiDescription;
	
	public CloudstackNetworkAPIException(int napiCode, String napiDescription) {
		super(napiCode + " - " + napiDescription);
		this.napiCode = napiCode;
		this.napiDescription = napiDescription;
	}
	
	public CloudstackNetworkAPIException(String message) {
		super(message);
	}
	
	public int getNapiCode() {
		return napiCode;
	}
	public String getNapiDescription() {
		return napiDescription;
	}
}
