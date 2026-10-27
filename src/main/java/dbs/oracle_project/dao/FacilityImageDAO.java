package dbs.oracle_project.dao;

import dbs.oracle_project.model.Facility;
import dbs.oracle_project.model.FacilityCategory;
import dbs.oracle_project.model.FacilityImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import oracle.jdbc.OraclePreparedStatement;
import oracle.jdbc.OracleResultSet;
import oracle.ord.im.OrdImage;

/**
 * CRUD and similarity search for the {@code facility_image} table.
 *
 * <p>Upload flow:
 * <ol>
 *   <li>Insert the row with {@code ORDSYS.ORDImage.init()} to obtain an id;</li>
 *   <li>{@code SELECT ... FOR UPDATE} to get back the editable OrdImage object;</li>
 *   <li>Load the bytes into it, call {@code setProperties()}, UPDATE;</li>
 *   <li>Rebuild {@code image_si = SI_StillImage(image.getContent())};</li>
 *   <li>Rebuild the four feature columns {@code SI_AverageColor} …
 *       {@code SI_Texture}.</li>
 * </ol>
 * Rotation and replace follow the same flow starting at step 2.
 *
 * <p>Similarity search uses {@code SI_ScoreByFtrList} with user-chosen weights.
 */
public class FacilityImageDAO {

  // -------------------------------------------------------- read

  /** Metadata rows for every image attached to the given facility. */
  public List<FacilityImage> listForFacility(Connection c, Facility f) throws SQLException {
    String sql =
        "SELECT id, caption, uploaded_at FROM facility_image "
            + "WHERE category = ? AND facility_id = ? ORDER BY uploaded_at DESC, id DESC";
    try (PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, f.getCategory().code());
      ps.setLong(2, f.getId());
      try (ResultSet rs = ps.executeQuery()) {
        List<FacilityImage> out = new ArrayList<>();
        while (rs.next()) {
          out.add(new FacilityImage(
              rs.getLong("id"),
              f.getCategory(),
              f.getId(),
              rs.getString("caption"),
              rs.getTimestamp("uploaded_at")));
        }
        return out;
      }
    }
  }

  /** Full image bytes for one row. */
  public byte[] loadBytes(Connection c, long imageId) throws SQLException, IOException {
    String sql = "SELECT image FROM facility_image WHERE id = ?";
    try (PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setLong(1, imageId);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) throw new SQLException("Image " + imageId + " not found");
        OracleResultSet ors = (OracleResultSet) rs;
        OrdImage ordImage = (OrdImage) ors.getORAData(1, OrdImage.getORADataFactory());
        if (ordImage == null) throw new SQLException("Image " + imageId + " has no content");
        return ordImage.getDataInByteArray();
      }
    }
  }

  // -------------------------------------------------------- write

  /** Insert a new image for a facility and return its id. */
  public long upload(Connection c, Facility f, String caption, byte[] bytes)
      throws SQLException, IOException {
    boolean previousAutoCommit = c.getAutoCommit();
    c.setAutoCommit(false);
    try {
      long newId = insertEmpty(c, f, caption);
      loadBytesIntoRow(c, newId, bytes);
      refreshFeatures(c, newId);
      c.commit();
      return newId;
    } catch (SQLException | IOException ex) {
      c.rollback();
      throw ex;
    } finally {
      c.setAutoCommit(previousAutoCommit);
    }
  }

  /** Replace the bytes of an existing image row and re-run feature extraction. */
  public void replace(Connection c, long imageId, byte[] bytes)
      throws SQLException, IOException {
    boolean previousAutoCommit = c.getAutoCommit();
    c.setAutoCommit(false);
    try {
      loadBytesIntoRow(c, imageId, bytes);
      refreshFeatures(c, imageId);
      c.commit();
    } catch (SQLException | IOException ex) {
      c.rollback();
      throw ex;
    } finally {
      c.setAutoCommit(previousAutoCommit);
    }
  }

  public void delete(Connection c, long imageId) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement("DELETE FROM facility_image WHERE id = ?")) {
      ps.setLong(1, imageId);
      ps.executeUpdate();
    }
  }

  // -------------------------------------------------------- similarity

  /**
   * Content-based image search. Weights sum to 1.0; smaller returned score
   * means more similar.
   */
  public List<Scored> findSimilar(Connection c, long queryImageId,
                                  double wAC, double wCH, double wPC, double wTX) throws SQLException {
    String sql =
        "SELECT dst.id, dst.category, dst.facility_id, dst.caption, "
            + "SI_ScoreByFtrList("
            + "  new SI_FeatureList(src.image_ac, ?, src.image_ch, ?, src.image_pc, ?, src.image_tx, ?), "
            + "  dst.image_si) AS similarity "
            + "FROM facility_image src, facility_image dst "
            + "WHERE src.id = ? "
            + "  AND dst.id <> src.id "
            + "  AND dst.image_si IS NOT NULL "
            + "ORDER BY similarity ASC";
    try (PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setDouble(1, wAC);
      ps.setDouble(2, wCH);
      ps.setDouble(3, wPC);
      ps.setDouble(4, wTX);
      ps.setLong(5, queryImageId);
      try (ResultSet rs = ps.executeQuery()) {
        List<Scored> out = new ArrayList<>();
        while (rs.next()) {
          out.add(new Scored(
              rs.getLong("id"),
              FacilityCategory.fromCode(rs.getString("category")),
              rs.getLong("facility_id"),
              rs.getString("caption"),
              rs.getDouble("similarity")));
        }
        return out;
      }
    }
  }

  // -------------------------------------------------------- helpers

  private long insertEmpty(Connection c, Facility f, String caption) throws SQLException {
    String sql =
        "INSERT INTO facility_image (category, facility_id, caption, image) "
            + "VALUES (?, ?, ?, ORDSYS.ORDImage.init())";
    try (PreparedStatement ps = c.prepareStatement(sql, new String[] {"id"})) {
      ps.setString(1, f.getCategory().code());
      ps.setLong(2, f.getId());
      ps.setString(3, caption == null ? "" : caption);
      ps.executeUpdate();
      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (!keys.next()) throw new SQLException("No id returned from image insert");
        return keys.getLong(1);
      }
    }
  }

  /** SELECT ... FOR UPDATE, load bytes, setProperties, UPDATE. */
  private void loadBytesIntoRow(Connection c, long imageId, byte[] bytes)
      throws SQLException, IOException {
    OrdImage ordImage;
    try (PreparedStatement sel =
        c.prepareStatement("SELECT image FROM facility_image WHERE id = ? FOR UPDATE")) {
      sel.setLong(1, imageId);
      try (ResultSet rs = sel.executeQuery()) {
        if (!rs.next()) throw new SQLException("Image " + imageId + " not found");
        OracleResultSet ors = (OracleResultSet) rs;
        ordImage = (OrdImage) ors.getORAData(1, OrdImage.getORADataFactory());
      }
    }
    try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
      ordImage.loadDataFromInputStream(in);
    }
    ordImage.setProperties();
    try (PreparedStatement upd =
        c.prepareStatement("UPDATE facility_image SET image = ? WHERE id = ?")) {
      OraclePreparedStatement ops = (OraclePreparedStatement) upd;
      ops.setORAData(1, ordImage);
      upd.setLong(2, imageId);
      upd.executeUpdate();
    }
  }

  /** Rebuild SI_StillImage, then the four feature columns, from the current image bytes. */
  private void refreshFeatures(Connection c, long imageId) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement(
        "UPDATE facility_image p SET p.image_si = SI_StillImage(p.image.getContent()) WHERE p.id = ?")) {
      ps.setLong(1, imageId);
      ps.executeUpdate();
    }
    try (PreparedStatement ps = c.prepareStatement(
        "UPDATE facility_image SET "
            + "image_ac = SI_AverageColor(image_si), "
            + "image_ch = SI_ColorHistogram(image_si), "
            + "image_pc = SI_PositionalColor(image_si), "
            + "image_tx = SI_Texture(image_si) "
            + "WHERE id = ?")) {
      ps.setLong(1, imageId);
      ps.executeUpdate();
    }
  }

  /** One row of the similarity-search result set. */
  public static final class Scored {
    public final long id;
    public final FacilityCategory category;
    public final long facilityId;
    public final String caption;
    public final double similarity;

    public Scored(long id, FacilityCategory category, long facilityId, String caption, double similarity) {
      this.id = id;
      this.category = category;
      this.facilityId = facilityId;
      this.caption = caption;
      this.similarity = similarity;
    }
  }
}
