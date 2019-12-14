package com.panther.model;

public class PantherAnalytics {

	private int passedCaseCount;
	private int failedCaseCount;
	private int totalCaseCount;

	public PantherAnalytics() {

	}

	public PantherAnalytics(int passedCaseCount, int failedCaseCount) {
		super();
		this.passedCaseCount = passedCaseCount;
		this.failedCaseCount = failedCaseCount;
		this.totalCaseCount = passedCaseCount + failedCaseCount;
	}

	public int getPassedCaseCount() {
		return passedCaseCount;
	}

	public void setPassedCaseCount(int passedCaseCount) {
		this.passedCaseCount = passedCaseCount;
	}

	public int getFailedCaseCount() {
		return failedCaseCount;
	}

	public void setFailedCaseCount(int failedCaseCount) {
		this.failedCaseCount = failedCaseCount;
	}

	public int getTotalCaseCount() {
		return totalCaseCount;
	}

	public void setTotalCaseCount(int totalCaseCount) {
		this.totalCaseCount = totalCaseCount;
	}

}
