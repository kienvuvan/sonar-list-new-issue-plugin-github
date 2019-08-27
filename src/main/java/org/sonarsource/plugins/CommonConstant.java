package org.sonarsource.plugins;

public class CommonConstant {
	private CommonConstant() {
		throw new IllegalStateException("Common Constant");
	}

	// Parameters for mailing
	// public static final String SMTP_HOST = "mail.luvina.net";
	// public static final String MAIL_FROM = "noreply@luvina.net";
	// public static final List<String> MAIL_TO =
	// Collections.unmodifiableList(Arrays.asList("vuvankien@luvina.net"));

	// Eg : localhost
	// public static final String SONAR_QUBE_SERVER = "http://localhost:9000/";

	public static final String SONAR_QUBE_SERVER = "http://192.168.0.235:9000/sonar/";

	// Parameter sonar mysql
	public static final String SONAR_QUBE_JDBC_URL = "jdbc:mysql://localhost:3306/sonar";
	public static final String SONAR_QUBE_JDBC_USERNAME = "sonar";
	public static final String SONAR_QUBE_JDBC_PASS = "nam0687";

	// Root folder Jenkins build jobs
	public static final String ROOT_JENKINS_BUILD = "C:\\Program Files (x86)\\Jenkins\\jobs\\";

//	public static final String ROOT_JENKINS_BUILD = "C:\\Windows\\System32\\config\\systemprofile\\.jenkins\\jobs\\";

	public static final String URI_REDMINE = "http://192.168.0.136/redmine/";
	public static final String API_ACCESS_KEY = "7555f0adb09521517b42402e7ed4ce300d24cd4d";

	// Parameters customer in redmine
	public static final int EXPECTED_RELEASE_C1S = 7; // 7 Days
	public static final String TEAM = "Team Kỹ Thuật"; // Eg: Team Bảo Trì
	public static final String C1S = "Bug"; // Length <= 6 character
	public static final String ORIGINAL_PRIORITY = "4 : Trung bình";
	/*
	 * Eg: 1 : Ngay lập tức 2 : Gấp 3 : Cao 4 : Trung bình 5 : Thấp
	 */

	public static final String END_MAIL = "@luvina.net";
}
