package org.sonarsource.plugins;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IssueReportDto implements Serializable {
	private static final long serialVersionUID = 7578975835435939797L;

	private String projectName;
	private String qualityGateStatus;
	private String version;
	private Map<String, List<String>> issues;
	private Set<String> issueKees;

	public IssueReportDto() {
		issues = Collections.emptyMap();
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Map<String, List<String>> getIssues() {
		return issues;
	}

	public void setIssues(Map<String, List<String>> issues) {
		this.issues = issues;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getQualityGateStatus() {
		return qualityGateStatus;
	}

	public void setQualityGateStatus(String qualityGateStatus) {
		this.qualityGateStatus = qualityGateStatus;
	}

	public Set<String> getIssueKees() {
		return issueKees;
	}

	public void setIssueKees(Set<String> issueKees) {
		this.issueKees = issueKees;
	}
}
