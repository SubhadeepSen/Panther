package com.panther.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class PantherModel {

	private String description;
	private boolean fieldValidationEnable;
	private PantherRequest request;
	private PantherResponse response;
	@JsonIgnore
	private boolean caseStatus;
	@JsonIgnore
	private String caseMessage;
	@JsonIgnore
	private String actualResponse = "";

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isFieldValidationEnable() {
		return fieldValidationEnable;
	}

	public void setFieldValidationEnable(boolean fieldValidationEnable) {
		this.fieldValidationEnable = fieldValidationEnable;
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
