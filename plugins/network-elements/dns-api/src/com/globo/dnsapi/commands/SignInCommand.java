package com.globo.dnsapi.commands;

import com.cloud.agent.api.Command;

public class SignInCommand extends Command {

	private String email;
	
	private String password;
	
	public SignInCommand(String email, String password) {
		this.email = email;
		this.password = password;
	}
	
	@Override
	public boolean executeInSequence() {
		return false;
	}
	
	public String getEmail() {
		return this.email;
	}
	
	public String getPassword() {
		return this.password;
	}
}
