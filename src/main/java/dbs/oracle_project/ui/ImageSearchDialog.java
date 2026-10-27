package dbs.oracle_project.ui;

import dbs.oracle_project.dao.FacilityImageDAO;
import dbs.oracle_project.dao.FacilityImageDAO.Scored;
import dbs.oracle_project.db.ConnectionManager;
import dbs.oracle_project.model.FacilityImage;
import dbs.oracle_project.util.ImageTools;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

/**
 * Content-based image search over {@code facility_image.image_si}. Four
 * sliders (AC/CH/PC/TX) drive the weights handed to {@code SI_ScoreByFtrList};
 * the "Normalise" button scales them to sum to 1 before each run.
 */
public class ImageSearchDialog extends JDialog {

  private final FacilityImageDAO dao = new FacilityImageDAO();
  private final FacilityImage query;

  private final JSlider wAC = slider();
  private final JSlider wCH = slider();
  private final JSlider wPC = slider();
  private final JSlider wTX = slider();

  private final JLabel queryPreview = new JLabel("", SwingConstants.CENTER);
  private final DefaultListModel<Scored> resultsModel = new DefaultListModel<>();
  private final JList<Scored> resultsList = new JList<>(resultsModel);
  private final JLabel resultPreview = new JLabel("Select a result to preview", SwingConstants.CENTER);

  public ImageSearchDialog(Frame owner, FacilityImage query) {
    super(owner, "Image similarity search — query #" + query.getId(), false);
    this.query = query;
    buildUi();
    loadQueryPreview();
    setSize(900, 560);
    setLocationRelativeTo(owner);
  }

  private static JSlider slider() {
    JSlider s = new JSlider(0, 100, 25);
    s.setMajorTickSpacing(25);
    s.setPaintTicks(true);
    s.setPaintLabels(true);
    return s;
  }

  private void buildUi() {
    setLayout(new BorderLayout(8, 8));

    JPanel left = new JPanel(new BorderLayout(4, 4));
    left.setBorder(BorderFactory.createTitledBorder("Query image"));
    queryPreview.setPreferredSize(new Dimension(280, 260));
    queryPreview.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
    left.add(queryPreview, BorderLayout.CENTER);

    JPanel weights = new JPanel(new GridLayout(4, 2, 4, 4));
    weights.setBorder(BorderFactory.createTitledBorder("Feature weights"));
    weights.add(new JLabel("Average colour (AC)"));
    weights.add(wAC);
    weights.add(new JLabel("Colour histogram (CH)"));
    weights.add(wCH);
    weights.add(new JLabel("Positional colour (PC)"));
    weights.add(wPC);
    weights.add(new JLabel("Texture (TX)"));
    weights.add(wTX);

    JButton normBtn = new JButton("Normalise");
    normBtn.addActionListener(e -> normaliseSliders());
    JButton runBtn = new JButton("Search");
    runBtn.addActionListener(e -> runSearch());

    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttons.add(normBtn);
    buttons.add(runBtn);

    JPanel controls = new JPanel(new BorderLayout(4, 4));
    controls.add(weights, BorderLayout.CENTER);
    controls.add(buttons, BorderLayout.SOUTH);

    left.add(controls, BorderLayout.SOUTH);

    resultsList.addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) refreshResultPreview();
    });
    resultsList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
      JLabel l = new JLabel(String.format("[%s] facility #%d · image #%d · score %.3f",
          value.category.label(), value.facilityId, value.id, value.similarity));
      l.setOpaque(true);
      l.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
      l.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
      l.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
      return l;
    });

    JPanel right = new JPanel(new BorderLayout(4, 4));
    right.setBorder(BorderFactory.createTitledBorder("Results"));
    JScrollPane listScroll = new JScrollPane(resultsList);
    listScroll.setPreferredSize(new Dimension(300, 10));
    right.add(listScroll, BorderLayout.CENTER);

    resultPreview.setPreferredSize(new Dimension(240, 240));
    resultPreview.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
    right.add(resultPreview, BorderLayout.EAST);

    add(left, BorderLayout.WEST);
    add(right, BorderLayout.CENTER);
  }

  private void loadQueryPreview() {
    try (Connection c = ConnectionManager.borrow()) {
      byte[] bytes = dao.loadBytes(c, query.getId());
      BufferedImage img = ImageTools.read(bytes);
      Image scaled = fit(img, 260, 240);
      queryPreview.setIcon(new ImageIcon(scaled));
    } catch (SQLException | IOException ex) {
      queryPreview.setText("Cannot preview: " + ex.getMessage());
    }
  }

  private void refreshResultPreview() {
    Scored sel = resultsList.getSelectedValue();
    if (sel == null) {
      resultPreview.setIcon(null);
      resultPreview.setText("Select a result to preview");
      return;
    }
    try (Connection c = ConnectionManager.borrow()) {
      byte[] bytes = dao.loadBytes(c, sel.id);
      BufferedImage img = ImageTools.read(bytes);
      Image scaled = fit(img, 220, 220);
      resultPreview.setIcon(new ImageIcon(scaled));
      resultPreview.setText("");
    } catch (SQLException | IOException ex) {
      resultPreview.setIcon(null);
      resultPreview.setText("Cannot preview: " + ex.getMessage());
    }
  }

  private void normaliseSliders() {
    double[] w = currentWeights();
    double sum = w[0] + w[1] + w[2] + w[3];
    if (sum <= 0) { // reset to uniform if everything is zero
      wAC.setValue(25); wCH.setValue(25); wPC.setValue(25); wTX.setValue(25);
      return;
    }
    wAC.setValue((int) Math.round(w[0] / sum * 100));
    wCH.setValue((int) Math.round(w[1] / sum * 100));
    wPC.setValue((int) Math.round(w[2] / sum * 100));
    wTX.setValue((int) Math.round(w[3] / sum * 100));
  }

  private double[] currentWeights() {
    return new double[] {
        wAC.getValue() / 100.0,
        wCH.getValue() / 100.0,
        wPC.getValue() / 100.0,
        wTX.getValue() / 100.0
    };
  }

  private void runSearch() {
    normaliseSliders();
    double[] w = currentWeights();
    resultsModel.clear();
    try (Connection c = ConnectionManager.borrow()) {
      List<Scored> rows = dao.findSimilar(c, query.getId(), w[0], w[1], w[2], w[3]);
      for (Scored s : rows) resultsModel.addElement(s);
      if (rows.isEmpty()) {
        JOptionPane.showMessageDialog(this,
            "No other images with feature vectors found. Upload more images first.",
            "No results", JOptionPane.INFORMATION_MESSAGE);
      }
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(this, "Search failed: " + ex.getMessage(),
          "CampusAtlas", JOptionPane.ERROR_MESSAGE);
    }
  }

  private static Image fit(BufferedImage src, int maxW, int maxH) {
    double k = Math.min((double) maxW / src.getWidth(), (double) maxH / src.getHeight());
    int w = Math.max(1, (int) (src.getWidth() * k));
    int h = Math.max(1, (int) (src.getHeight() * k));
    return src.getScaledInstance(w, h, Image.SCALE_SMOOTH);
  }

  @SuppressWarnings("unused")
  private static List<Scored> emptyScored() {
    return new ArrayList<>();
  }
}
