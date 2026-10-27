package dbs.oracle_project.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the demonstration sample data (DELETE + INSERT statements only, no
 * DDL) from a classpath resource. The DDL in {@code schema/schema.sql} is run
 * once by the DBA during setup; the Java app only reinitialises data.
 */
public final class Schema {

  private static final String SAMPLE_RESOURCE = "/data/sample_facilities.sql";

  private Schema() {}

  /** Replace all demonstration data with a fresh copy. One transaction. */
  public static void reinitSampleData(Connection c) throws SQLException, IOException {
    List<String> statements = loadStatements(SAMPLE_RESOURCE);
    boolean previousAutoCommit = c.getAutoCommit();
    c.setAutoCommit(false);
    try (Statement s = c.createStatement()) {
      for (String sql : statements) {
        s.execute(sql);
      }
      c.commit();
    } catch (SQLException ex) {
      c.rollback();
      throw ex;
    } finally {
      c.setAutoCommit(previousAutoCommit);
    }
  }

  /**
   * Read a .sql resource, strip {@code --} comment lines, and split on
   * semicolons. Each resulting non-blank chunk is one JDBC statement.
   */
  private static List<String> loadStatements(String resource) throws IOException {
    StringBuilder buf = new StringBuilder();
    try (InputStream in = Schema.class.getResourceAsStream(resource)) {
      if (in == null) {
        throw new IOException("Resource not found on classpath: " + resource);
      }
      try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
        String line;
        while ((line = r.readLine()) != null) {
          if (line.trim().startsWith("--")) continue;
          buf.append(line).append('\n');
        }
      }
    }
    List<String> out = new ArrayList<>();
    for (String chunk : buf.toString().split(";")) {
      String trimmed = chunk.trim();
      if (!trimmed.isEmpty()) out.add(trimmed);
    }
    return out;
  }
}
