package dbs.oracle_project.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Placeholder help text. Phase 4 will replace this with per-tool guidance and
 * screenshots. For now it covers the controls the Phase 2 UI actually exposes.
 */
public class HelpWindow extends JDialog {

  private static final String TEXT =
      "CampusAtlas quick help\n"
          + "----------------------\n\n"
          + "1. File > Connect...  Enter your Oracle host, service, user and\n"
          + "   password. The defaults match the local 18c XE instance.\n\n"
          + "2. File > Reinitialise sample data  Deletes every facility and\n"
          + "   image, then loads the 18-object demo set.\n\n"
          + "3. Toolbar\n"
          + "   - Select           Default mode. Click a facility to select it.\n"
          + "   - + Building, etc. Enter draw mode for a category.\n"
          + "   - Delete selected  Remove the current facility.\n\n"
          + "4. Map interactions\n"
          + "   - Left click       Select a facility (SELECT mode)\n"
          + "                      or add a vertex (DRAW mode).\n"
          + "   - Left drag        Move the selected facility.\n"
          + "   - Right click or double click (DRAW mode)  Finish and name.\n"
          + "   - Middle drag      Pan the view.\n"
          + "   - Mouse wheel      Zoom in/out at the cursor.\n"
          + "   - Delete key       Delete the selected facility.\n"
          + "   - Esc              Cancel drawing.\n\n"
          + "5. Side panel\n"
          + "   - Shows the selected facility.\n"
          + "   - Rename... updates its name in the database.\n";

  public HelpWindow(Frame owner) {
    super(owner, "CampusAtlas — Help", false);
    JTextArea area = new JTextArea(TEXT);
    area.setEditable(false);
    area.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
    setLayout(new BorderLayout());
    add(new JScrollPane(area), BorderLayout.CENTER);
    setSize(new Dimension(560, 500));
    setLocationRelativeTo(owner);
  }
}
