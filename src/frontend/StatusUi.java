/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package frontend;

import frontend.chatGUI.ChatUi;
import database.SenderInfo;
import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author theunknown
 */
public class StatusUi {

    protected JPanel statusPanel = null;
    private JLabel status;

    public StatusUi() {

        this.status = new JLabel("Offline");
        this.status.setForeground(Color.RED);

        statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(this.status, BorderLayout.EAST);
    }

    public void updateStatus(Boolean state) {
        if (state) {
            SwingUtilities.invokeLater(() -> {
                status.setText("Online");
                status.setForeground(Color.GREEN);
            });
        } else {
            SwingUtilities.invokeLater(() -> {
                status. setText("Offline");
                status.setForeground(Color.RED);
            });
        }
    }
}
