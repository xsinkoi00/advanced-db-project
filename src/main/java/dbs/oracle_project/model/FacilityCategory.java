package dbs.oracle_project.model;

import java.awt.Color;

/**
 * Enumeration of the five facility categories. Carries its table name, the
 * kind of geometry it stores, and the colour used to draw it on the map.
 */
public enum FacilityCategory {
  BUILDING(     "BUILDING",     "building",     GeometryKind.POLYGON, new Color(120, 120, 140), "Building"),
  PARKING_AREA( "PARKING_AREA", "parking_area", GeometryKind.POLYGON, new Color(200, 180, 100), "Parking area"),
  WALKWAY(      "WALKWAY",      "walkway",      GeometryKind.LINE,    new Color( 90,  90,  90), "Walkway"),
  GREEN_SPACE(  "GREEN_SPACE",  "green_space",  GeometryKind.POLYGON, new Color(144, 200, 144), "Green space"),
  WATER_AREA(   "WATER_AREA",   "water_area",   GeometryKind.POLYGON, new Color(135, 170, 210), "Water area");

  public enum GeometryKind { POLYGON, LINE }

  private final String code;
  private final String tableName;
  private final GeometryKind geometryKind;
  private final Color drawColor;
  private final String label;

  FacilityCategory(String code, String tableName, GeometryKind geometryKind, Color drawColor, String label) {
    this.code = code;
    this.tableName = tableName;
    this.geometryKind = geometryKind;
    this.drawColor = drawColor;
    this.label = label;
  }

  public String code() { return code; }
  public String tableName() { return tableName; }
  public GeometryKind geometryKind() { return geometryKind; }
  public Color drawColor() { return drawColor; }
  public String label() { return label; }

  public static FacilityCategory fromCode(String code) {
    for (FacilityCategory f : values()) {
      if (f.code.equals(code)) return f;
    }
    throw new IllegalArgumentException("Unknown facility category code: " + code);
  }
}
