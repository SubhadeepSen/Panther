package com.panther.core;

import static com.panther.util.PantherUtils.EMPTY_STRING;
import static com.panther.util.PantherUtils.isNotValidString;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.panther.config.ConfigLoader;
import com.panther.exception.PantherException;
import com.panther.model.PantherModel;
import com.panther.model.PantherRequest;
import com.panther.model.PantherResponse;
import com.panther.util.PantherUtils;

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

public class PantherBuilder {

	private static final Logger LOGGER = LoggerFactory.getLogger(PantherBuilder.class);

	public void buildCaseTemplate(String pathOfApiSpecification, String outputPath) {
		if (isNotValidString(pathOfApiSpecification) || isNotValidString(outputPath)) {
			LOGGER.error("Invalid location of api specification / output path >> " + "[" + pathOfApiSpecification + ", "
					+ outputPath + "]");
			throw new PantherException("Invalid location of api specification / output path.");
		}
		Map<String, List<PantherModel>> fileToPantherModels = buildPantherModels(pathOfApiSpecification);
		fileToPantherModels.entrySet().forEach(entry -> {
			try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath + "/" + entry.getKey() + ".json"))) {
				bw.write(PantherUtils.convertToJsonString(entry.getValue()));
			} catch (IOException e) {
				LOGGER.error(e.getMessage());
				throw new PantherException(e.getMessage());
			}
			LOGGER.info("Template generation completed >> " + outputPath);
		});
	}

	private Map<String, List<PantherModel>> buildPantherModels(String pathOfApiSpecification) {
		Map<String, List<PantherModel>> fileToPantherModels = new HashMap<String, List<PantherModel>>();
		PantherModel pantherModel = null;
		PantherRequest pantherRequest = null;
		PantherResponse pantherResponse = null;
		Swagger swagger = null;
		String tag = "";
		Operation operation = null;

		if (pathOfApiSpecification.startsWith("http")) {
			// retrieving and parsing API specification from http(s) location
			HttpClient httpClient = HttpClientBuilder.create().build();
			try {
				String swaggerDefinition = EntityUtils
						.toString(httpClient.execute(new HttpGet(pathOfApiSpecification)).getEntity());
				swagger = new SwaggerParser().read(PantherUtils.convertToJsonNode(swaggerDefinition));
			} catch (ParseException | IOException e) {
				LOGGER.error(e.getMessage());
				throw new PantherException(e.getMessage());
			}
		} else {
			// retrieving and parsing API specification from local
			swagger = new SwaggerParser().read(pathOfApiSpecification);
		}

		String baseUrl = ConfigLoader.getConfig(null).getApiScheme() + "://" + swagger.getHost() + swagger.getBasePath();

		for (Entry<String, Path> pathEntrySet : swagger.getPaths().entrySet()) {
			pantherModel = new PantherModel();
			pantherRequest = new PantherRequest();
			pantherResponse = new PantherResponse();
			pantherRequest.setUrl(baseUrl + pathEntrySet.getKey());
			for (Entry<HttpMethod, Operation> operationEntrySet : pathEntrySet.getValue().getOperationMap()
					.entrySet()) {
				operation = operationEntrySet.getValue();
				pantherModel.setDescription(operation.getSummary());
				pantherRequest.setMethod(operationEntrySet.getKey().toString());
				buildRequestTemplate(swagger, pantherRequest, operation.getParameters());
				buildResponseTemplate(swagger, pantherResponse, operation.getResponses());
				pantherRequest.getHeaders().put("Accept", operation.getConsumes().get(0));
				pantherRequest.getHeaders().put("Content-Type", operation.getConsumes().get(0));
				tag = operation.getTags().get(0);
			}
			pantherModel.setRequest(pantherRequest);
			pantherModel.setResponse(pantherResponse);

			if (null == fileToPantherModels.get(tag)) {
				fileToPantherModels.put(tag, new ArrayList<PantherModel>());
			}
			fileToPantherModels.get(tag).add(pantherModel);
		}
		return fileToPantherModels;
	}

	private void buildRequestTemplate(Swagger swagger, PantherRequest pantherRequest, List<Parameter> parameters) {
		Map<String, String> pathParams = new LinkedHashMap<String, String>();
		Map<String, String> queryParams = new LinkedHashMap<String, String>();
		Map<String, String> headers = new LinkedHashMap<String, String>();
		String[] definitionKey = null;
		JsonNode body = null;
		Model model = null;
		BodyParameter bodyParameter = null;
		for (Parameter parameter : parameters) {
			if (parameter instanceof PathParameter) {
				pathParams.put(parameter.getName(), EMPTY_STRING);
			} else if (parameter instanceof QueryParameter) {
				queryParams.put(parameter.getName(), EMPTY_STRING);
			} else if (parameter instanceof HeaderParameter) {
				headers.put(parameter.getName(), EMPTY_STRING);
			} else if (parameter instanceof BodyParameter) {
				bodyParameter = (BodyParameter) parameter;
				definitionKey = bodyParameter.getSchema().getReference().split("/");
				model = swagger.getDefinitions().get(definitionKey[definitionKey.length - 1]);
				body = toJsonNode(model);
			}
		}
		pantherRequest.setBody(body);
		pantherRequest.setPathParams(pathParams);
		pantherRequest.setQueryParams(queryParams);
		pantherRequest.setHeaders(headers);
	}

	private void buildResponseTemplate(Swagger swagger, PantherResponse pantherResponse,
			Map<String, Response> responses) {
		String[] definitionKey = null;
		Model model;
		JsonNode body = null;
		Model responseModel = null;
		RefModel refModel = null;
		ModelImpl modelImpl = null;
		ArrayModel arrayModel = null;
		RefProperty refProperty = null;
		for (Entry<String, io.swagger.models.Response> responseEntrySet : responses.entrySet()) {
			responseModel = responseEntrySet.getValue().getResponseSchema();
			if (null != responseModel) {
				if (responseModel instanceof RefModel && null != responseModel.getReference()) {
					refModel = (RefModel) responseModel;
					definitionKey = refModel.getReference().split("/");
					model = swagger.getDefinitions().get(definitionKey[definitionKey.length - 1]);
					body = toJsonNode(model);
					pantherResponse.setStatus(responseEntrySet.getKey());
					pantherResponse.setBody(body);
				} else if (responseModel instanceof ModelImpl) {
					modelImpl = (ModelImpl) responseModel;
					pantherResponse.setStatus(responseEntrySet.getKey());
					pantherResponse.setBody(PantherUtils.createObjectNode().put("type", modelImpl.getType()));
				} else if (responseModel instanceof ArrayModel) {
					arrayModel = (ArrayModel) responseModel;
					refProperty = (RefProperty) arrayModel.getItems();
					definitionKey = refProperty.getOriginalRef().split("/");
					model = swagger.getDefinitions().get(definitionKey[definitionKey.length - 1]);
					pantherResponse.setStatus(responseEntrySet.getKey());
					pantherResponse.setBody(PantherUtils.createArrayNode().add(toJsonNode(model)));
				}
			}
		}
	}

	private JsonNode toJsonNode(Model model) {
		Map<String, String> map = new HashMap<String, String>();
		model.getProperties().forEach((k, v) -> map.put(k, v.getType()));
		return PantherUtils.convertToJsonNode(PantherUtils.convertToJsonString(map));
	}
}
