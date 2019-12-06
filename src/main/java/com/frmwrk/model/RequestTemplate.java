package com.frmwrk.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;

public class RequestTemplate {
	@JsonInclude(Include.NON_NULL)
	private String description;

	@JsonInclude(Include.NON_NULL)
	private String url;

	@JsonInclude(Include.NON_NULL)
	private String method;

	@JsonInclude(Include.NON_NULL)
	private JsonNode body;

	@JsonInclude(Include.NON_EMPTY)
	private Map<String, String> pathParams;

	@JsonInclude(Include.NON_EMPTY)
	private Map<String, String> queryParams;

	@JsonInclude(Include.NON_EMPTY)
	private Map<String, String> headers;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public JsonNode getBody() {
		return body;
	}

	public void setBody(JsonNode body) {
		this.body = body;
	}

	public Map<String, String> getPathParams() {
		return pathParams;
	}

	public void setPathParams(Map<String, String> pathParams) {
		this.pathParams = pathParams;
	}

	public Map<String, String> getQueryParams() {
		return queryParams;
	}

	public void setQueryParams(Map<String, String> queryParams) {
		this.queryParams = queryParams;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}
}
