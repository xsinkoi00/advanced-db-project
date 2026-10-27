package dbs.oracle_project;

import dbs.oracle_project.ui.LoginDialog;
import javax.swing.SwingUtilities;

/**
 * CampusAtlas entry point.
 *
 * Phase 0 scaffolding only: opens the (placeholder) login dialog on the Swing
 * event dispatch thread and exits once it closes. The real connect/launch
 * logic lands in Phase 2 once {@link LoginDialog} and the main frame exist.
 */
public class App {

  public static void main(String[] args) {
    SwingUtilities.invokeLater(
        () -> {
          LoginDialog dialog = new LoginDialog(null);
          dialog.setVisible(true);
          System.exit(0);
        });
  }
}
