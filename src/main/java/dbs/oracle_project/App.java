package dbs.oracle_project;

import dbs.oracle_project.ui.LoginDialog;
import dbs.oracle_project.ui.MainFrame;
import javax.swing.SwingUtilities;

/**
 * CampusAtlas entry point. Shows the main window, then prompts for a database
 * connection. Cancelling leaves the window up so the user can still open
 * File > Connect manually.
 */
public class App {

  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      MainFrame frame = new MainFrame();
      frame.setVisible(true);
      if (LoginDialog.showAndConnect(frame)) {
        frame.onConnected();
      }
    });
  }
}
