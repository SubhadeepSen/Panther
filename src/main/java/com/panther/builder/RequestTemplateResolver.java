package com.panther.builder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panther.init.ConfigLoader;
import com.panther.model.RequestResponseTemplate;
import com.panther.model.RequestTemplate;
import com.panther.model.ResponseTemplate;

public class RequestTemplateResolver {

	public List<RequestResponseTemplate> buildRequestObjects(String pathOfRequestTemplate) {

		if (null == pathOfRequestTemplate || pathOfRequestTemplate == "") {
			System.err.println("invalid location input/output location: throw exception...");
		}

		List<RequestResponseTemplate> requestResponseTemplate = readFromTemplate(pathOfRequestTemplate);

		for (RequestResponseTemplate template : requestResponseTemplate) {
			if (null != template.getRequest().getPathParams()) {
				template.getRequest().getPathParams().entrySet().forEach(e -> {
					String url = template.getRequest().getUrl();
					if (url.contains("{" + e.getKey() + "}") && null != e.getValue()) {
						url = url.replace("{" + e.getKey() + "}", e.getValue());
						template.getRequest().setUrl(url);
					}
				});
			}
			if (null != template.getRequest().getQueryParams()) {
				template.getRequest().getQueryParams().entrySet().forEach(e -> {
					String url = template.getRequest().getUrl();
					if (url.contains("{" + e.getKey() + "}") && null != e.getValue()) {
						url = url.replace("{" + e.getKey() + "}", e.getValue());
						template.getRequest().setUrl(url);
					}
				});
			}
		}
		return requestResponseTemplate;
	}

	private List<RequestResponseTemplate> readFromTemplate(String pathOfRequestTemplate) {
		try {
			return new ObjectMapper().readValue(Files.readAllBytes(Paths.get(pathOfRequestTemplate)),
					new TypeReference<List<RequestResponseTemplate>>() {
					});
		} catch (IOException e) {
			System.out.println("Unable to locate file: " + e.getMessage());
		}
		return new ArrayList<RequestResponseTemplate>();
	}

	public void makeHttpCalls(List<RequestResponseTemplate> requestResponseTemplate)
			throws UnsupportedEncodingException {
		HttpClient httpClient = HttpClientBuilder.create().build();
		RequestTemplate requestTemplate = null;
		HttpRequestBase baseRequest = null;
		for (RequestResponseTemplate template : requestResponseTemplate) {
			requestTemplate = template.getRequest();
			switch (requestTemplate.getMethod()) {
			case "GET":
				baseRequest = new HttpGet(requestTemplate.getUrl());
				break;
			case "POST":
				baseRequest = new HttpPost(requestTemplate.getUrl());
				if (null != requestTemplate.getBody()) {
					HttpPost httpPost = (HttpPost) baseRequest;
					httpPost.setEntity(new StringEntity(requestTemplate.getBody().toString()));
					baseRequest = httpPost;
				}
				break;
			case "PUT":
				baseRequest = new HttpPut(requestTemplate.getUrl());
				if (null != requestTemplate.getBody()) {
					HttpPut httpPut = (HttpPut) baseRequest;
					httpPut.setEntity(new StringEntity(requestTemplate.getBody().toString(),
							ContentType.getByMimeType(requestTemplate.getHeaders().get("Accept"))));
					baseRequest = httpPut;
				}
				break;
			case "DELETE":
				baseRequest = new HttpDelete(requestTemplate.getUrl());
				break;
			default:
				System.out.println("Unsupported http method: " + requestTemplate.getMethod());
				break;
			}

			for (Entry<String, String> headerEntrySet : requestTemplate.getHeaders().entrySet()) {
				if (null != headerEntrySet.getValue() && "".equals(headerEntrySet.getValue())) {
					baseRequest.addHeader(headerEntrySet.getKey(), headerEntrySet.getValue());
				}
			}
			for (Entry<String, String> entry : ConfigLoader.getConfig(null).getCredHeaders().entrySet()) {
				baseRequest.addHeader(entry.getKey(), entry.getValue());
			}

			try {
				System.out.println("Executing: " + template.getDescription() + " at " + template.getRequest().getUrl());
				long startTime = System.currentTimeMillis();
				HttpResponse httpResponse = httpClient.execute(baseRequest);
				verifyResponse(httpResponse, template.getResponse());
				long endTime = System.currentTimeMillis();
				template.getResponse().setResponseTime(String.valueOf(endTime - startTime));
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println();
		}
	}

	private void verifyResponse(HttpResponse httpResponse, ResponseTemplate response)
			throws ParseException, IOException {
		httpResponse.getAllHeaders();
		httpResponse.getStatusLine().getStatusCode();
		if (null != httpResponse.getEntity()) {
			String actualResponse = EntityUtils.toString(httpResponse.getEntity());
			String expectedResponse = response.getBody().toString();
			System.out.println(actualResponse.contentEquals(expectedResponse));
		}
	}
}
