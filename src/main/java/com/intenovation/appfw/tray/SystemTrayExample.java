package com.intenovation.appfw.tray;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

public class SystemTrayExample {

    public static void main(String[] args) {
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();

            // Create a TrayIcon
            Image image = Toolkit.getDefaultToolkit().getImage("path/to/your/image.png"); // Replace with your image path
            TrayIcon trayIcon = new TrayIcon(image, "Tray Demo", createPopupMenu());

            // Add the TrayIcon to the system tray
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                System.err.println("Failed to add TrayIcon: " + e);
            }
        } else {
            System.err.println("System Tray not supported on this platform.");
        }
    }

    private static PopupMenu createPopupMenu() {
        PopupMenu popup = new PopupMenu();

        // Add menu items
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        popup.add(exitItem);

        return popup;
    }
}