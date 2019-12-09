package com.panther.runner;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.TreeMap;

import com.panther.builder.RequestResponseTemplateBuilder;
import com.panther.builder.RequestTemplateResolver;
import com.panther.init.Authentication;
import com.panther.init.ConfigLoader;
import com.panther.model.PantherConfig;
import com.panther.model.RequestResponseTemplate;

public class PantherRunner {

	public static void main(String[] args) {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put("Authorization", "Basic cXVldWUtbWFuYWdlcjpxdWV1ZU1hbmFnZXJAMTIzNDU=");
		new PantherRunner().executeTests(() -> map);
	}

	public void executeTests(Authentication authentication) {
		PantherConfig pantherConfig = ConfigLoader.getConfig(authentication);

		if (pantherConfig.wantToParse()) {
			new RequestResponseTemplateBuilder().writeToJsonFile(pantherConfig.getApiDocsLocation(),
					pantherConfig.getTemplateLocation());
		} else if (!pantherConfig.wantToParse() && pantherConfig.getTemplateLocation() != null
				&& pantherConfig.getTemplateLocation() != "") {
			RequestTemplateResolver resolver = new RequestTemplateResolver();
			List<RequestResponseTemplate> build = resolver.buildRequestObjects(pantherConfig.getTemplateLocation());
			try {
				resolver.makeHttpCalls(build);
			} catch (UnsupportedEncodingException e) {
				System.err.println("handle exception here....");
			}
		} else {
			// TODO: throw exception
		}

	}

}
