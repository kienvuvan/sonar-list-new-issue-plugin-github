package org.sonarsource.plugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.Project;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.RedmineManager.INCLUDE;
import com.taskadapter.redmineapi.bean.CustomField;
import com.taskadapter.redmineapi.bean.Issue;
import com.taskadapter.redmineapi.bean.User;

public class ExportLeakPeriodNewIssue implements PostProjectAnalysisTask {
	private static final Logger LOGGER = Loggers.get(ExportLeakPeriodNewIssue.class);
	private static final RedmineManager REDMINE_MANAGER = new RedmineManager(CommonConstant.URI_REDMINE,
			CommonConstant.API_ACCESS_KEY);
	private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	private ResultSeparation resultSeparation = null;
	private static Map<String, User> users = new HashMap<>();

	@Override
	public void finished(ProjectAnalysis analysis) {
		Project project = analysis.getProject();
		// Get project id
		String projectUuid = project.getUuid();
		// Get project key
		String projectKey = project.getKey();
		// Get project name
		String projectName = project.getName();
		// Get qualityGate Analysic
		QualityGate gate = analysis.getQualityGate();
		String qualityGateStatus = gate == null ? null : gate.getStatus().toString();
		// Get all user in redmine
		// getAllUserRedmine();
		// Get information in file changelog.xml when Jenkins build success
		List<ResultSeparation> resultSeparations = getInfoIdTicket(getInfoFileChangeLog(projectKey));

		IssueDao issueDao = new IssueDao();

		// Filter data in file changelog.xml to
		// Map<String - EmailUser, Map<Integer - IdTicketRedmine, Set<String -
		// Revision Commit>>>
		Map<String, Map<Integer, Set<String>>> datas = filterData(resultSeparations);
		// If it don't have data
		if (!datas.isEmpty()) {
			for (String emailUser : datas.keySet()) {
				Map<Integer, Set<String>> listIdTickets = datas.get(emailUser);
				LOGGER.info("\n" + emailUser);
				for (Integer idTicket : listIdTickets.keySet()) {
					// Get issue kees for user commit code to github
					IssueReportDto issueReportDto = issueDao.getLeakPeriodNewIssueForUser(projectName, projectUuid,
							emailUser, listIdTickets.get(idTicket));
					LOGGER.info("\n\t" + idTicket);
					LOGGER.info("\n\tNumber issues :" + issueReportDto.getIssueKees().size());
					issueReportDto.setProjectName(projectName);
					issueReportDto.setQualityGateStatus(qualityGateStatus);
					createSubtaskRedmine(issueReportDto, emailUser, idTicket);
				}
			}
		}
	}

	public static void main(String[] args) {
		ExportLeakPeriodNewIssue exportLeakPeriodNewIssue = new ExportLeakPeriodNewIssue();
		String projectKey = "jee7_c1s-all";
		String projectName = "jee7_c1s-all";
		String projectUuid = "AWy9vL77IO9B8pPmv1lm";
		// Get information in file changelog.xml when Jenkins build success
		List<ResultSeparation> resultSeparations = exportLeakPeriodNewIssue
				.getInfoIdTicket(exportLeakPeriodNewIssue.getInfoFileChangeLog(projectKey));

		IssueDao issueDao = new IssueDao();

		Map<String, Map<Integer, Set<String>>> datas = exportLeakPeriodNewIssue.filterData(resultSeparations);
		if (!datas.isEmpty()) {
			for (String emailUser : datas.keySet()) {
				LOGGER.info(emailUser);
				Map<Integer, Set<String>> listIdTickets = datas.get(emailUser);
				for (Integer idTicket : listIdTickets.keySet()) {
					LOGGER.info("\t" + idTicket);
					LOGGER.info("\t\t" + listIdTickets.get(idTicket));
					IssueReportDto issueReportDto = issueDao.getLeakPeriodNewIssueForUser(projectName, projectUuid,
							emailUser, listIdTickets.get(idTicket));
					LOGGER.info("Number issues :" + issueReportDto.getIssueKees().size());
					issueReportDto.setProjectName(projectName);
				}
			}
		} else {
			LOGGER.info("Empty");
		}

		// LOGGER.info("++++++++++++++++++++++");
		// Set<String> committers =
		// exportLeakPeriodNewIssue.getCommitterList(resultSeparations);
		// IssueReportDto is = issueDao.getLeakPeriodNewIssue(projectName,
		// projectUuid, committers);
		// Map<String, List<String>> issues = is.getIssues();
		// for (String emailUser : issues.keySet()) {
		// LOGGER.info(emailUser);
		// LOGGER.info("Number issues :" + issues.get(emailUser).size() + "");
		// }
	}

	/**
	 * Filter data to format Map<String - EmailUser, Map<Integer -
	 * IdTicketRedmine, Set<String - Revision Commit>>>
	 * 
	 * @param resultSeparations
	 *            Data get from file changelog.xml of Jenkins builds
	 * @return
	 */
	private Map<String, Map<Integer, Set<String>>> filterData(List<ResultSeparation> resultSeparations) {
		Map<String, Map<Integer, Set<String>>> result = new HashMap<>();
		// If data isn't empty
		if (!resultSeparations.isEmpty()) {
			for (ResultSeparation rs : resultSeparations) {
				String emailUser = rs.getEmailUser();
				Map<Integer, Set<String>> revisionsInTicket = null;
				// If map haven't key email user
				if (!result.containsKey(rs.getEmailUser())) {
					// Add new
					revisionsInTicket = new HashMap<>();
					revisionsInTicket.put(rs.getRedmine(), new HashSet<>(Arrays.asList(rs.getRevision())));
					result.put(emailUser, revisionsInTicket);
				} else {
					revisionsInTicket = result.get(emailUser);
					int idRedmine = rs.getRedmine();
					if (revisionsInTicket.containsKey(idRedmine)) {
						Set<String> revisions = revisionsInTicket.get(idRedmine);
						revisions.add(rs.getRevision());
						revisionsInTicket.put(idRedmine, revisions);
					} else {
						revisionsInTicket.put(idRedmine, new HashSet<>(Arrays.asList(rs.getRevision())));
					}
					result.put(emailUser, revisionsInTicket);
				}
			}
		}
		return result;
	}

	/**
	 * Method get info id ticket, id redmine, revision commit
	 * 
	 * @param projectKey
	 *            name of project
	 * @return ResultSeparation
	 */
	private List<ResultSeparation> getInfoFileChangeLog(String projectKey) {
		String buildPath = CommonConstant.ROOT_JENKINS_BUILD + projectKey + "\\builds";
		List<ResultSeparation> results = new ArrayList<>();
		// Get lastBuildNumber in jenkins build
		File nextBuildNumberFile = new File(buildPath);
		List<Integer> buildNumber = new ArrayList<>();
		nextBuildNumberFile.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (!name.matches("\\d+")) {
					return false;
				} else {
					buildNumber.add(Integer.parseInt(name.trim()));
					return true;
				}
			}
		});
		int lastBuildNumber = Collections.max(buildNumber);
		LOGGER.info("Last Build : " + lastBuildNumber);
		File changeLog = new File(buildPath + "\\" + lastBuildNumber + "\\changelog.xml");
		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(changeLog))) {
			String line = "";
			int indexLine = 1;
			int startBlock = 1;
			String revision = "";
			while ((line = bufferedReader.readLine()) != null) {
				indexLine++;
				line = line.trim();

				if (line.startsWith("commit ")) {
					startBlock = indexLine;
					revision = line.trim().split("\\s+")[1];
					if (indexLine > 2) {
						results.add(resultSeparation);
						continue;
					}
				}

				if (indexLine == startBlock + 4) {
					resultSeparation = new ResultSeparation();
					resultSeparation.setRevision(revision);
					String committer = line.substring(line.indexOf('<') + 1, line.lastIndexOf('>'));
					resultSeparation.setEmailUser(committer);
				}

				if (indexLine >= startBlock + 6) {
					if (!StringUtils.isEmpty(line)) {
						if (!line.startsWith(":")) {
							resultSeparation.getMessage().append(line + " ");
						} else {
							resultSeparation.getCommits().add(line);
						}
					}
				}
			}
			// Add end block information commit
			if (bufferedReader.readLine() == null) {
				if (resultSeparation != null) {
					results.add(resultSeparation);
				}
			}
		} catch (IOException e) {
			LOGGER.error("Error read file : " + e.getMessage());
		}
		return results;
	}

	/**
	 * Get info id ticket
	 * 
	 * @param resultSeparations
	 * @return
	 */
	private List<ResultSeparation> getInfoIdTicket(List<ResultSeparation> resultSeparations) {
		List<ResultSeparation> results = new ArrayList<>();
		if (!resultSeparations.isEmpty()) {
			for (ResultSeparation rs : resultSeparations) {
				// Get info id ticket
				String messageCommit = rs.getMessage().toString().trim();
				String[] contents = messageCommit.split("[-]+");
				String message = contents[contents.length - 1].trim();
				String[] tokens = message.replace("#", " ").split("\\s+");
				String ticketId = tokens[1].replace(":", "");
				if (ticketId.matches("\\d+") && ticketId.length() == 7) {
					rs.setRedmine(Integer.valueOf(ticketId));
					results.add(rs);
				}
			}
		}
		return results;
	}

	/**
	 * Method create subtask of issue in redmine
	 * 
	 * @param issueReportDto
	 * @param redmine
	 */
	private void createSubtaskRedmine(IssueReportDto issueReportDto, String emailUser, int idRedmine) {
		try {
			// If list issues of committer not empty
			if (!issueReportDto.getIssueKees().isEmpty()) {
				// Get user commit github
				User assignee = getAssigned(emailUser, users);
				// If exist user in redmine
				if (assignee != null) {

					// KienVV Start Test
					// StringBuilder sb = new StringBuilder();
					// sb.append(buildReportContent(issueReportDto, emailUser));
					// sb.append("\nAssignee : " + getAssigned(emailUser,
					// users));
					// sb.append("\nEmail : " + emailUser);
					// sb.append("\nId ticket : " + idRedmine);
					// LOGGER.info("\n" + sb.toString());
					// KienVV End Test

					// KienVV Start Test
					// idRedmine = 9034516;
					// KienVV End Test

					Issue issueParent = REDMINE_MANAGER.getIssueById(idRedmine, INCLUDE.journals, INCLUDE.relations);
					Issue issue = new Issue();
					issue.setSubject("[SonarQube] Analysis result for" + " [" + issueReportDto.getProjectName() + "]"
							+ " At rev " + issueReportDto.getVersion() + " ");

					// KienVV Start Test
					// issue.setDescription(sb.toString());
					// assignee = new User();
					// assignee.setId(110);
					// issue.setAssignee(assignee);
					// KienVV End Test

					String projectKey = issueParent.getProject().getId() + "";

					issue.setDescription(buildReportContent(issueReportDto, emailUser));

					// KienVV Start Test
					// issue.setDescription(buildReportContent(issueReportDto,
					// emailUser) + "\n" + sb.toString());
					// KienVV End Test

					issue.setParentId(idRedmine);

					issue.setAssignee(assignee);

					Calendar calendar = Calendar.getInstance();
					calendar.add(Calendar.DATE, CommonConstant.EXPECTED_RELEASE_C1S);

					// Add custom field required
					List<CustomField> customFields = new ArrayList<>();
					CustomField customField1 = new CustomField(113, "Độ ưu tiên gốc", CommonConstant.ORIGINAL_PRIORITY);
					CustomField customField2 = new CustomField(8, "C1S #", CommonConstant.C1S);
					CustomField customField3 = new CustomField(93, "Kỳ vọng release của C1S",
							FORMAT.format(calendar.getTime()));
					CustomField customField4 = new CustomField(130, "Team", CommonConstant.TEAM);
					customFields.addAll(Collections
							.unmodifiableList(Arrays.asList(customField1, customField2, customField3, customField4)));
					issue.setCustomFields(customFields);

					issue.setStartDate(new Date());

					REDMINE_MANAGER.createIssue(projectKey, issue);
				}
			}
		} catch (RedmineException e) {
			LOGGER.error("Create issue error : " + e.getMessage());
		}
	}

	/**
	 * build String report content for send mail after sonarQube analysis
	 *
	 * @param issueReportDto
	 * @return report content
	 */
	private String buildReportContent(IssueReportDto issueReportDto, String author) {
		StringBuilder reportContent = new StringBuilder("SonarQube report");
		reportContent.append("\n " + "The project name is : " + issueReportDto.getProjectName());
		reportContent.append("\n " + "Revision is : " + issueReportDto.getVersion());
		reportContent.append("\n " + "Quality gate is : " + issueReportDto.getQualityGateStatus());
		int rowCount = 0;
		for (String kee : issueReportDto.getIssueKees()) {
			if (rowCount == 0) {
				reportContent.append("\n " + "The bug are: ");
			}
			reportContent.append("\n " + CommonConstant.SONAR_QUBE_SERVER + "project/issues?id="
					+ issueReportDto.getProjectName() + "&open=" + kee + "&resolved=false&sinceLeakPeriod=true");
			++rowCount;
		}
		reportContent.append("\n " + "Total bug = " + rowCount);

		return reportContent.toString();
	}

	/**
	 * Get user commit code to github
	 * 
	 * @param projectkey
	 * @param author
	 * @return
	 */
	private User getAssigned(String authorMail, Map<String, User> users) {
		User assigned = null;
		if (users.keySet().contains(authorMail)) {
			assigned = users.get(authorMail);
		}
		return assigned;
	}

	// /**
	// * Get all user in redmine
	// */
	// private void getAllUserRedmine() {
	// try {
	// for (User user : REDMINE_MANAGER.getUsers()) {
	// if (user != null) {
	// users.put(user.getMail(), user);
	// }
	// }
	// } catch (RedmineException e) {
	// LOGGER.error("Error API Redmine : " + e.getMessage());
	// }
	// }

	static {
		try {
			for (User user : REDMINE_MANAGER.getUsers()) {
				if (user != null) {
					users.put(user.getMail(), user);
				}
			}
		} catch (RedmineException e) {
			LOGGER.error("Error API Redmine : " + e.getMessage());
		}
	}

	// /**
	// * Get list email committer github
	// */
	// private Set<String> getCommitterList(List<ResultSeparation>
	// resultSeparations) {
	// Set<String> committers = new HashSet<>();
	// for (ResultSeparation rs : resultSeparations) {
	// committers.add(rs.getEmailUser());
	// }
	// return committers;
	// }

}
