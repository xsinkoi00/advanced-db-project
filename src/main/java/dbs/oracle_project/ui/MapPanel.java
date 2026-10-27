package dbs.oracle_project.ui;

import dbs.oracle_project.dao.FacilityDAO;
import dbs.oracle_project.db.ConnectionManager;
import dbs.oracle_project.model.Facility;
import dbs.oracle_project.model.FacilityCategory;
import dbs.oracle_project.model.FacilityCategory.GeometryKind;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * The campus map. Draws facilities in a Cartesian 0..1000 world with Y pointing up,
 * supports pan (middle button), zoom (wheel), selection (left click), move (left drag
 * on a selected facility), and drawing (left click to lay vertices, right click or
 * double-click to finish).
 */
public class MapPanel extends JComponent {

  public enum Mode { SELECT, DRAW }

  private final FacilityDAO dao = new FacilityDAO();
  private final List<Facility> facilities = new ArrayList<>();

  private Mode mode = Mode.SELECT;
  private FacilityCategory drawCategory;
  private final List<double[]> drawVertices = new ArrayList<>();

  private Facility selected;
  private Consumer<Facility> selectionListener = f -> {};

  // view transform: screen = origin + world * scale, with Y flipped
  private double scale = 0.6;
  private double originX = 20;
  private double originY = 20;
  private boolean viewInitialised = false;

  // drag state
  private Point dragLast;
  private boolean dragMovingFeature;

  public MapPanel() {
    setOpaque(true);
    setBackground(new Color(248, 248, 244));
    setFocusable(true);
    setPreferredSize(new Dimension(900, 700));
    installMouseHandlers();
  }

  public void setSelectionListener(Consumer<Facility> l) {
    this.selectionListener = l == null ? f -> {} : l;
  }

  public Facility getSelected() {
    return selected;
  }

  public void setSelected(Facility f) {
    this.selected = f;
    selectionListener.accept(f);
    repaint();
  }

  public void enterSelectMode() {
    this.mode = Mode.SELECT;
    this.drawCategory = null;
    this.drawVertices.clear();
    repaint();
  }

  public void enterDrawMode(FacilityCategory cat) {
    this.mode = Mode.DRAW;
    this.drawCategory = cat;
    this.drawVertices.clear();
    setSelected(null);
    repaint();
  }

  /** Pull every facility from the DB and repaint. */
  public void reload() {
    facilities.clear();
    selected = null;
    try (Connection c = ConnectionManager.borrow()) {
      facilities.addAll(dao.listAll(c));
    } catch (SQLException ex) {
      showError("Failed to load facilities: " + ex.getMessage());
    }
    selectionListener.accept(null);
    repaint();
  }

  public void deleteSelected() {
    if (selected == null) return;
    int ans = JOptionPane.showConfirmDialog(this,
        "Delete " + selected.getCategory().label() + " \"" + selected.getName() + "\"?",
        "Confirm delete", JOptionPane.YES_NO_OPTION);
    if (ans != JOptionPane.YES_OPTION) return;
    try (Connection c = ConnectionManager.borrow()) {
      dao.delete(c, selected);
    } catch (SQLException ex) {
      showError("Delete failed: " + ex.getMessage());
      return;
    }
    reload();
  }

  // ------------------------------------------------------------------- painting

  @Override
  protected void paintComponent(Graphics g) {
    initViewIfNeeded();
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setColor(getBackground());
    g2.fillRect(0, 0, getWidth(), getHeight());

    drawGrid(g2);

    for (Facility f : facilities) {
      drawFacility(g2, f, f == selected);
    }

    drawInProgress(g2);
    drawLegend(g2);
    drawModeBanner(g2);
    g2.dispose();
  }

  private void drawGrid(Graphics2D g2) {
    g2.setColor(new Color(230, 230, 225));
    g2.setStroke(new BasicStroke(1f));
    for (int v = 0; v <= 1000; v += 100) {
      int sx = worldToScreenX(v);
      int sy = worldToScreenY(v);
      g2.drawLine(sx, worldToScreenY(0), sx, worldToScreenY(1000));
      g2.drawLine(worldToScreenX(0), sy, worldToScreenX(1000), sy);
    }
    g2.setColor(new Color(190, 190, 180));
    g2.drawRect(worldToScreenX(0), worldToScreenY(1000),
        (int) Math.round(1000 * scale), (int) Math.round(1000 * scale));
  }

  private void drawFacility(Graphics2D g2, Facility f, boolean highlight) {
    double[] c = f.getCoordinates();
    if (c.length < 4) return;
    FacilityCategory cat = f.getCategory();
    if (cat.geometryKind() == GeometryKind.POLYGON) {
      Polygon p = toScreenPolygon(c);
      g2.setColor(withAlpha(cat.drawColor(), highlight ? 220 : 170));
      g2.fillPolygon(p);
      g2.setColor(highlight ? Color.RED : cat.drawColor().darker());
      g2.setStroke(new BasicStroke(highlight ? 2.5f : 1.3f));
      g2.drawPolygon(p);
    } else {
      g2.setColor(highlight ? Color.RED : cat.drawColor());
      g2.setStroke(new BasicStroke(highlight ? 3.5f : 2.2f));
      for (int i = 0; i < c.length - 2; i += 2) {
        g2.drawLine(worldToScreenX(c[i]), worldToScreenY(c[i + 1]),
            worldToScreenX(c[i + 2]), worldToScreenY(c[i + 3]));
      }
    }
    // label at centroid
    double[] centre = centroid(c);
    g2.setColor(Color.DARK_GRAY);
    g2.setFont(getFont().deriveFont(Font.PLAIN, 11f));
    g2.drawString(f.getName(), worldToScreenX(centre[0]) + 4, worldToScreenY(centre[1]) - 4);
  }

  private void drawInProgress(Graphics2D g2) {
    if (mode != Mode.DRAW || drawVertices.isEmpty()) return;
    g2.setColor(new Color(200, 60, 60));
    g2.setStroke(new BasicStroke(2f));
    for (int i = 0; i < drawVertices.size() - 1; i++) {
      double[] a = drawVertices.get(i);
      double[] b = drawVertices.get(i + 1);
      g2.drawLine(worldToScreenX(a[0]), worldToScreenY(a[1]),
          worldToScreenX(b[0]), worldToScreenY(b[1]));
    }
    for (double[] v : drawVertices) {
      int sx = worldToScreenX(v[0]);
      int sy = worldToScreenY(v[1]);
      g2.fillOval(sx - 3, sy - 3, 7, 7);
    }
  }

  private void drawLegend(Graphics2D g2) {
    int x = 12, y = 14;
    g2.setFont(getFont().deriveFont(Font.PLAIN, 11f));
    for (FacilityCategory cat : FacilityCategory.values()) {
      g2.setColor(cat.drawColor());
      g2.fillRect(x, y, 14, 10);
      g2.setColor(Color.DARK_GRAY);
      g2.drawRect(x, y, 14, 10);
      g2.drawString(cat.label(), x + 20, y + 10);
      y += 16;
    }
  }

  private void drawModeBanner(Graphics2D g2) {
    String text = mode == Mode.SELECT
        ? "Mode: Select — left-click to pick, drag to move, Del to remove"
        : "Mode: Draw " + drawCategory.label()
            + " — left-click for vertices, right-click (or double-click) to finish, Esc to cancel";
    g2.setFont(getFont().deriveFont(Font.PLAIN, 11f));
    int w = g2.getFontMetrics().stringWidth(text) + 12;
    int h = 20;
    int x = getWidth() - w - 10, y = 10;
    g2.setColor(new Color(255, 255, 255, 200));
    g2.fillRoundRect(x, y, w, h, 8, 8);
    g2.setColor(Color.DARK_GRAY);
    g2.drawRoundRect(x, y, w, h, 8, 8);
    g2.drawString(text, x + 6, y + 14);
  }

  // ------------------------------------------------------------------- interactions

  private void installMouseHandlers() {
    MouseAdapter m = new MouseAdapter() {
      @Override public void mousePressed(MouseEvent e) {
        requestFocusInWindow();
        if (SwingUtilities.isMiddleMouseButton(e)) {
          dragLast = e.getPoint();
          dragMovingFeature = false;
          return;
        }
        if (SwingUtilities.isRightMouseButton(e) && mode == Mode.DRAW) {
          finishDrawing();
          return;
        }
        if (mode == Mode.DRAW && SwingUtilities.isLeftMouseButton(e)) {
          if (e.getClickCount() == 2) { finishDrawing(); return; }
          drawVertices.add(new double[] { screenToWorldX(e.getX()), screenToWorldY(e.getY()) });
          repaint();
          return;
        }
        if (mode == Mode.SELECT && SwingUtilities.isLeftMouseButton(e)) {
          Facility hit = pickAt(e.getX(), e.getY());
          setSelected(hit);
          if (hit != null) {
            dragLast = e.getPoint();
            dragMovingFeature = true;
          }
        }
      }
      @Override public void mouseReleased(MouseEvent e) {
        if (dragMovingFeature && selected != null) {
          try (Connection c = ConnectionManager.borrow()) {
            dao.save(c, selected);
          } catch (SQLException ex) {
            showError("Save failed, reloading: " + ex.getMessage());
            reload();
          }
        }
        dragLast = null;
        dragMovingFeature = false;
      }
      @Override public void mouseWheelMoved(MouseWheelEvent e) {
        double factor = e.getPreciseWheelRotation() < 0 ? 1.1 : 0.9;
        double wx = screenToWorldX(e.getX());
        double wy = screenToWorldY(e.getY());
        scale *= factor;
        // keep the point under the cursor stable
        originX = e.getX() - wx * scale;
        originY = e.getY() + wy * scale;
        repaint();
      }
    };
    MouseMotionAdapter mm = new MouseMotionAdapter() {
      @Override public void mouseDragged(MouseEvent e) {
        if (dragLast == null) return;
        if (dragMovingFeature && selected != null) {
          double dx = (e.getX() - dragLast.x) / scale;
          double dy = -(e.getY() - dragLast.y) / scale;
          translate(selected, dx, dy);
          repaint();
        } else {
          originX += e.getX() - dragLast.x;
          originY += e.getY() - dragLast.y;
          repaint();
        }
        dragLast = e.getPoint();
      }
    };
    addMouseListener(m);
    addMouseMotionListener(mm);
    addMouseWheelListener(m);

    // Esc cancels drawing; Delete removes selected
    getInputMap().put(javax.swing.KeyStroke.getKeyStroke("ESCAPE"), "cancel");
    getActionMap().put("cancel", new javax.swing.AbstractAction() {
      @Override public void actionPerformed(java.awt.event.ActionEvent e) {
        drawVertices.clear();
        enterSelectMode();
      }
    });
    getInputMap().put(javax.swing.KeyStroke.getKeyStroke("DELETE"), "delete");
    getActionMap().put("delete", new javax.swing.AbstractAction() {
      @Override public void actionPerformed(java.awt.event.ActionEvent e) {
        deleteSelected();
      }
    });
  }

  private void finishDrawing() {
    int needed = drawCategory.geometryKind() == GeometryKind.LINE ? 2 : 3;
    if (drawVertices.size() < needed) {
      showError("Need at least " + needed + " vertices.");
      return;
    }
    String name = JOptionPane.showInputDialog(this,
        "Name for the new " + drawCategory.label() + ":",
        "New " + drawCategory.label(),
        JOptionPane.PLAIN_MESSAGE);
    if (name == null || name.trim().isEmpty()) {
      drawVertices.clear();
      enterSelectMode();
      return;
    }
    double[] flat = new double[drawVertices.size() * 2];
    for (int i = 0; i < drawVertices.size(); i++) {
      flat[2 * i]     = drawVertices.get(i)[0];
      flat[2 * i + 1] = drawVertices.get(i)[1];
    }
    Facility nf = new Facility(drawCategory, name.trim(), flat);
    try (Connection c = ConnectionManager.borrow()) {
      dao.save(c, nf);
    } catch (SQLException ex) {
      showError("Insert failed: " + ex.getMessage());
      drawVertices.clear();
      enterSelectMode();
      return;
    }
    drawVertices.clear();
    enterSelectMode();
    reload();
  }

  private Facility pickAt(int sx, int sy) {
    // iterate top-down so the last-painted (likely small, on top) wins
    for (int i = facilities.size() - 1; i >= 0; i--) {
      Facility f = facilities.get(i);
      if (f.getCategory().geometryKind() == GeometryKind.POLYGON) {
        if (toScreenPolygon(f.getCoordinates()).contains(sx, sy)) return f;
      } else {
        double[] c = f.getCoordinates();
        for (int j = 0; j < c.length - 2; j += 2) {
          if (pointNearSegment(sx, sy,
              worldToScreenX(c[j]), worldToScreenY(c[j + 1]),
              worldToScreenX(c[j + 2]), worldToScreenY(c[j + 3]), 6.0)) return f;
        }
      }
    }
    return null;
  }

  private void translate(Facility f, double dx, double dy) {
    double[] c = f.getCoordinates();
    for (int i = 0; i < c.length; i += 2) {
      c[i]     += dx;
      c[i + 1] += dy;
    }
  }

  // ------------------------------------------------------------------- geometry helpers

  private Polygon toScreenPolygon(double[] c) {
    Polygon p = new Polygon();
    for (int i = 0; i < c.length; i += 2) {
      p.addPoint(worldToScreenX(c[i]), worldToScreenY(c[i + 1]));
    }
    return p;
  }

  private static double[] centroid(double[] c) {
    double sx = 0, sy = 0;
    int n = c.length / 2;
    for (int i = 0; i < c.length; i += 2) { sx += c[i]; sy += c[i + 1]; }
    return new double[] { sx / n, sy / n };
  }

  private static boolean pointNearSegment(int px, int py, int x1, int y1, int x2, int y2, double tol) {
    double vx = x2 - x1, vy = y2 - y1;
    double wx = px - x1, wy = py - y1;
    double len2 = vx * vx + vy * vy;
    if (len2 == 0) return Math.hypot(wx, wy) <= tol;
    double t = Math.max(0, Math.min(1, (wx * vx + wy * vy) / len2));
    double cx = x1 + t * vx, cy = y1 + t * vy;
    return Math.hypot(px - cx, py - cy) <= tol;
  }

  private static Color withAlpha(Color c, int alpha) {
    return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
  }

  private int worldToScreenX(double wx) { return (int) Math.round(originX + wx * scale); }
  private int worldToScreenY(double wy) { return (int) Math.round(originY - wy * scale); }
  private double screenToWorldX(int sx) { return (sx - originX) / scale; }
  private double screenToWorldY(int sy) { return (originY - sy) / scale; }

  private void initViewIfNeeded() {
    if (viewInitialised || getWidth() == 0 || getHeight() == 0) return;
    double sx = (getWidth() - 40) / 1000.0;
    double sy = (getHeight() - 60) / 1000.0;
    scale = Math.min(sx, sy);
    originX = 20;
    originY = getHeight() - 20;
    viewInitialised = true;
  }

  private void showError(String msg) {
    JOptionPane.showMessageDialog(this, msg, "CampusAtlas", JOptionPane.ERROR_MESSAGE);
  }
}
