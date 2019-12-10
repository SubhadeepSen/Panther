package com.panther.runner;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.panther.auth.Authentication;
import com.panther.builder.RequestResponseTemplateBuilder;
import com.panther.builder.RequestTemplateResolver;
import com.panther.config.ConfigLoader;
import com.panther.model.PantherConfig;
import com.panther.model.PantherModel;

public class PantherRunner {

	public static void main(String[] args) {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put("Authorization", "Basic cXVldWUtbWFuYWdlcjpxdWV1ZU1hbmFnZXJAMTIzNDU=");
		new PantherRunner().executeTests(() -> map);
	}

	public void executeTests(Authentication authentication) {
		PantherConfig pantherConfig = ConfigLoader.getConfig(authentication);
		Map<String, List<PantherModel>> map = null;

		if (pantherConfig.wantToParse()) {
			new RequestResponseTemplateBuilder().writeToJsonFile(pantherConfig.getApiDocsLocation(),
					pantherConfig.getTestCasesLocation());
		} else if (!pantherConfig.wantToParse() && pantherConfig.getTestCasesLocation() != null
				&& pantherConfig.getTestCasesLocation() != "") {
			RequestTemplateResolver resolver = new RequestTemplateResolver();
			map = resolver.buildRequestObjects(pantherConfig.getTestCasesLocation());
			map.entrySet().forEach(entry -> {
				try {
					System.out.println(">>>>>> " + entry.getKey());
					resolver.makeHttpCalls(entry.getValue());
				} catch (UnsupportedEncodingException e) {
					System.err.println("handle exception here....");
				}
			});
		} else {
			// TODO: throw exception
		}
	}

}
