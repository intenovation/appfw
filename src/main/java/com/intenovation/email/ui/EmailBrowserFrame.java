package com.intenovation.email.ui;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main frame for the email browser application.
 * This is the top-level container that holds all the UI components.
 */
public class EmailBrowserFrame extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(EmailBrowserFrame.class.getName());
    private static final int DEFAULT_WIDTH = 1200;
    private static final int DEFAULT_HEIGHT = 800;

    // Data
    private final Store emailStore;
    private final File emailDirectory;

    // UI components
    private final FolderTreePanel folderTreePanel;
    private final EmailListPanel emailListPanel;
    private final EmailViewPanel emailViewPanel;
    private final JToolBar toolBar;
    private final JSplitPane mainSplitPane;
    private final JSplitPane leftSplitPane;

    /**
     * Create a new email browser frame
     *
     * @param emailStore The email store to browse
     * @param emailDirectory The directory containing downloaded emails
     */
    public EmailBrowserFrame(Store emailStore, File emailDirectory) {
        this.emailStore = emailStore;
        this.emailDirectory = emailDirectory;

        // Configure frame
        setTitle("Email Browser - " + emailDirectory.getPath());
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Add window listener to disconnect from store when closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    emailStore.close();
                } catch (MessagingException ex) {
                    LOGGER.log(Level.WARNING, "Error closing email store", ex);
                }
            }
        });

        // Create UI components
        folderTreePanel = new FolderTreePanel(emailStore);
        emailListPanel = new EmailListPanel();
        emailViewPanel = new EmailViewPanel();
        
        // Set up listeners
        folderTreePanel.addFolderSelectionListener(folder -> {
            try {
                emailListPanel.setFolder(folder);
            } catch (MessagingException e) {
                LOGGER.log(Level.WARNING, "Error loading messages", e);
                JOptionPane.showMessageDialog(this,
                        "Error loading messages: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        emailListPanel.addMessageSelectionListener(message -> {
            try {
                emailViewPanel.setMessage(message);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error displaying message", e);
                JOptionPane.showMessageDialog(this,
                        "Error displaying message: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Create toolbar
        toolBar = createToolBar();
        add(toolBar, BorderLayout.NORTH);

        // Layout - split pane with folder tree on left, email list and view on right
        leftSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
                folderTreePanel, emailListPanel);
        leftSplitPane.setDividerLocation(250);
        
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                leftSplitPane, emailViewPanel);
        mainSplitPane.setDividerLocation(600);
        
        add(mainSplitPane, BorderLayout.CENTER);
        
        // Add status bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
        JLabel statusLabel = new JLabel("  Ready");
        statusBar.add(statusLabel, BorderLayout.WEST);
        add(statusBar, BorderLayout.SOUTH);
        
        // Initial load of folders
        refreshFolders();
    }

    /**
     * Create the toolbar with action buttons
     *
     * @return The toolbar component
     */
    private JToolBar createToolBar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        // Refresh button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.setToolTipText("Refresh folders and messages");
        refreshButton.addActionListener(e -> refreshFolders());
        toolbar.add(refreshButton);
        
        toolbar.addSeparator();
        
        // Reply button
        JButton replyButton = new JButton("Reply");
        replyButton.setToolTipText("Reply to selected message");
        replyButton.setEnabled(false); // Not implemented yet
        toolbar.add(replyButton);
        
        // Forward button
        JButton forwardButton = new JButton("Forward");
        forwardButton.setToolTipText("Forward selected message");
        forwardButton.setEnabled(false); // Not implemented yet
        toolbar.add(forwardButton);
        
        toolbar.addSeparator();
        
        // Print button
        JButton printButton = new JButton("Print");
        printButton.setToolTipText("Print selected message");
        printButton.setEnabled(false); // Not implemented yet
        toolbar.add(printButton);
        
        // Save button
        JButton saveButton = new JButton("Save");
        saveButton.setToolTipText("Save selected message");
        saveButton.setEnabled(false); // Not implemented yet
        toolbar.add(saveButton);
        
        return toolbar;
    }

    /**
     * Refresh the folder tree and message list
     */
    public void refreshFolders() {
        try {
            folderTreePanel.refreshFolders();
        } catch (MessagingException e) {
            LOGGER.log(Level.WARNING, "Error refreshing folders", e);
            JOptionPane.showMessageDialog(this,
                    "Error refreshing folders: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}