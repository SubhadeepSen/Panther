package com.panther.model;

public class PantherModel {

	private String description;
	private PantherRequest request;
	private PantherResponse response;
	private boolean caseStatus;
	private String caseMessage;
	private String actualResponse = "";

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public PantherRequest getRequest() {
		return request;
	}

	public void setRequest(PantherRequest request) {
		this.request = request;
	}

	public PantherResponse getResponse() {
		return response;
	}

	public void setResponse(PantherResponse response) {
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

	public String getActualResponse() {
		return actualResponse;
	}

	public void setActualResponse(String actualResponse) {
		this.actualResponse = actualResponse;
	}
}
