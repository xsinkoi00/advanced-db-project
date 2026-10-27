package dbs.oracle_project.dao;

import dbs.oracle_project.model.Facility;
import dbs.oracle_project.model.FacilityCategory;
import dbs.oracle_project.util.WktGeometry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes facility rows across all five category tables, dispatching
 * on {@link FacilityCategory#tableName()}. Geometry travels as a WKT string via
 * {@code SDO_UTIL.TO_WKTGEOMETRY} / {@code SDO_UTIL.FROM_WKTGEOMETRY}.
 */
public class FacilityDAO {

  /** All facilities across all five tables, each row carrying its category. */
  public List<Facility> listAll(Connection c) throws SQLException {
    List<Facility> out = new ArrayList<>();
    for (FacilityCategory cat : FacilityCategory.values()) {
      out.addAll(listByCategory(c, cat));
    }
    return out;
  }

  public List<Facility> listByCategory(Connection c, FacilityCategory cat) throws SQLException {
    String sql =
        "SELECT id, name, SDO_UTIL.TO_WKTGEOMETRY(geom) AS wkt FROM " + cat.tableName();
    List<Facility> rows = new ArrayList<>();
    try (PreparedStatement ps = c.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        long id = rs.getLong("id");
        String name = rs.getString("name");
        String wkt = rs.getString("wkt");
        double[] coords = WktGeometry.parse(wkt);
        rows.add(new Facility(id, cat, name, coords));
      }
    }
    return rows;
  }

  /** Insert if {@link Facility#getId()} is null, otherwise update. */
  public void save(Connection c, Facility f) throws SQLException {
    double[] coords = WktGeometry.closeRingIfNeeded(f.getCategory().geometryKind(), f.getCoordinates());
    f.setCoordinates(coords);
    String wkt = WktGeometry.format(f.getCategory().geometryKind(), coords);
    if (f.getId() == null) {
      insert(c, f, wkt);
    } else {
      update(c, f, wkt);
    }
  }

  private void insert(Connection c, Facility f, String wkt) throws SQLException {
    String sql =
        "INSERT INTO " + f.getCategory().tableName()
            + " (name, geom) VALUES (?, SDO_UTIL.FROM_WKTGEOMETRY(?))";
    try (PreparedStatement ps = c.prepareStatement(sql, new String[] {"id"})) {
      ps.setString(1, f.getName());
      ps.setString(2, wkt);
      ps.executeUpdate();
      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (keys.next()) f.setId(keys.getLong(1));
      }
    }
  }

  private void update(Connection c, Facility f, String wkt) throws SQLException {
    String sql =
        "UPDATE " + f.getCategory().tableName()
            + " SET name = ?, geom = SDO_UTIL.FROM_WKTGEOMETRY(?) WHERE id = ?";
    try (PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, f.getName());
      ps.setString(2, wkt);
      ps.setLong(3, f.getId());
      ps.executeUpdate();
    }
  }

  public void delete(Connection c, Facility f) throws SQLException {
    if (f.getId() == null) return;
    String sql = "DELETE FROM " + f.getCategory().tableName() + " WHERE id = ?";
    try (PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setLong(1, f.getId());
      ps.executeUpdate();
    }
  }
}
