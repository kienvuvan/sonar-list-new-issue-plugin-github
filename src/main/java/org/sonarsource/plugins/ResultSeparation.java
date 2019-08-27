package org.sonarsource.plugins;

import java.util.ArrayList;
import java.util.List;

public class ResultSeparation {

	private String revision;
	private int redmine;
	private String emailUser;
	private StringBuilder message = new StringBuilder();
	private List<String> commits = new ArrayList<>();
	// private Set<String> pathFileIssues = new HashSet<>();

	public String getRevision() {
		return revision;
	}

	public void setRevision(String revision) {
		this.revision = revision;
	}

	public int getRedmine() {
		return redmine;
	}

	public void setRedmine(int redmine) {
		this.redmine = redmine;
	}

	public String getEmailUser() {
		return emailUser;
	}

	public void setEmailUser(String emailUser) {
		this.emailUser = emailUser;
	}

	public StringBuilder getMessage() {
		return message;
	}

	public void setMessage(StringBuilder message) {
		this.message = message;
	}

	public List<String> getCommits() {
		return commits;
	}

	public void setCommits(List<String> commits) {
		this.commits = commits;
	}

	// public Set<String> getPathFileIssues() {
	// return pathFileIssues;
	// }
	//
	// public void setPathFileIssues(Set<String> pathFileIssues) {
	// this.pathFileIssues = pathFileIssues;
	// }

}
