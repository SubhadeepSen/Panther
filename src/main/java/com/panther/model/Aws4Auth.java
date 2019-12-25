package com.panther.model;

import java.util.TreeMap;

public class Aws4Auth {

	private String accessKey;
	private String secretKey;
	private String regionName;
	private String serviceName;
	private String httpMethod;
	private String canonicalUri;
	private TreeMap<String, String> queryParametes;
	private String payload;

	private Aws4Auth() {
	}

	public static Aws4Auth create() {
		return new Aws4Auth();
	}

	public Aws4Auth accessKey(String accessKey) {
		this.accessKey = accessKey;
		return this;
	}

	public Aws4Auth secretKey(String secretKey) {
		this.secretKey = secretKey;
		return this;
	}

	public Aws4Auth regionName(String regionName) {
		this.regionName = regionName;
		return this;
	}

	public Aws4Auth serviceName(String serviceName) {
		this.serviceName = serviceName;
		return this;
	}

	public Aws4Auth httpMethod(String httpMethod) {
		this.httpMethod = httpMethod;
		return this;
	}

	public Aws4Auth canonicalUri(String canonicalUri) {
		this.canonicalUri = canonicalUri;
		return this;
	}

	public Aws4Auth queryParametes(TreeMap<String, String> queryParametes) {
		this.queryParametes = queryParametes;
		return this;
	}

	public Aws4Auth payload(String payload) {
		this.payload = payload;
		return this;
	}

	public String getAccessKey() {
		return accessKey;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public String getRegionName() {
		return regionName;
	}

	public String getServiceName() {
		return serviceName;
	}

	public String getHttpMethod() {
		return httpMethod;
	}

	public String getCanonicalUri() {
		return canonicalUri;
	}

	public TreeMap<String, String> getQueryParametes() {
		return queryParametes;
	}

	public String getPayload() {
		return payload;
	}
}
