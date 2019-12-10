package com.panther.model;

public class RequestResponseTemplate {

	private String description;
	private RequestTemplate request;
	private ResponseTemplate response;
	private boolean caseStatus;
	private String caseMessage;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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

	public boolean caseStatus() {
		return caseStatus;
	}

	public void setCaseStatus(boolean caseStatus) {
		this.caseStatus = caseStatus;
	}

	public String getCaseMessage() {
		return caseMessage;
	}

	public void setCaseMessage(String caseMessage) {
		this.caseMessage = caseMessage;
	}
}
