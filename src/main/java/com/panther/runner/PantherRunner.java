package com.panther.runner;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

import com.panther.auth.Auth;
import com.panther.auth.Authentication;
import com.panther.config.ConfigLoader;
import com.panther.core.PantherEngine;
import com.panther.core.TemplateBuilder;
import com.panther.core.TemplateResolver;
import com.panther.exception.PantherException;
import com.panther.model.PantherConfig;
import com.panther.model.PantherModel;

public class PantherRunner extends ParentRunner<PantherModel> {

	private Map<String, List<PantherModel>> map;

	private int count = 0;

	public PantherRunner(Class<?> testClass) throws InitializationError {
		super(testClass);

		Optional<Method> authMethod = Arrays.stream(testClass.getDeclaredMethods())
				.filter(m -> m.isAnnotationPresent(Auth.class)).findFirst();
		Authentication auth = null;
		if (authMethod.isPresent()) {
			try {
				auth = (Authentication) authMethod.get().invoke(testClass.newInstance());
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| InstantiationException e) {
				e.printStackTrace();
			}
		}
		PantherConfig pantherConfig = ConfigLoader.getConfig(auth);
		if (pantherConfig.wantToParse()) {
			new TemplateBuilder().writeToJsonFile(pantherConfig.getApiDocsLocation(),
					pantherConfig.getTestCasesLocation());
		} else if (!pantherConfig.wantToParse() && pantherConfig.getTestCasesLocation() != null
				&& pantherConfig.getTestCasesLocation() != "") {
			map = new TemplateResolver().resolve(pantherConfig.getTestCasesLocation());
		}
	}

	@Override
	protected List<PantherModel> getChildren() {
		List<PantherModel> pantherModels = new ArrayList<PantherModel>();
		map.entrySet().forEach(e -> {
			pantherModels.addAll(e.getValue());
		});
		count = pantherModels.size();
		return pantherModels;
	}

	@Override
	protected Description describeChild(PantherModel pantherModel) {
		return Description.createSuiteDescription(pantherModel.getDescription());
	}

	@Override
	protected void runChild(PantherModel pantherModel, RunNotifier notifier) {
		try {
			notifier.fireTestStarted(Description.createSuiteDescription(pantherModel.getDescription()));
			new PantherEngine().execute(pantherModel);
			System.out.println(pantherModel.caseStatus() + " --> " + pantherModel.getCaseMessage());
			if (!pantherModel.caseStatus()) {
				notifier.fireTestFailure(new Failure(Description.createSuiteDescription(pantherModel.getDescription()),
						new PantherException(pantherModel.getCaseMessage())));
			}
		} catch (UnsupportedEncodingException | PantherException e) {
			notifier.fireTestFailure(new Failure(Description.createSuiteDescription(pantherModel.getDescription()), e));
		} finally {
			notifier.fireTestFinished(Description.createSuiteDescription(pantherModel.getDescription()));
			count--;
		}

		if (count == 0) {
			// TODO: Build report
			System.out.println("Building report.....");
		}
	}
}
