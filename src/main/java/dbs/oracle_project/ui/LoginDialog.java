package dbs.oracle_project.ui;

import dbs.oracle_project.db.ConnectionManager;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Collects Oracle connection details from the user and configures
 * {@link ConnectionManager}. Prefilled with the local XE values; the user
 * overwrites them to point at the campus server.
 */
public class LoginDialog extends JDialog {

  private final JTextField hostField      = new JTextField("localhost", 20);
  private final JTextField portField      = new JTextField("1521", 6);
  private final JTextField serviceField   = new JTextField("XEPDB1", 15);
  private final JTextField userField      = new JTextField("cardb", 20);
  private final JPasswordField passField  = new JPasswordField(20);
  private final JLabel statusLabel        = new JLabel(" ");

  private boolean connected = false;

  public LoginDialog(Frame owner) {
    super(owner, "CampusAtlas — Connect", true);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    JPanel form = new JPanel(new GridBagLayout());
    form.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(4, 4, 4, 4);
    c.anchor = GridBagConstraints.WEST;

    addRow(form, c, 0, "Host:",         hostField);
    addRow(form, c, 1, "Port:",         portField);
    addRow(form, c, 2, "Service name:", serviceField);
    addRow(form, c, 3, "Username:",     userField);
    addRow(form, c, 4, "Password:",     passField);

    statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 8, 20));

    JButton testBtn   = new JButton("Test");
    JButton connectBtn = new JButton("Connect");
    JButton cancelBtn  = new JButton("Cancel");
    testBtn.addActionListener(e -> doConnect(false));
    connectBtn.addActionListener(e -> doConnect(true));
    cancelBtn.addActionListener(e -> dispose());
    getRootPane().setDefaultButton(connectBtn);

    JPanel buttons = new JPanel();
    buttons.add(testBtn);
    buttons.add(connectBtn);
    buttons.add(cancelBtn);

    JPanel bottom = new JPanel(new BorderLayout());
    bottom.add(statusLabel, BorderLayout.CENTER);
    bottom.add(buttons, BorderLayout.SOUTH);

    setLayout(new BorderLayout());
    add(form, BorderLayout.CENTER);
    add(bottom, BorderLayout.SOUTH);

    pack();
    setLocationRelativeTo(owner);
  }

  public boolean isConnected() {
    return connected;
  }

  private void addRow(JPanel form, GridBagConstraints c, int row, String label, java.awt.Component field) {
    c.gridx = 0; c.gridy = row; c.weightx = 0; c.fill = GridBagConstraints.NONE;
    form.add(new JLabel(label, SwingConstants.RIGHT), c);
    c.gridx = 1; c.gridy = row; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
    form.add(field, c);
  }

  private void doConnect(boolean closeOnSuccess) {
    statusLabel.setText("Connecting...");
    try {
      int port = Integer.parseInt(portField.getText().trim());
      ConnectionManager.configure(
          hostField.getText().trim(),
          port,
          serviceField.getText().trim(),
          userField.getText().trim(),
          new String(passField.getPassword()));
      connected = true;
      statusLabel.setText("Connected as " + ConnectionManager.displayName());
      if (closeOnSuccess) dispose();
    } catch (NumberFormatException ex) {
      statusLabel.setText("Port must be a number");
    } catch (SQLException ex) {
      statusLabel.setText("Failed: " + rootCauseMessage(ex));
    }
  }

  private static String rootCauseMessage(Throwable t) {
    Throwable cur = t;
    while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
    return cur.getMessage() == null ? cur.toString() : cur.getMessage();
  }

  /** Convenience: open modally and return whether a connection was made. */
  public static boolean showAndConnect(Frame owner) {
    LoginDialog d = new LoginDialog(owner);
    d.setVisible(true);
    return d.isConnected();
  }

  /** Not used by the app, but useful for a visual sanity check: null-safe JOptionPane. */
  @SuppressWarnings("unused")
  private void showInfo(String message) {
    JOptionPane.showMessageDialog(this, message);
  }
}
