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
import com.panther.builder.RequestResponseTemplateBuilder;
import com.panther.builder.RequestTemplateResolver;
import com.panther.config.ConfigLoader;
import com.panther.exception.PantherException;
import com.panther.model.PantherConfig;
import com.panther.model.PantherModel;

public class PantherRunner extends ParentRunner<PantherModel> {

	private Map<String, List<PantherModel>> map;

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
			new RequestResponseTemplateBuilder().writeToJsonFile(pantherConfig.getApiDocsLocation(),
					pantherConfig.getTestCasesLocation());
		} else if (!pantherConfig.wantToParse() && pantherConfig.getTestCasesLocation() != null
				&& pantherConfig.getTestCasesLocation() != "") {
			RequestTemplateResolver resolver = new RequestTemplateResolver();
			map = resolver.buildRequestObjects(pantherConfig.getTestCasesLocation());
		}
	}

	@Override
	protected List<PantherModel> getChildren() {
		List<PantherModel> list = new ArrayList<PantherModel>();
		map.entrySet().forEach(e -> {
			list.addAll(e.getValue());
		});
		return list;
	}

	@Override
	protected Description describeChild(PantherModel child) {
		return Description.createSuiteDescription(child.getDescription());
	}

	@Override
	protected void runChild(PantherModel child, RunNotifier notifier) {
		try {
			notifier.fireTestStarted(Description.createSuiteDescription(child.getDescription()));
			new RequestTemplateResolver().makeHttpCalls(child);
			System.out.println(child.caseStatus() + " --> " + child.getCaseMessage());
			if (!child.caseStatus()) {
				notifier.fireTestFailure(new Failure(Description.createSuiteDescription(child.getDescription()),
						new PantherException(child.getCaseMessage())));
				return;
			}
			notifier.fireTestFinished(Description.createSuiteDescription(child.getDescription()));
		} catch (UnsupportedEncodingException | PantherException e) {
			notifier.fireTestFailure(new Failure(Description.createSuiteDescription(child.getDescription()), e));
		}
	}

}
