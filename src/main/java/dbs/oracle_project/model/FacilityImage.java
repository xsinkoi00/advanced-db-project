package dbs.oracle_project.model;

import java.sql.Timestamp;

/**
 * Metadata about a stored image. The actual bytes travel separately so this
 * stays lightweight for lists and combo boxes.
 */
public final class FacilityImage {

  private final long id;
  private final FacilityCategory category;
  private final long facilityId;
  private String caption;
  private final Timestamp uploadedAt;

  public FacilityImage(long id, FacilityCategory category, long facilityId,
                       String caption, Timestamp uploadedAt) {
    this.id = id;
    this.category = category;
    this.facilityId = facilityId;
    this.caption = caption;
    this.uploadedAt = uploadedAt;
  }

  public long getId() { return id; }
  public FacilityCategory getCategory() { return category; }
  public long getFacilityId() { return facilityId; }
  public String getCaption() { return caption; }
  public void setCaption(String caption) { this.caption = caption; }
  public Timestamp getUploadedAt() { return uploadedAt; }

  @Override
  public String toString() {
    return "image #" + id + (caption == null || caption.isEmpty() ? "" : " — " + caption);
  }
}
