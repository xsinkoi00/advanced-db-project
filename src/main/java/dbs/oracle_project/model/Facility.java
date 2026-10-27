package dbs.oracle_project.model;

/**
 * In-memory representation of a facility row. Geometry is kept as a flat
 * array of coordinates ({@code x0, y0, x1, y1, ...}) because that is what the
 * drawing code and the WKT formatter both want. A polygon's ring is expected
 * to be closed (first point repeated at the end).
 */
public final class Facility {

  private Long id; // null for a facility not yet inserted
  private final FacilityCategory category;
  private String name;
  private double[] coordinates;

  public Facility(FacilityCategory category, String name, double[] coordinates) {
    this(null, category, name, coordinates);
  }

  public Facility(Long id, FacilityCategory category, String name, double[] coordinates) {
    this.id = id;
    this.category = category;
    this.name = name;
    this.coordinates = coordinates;
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public FacilityCategory getCategory() { return category; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public double[] getCoordinates() { return coordinates; }
  public void setCoordinates(double[] coordinates) { this.coordinates = coordinates; }

  @Override
  public String toString() {
    return category.label() + " #" + id + " " + name;
  }
}
