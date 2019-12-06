package com.panther.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class RequestResponseTemplate {

	private String description;
	@JsonInclude(Include.NON_NULL)
	private String authToken;
	private RequestTemplate request;
	private ResponseTemplate response;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getAuthToken() {
		return authToken;
	}

	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}

	public RequestTemplate getRequest() {
		return request;
	}

	public void setRequest(RequestTemplate request) {
		this.request = request;
	}

	public ResponseTemplate getResponse() {
		return response;
	}

	public void setResponse(ResponseTemplate response) {
		this.response = response;
	}
}
