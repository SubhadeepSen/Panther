package com.panther.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.joda.time.LocalDate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.panther.config.ConfigLoader;
import com.panther.model.PantherAnalytics;
import com.panther.model.PantherModel;

public class PantherReport {

	private static final String REPORT_SRC = "./src/main/java/com/panther/report";
	private static final String REPORT_DEST = "./target/panther-report";
	private String reportFile;
	private Map<String, PantherAnalytics> analytics;
	private int totalPassed = 0;
	private int totalFailed = 0;
	private int minResponseTime = Integer.MAX_VALUE;
	private int maxResponseTime = Integer.MIN_VALUE;

	public void build(Map<String, List<PantherModel>> fileNameToPantherList) {
		createAnalytics(fileNameToPantherList);
		try {
			copyResources(new File(REPORT_SRC), new File(REPORT_DEST));
			updatePieChartScript();
			this.reportFile = REPORT_DEST + "/panther-report-" + dateTimeString() + ".html";
			Files.createFile(Paths.get(reportFile));
			copyReportDefinition();
			generateReport(fileNameToPantherList);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void updatePieChartScript() throws IOException {
		StringBuilder scriptData = new StringBuilder();
		StringBuilder sb = new StringBuilder();
		scriptData.append("[{value: ").append(totalPassed).append(", color: \"#4caf50\"},").append("{value: ")
				.append(totalFailed).append(", color: \"#f44336\"}]");
		List<String> lines = Files.readAllLines(Paths.get(REPORT_DEST + "/js/panther.js"));
		for (String line : lines) {
			if (line.contains("<<analyticsData>>")) {
				line = line.replaceAll("<<analyticsData>>", scriptData.toString());
			}
			sb.append(line).append("\n");
		}
		Files.write(Paths.get(REPORT_DEST + "/js/panther.js"), sb.toString().getBytes());
	}

	private void createAnalytics(Map<String, List<PantherModel>> fileNameToPantherList) {
		analytics = new HashMap<String, PantherAnalytics>();
		int passedCount;
		int failedCount;
		for (Entry<String, List<PantherModel>> entry : fileNameToPantherList.entrySet()) {
			passedCount = 0;
			failedCount = 0;
			for (PantherModel model : entry.getValue()) {
				if (model.caseStatus()) {
					passedCount++;
					totalPassed++;
				} else {
					failedCount++;
					totalFailed++;
				}
				if (Integer.parseInt(model.getResponse().getResponseTime()) < minResponseTime) {
					minResponseTime = Integer.parseInt(model.getResponse().getResponseTime());
				}
				if (Integer.parseInt(model.getResponse().getResponseTime()) > maxResponseTime) {
					maxResponseTime = Integer.parseInt(model.getResponse().getResponseTime());
				}
			}
			analytics.put(entry.getKey(), new PantherAnalytics(passedCount, failedCount));
		}
	}

	private void generateReport(Map<String, List<PantherModel>> fileNameToPantherList) throws IOException {
		StringBuilder sb = new StringBuilder();
		List<String> defLines = Files.readAllLines(Paths.get(reportFile));
		for (String defLine : defLines) {
			if (defLine.contains("<<appName>>")) {
				sb.append(defLine.replaceAll("<<appName>>", ConfigLoader.getConfig(null).getReportName()));
			} else if (defLine.contains("<<rpt-body>>")) {
				fileNameToPantherList.entrySet().forEach(entry -> {
					try {
						sb.append(defLine.replaceAll("<<rpt-body>>", parseAndLoadBody(entry)));
					} catch (IOException e) {
					}
				});
			} else {
				sb.append(defLine);
			}
		}
		Files.write(Paths.get(reportFile), sb.toString().getBytes());
	}

	private String parseAndLoadBody(Entry<String, List<PantherModel>> entry) throws IOException {
		StringBuilder result = new StringBuilder();
		List<String> bodyLines = Files.readAllLines(Paths.get(REPORT_SRC + "/rpt-body.txt"));
		int passedCount = analytics.get(entry.getKey()).getPassedCaseCount();
		int totalCount = analytics.get(entry.getKey()).getTotalCaseCount();
		for (String line : bodyLines) {
			if (line.contains("<<fileName>>")) {
				line = line.replaceAll("<<fileName>>", entry.getKey().replaceAll(".json", ""));
			}
			if (line.contains("<<caseStatusColor>>")) {
				if (passedCount == totalCount) {
					line = line.replaceAll("<<caseStatusColor>>", "green-background");
				} else if (passedCount >= (totalCount / 2)) {
					line = line.replaceAll("<<caseStatusColor>>", "orange-background");
				} else {
					line = line.replaceAll("<<caseStatusColor>>", "red-background");
				}
			}
			if (line.contains("<<passedCount>>")) {
				line = line.replaceAll("<<passedCount>>", String.valueOf(passedCount));
			}
			if (line.contains("<<totalCount>>")) {
				line = line.replaceAll("<<totalCount>>", String.valueOf(totalCount));
			}
			if (line.contains("<<loadCase>>")) {
				line = line.replaceAll("<<loadCase>>", parseAndLoadCase(entry.getValue()));
			}
			result.append(line).append("\n");
		}
		return result.toString();
	}

	private String parseAndLoadCase(List<PantherModel> pantherModels) throws IOException {
		StringBuilder result = new StringBuilder();
		List<String> caseLines = null;
		for (PantherModel model : pantherModels) {
			caseLines = Files.readAllLines(Paths.get(REPORT_SRC + "/rpt-case.txt"));
			for (String line : caseLines) {
				if (line.contains("<<description>>")) {
					line = line.replaceAll("<<description>>", model.getDescription().replaceAll(" ", ""));
				}
				if (line.contains("<<caseStatusColor>>")) {
					if (!model.caseStatus()) {
						line = line.replaceAll("<<caseStatusColor>>", "red-background");
					} else {
						line = line.replaceAll("<<caseStatusColor>>", "green-background");
					}
				}
				if (line.contains("<<caseStatus>>")) {
					if (!model.caseStatus()) {
						line = line.replaceAll("<<caseStatus>>", "Failed");
					} else {
						line = line.replaceAll("<<caseStatus>>", "Passed");
					}
				}
				if (line.contains("<<responseTime>>")) {
					line = line.replaceAll("<<responseTime>>", "(" + model.getResponse().getResponseTime() + " ms)");
				}
				if (line.contains("<<errorMessage>>")) {
					line = line.replaceAll("<<errorMessage>>", model.getCaseMessage());
				}
				if (line.contains("<<rpt-rqst-rspn>>")) {
					if (ConfigLoader.getConfig(null).isEnableReportLogging()) {
						line = line.replaceAll("<<rpt-rqst-rspn>>", addRequestResponse(model));
					} else {
						line = line.replaceAll("<<rpt-rqst-rspn>>", "");
					}
				}
				result.append(line).append("\n");
			}
		}
		return result.toString();
	}

	private String addRequestResponse(PantherModel model) throws IOException {
		StringBuilder result = new StringBuilder();
		List<String> caseLines = null;
		caseLines = Files.readAllLines(Paths.get(REPORT_SRC + "/rpt-rqst-rspn.txt"));
		for (String line : caseLines) {
			if (line.contains("<<rqst-rspn>>")) {
				line = line.replaceAll("<<rqst-rspn>>", model.getDescription().replaceAll(" ", "") + "-rr");
			}
			if (line.contains("<<caseStatusColor>>")) {
				if (model.caseStatus()) {
					line = line.replaceAll("<<caseStatusColor>>", "green-background");
				} else {
					line = line.replaceAll("<<caseStatusColor>>", "red-background");
				}
			}
			//TODO: need to properly log request and response
			if (line.contains("<<request-body>>")) {
				line = line.replaceAll("<<request-body>>", new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(model.getRequest()));
			}
			if (line.contains("<<response-body>>")) {
				if (null != model.getResponse().getBody()) {
					line = line.replaceAll("<<response-body>>", model.getResponse().getBody().toString());
				} else {
					line = line.replaceAll("<<response-body>>", "");
				}
			}
			result.append(line).append("\n");
		}
		return result.toString();
	}

	private void copyReportDefinition() throws IOException {
		Files.copy(Paths.get(REPORT_SRC + "/rpt-def.txt"), Paths.get(reportFile), StandardCopyOption.REPLACE_EXISTING);
	}

	private static void copyResources(File sourceFolder, File destinationFolder) throws IOException {
		if (sourceFolder.isDirectory()) {
			if (!destinationFolder.exists()) {
				destinationFolder.mkdir();
			}
			String files[] = sourceFolder.list();
			for (String file : files) {
				copyResources(new File(sourceFolder, file), new File(destinationFolder, file));
			}
		} else if (!sourceFolder.getName().startsWith("rpt-")) {
			Files.copy(sourceFolder.toPath(), destinationFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private String dateTimeString() {
		String date = LocalDate.now().toString().replaceAll("-", "");
		String time = LocalTime.now().toString().split("\\.")[0].replaceAll(":", "");
		return date + time;
	}
}
