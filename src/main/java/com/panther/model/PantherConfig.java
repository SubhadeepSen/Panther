package com.panther.model;

import java.util.TreeMap;

public class PantherConfig {
	private String apiDocsLocation;
	private boolean wantToParse;
	private String templateLocation;
	private String apiScheme;
	private TreeMap<String, String> credHeaders;

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

	public String getTemplateLocation() {
		return templateLocation;
	}

	public void setTemplateLocation(String templateLocation) {
		this.templateLocation = templateLocation;
	}

	public String getApiScheme() {
		return apiScheme;
	}

	public void setApiScheme(String apiScheme) {
		this.apiScheme = apiScheme;
	}

	public TreeMap<String, String> getCredHeaders() {
		return credHeaders;
	}

	public void setCredHeaders(TreeMap<String, String> credHeaders) {
		this.credHeaders = credHeaders;
	}
}
