package dbs.oracle_project.ui;

import dbs.oracle_project.dao.SpatialQueryDAO;
import dbs.oracle_project.db.ConnectionManager;
import dbs.oracle_project.model.Facility;
import dbs.oracle_project.model.FacilityCategory;
import dbs.oracle_project.model.FacilityCategory.GeometryKind;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Non-modal panel that drives {@link SpatialQueryDAO}. The query type combo
 * swaps the parameter panel via CardLayout; results print to a text area and
 * highlight on the main {@link MapPanel}.
 */
public class SpatialQueryDialog extends JDialog {

  private enum QueryType {
    RELATE("Interacting facilities (SDO_RELATE)"),
    WITHIN("Within distance (SDO_WITHIN_DISTANCE)"),
    NN("Nearest neighbours (SDO_NN)"),
    AREA("Area (SDO_GEOM.SDO_AREA)"),
    LENGTH("Length (SDO_GEOM.SDO_LENGTH)"),
    DISTANCE("Distance between two facilities (SDO_GEOM.SDO_DISTANCE)"),
    RANK("Rank by area (RANK() OVER)");
    final String label;
    QueryType(String l) { this.label = l; }
    @Override public String toString() { return label; }
  }

  private final MapPanel mapPanel;
  private final SpatialQueryDAO dao = new SpatialQueryDAO();

  private final JComboBox<QueryType> typeCombo = new JComboBox<>(QueryType.values());
  private final CardLayout paramCards = new CardLayout();
  private final JPanel paramPanel = new JPanel(paramCards);

  private final JTextField distanceField = new JTextField("200", 8);
  private final JTextField nField = new JTextField("3", 4);
  private final JComboBox<FacilityCategory> nnTargetCombo = new JComboBox<>(FacilityCategory.values());
  private final JComboBox<Facility> secondFacilityCombo = new JComboBox<>();
  private final JComboBox<FacilityCategory> rankCategoryCombo = new JComboBox<>();

  private final JTextArea resultArea = new JTextArea(10, 40);

  public SpatialQueryDialog(Frame owner, MapPanel mapPanel) {
    super(owner, "CampusAtlas — Spatial queries", false);
    this.mapPanel = mapPanel;
    buildUi();
    setSize(560, 480);
    setLocationRelativeTo(owner);
  }

  /** Refresh combo contents from the current MapPanel facility list. */
  public void refreshFacilityCombos() {
    List<Facility> all = mapPanel.getFacilitiesSnapshot();
    Collections.sort(all, (a, b) -> a.getCategory().compareTo(b.getCategory()));
    secondFacilityCombo.setModel(new DefaultComboBoxModel<>(all.toArray(new Facility[0])));
    // only polygon categories make sense for "Rank by area"
    List<FacilityCategory> polygonCats = new ArrayList<>();
    for (FacilityCategory c : FacilityCategory.values()) {
      if (c.geometryKind() == GeometryKind.POLYGON) polygonCats.add(c);
    }
    rankCategoryCombo.setModel(new DefaultComboBoxModel<>(polygonCats.toArray(new FacilityCategory[0])));
  }

  private void buildUi() {
    setLayout(new BorderLayout(8, 8));
    JPanel top = new JPanel(new BorderLayout(8, 8));
    top.setBorder(BorderFactory.createEmptyBorder(10, 12, 4, 12));

    JPanel typeRow = new JPanel(new BorderLayout(6, 0));
    typeRow.add(new JLabel("Query:"), BorderLayout.WEST);
    typeRow.add(typeCombo, BorderLayout.CENTER);
    top.add(typeRow, BorderLayout.NORTH);

    buildParamPanel();
    top.add(paramPanel, BorderLayout.CENTER);

    JButton run = new JButton("Run");
    run.addActionListener(e -> runSelected());
    JButton clearHl = new JButton("Clear highlight");
    clearHl.addActionListener(e -> {
      mapPanel.clearHighlighted();
      resultArea.setText("");
    });
    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
    buttons.add(clearHl);
    buttons.add(run);
    top.add(buttons, BorderLayout.SOUTH);

    add(top, BorderLayout.NORTH);

    resultArea.setEditable(false);
    resultArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
    add(new JScrollPane(resultArea), BorderLayout.CENTER);

    typeCombo.addActionListener(e -> paramCards.show(paramPanel, ((QueryType) typeCombo.getSelectedItem()).name()));
  }

  private void buildParamPanel() {
    // RELATE / AREA / LENGTH: nothing to configure beyond the current map selection
    paramPanel.add(hint("Uses the facility currently selected on the map."), QueryType.RELATE.name());
    paramPanel.add(hint("Selected facility must be a polygon."), QueryType.AREA.name());
    paramPanel.add(hint("Selected facility must be a walkway (line)."), QueryType.LENGTH.name());

    paramPanel.add(oneField("Distance (world units):", distanceField), QueryType.WITHIN.name());
    paramPanel.add(twoField("Target category:", nnTargetCombo, "How many (N):", nField), QueryType.NN.name());
    paramPanel.add(oneField("Second facility:", secondFacilityCombo), QueryType.DISTANCE.name());
    paramPanel.add(oneField("Category:", rankCategoryCombo), QueryType.RANK.name());
  }

  private JPanel hint(String text) {
    JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
    p.add(new JLabel(text));
    return p;
  }

  private JPanel oneField(String label, java.awt.Component field) {
    JPanel p = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(2, 2, 2, 6); c.anchor = GridBagConstraints.WEST;
    p.add(new JLabel(label), c);
    c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
    p.add(field, c);
    return p;
  }

  private JPanel twoField(String la, java.awt.Component fa, String lb, java.awt.Component fb) {
    JPanel p = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(2, 2, 2, 6); c.anchor = GridBagConstraints.WEST;
    p.add(new JLabel(la), c);
    c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
    p.add(fa, c);
    c.gridx = 0; c.gridy = 1; c.weightx = 0; c.fill = GridBagConstraints.NONE;
    p.add(new JLabel(lb), c);
    c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
    p.add(fb, c);
    return p;
  }

  // ---------------------------------------------------------- run

  private void runSelected() {
    QueryType q = (QueryType) typeCombo.getSelectedItem();
    try (Connection c = ConnectionManager.borrow()) {
      switch (q) {
        case RELATE:   runRelate(c); break;
        case WITHIN:   runWithin(c); break;
        case NN:       runNN(c); break;
        case AREA:     runArea(c); break;
        case LENGTH:   runLength(c); break;
        case DISTANCE: runDistance(c); break;
        case RANK:     runRank(c); break;
      }
    } catch (SQLException ex) {
      resultArea.setText("Query failed: " + ex.getMessage());
    } catch (RuntimeException ex) {
      resultArea.setText(ex.getMessage() == null ? ex.toString() : ex.getMessage());
    }
  }

  private Facility requireSelected() {
    Facility f = mapPanel.getSelected();
    if (f == null) throw new RuntimeException("Select a facility on the map first.");
    return f;
  }

  private void runRelate(Connection c) throws SQLException {
    Facility ref = requireSelected();
    List<Facility> hits = dao.relateAnyInteract(c, ref);
    showFeatureHits("Interacting with " + ref.getName(), hits);
  }

  private void runWithin(Connection c) throws SQLException {
    Facility ref = requireSelected();
    double d;
    try { d = Double.parseDouble(distanceField.getText().trim()); }
    catch (NumberFormatException ex) { throw new RuntimeException("Distance must be a number"); }
    List<Facility> hits = dao.withinDistance(c, ref, d);
    showFeatureHits("Within " + d + " of " + ref.getName(), hits);
  }

  private void runNN(Connection c) throws SQLException {
    Facility ref = requireSelected();
    FacilityCategory target = (FacilityCategory) nnTargetCombo.getSelectedItem();
    int n;
    try { n = Integer.parseInt(nField.getText().trim()); }
    catch (NumberFormatException ex) { throw new RuntimeException("N must be an integer"); }
    List<Facility> hits = dao.nearestNeighbours(c, ref, target, n);
    showFeatureHits(n + " nearest " + target.label() + " to " + ref.getName(), hits);
  }

  private void runArea(Connection c) throws SQLException {
    Facility ref = requireSelected();
    if (ref.getCategory().geometryKind() != GeometryKind.POLYGON) {
      throw new RuntimeException("Selected facility is not a polygon.");
    }
    double a = dao.area(c, ref);
    mapPanel.setHighlighted(Collections.singletonList(ref));
    resultArea.setText(ref.getName() + ": area = " + formatScalar(a) + " square units");
  }

  private void runLength(Connection c) throws SQLException {
    Facility ref = requireSelected();
    if (ref.getCategory().geometryKind() != GeometryKind.LINE) {
      throw new RuntimeException("Selected facility is not a line.");
    }
    double l = dao.length(c, ref);
    mapPanel.setHighlighted(Collections.singletonList(ref));
    resultArea.setText(ref.getName() + ": length = " + formatScalar(l) + " units");
  }

  private void runDistance(Connection c) throws SQLException {
    Facility ref = requireSelected();
    Facility other = (Facility) secondFacilityCombo.getSelectedItem();
    if (other == null) throw new RuntimeException("Pick a second facility");
    double d = dao.distance(c, ref, other);
    mapPanel.setHighlighted(java.util.Arrays.asList(ref, other));
    resultArea.setText("Distance " + ref.getName() + " ↔ " + other.getName()
        + " = " + formatScalar(d) + " units");
  }

  private void runRank(Connection c) throws SQLException {
    FacilityCategory cat = (FacilityCategory) rankCategoryCombo.getSelectedItem();
    List<SpatialQueryDAO.Ranked> rows = dao.rankByArea(c, cat);
    StringBuilder sb = new StringBuilder("Rank by area — " + cat.label() + "\n");
    sb.append(String.format("%-4s %-30s %10s%n", "Rank", "Name", "Area"));
    for (SpatialQueryDAO.Ranked r : rows) {
      sb.append(String.format("%-4d %-30s %10s%n", r.rank, r.name, formatScalar(r.area)));
    }
    resultArea.setText(sb.toString());
    // highlight all ranked facilities
    List<Facility> all = mapPanel.getFacilitiesSnapshot();
    List<Facility> hl = new ArrayList<>();
    for (Facility f : all) {
      if (f.getCategory() == cat) hl.add(f);
    }
    mapPanel.setHighlighted(hl);
  }

  private void showFeatureHits(String title, List<Facility> hits) {
    mapPanel.setHighlighted(hits);
    StringBuilder sb = new StringBuilder(title).append("\n");
    sb.append(hits.size()).append(" result(s)\n");
    for (Facility f : hits) {
      sb.append("  - [").append(f.getCategory().label()).append("] ").append(f.getName()).append('\n');
    }
    resultArea.setText(sb.toString());
  }

  private static String formatScalar(double v) {
    if (Math.abs(v - Math.round(v)) < 1e-6) return Long.toString(Math.round(v));
    return String.format("%.3f", v);
  }
}
