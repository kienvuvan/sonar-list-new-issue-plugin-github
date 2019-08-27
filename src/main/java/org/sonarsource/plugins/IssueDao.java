package org.sonarsource.plugins;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class IssueDao {
	private static final Logger LOGGER = Loggers.get(IssueDao.class);

	/**
	 * Connect db and get Kee (unique column) of new issue form Leak Period
	 * 
	 * @param projectName
	 *            name of project
	 * @param projectUuid
	 *            unique id of project
	 * @return IssueReportDto
	 */
	public IssueReportDto getLeakPeriodNewIssue(String projectName, String projectUuid, Set<String> committers) {
		LOGGER.info("\nproject is : " + projectName + ", " + projectUuid);
		IssueReportDto issueReportDto = new IssueReportDto();
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			LOGGER.info(e.getMessage());
		}
		try (Connection conn = DriverManager.getConnection(CommonConstant.SONAR_QUBE_JDBC_URL,
				CommonConstant.SONAR_QUBE_JDBC_USERNAME, CommonConstant.SONAR_QUBE_JDBC_PASS);
				Statement connStatement = conn.createStatement()) {
			String sqlGetSnapshotPeriodTime = " SELECT snapshots.period1_date, " + "   snapshots.build_date, "
					+ "   snapshots.version " + " FROM snapshots " + " INNER JOIN projects "
					+ "   ON projects.uuid = snapshots.component_uuid " + " WHERE projects.project_uuid = \""
					+ projectUuid + "\"" + "   AND projects.name = \"" + projectName + "\" "
					+ "   AND snapshots.islast = 1; ";
			ResultSet snapshotPeriodTimeSet = connStatement.executeQuery(sqlGetSnapshotPeriodTime);
			LOGGER.info(sqlGetSnapshotPeriodTime);

			snapshotPeriodTimeSet.next();

			issueReportDto.setVersion(snapshotPeriodTimeSet.getString("version"));

			String sqlGetPeriodIssueKee = " SELECT issues.kee, issues.author_login " + " FROM issues "
					+ " WHERE issues.project_uuid = \"" + projectUuid + "\"" + "   AND issues.issue_creation_date > "
					+ snapshotPeriodTimeSet.getLong("period1_date") + "   AND issues.issue_creation_date <  "
					+ snapshotPeriodTimeSet.getLong("build_date") + " AND issues.line > 0 ";

			StringBuilder sb = new StringBuilder(sqlGetPeriodIssueKee);
			if (!committers.isEmpty()) {
				sb.append(" AND issues.author_login IN (");
				StringBuilder committerList = new StringBuilder();
				for (String committer : committers) {
					if (committerList.toString().isEmpty()) {
						committerList.append("\"" + committer + "\"");
					} else {
						committerList.append(", \"" + committer + "\"");
					}
				}
				sb.append(committerList.toString());
				sb.append(");");
			}
			ResultSet issueSet = connStatement.executeQuery(sb.toString());
			LOGGER.info(sb.toString());

			Map<String, List<String>> issues = new HashMap<>();
			while (issueSet.next()) {
				String author = issueSet.getString("author_login");
				String issueKee = issueSet.getString("kee");
				if (issues.containsKey(author)) {
					issues.get(author).add(issueKee);
				} else {
					List<String> issueKees = new ArrayList<>();
					issueKees.add(issueKee);
					issues.put(author, issueKees);
				}
			}

			issueReportDto.setIssues(issues);

			// close result set
			snapshotPeriodTimeSet.close();
			issueSet.close();
		} catch (SQLException ex) {
			LOGGER.error(ex.getMessage());
		}
		return issueReportDto;
	}

	/**
	 * Connect db and get Kee (unique column) of new issue form Leak Period for
	 * committer
	 * 
	 * @param projectName
	 *            name of project
	 * @param projectUuid
	 *            unique id of project
	 * @return IssueReportDto
	 */
	public IssueReportDto getLeakPeriodNewIssueForUser(String projectName, String projectUuid, String committer,
			Set<String> revisions) {
		LOGGER.info("\nproject is : " + projectName + ", " + projectUuid);
		IssueReportDto issueReportDto = new IssueReportDto();
		try (Connection connection = MysqlDao.getInstance().getConnection();
				Statement connStatement = connection.createStatement()) {
			String sqlGetSnapshotPeriodTime = " SELECT snapshots.period1_date, " + "   snapshots.build_date, "
					+ "   snapshots.version " + " FROM snapshots " + " INNER JOIN projects "
					+ "   ON projects.uuid = snapshots.component_uuid " + " WHERE projects.project_uuid = \""
					+ projectUuid + "\"" + "   AND projects.name = \"" + projectName + "\" "
					+ "   AND snapshots.islast = 1; ";
			ResultSet snapshotPeriodTimeSet = connStatement.executeQuery(sqlGetSnapshotPeriodTime);
			LOGGER.info(sqlGetSnapshotPeriodTime);

			snapshotPeriodTimeSet.next();

			issueReportDto.setVersion(snapshotPeriodTimeSet.getString("version"));

			String sqlGetPeriodIssueKee = " SELECT issues.kee FROM issues " + " WHERE issues.project_uuid = \""
					+ projectUuid + "\"" + "   AND issues.issue_creation_date > "
					+ snapshotPeriodTimeSet.getLong("period1_date") + "   AND issues.issue_creation_date <  "
					+ snapshotPeriodTimeSet.getLong("build_date") + " AND issues.line > 0 "
					+ " AND issues.author_login  = \"" + committer + "\" ";

			StringBuilder sb = new StringBuilder(sqlGetPeriodIssueKee);
			if (!revisions.isEmpty()) {
				sb.append(
						" AND issues.component_uuid IN (SELECT file_sources.file_uuid FROM file_sources WHERE file_sources.revision IN (");
				StringBuilder revisionList = new StringBuilder();
				for (String revision : revisions) {
					if (revisionList.toString().isEmpty()) {
						revisionList.append("\"" + revision + "\"");
					} else {
						revisionList.append(", \"" + revision + "\"");
					}
				}
				sb.append(revisionList.toString());
				sb.append(") AND file_sources.project_uuid = \"" + projectUuid + "\");");
			}
			ResultSet issueSet = connStatement.executeQuery(sb.toString());
			LOGGER.info(sb.toString());
			Set<String> issueKees = new HashSet<>();
			while (issueSet.next()) {
				String issueKee = issueSet.getString("kee");
				issueKees.add(issueKee);
			}

			issueReportDto.setIssueKees(issueKees);

			// close result set
			snapshotPeriodTimeSet.close();
			issueSet.close();
		} catch (SQLException ex) {
			LOGGER.error(ex.getMessage());
		}
		return issueReportDto;
	}
}
