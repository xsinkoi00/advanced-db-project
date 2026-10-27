package dbs.oracle_project.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Placeholder login dialog.
 *
 * Phase 2 will replace this with fields for host, service, user, password, a
 * "Test connection" button, and the real DataSource wiring. For now it only
 * shows a message so the main launches cleanly and the Swing wiring is in place.
 */
public class LoginDialog extends JDialog {

  public LoginDialog(Frame owner) {
    super(owner, "CampusAtlas — Connect", true);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    JPanel content = new JPanel(new BorderLayout(12, 12));
    content.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

    JLabel message =
        new JLabel(
            "<html><h2>CampusAtlas</h2>"
                + "<p>Connection dialog not yet implemented.</p>"
                + "<p>This window is a Phase 0 placeholder; the full login flow arrives in Phase 2.</p></html>",
            SwingConstants.LEFT);
    content.add(message, BorderLayout.CENTER);

    JButton close = new JButton("Close");
    close.addActionListener(e -> dispose());
    JPanel buttons = new JPanel();
    buttons.add(close);
    content.add(buttons, BorderLayout.SOUTH);

    setContentPane(content);
    setPreferredSize(new Dimension(420, 220));
    pack();
    setLocationRelativeTo(owner);
  }
}
