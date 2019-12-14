package com.panther.model;

import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class PantherConfig {
	private String apiDocsLocation;
	private boolean wantToParse;
	private String testCasesLocation;
	private String apiScheme;
	private String payloadLocation;
	private boolean enableReportLogging;
	private String reportName;
	@JsonIgnore
	private TreeMap<String, String> secureHeaders;

	public String getApiDocsLocation() {
		return apiDocsLocation;
	}

	public void setApiDocsLocation(String apiDocsLocation) {
		this.apiDocsLocation = apiDocsLocation;
	}

	public boolean wantToParse() {
		return wantToParse;
	}

	public void setWantToParse(boolean wantToParse) {
		this.wantToParse = wantToParse;
	}

	public String getTestCasesLocation() {
		return testCasesLocation;
	}

	public void setTestCasesLocation(String testCasesLocation) {
		this.testCasesLocation = testCasesLocation;
	}

	public String getApiScheme() {
		return apiScheme;
	}

	public void setApiScheme(String apiScheme) {
		this.apiScheme = apiScheme;
	}

	public String getPayloadLocation() {
		return payloadLocation;
	}

	public void setPayloadLocation(String payloadLocation) {
		this.payloadLocation = payloadLocation;
	}

	public boolean isEnableReportLogging() {
		return enableReportLogging;
	}

	public void setEnableReportLogging(boolean enableReportLogging) {
		this.enableReportLogging = enableReportLogging;
	}

	public TreeMap<String, String> getSecureHeaders() {
		return secureHeaders;
	}

	public void setSecureHeaders(TreeMap<String, String> secureHeaders) {
		this.secureHeaders = secureHeaders;
	}

	public String getReportName() {
		return reportName;
	}

	public void setReportName(String reportName) {
		this.reportName = reportName;
	}
}
