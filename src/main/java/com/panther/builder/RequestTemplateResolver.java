package com.panther.builder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.Header;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panther.config.ConfigLoader;
import com.panther.exception.PantherException;
import com.panther.model.RequestResponseTemplate;
import com.panther.model.RequestTemplate;
import com.panther.model.ResponseTemplate;

public class RequestTemplateResolver {

	public Map<String, List<RequestResponseTemplate>> buildRequestObjects(String pathOfRequestTemplate) {

		if (null == pathOfRequestTemplate || pathOfRequestTemplate == "") {
			throw new PantherException("Invalid path for test cases > " + pathOfRequestTemplate);
		}

		Map<String, List<RequestResponseTemplate>> map = new HashMap<>();
		try {
			Files.list(Paths.get(pathOfRequestTemplate)).forEach(c -> {
				String fileName = c.getFileName().toString();
				map.put(fileName, readFromTemplate(pathOfRequestTemplate + "/" + fileName));
			});
		} catch (IOException e) {
			throw new PantherException(e.getMessage());
		}

		RequestTemplate requestTemplate = null;
		ResponseTemplate responseTemplate = null;
		String bodyLocation = "";
		String fileName = "";
		for (Entry<String, List<RequestResponseTemplate>> entry : map.entrySet()) {
			for (RequestResponseTemplate template : entry.getValue()) {
				requestTemplate = template.getRequest();
				if (requestTemplate.getBody() != null) {
					bodyLocation = requestTemplate.getBody().textValue();
					if (bodyLocation != null && bodyLocation.startsWith("$load")) {
						bodyLocation = bodyLocation.replace("load", "").replace("$", "");
						fileName = bodyLocation.substring(1, bodyLocation.length() - 1);
						requestTemplate.setBody(loadAndGetBody(fileName));
					}
				}

				responseTemplate = template.getResponse();
				if (responseTemplate.getBody() != null) {
					bodyLocation = responseTemplate.getBody().textValue();
					if (bodyLocation != null && bodyLocation.startsWith("$load")) {
						bodyLocation = bodyLocation.replace("load", "").replace("$", "");
						fileName = bodyLocation.substring(1, bodyLocation.length() - 1);
						responseTemplate.setBody(loadAndGetBody(fileName));
					}
				}

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
		}

		return map;
	}

	private List<RequestResponseTemplate> readFromTemplate(String pathOfRequestTemplate) {
		try {
			return new ObjectMapper().readValue(Files.readAllBytes(Paths.get(pathOfRequestTemplate)),
					new TypeReference<List<RequestResponseTemplate>>() {
					});
		} catch (IOException e) {
			throw new PantherException(e.getMessage());
		}
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
				if (null != headerEntrySet.getValue() && !"".equals(headerEntrySet.getValue())) {
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
				verifyResponse(httpResponse, template);
				long endTime = System.currentTimeMillis();
				template.getResponse().setResponseTime(String.valueOf(endTime - startTime));
			} catch (IOException e) {
				throw new PantherException(e.getMessage());
			}
			System.out.println();
		}
	}

	private void verifyResponse(HttpResponse httpResponse, RequestResponseTemplate requestResponseTemplate)
			throws ParseException, IOException {
		ResponseTemplate responseTemplate = requestResponseTemplate.getResponse();

		// status code verification
		String statusCode = String.valueOf(httpResponse.getStatusLine().getStatusCode());
		if (!responseTemplate.getStatus().equals(statusCode)) {
			requestResponseTemplate.setCaseStatus(false);
			requestResponseTemplate.setCaseMessage("Status code mismatched: { actual: " + statusCode + ", expected: "
					+ responseTemplate.getStatus() + " }");
			return;
		}

		// body verification
		if (null != httpResponse.getEntity()
				&& httpResponse.getFirstHeader("Content-Type").getValue().contains("application/json")) {
			String actualResponse = EntityUtils.toString(httpResponse.getEntity());
			String expectedResponse = responseTemplate.getBody().toString();
			if (!new ObjectMapper().readTree(actualResponse).equals(responseTemplate.getBody())) {
				requestResponseTemplate.setCaseStatus(false);
				requestResponseTemplate.setCaseMessage(
						"body mismatched: { actual: " + actualResponse + ", expected: " + expectedResponse + " }");
				return;
			}
		}

		// header verification
		if (null != responseTemplate.getHeaders()) {
			Map<String, String> actualHeaders = new HashMap<String, String>();
			for (Header header : httpResponse.getAllHeaders()) {
				actualHeaders.put(header.getName(), header.getValue());
			}
			responseTemplate.getHeaders().entrySet().forEach(e -> {
				if (!actualHeaders.containsKey(e.getKey()) || !actualHeaders.containsKey(e.getValue())) {
					requestResponseTemplate.setCaseStatus(false);
					requestResponseTemplate.setCaseMessage("header mismatched: " + e.getKey() + " : { actual: "
							+ actualHeaders.get(e.getKey()) + ", expected: " + e.getValue() + " }");
					return;
				}
			});
		}
		requestResponseTemplate.setCaseStatus(true);
		requestResponseTemplate.setCaseMessage("");
	}

	private JsonNode loadAndGetBody(String fileName) {
		String payloadLocation = ConfigLoader.getConfig(null).getPayloadLocation();
		if (null == payloadLocation || "".equals(payloadLocation)) {
			throw new PantherException("Invalid payload location > " + payloadLocation + ", add location in panther-config.json.");
		}
		String payloadPath = payloadLocation + "/" + fileName;
		try {
			return new ObjectMapper().readTree(Files.readAllBytes(Paths.get(payloadPath)));
		} catch (IOException e) {
			throw new PantherException(e.getMessage());
		}
	}
}
