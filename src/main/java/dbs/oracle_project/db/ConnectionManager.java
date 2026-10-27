package dbs.oracle_project.db;

import java.sql.Connection;
import java.sql.SQLException;
import oracle.jdbc.pool.OracleDataSource;

/**
 * Holds the OracleDataSource for the live session. Set once by the login
 * dialog, then any UI panel can borrow a Connection via {@link #borrow()}.
 */
public final class ConnectionManager {

  private static OracleDataSource dataSource;
  private static String displayName;

  private ConnectionManager() {}

  /** Build the datasource and prove it works by opening and closing one connection. */
  public static void configure(
      String host, int port, String service, String user, String password) throws SQLException {
    OracleDataSource ds = new OracleDataSource();
    ds.setURL("jdbc:oracle:thin:@//" + host + ":" + port + "/" + service);
    ds.setUser(user);
    ds.setPassword(password);
    try (Connection c = ds.getConnection()) {
      if (!c.isValid(5)) {
        throw new SQLException("Connection is not valid after open");
      }
    }
    dataSource = ds;
    displayName = user + "@" + host + ":" + port + "/" + service;
  }

  public static Connection borrow() throws SQLException {
    if (dataSource == null) {
      throw new IllegalStateException("Not connected. Open File > Connect first.");
    }
    return dataSource.getConnection();
  }

  public static boolean isConfigured() {
    return dataSource != null;
  }

  public static String displayName() {
    return displayName == null ? "<not connected>" : displayName;
  }
}
