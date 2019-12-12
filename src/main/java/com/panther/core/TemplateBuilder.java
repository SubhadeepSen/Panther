package com.panther.core;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panther.config.ConfigLoader;
import com.panther.model.PantherModel;
import com.panther.model.PantherRequest;
import com.panther.model.PantherResponse;

import io.swagger.models.ArrayModel;
import io.swagger.models.HttpMethod;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.RefProperty;
import io.swagger.parser.SwaggerParser;

public class TemplateBuilder {

	public void writeToJsonFile(String pathOfApiSpecification, String outputPath) {
		if (null == pathOfApiSpecification || null == outputPath || outputPath == "" || pathOfApiSpecification == "") {
			System.err.println("invalid location input/output location: throw exception...");
		}
		List<PantherModel> build = build(pathOfApiSpecification);
		String jsonString = toJson(build);
		try (FileWriter writer = new FileWriter(outputPath); BufferedWriter bw = new BufferedWriter(writer)) {
			bw.write(jsonString);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public List<PantherModel> build(String pathOfApiSpecification) {
		List<PantherModel> requests = new ArrayList<>();
		PantherModel requestResponseTemplate = null;
		PantherRequest requestTemplate = null;
		PantherResponse responseTemplate = null;
		Swagger swagger = null;

		if (pathOfApiSpecification.startsWith("http")) {
			HttpClient httpClient = HttpClientBuilder.create().build();
			try {
				String swaggerDefinition = EntityUtils
						.toString(httpClient.execute(new HttpGet(pathOfApiSpecification)).getEntity());
				swagger = new SwaggerParser().read(new ObjectMapper().readTree(swaggerDefinition));
			} catch (ParseException | IOException e) {
				System.err.println("handle exception here");
			}
		} else {
			swagger = new SwaggerParser().read(pathOfApiSpecification);
		}

		String baseUrl = ConfigLoader.getConfig(null).getApiScheme() + swagger.getHost() + swagger.getBasePath();

		for (Entry<String, Path> pathEntrySet : swagger.getPaths().entrySet()) {
			requestResponseTemplate = new PantherModel();
			requestTemplate = new PantherRequest();
			responseTemplate = new PantherResponse();
			requestTemplate.setUrl(baseUrl + pathEntrySet.getKey());
			for (Entry<HttpMethod, Operation> operationEntrySet : pathEntrySet.getValue().getOperationMap()
					.entrySet()) {
				requestResponseTemplate.setDescription(operationEntrySet.getValue().getSummary());
				requestTemplate.setMethod(operationEntrySet.getKey().toString());
				createRequestTemplate(swagger, requestTemplate, operationEntrySet.getValue().getParameters());
				createResponseTemplate(swagger, responseTemplate, operationEntrySet.getValue().getResponses());
				requestTemplate.getHeaders().put("Accept", operationEntrySet.getValue().getConsumes().get(0));
				requestTemplate.getHeaders().put("Content-Type", operationEntrySet.getValue().getConsumes().get(0));
			}
			requestResponseTemplate.setRequest(requestTemplate);
			requestResponseTemplate.setResponse(responseTemplate);
			requests.add(requestResponseTemplate);
		}
		return requests;
	}

	private void createRequestTemplate(Swagger swagger, PantherRequest requestTemplate, List<Parameter> parameters) {
		Map<String, String> pathParams = new LinkedHashMap<String, String>();
		Map<String, String> queryParams = new LinkedHashMap<String, String>();
		Map<String, String> headers = new LinkedHashMap<String, String>();
		String[] definitionKey = null;
		JsonNode body = null;
		Model model = null;
		for (Parameter parameter : parameters) {
			if (parameter instanceof PathParameter) {
				pathParams.put(parameter.getName(), null);
			} else if (parameter instanceof QueryParameter) {
				queryParams.put(parameter.getName(), null);
			} else if (parameter instanceof HeaderParameter) {
				headers.put(parameter.getName(), null);
			} else if (parameter instanceof BodyParameter) {
				BodyParameter BodyParameter = (BodyParameter) parameter;
				definitionKey = BodyParameter.getSchema().getReference().split("/");
				model = swagger.getDefinitions().get(definitionKey[definitionKey.length - 1]);
				body = toJsonNode(model);
			}
		}
		requestTemplate.setBody(body);
		requestTemplate.setPathParams(pathParams);
		requestTemplate.setQueryParams(queryParams);
		requestTemplate.setHeaders(headers);
	}

	private void createResponseTemplate(Swagger swagger, PantherResponse responseTemplate,
			Map<String, Response> responses) {
		String[] definitionKey = null;
		Model model;
		JsonNode body = null;
		for (Entry<String, io.swagger.models.Response> responseEntry : responses.entrySet()) {
			Model responseSchema = responseEntry.getValue().getResponseSchema();
			if (null != responseEntry.getValue().getResponseSchema()) {
				if (responseSchema instanceof RefModel && null != responseSchema.getReference()) {
					RefModel refModel = (RefModel) responseSchema;
					definitionKey = refModel.getReference().split("/");
					model = swagger.getDefinitions().get(definitionKey[definitionKey.length - 1]);
					body = toJsonNode(model);
					responseTemplate.setStatus(responseEntry.getKey());
					responseTemplate.setBody(body);
				} else if (responseSchema instanceof ModelImpl) {
					ModelImpl modelImpl = (ModelImpl) responseSchema;
					responseTemplate.setStatus(responseEntry.getKey());
					responseTemplate.setBody(new ObjectMapper().createObjectNode().put("type", modelImpl.getType()));
				} else if (responseSchema instanceof ArrayModel) {
					ArrayModel arrayModel = (ArrayModel) responseSchema;
					RefProperty RefProperty = (RefProperty) arrayModel.getItems();
					definitionKey = RefProperty.getOriginalRef().split("/");
					model = swagger.getDefinitions().get(definitionKey[definitionKey.length - 1]);
					responseTemplate.setStatus(responseEntry.getKey());
					responseTemplate.setBody(new ObjectMapper().createArrayNode().add(toJsonNode(model)));
				}
			}
		}
	}

	private JsonNode toJsonNode(Model model) {
		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, String> map = new HashMap<String, String>();
		model.getProperties().forEach((k, v) -> map.put(k, v.getType()));
		try {
			return objectMapper.readTree(objectMapper.writeValueAsString(map));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private String toJson(Object obj) {
		try {
			return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
	}
}