package dbs.oracle_project.dao;

import dbs.oracle_project.model.Facility;
import dbs.oracle_project.model.FacilityCategory;
import dbs.oracle_project.model.FacilityCategory.GeometryKind;
import dbs.oracle_project.util.WktGeometry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * The spatial-query half of the course requirement:
 * <ul>
 *   <li>1 non-trivial operator — {@link #relateAnyInteract} using {@code SDO_RELATE};</li>
 *   <li>2 further operators — {@link #withinDistance} using {@code SDO_WITHIN_DISTANCE},
 *       {@link #nearestNeighbours} using {@code SDO_NN};</li>
 *   <li>3 analytic functions — {@link #area}, {@link #length}, {@link #distance}
 *       using {@code SDO_GEOM.SDO_AREA/SDO_LENGTH/SDO_DISTANCE};</li>
 *   <li>a SQL analytic function — {@link #rankByArea} using {@code RANK() OVER (...)}
 *       as insurance against a strict reading of "analytic function".</li>
 * </ul>
 * All functions accept a reference geometry as WKT and bind it through
 * {@code SDO_UTIL.FROM_WKTGEOMETRY}, so we never ship an SDO_GEOMETRY struct
 * from Java.
 */
public class SpatialQueryDAO {

  /** Tolerance for SDO_GEOM.* calls, in the geometry metadata's units. */
  private static final double TOL = 0.005;

  // ---------------------------------------------------------- SDO_RELATE

  /** Facilities (in any category) whose geometry has any interaction with ref. */
  public List<Facility> relateAnyInteract(Connection c, Facility ref) throws SQLException {
    String wkt = WktGeometry.format(ref.getCategory().geometryKind(), ref.getCoordinates());
    List<Facility> out = new ArrayList<>();
    for (FacilityCategory cat : FacilityCategory.values()) {
      String sql =
          "SELECT id, name, SDO_UTIL.TO_WKTGEOMETRY(geom) AS wkt "
              + "FROM " + cat.tableName()
              + " WHERE SDO_RELATE(geom, SDO_UTIL.FROM_WKTGEOMETRY(?), 'mask=ANYINTERACT') = 'TRUE'";
      try (PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setString(1, wkt);
        try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            long id = rs.getLong("id");
            if (sameFacility(cat, id, ref)) continue;
            out.add(new Facility(id, cat, rs.getString("name"),
                WktGeometry.parse(rs.getString("wkt"))));
          }
        }
      }
    }
    return out;
  }

  // ---------------------------------------------------------- SDO_WITHIN_DISTANCE

  /** Facilities whose geometry is within {@code distance} units of ref. */
  public List<Facility> withinDistance(Connection c, Facility ref, double distance) throws SQLException {
    String wkt = WktGeometry.format(ref.getCategory().geometryKind(), ref.getCoordinates());
    String params = "distance=" + distance;
    List<Facility> out = new ArrayList<>();
    for (FacilityCategory cat : FacilityCategory.values()) {
      String sql =
          "SELECT id, name, SDO_UTIL.TO_WKTGEOMETRY(geom) AS wkt "
              + "FROM " + cat.tableName()
              + " WHERE SDO_WITHIN_DISTANCE(geom, SDO_UTIL.FROM_WKTGEOMETRY(?), ?) = 'TRUE'";
      try (PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setString(1, wkt);
        ps.setString(2, params);
        try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            long id = rs.getLong("id");
            if (sameFacility(cat, id, ref)) continue;
            out.add(new Facility(id, cat, rs.getString("name"),
                WktGeometry.parse(rs.getString("wkt"))));
          }
        }
      }
    }
    return out;
  }

  // ---------------------------------------------------------- SDO_NN

  /**
   * The {@code n} facilities in {@code target} closest to {@code ref}, ordered
   * by increasing distance.
   */
  public List<Facility> nearestNeighbours(Connection c, Facility ref, FacilityCategory target, int n)
      throws SQLException {
    String wkt = WktGeometry.format(ref.getCategory().geometryKind(), ref.getCoordinates());
    // Over-request by 1 so we can safely drop ref if it appears in its own category.
    String params = "sdo_num_res=" + (n + 1);
    String sql =
        "SELECT id, name, SDO_UTIL.TO_WKTGEOMETRY(geom) AS wkt "
            + "FROM " + target.tableName()
            + " WHERE SDO_NN(geom, SDO_UTIL.FROM_WKTGEOMETRY(?), ?, 1) = 'TRUE' "
            + "ORDER BY SDO_NN_DISTANCE(1)";
    List<Facility> out = new ArrayList<>();
    try (PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, wkt);
      ps.setString(2, params);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next() && out.size() < n) {
          long id = rs.getLong("id");
          if (sameFacility(target, id, ref)) continue;
          out.add(new Facility(id, target, rs.getString("name"),
              WktGeometry.parse(rs.getString("wkt"))));
        }
      }
    }
    return out;
  }

  // ---------------------------------------------------------- SDO_GEOM.*

  public double area(Connection c, Facility f) throws SQLException {
    if (f.getCategory().geometryKind() != GeometryKind.POLYGON) {
      throw new IllegalArgumentException("Area is only defined for polygon facilities");
    }
    return scalar(c, "SELECT SDO_GEOM.SDO_AREA(geom, " + TOL + ") FROM "
        + f.getCategory().tableName() + " WHERE id = ?", f.getId());
  }

  public double length(Connection c, Facility f) throws SQLException {
    if (f.getCategory().geometryKind() != GeometryKind.LINE) {
      throw new IllegalArgumentException("Length is only defined for line facilities");
    }
    return scalar(c, "SELECT SDO_GEOM.SDO_LENGTH(geom, " + TOL + ") FROM "
        + f.getCategory().tableName() + " WHERE id = ?", f.getId());
  }

  public double distance(Connection c, Facility a, Facility b) throws SQLException {
    String sql =
        "SELECT SDO_GEOM.SDO_DISTANCE(a.geom, b.geom, " + TOL + ") "
            + "FROM " + a.getCategory().tableName() + " a, " + b.getCategory().tableName() + " b "
            + "WHERE a.id = ? AND b.id = ?";
    try (PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setLong(1, a.getId());
      ps.setLong(2, b.getId());
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return rs.getDouble(1);
      }
    }
    throw new SQLException("No distance returned");
  }

  // ---------------------------------------------------------- RANK() OVER

  public List<Ranked> rankByArea(Connection c, FacilityCategory cat) throws SQLException {
    if (cat.geometryKind() != GeometryKind.POLYGON) {
      throw new IllegalArgumentException("Rank by area only applies to polygon categories");
    }
    String sql =
        "SELECT id, name, SDO_GEOM.SDO_AREA(geom, " + TOL + ") AS area_m2, "
            + "RANK() OVER (ORDER BY SDO_GEOM.SDO_AREA(geom, " + TOL + ") DESC) AS rk "
            + "FROM " + cat.tableName();
    List<Ranked> out = new ArrayList<>();
    try (PreparedStatement ps = c.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        out.add(new Ranked(rs.getLong("id"), rs.getString("name"),
            rs.getDouble("area_m2"), rs.getInt("rk")));
      }
    }
    out.sort((x, y) -> Integer.compare(x.rank, y.rank));
    return out;
  }

  // ---------------------------------------------------------- helpers

  private static boolean sameFacility(FacilityCategory cat, long id, Facility ref) {
    return ref.getId() != null && cat == ref.getCategory() && id == ref.getId();
  }

  private static double scalar(Connection c, String sql, long id) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return rs.getDouble(1);
      }
    }
    throw new SQLException("No scalar returned");
  }

  /** Row returned from {@link #rankByArea}. */
  public static final class Ranked {
    public final long id;
    public final String name;
    public final double area;
    public final int rank;

    public Ranked(long id, String name, double area, int rank) {
      this.id = id;
      this.name = name;
      this.area = area;
      this.rank = rank;
    }
  }
}
