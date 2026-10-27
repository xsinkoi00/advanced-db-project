package dbs.oracle_project.ui;

import dbs.oracle_project.db.ConnectionManager;
import dbs.oracle_project.db.Schema;
import dbs.oracle_project.model.FacilityCategory;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;

/**
 * Main application window: menu bar, drawing toolbar, map canvas and a thin
 * side panel showing information about the selected facility.
 */
public class MainFrame extends JFrame {

  private final MapPanel mapPanel = new MapPanel();
  private final JLabel statusLabel = new JLabel(" ");
  private final JLabel selectionLabel = new JLabel("No facility selected");
  private final JButton renameButton = new JButton("Rename...");

  public MainFrame() {
    super("CampusAtlas");
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setJMenuBar(buildMenuBar());

    JPanel content = new JPanel(new BorderLayout());
    content.add(buildToolBar(), BorderLayout.NORTH);

    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapPanel, buildSidePanel());
    split.setResizeWeight(1.0);
    split.setDividerLocation(900);
    content.add(split, BorderLayout.CENTER);
    content.add(buildStatusBar(), BorderLayout.SOUTH);

    setContentPane(content);
    mapPanel.setSelectionListener(f -> {
      if (f == null) {
        selectionLabel.setText("No facility selected");
        renameButton.setEnabled(false);
      } else {
        selectionLabel.setText("<html>Selected: <b>" + escape(f.getName())
            + "</b><br>Category: " + f.getCategory().label()
            + "<br>ID: " + f.getId() + "</html>");
        renameButton.setEnabled(true);
      }
    });

    renameButton.setEnabled(false);
    setSize(1280, 820);
    setLocationRelativeTo(null);
  }

  private JMenuBar buildMenuBar() {
    JMenuBar bar = new JMenuBar();

    JMenu fileMenu = new JMenu("File");
    JMenuItem connect = new JMenuItem("Connect...");
    connect.addActionListener(e -> onConnect());
    JMenuItem reinit = new JMenuItem("Reinitialise sample data");
    reinit.addActionListener(e -> onReinit());
    JMenuItem reload = new JMenuItem("Reload from DB");
    reload.addActionListener(e -> mapPanel.reload());
    JMenuItem exit = new JMenuItem("Exit");
    exit.addActionListener(e -> System.exit(0));
    fileMenu.add(connect);
    fileMenu.add(reinit);
    fileMenu.add(reload);
    fileMenu.addSeparator();
    fileMenu.add(exit);

    JMenu viewMenu = new JMenu("View");
    JMenuItem spatial = new JMenuItem("Spatial queries...");
    spatial.addActionListener(e -> openSpatialQueries());
    viewMenu.add(spatial);

    JMenu helpMenu = new JMenu("Help");
    JMenuItem helpItem = new JMenuItem("Help");
    helpItem.addActionListener(e -> new HelpWindow(this).setVisible(true));
    JMenuItem aboutItem = new JMenuItem("About");
    aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
        "CampusAtlas\nPDBe 26/27Z course project\nFIT BUT",
        "About CampusAtlas", JOptionPane.INFORMATION_MESSAGE));
    helpMenu.add(helpItem);
    helpMenu.add(aboutItem);

    bar.add(fileMenu);
    bar.add(viewMenu);
    bar.add(helpMenu);
    return bar;
  }

  private SpatialQueryDialog spatialQueryDialog;

  private void openSpatialQueries() {
    if (!ConnectionManager.isConfigured()) {
      JOptionPane.showMessageDialog(this, "Connect first.", "CampusAtlas", JOptionPane.WARNING_MESSAGE);
      return;
    }
    if (spatialQueryDialog == null) {
      spatialQueryDialog = new SpatialQueryDialog(this, mapPanel);
    }
    spatialQueryDialog.refreshFacilityCombos();
    spatialQueryDialog.setVisible(true);
    spatialQueryDialog.toFront();
  }

  private JToolBar buildToolBar() {
    JToolBar tb = new JToolBar();
    tb.setFloatable(false);

    JButton selectBtn = new JButton("Select");
    selectBtn.addActionListener(e -> mapPanel.enterSelectMode());
    tb.add(selectBtn);
    tb.addSeparator();

    for (FacilityCategory cat : FacilityCategory.values()) {
      JButton b = new JButton("+ " + cat.label());
      b.addActionListener(e -> mapPanel.enterDrawMode(cat));
      tb.add(b);
    }

    tb.addSeparator();
    JButton deleteBtn = new JButton("Delete selected");
    deleteBtn.addActionListener(e -> mapPanel.deleteSelected());
    tb.add(deleteBtn);

    tb.add(Box.createHorizontalGlue());
    return tb;
  }

  private JPanel buildSidePanel() {
    JPanel p = new JPanel();
    p.setLayout(new java.awt.GridBagLayout());
    p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    p.setPreferredSize(new Dimension(260, 10));

    java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
    c.gridx = 0; c.gridy = 0; c.weightx = 1; c.fill = java.awt.GridBagConstraints.HORIZONTAL;
    c.anchor = java.awt.GridBagConstraints.NORTHWEST;
    c.insets = new java.awt.Insets(0, 0, 8, 0);

    JLabel title = new JLabel("<html><h3>Facility</h3></html>");
    p.add(title, c);

    c.gridy++;
    p.add(selectionLabel, c);

    c.gridy++;
    renameButton.addActionListener(e -> onRename());
    p.add(renameButton, c);

    c.gridy++;
    c.weighty = 1;
    c.fill = java.awt.GridBagConstraints.BOTH;
    p.add(Box.createVerticalGlue(), c);
    return p;
  }

  private JPanel buildStatusBar() {
    JPanel bar = new JPanel(new BorderLayout());
    bar.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
    bar.add(statusLabel, BorderLayout.WEST);
    refreshStatus();
    return bar;
  }

  private void refreshStatus() {
    statusLabel.setText(ConnectionManager.isConfigured()
        ? "Connected: " + ConnectionManager.displayName()
        : "Not connected. Use File > Connect...");
  }

  private void onConnect() {
    if (LoginDialog.showAndConnect(this)) {
      onConnected();
    }
  }

  /** Called after a successful connection, from either File > Connect or App startup. */
  public void onConnected() {
    refreshStatus();
    mapPanel.reload();
  }

  private void onReinit() {
    if (!ConnectionManager.isConfigured()) {
      JOptionPane.showMessageDialog(this, "Connect first.", "CampusAtlas", JOptionPane.WARNING_MESSAGE);
      return;
    }
    int ans = JOptionPane.showConfirmDialog(this,
        "This will delete every facility and image, then reload sample data.\nContinue?",
        "Confirm reinitialise", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    if (ans != JOptionPane.YES_OPTION) return;
    try (Connection c = ConnectionManager.borrow()) {
      Schema.reinitSampleData(c);
    } catch (SQLException | IOException ex) {
      JOptionPane.showMessageDialog(this, "Reinit failed: " + ex.getMessage(),
          "CampusAtlas", JOptionPane.ERROR_MESSAGE);
      return;
    }
    mapPanel.reload();
  }

  private void onRename() {
    if (mapPanel.getSelected() == null) return;
    String current = mapPanel.getSelected().getName();
    String next = JOptionPane.showInputDialog(this, "New name:", current);
    if (next == null || next.trim().isEmpty() || next.trim().equals(current)) return;
    mapPanel.getSelected().setName(next.trim());
    try (Connection c = ConnectionManager.borrow()) {
      new dbs.oracle_project.dao.FacilityDAO().save(c, mapPanel.getSelected());
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(this, "Rename failed: " + ex.getMessage(),
          "CampusAtlas", JOptionPane.ERROR_MESSAGE);
    }
    mapPanel.reload();
  }

  private static String escape(String s) {
    if (s == null) return "";
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  @SuppressWarnings("unused")
  private static Component spacer(int w) {
    return Box.createRigidArea(new Dimension(w, 1));
  }
}
