package org.sonarsource.plugins;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class MysqlDao {
	private static MysqlDao instance;
	private Connection connection;
	private static final Logger LOGGER = Loggers.get(MysqlDao.class);

	private MysqlDao() throws SQLException {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			this.connection = DriverManager.getConnection(CommonConstant.SONAR_QUBE_JDBC_URL,
					CommonConstant.SONAR_QUBE_JDBC_USERNAME, CommonConstant.SONAR_QUBE_JDBC_PASS);
		} catch (ClassNotFoundException ex) {
			LOGGER.info("Database Connection Creation Failed : " + ex.getMessage());
		}
	}

	public Connection getConnection() {
		return connection;
	}

	public static MysqlDao getInstance() throws SQLException {
		if (instance == null) {
			instance = new MysqlDao();
		} else if (instance.getConnection().isClosed()) {
			instance = new MysqlDao();
		}

		return instance;
	}
}
