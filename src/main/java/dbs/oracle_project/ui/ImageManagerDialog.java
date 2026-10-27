package dbs.oracle_project.ui;

import dbs.oracle_project.dao.FacilityImageDAO;
import dbs.oracle_project.db.ConnectionManager;
import dbs.oracle_project.model.Facility;
import dbs.oracle_project.model.FacilityImage;
import dbs.oracle_project.util.ImageTools;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Manage the images attached to one facility.
 *
 * <p>Add/replace use a file chooser; rotate works in memory via
 * {@link ImageTools}; "Find similar..." opens the content-based search dialog
 * seeded with the currently selected image.
 */
public class ImageManagerDialog extends JDialog {

  private final FacilityImageDAO dao = new FacilityImageDAO();
  private final Facility facility;

  private final DefaultListModel<FacilityImage> listModel = new DefaultListModel<>();
  private final JList<FacilityImage> imageList = new JList<>(listModel);
  private final JLabel preview = new JLabel("No image selected", SwingConstants.CENTER);
  private final JLabel info    = new JLabel(" ");
  private byte[] previewBytes;

  private final JButton addBtn     = new JButton("Add...");
  private final JButton replaceBtn = new JButton("Replace...");
  private final JButton rotCwBtn   = new JButton("Rotate 90° CW");
  private final JButton rotCcwBtn  = new JButton("Rotate 90° CCW");
  private final JButton deleteBtn  = new JButton("Delete");
  private final JButton searchBtn  = new JButton("Find similar...");
  private final JButton exportBtn  = new JButton("Export...");

  public ImageManagerDialog(Frame owner, Facility facility) {
    super(owner, "Images — " + facility.getCategory().label() + " \"" + facility.getName() + "\"", true);
    this.facility = facility;
    buildUi();
    setSize(820, 560);
    setLocationRelativeTo(owner);
    reload();
  }

  private void buildUi() {
    setLayout(new BorderLayout(8, 8));

    imageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    imageList.addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) refreshPreview();
    });
    JScrollPane leftScroll = new JScrollPane(imageList);
    leftScroll.setPreferredSize(new Dimension(240, 10));
    leftScroll.setBorder(BorderFactory.createTitledBorder("Images"));

    preview.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
    preview.setPreferredSize(new Dimension(500, 400));
    info.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

    JPanel centre = new JPanel(new BorderLayout(4, 4));
    centre.add(preview, BorderLayout.CENTER);
    centre.add(info, BorderLayout.SOUTH);

    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
    addBtn.addActionListener(e -> onAdd());
    replaceBtn.addActionListener(e -> onReplace());
    rotCwBtn.addActionListener(e -> onRotate(true));
    rotCcwBtn.addActionListener(e -> onRotate(false));
    deleteBtn.addActionListener(e -> onDelete());
    searchBtn.addActionListener(e -> onFindSimilar());
    exportBtn.addActionListener(e -> onExport());
    buttons.add(addBtn);
    buttons.add(replaceBtn);
    buttons.add(rotCwBtn);
    buttons.add(rotCcwBtn);
    buttons.add(deleteBtn);
    buttons.add(exportBtn);
    buttons.add(searchBtn);

    JButton close = new JButton("Close");
    close.addActionListener(e -> dispose());
    JPanel south = new JPanel(new BorderLayout());
    south.add(buttons, BorderLayout.CENTER);
    JPanel closeRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    closeRow.add(close);
    south.add(closeRow, BorderLayout.EAST);

    add(leftScroll, BorderLayout.WEST);
    add(centre, BorderLayout.CENTER);
    add(south, BorderLayout.SOUTH);

    updateButtons();
  }

  private void reload() {
    listModel.clear();
    try (Connection c = ConnectionManager.borrow()) {
      List<FacilityImage> imgs = dao.listForFacility(c, facility);
      for (FacilityImage i : imgs) listModel.addElement(i);
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(this, "Failed to list images: " + ex.getMessage(),
          "CampusAtlas", JOptionPane.ERROR_MESSAGE);
    }
    if (!listModel.isEmpty()) imageList.setSelectedIndex(0);
    else refreshPreview();
  }

  private void refreshPreview() {
    FacilityImage sel = imageList.getSelectedValue();
    previewBytes = null;
    if (sel == null) {
      preview.setIcon(null);
      preview.setText("No image selected");
      info.setText(" ");
      updateButtons();
      return;
    }
    try (Connection c = ConnectionManager.borrow()) {
      previewBytes = dao.loadBytes(c, sel.getId());
      BufferedImage img = ImageTools.read(previewBytes);
      Image scaled = fit(img, preview.getWidth() == 0 ? 500 : preview.getWidth(),
          preview.getHeight() == 0 ? 400 : preview.getHeight());
      preview.setText("");
      preview.setIcon(new ImageIcon(scaled));
      info.setText("Image #" + sel.getId() + " · " + img.getWidth() + "×" + img.getHeight()
          + " · uploaded " + sel.getUploadedAt()
          + (sel.getCaption() == null || sel.getCaption().isEmpty() ? "" : " · " + sel.getCaption()));
    } catch (SQLException | IOException ex) {
      preview.setIcon(null);
      preview.setText("Cannot preview: " + ex.getMessage());
      info.setText(" ");
    }
    updateButtons();
  }

  private void updateButtons() {
    boolean has = imageList.getSelectedValue() != null;
    replaceBtn.setEnabled(has);
    rotCwBtn.setEnabled(has);
    rotCcwBtn.setEnabled(has);
    deleteBtn.setEnabled(has);
    searchBtn.setEnabled(has);
    exportBtn.setEnabled(has);
  }

  private static Image fit(BufferedImage src, int maxW, int maxH) {
    double k = Math.min((double) maxW / src.getWidth(), (double) maxH / src.getHeight());
    int w = Math.max(1, (int) (src.getWidth() * k));
    int h = Math.max(1, (int) (src.getHeight() * k));
    return src.getScaledInstance(w, h, Image.SCALE_SMOOTH);
  }

  private File chooseImageFile() {
    JFileChooser fc = new JFileChooser();
    fc.setFileFilter(new FileNameExtensionFilter("Images (jpg, png, gif, bmp)", "jpg", "jpeg", "png", "gif", "bmp"));
    if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return null;
    return fc.getSelectedFile();
  }

  // -------------------------------------------------------- actions

  private void onAdd() {
    File f = chooseImageFile();
    if (f == null) return;
    String caption = JOptionPane.showInputDialog(this, "Caption (optional):", f.getName());
    if (caption == null) return;
    try (Connection c = ConnectionManager.borrow()) {
      byte[] bytes = Files.readAllBytes(f.toPath());
      dao.upload(c, facility, caption, bytes);
    } catch (SQLException | IOException ex) {
      JOptionPane.showMessageDialog(this, "Upload failed: " + ex.getMessage(),
          "CampusAtlas", JOptionPane.ERROR_MESSAGE);
      return;
    }
    reload();
  }

  private void onReplace() {
    FacilityImage sel = imageList.getSelectedValue();
    if (sel == null) return;
    File f = chooseImageFile();
    if (f == null) return;
    try (Connection c = ConnectionManager.borrow()) {
      byte[] bytes = Files.readAllBytes(f.toPath());
      dao.replace(c, sel.getId(), bytes);
    } catch (SQLException | IOException ex) {
      JOptionPane.showMessageDialog(this, "Replace failed: " + ex.getMessage(),
          "CampusAtlas", JOptionPane.ERROR_MESSAGE);
      return;
    }
    refreshPreview();
  }

  private void onRotate(boolean clockwise) {
    FacilityImage sel = imageList.getSelectedValue();
    if (sel == null || previewBytes == null) return;
    try (Connection c = ConnectionManager.borrow()) {
      byte[] rotated = ImageTools.rotate90(previewBytes, clockwise);
      dao.replace(c, sel.getId(), rotated);
    } catch (SQLException | IOException ex) {
      JOptionPane.showMessageDialog(this, "Rotate failed: " + ex.getMessage(),
          "CampusAtlas", JOptionPane.ERROR_MESSAGE);
      return;
    }
    refreshPreview();
  }

  private void onDelete() {
    FacilityImage sel = imageList.getSelectedValue();
    if (sel == null) return;
    int ans = JOptionPane.showConfirmDialog(this,
        "Delete image #" + sel.getId() + "?", "Confirm", JOptionPane.YES_NO_OPTION);
    if (ans != JOptionPane.YES_OPTION) return;
    try (Connection c = ConnectionManager.borrow()) {
      dao.delete(c, sel.getId());
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(this, "Delete failed: " + ex.getMessage(),
          "CampusAtlas", JOptionPane.ERROR_MESSAGE);
      return;
    }
    reload();
  }

  private void onExport() {
    if (previewBytes == null) return;
    JFileChooser fc = new JFileChooser();
    fc.setSelectedFile(new File("image_" + imageList.getSelectedValue().getId() + ".png"));
    if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
    try {
      Files.write(fc.getSelectedFile().toPath(), previewBytes);
    } catch (IOException ex) {
      JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(),
          "CampusAtlas", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void onFindSimilar() {
    FacilityImage sel = imageList.getSelectedValue();
    if (sel == null) return;
    ImageSearchDialog d = new ImageSearchDialog((Frame) getOwner(), sel);
    d.setVisible(true);
  }
}
