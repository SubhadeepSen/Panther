package com.panther.auth;

import java.util.Base64;
import java.util.TreeMap;

import com.panther.util.PantherUtils;

public class BasicAuthentication implements Authentication {
	private String username;
	private String password;
	private String authorization;

	public BasicAuthentication(String username, String password) {
		this.username = username;
		this.password = password;
	}

	public BasicAuthentication(String authorization) {
		this.authorization = authorization;
	}

	@Override
	public TreeMap<String, String> headers() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		if (null != authorization && !PantherUtils.EMPTY_STRING.equals(authorization)) {
			map.put("Authorization", authorization);
		} else {
			String usernameColonPassword = username + ":" + password;
			this.authorization = "Basic " + Base64.getEncoder().encodeToString(usernameColonPassword.getBytes());
			map.put("Authorization", authorization);
		}
		return map;
	}

	public String getAuthorization() {
		return authorization;
	}
}
