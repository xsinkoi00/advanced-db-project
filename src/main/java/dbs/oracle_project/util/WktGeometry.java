package dbs.oracle_project.util;

import dbs.oracle_project.model.FacilityCategory.GeometryKind;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal WKT support for the two geometry shapes CampusAtlas uses:
 * {@code LINESTRING (x y, ...)} and {@code POLYGON ((x y, ..., x y))}.
 * Oracle serves these via {@code SDO_UTIL.TO_WKTGEOMETRY} and accepts them
 * via {@code SDO_UTIL.FROM_WKTGEOMETRY}, so this class is the only thing we
 * need to translate between SDO_GEOMETRY and Java coordinates.
 */
public final class WktGeometry {

  private WktGeometry() {}

  /** Parse the coordinates out of a POLYGON or LINESTRING WKT string. */
  public static double[] parse(String wkt) {
    if (wkt == null) throw new IllegalArgumentException("WKT is null");
    String upper = wkt.trim().toUpperCase();
    String body;
    if (upper.startsWith("POLYGON")) {
      int open = wkt.indexOf('(');
      int close = wkt.lastIndexOf(')');
      body = wkt.substring(open + 1, close);
      // strip the inner parentheses that wrap the ring
      body = body.replace("(", "").replace(")", "");
    } else if (upper.startsWith("LINESTRING")) {
      int open = wkt.indexOf('(');
      int close = wkt.lastIndexOf(')');
      body = wkt.substring(open + 1, close);
    } else {
      throw new IllegalArgumentException("Unsupported WKT: " + wkt);
    }
    List<Double> values = new ArrayList<>();
    for (String pair : body.split(",")) {
      String[] parts = pair.trim().split("\\s+");
      if (parts.length < 2) continue;
      values.add(Double.parseDouble(parts[0]));
      values.add(Double.parseDouble(parts[1]));
    }
    double[] out = new double[values.size()];
    for (int i = 0; i < out.length; i++) out[i] = values.get(i);
    return out;
  }

  /** Build a WKT string from the flat coordinate array. */
  public static String format(GeometryKind kind, double[] coords) {
    if (coords == null || coords.length < 4 || coords.length % 2 != 0) {
      throw new IllegalArgumentException("Need an even number of coordinates, at least 2 points");
    }
    StringBuilder points = new StringBuilder();
    for (int i = 0; i < coords.length; i += 2) {
      if (i > 0) points.append(", ");
      points.append(stripZero(coords[i])).append(' ').append(stripZero(coords[i + 1]));
    }
    switch (kind) {
      case LINE:    return "LINESTRING (" + points + ")";
      case POLYGON: return "POLYGON ((" + points + "))";
      default: throw new IllegalStateException("Unknown kind: " + kind);
    }
  }

  private static String stripZero(double v) {
    if (v == Math.floor(v) && !Double.isInfinite(v)) {
      return Long.toString((long) v);
    }
    return Double.toString(v);
  }

  /** Make sure a polygon ring ends where it began. No-op for a line. */
  public static double[] closeRingIfNeeded(GeometryKind kind, double[] coords) {
    if (kind != GeometryKind.POLYGON || coords.length < 4) return coords;
    if (coords[0] == coords[coords.length - 2] && coords[1] == coords[coords.length - 1]) {
      return coords;
    }
    double[] out = new double[coords.length + 2];
    System.arraycopy(coords, 0, out, 0, coords.length);
    out[coords.length] = coords[0];
    out[coords.length + 1] = coords[1];
    return out;
  }
}
