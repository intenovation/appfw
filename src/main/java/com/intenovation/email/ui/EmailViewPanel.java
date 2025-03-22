package com.intenovation.email.ui;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panel that displays the content of a selected email, including
 * header information, text content, and attachments.
 */
public class EmailViewPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(EmailViewPanel.class.getName());

    // Data
    private Message currentMessage;
    private List<AttachmentInfo> attachments;
    
    // UI components
    private final JLabel subjectLabel;
    private final JLabel fromLabel;
    private final JLabel toLabel;
    private final JLabel dateLabel;
    private final JEditorPane contentPane;
    private final JPanel attachmentPanel;
    private final JPanel headerPanel;
    private final JSplitPane verticalSplitPane;
    private final JTabbedPane contentTabs;

    /**
     * Create a new email view panel
     */
    public EmailViewPanel() {
        // Configure panel
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Message"));
        
        // Create header panel
        headerPanel = new JPanel(new GridBagLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 0;
        labelConstraints.anchor = GridBagConstraints.NORTHEAST;
        labelConstraints.insets = new Insets(2, 5, 2, 5);
        
        GridBagConstraints valueConstraints = new GridBagConstraints();
        valueConstraints.gridx = 1;
        valueConstraints.gridy = 0;
        valueConstraints.fill = GridBagConstraints.HORIZONTAL;
        valueConstraints.weightx = 1.0;
        valueConstraints.insets = new Insets(2, 5, 2, 5);
        
        // Subject
        headerPanel.add(new JLabel("Subject:"), labelConstraints);
        subjectLabel = new JLabel();
        headerPanel.add(subjectLabel, valueConstraints);
        
        // From
        labelConstraints.gridy++;
        valueConstraints.gridy++;
        headerPanel.add(new JLabel("From:"), labelConstraints);
        fromLabel = new JLabel();
        headerPanel.add(fromLabel, valueConstraints);
        
        // To
        labelConstraints.gridy++;
        valueConstraints.gridy++;
        headerPanel.add(new JLabel("To:"), labelConstraints);
        toLabel = new JLabel();
        headerPanel.add(toLabel, valueConstraints);
        
        // Date
        labelConstraints.gridy++;
        valueConstraints.gridy++;
        headerPanel.add(new JLabel("Date:"), labelConstraints);
        dateLabel = new JLabel();
        headerPanel.add(dateLabel, valueConstraints);
        
        // Create content pane for text/html content
        contentPane = new JEditorPane();
        contentPane.setEditable(false);
        contentPane.setContentType("text/html");
        contentPane.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "Error opening URL: " + e.getURL(), ex);
                    }
                }
            }
        });
        
        // Create attachment panel
        attachmentPanel = new JPanel();
        attachmentPanel.setLayout(new BoxLayout(attachmentPanel, BoxLayout.Y_AXIS));
        
        // Create tabbed pane for content and attachments
        contentTabs = new JTabbedPane();
        contentTabs.addTab("Message", new JScrollPane(contentPane));
        contentTabs.addTab("Attachments", new JScrollPane(attachmentPanel));
        
        // Create split pane for header and content
        verticalSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                headerPanel, contentTabs);
        verticalSplitPane.setDividerLocation(120);
        verticalSplitPane.setResizeWeight(0.2);
        
        add(verticalSplitPane, BorderLayout.CENTER);
        
        // Initialize with empty message
        clearMessage();
    }

    /**
     * Set the message to display
     *
     * @param message The message to display
     * @throws MessagingException If there is an error accessing message content
     * @throws IOException If there is an error reading message content
     */
    public void setMessage(Message message) throws MessagingException, IOException {
        currentMessage = message;
        
        if (message == null) {
            clearMessage();
            return;
        }
        
        // Update header information
        subjectLabel.setText(message.getSubject() != null ? message.getSubject() : "(No Subject)");
        
        // From
        if (message.getFrom() != null && message.getFrom().length > 0) {
            fromLabel.setText(message.getFrom()[0].toString());
        } else {
            fromLabel.setText("(Unknown)");
        }
        
        // To
        Address[] recipients = message.getRecipients(Message.RecipientType.TO);
        if (recipients != null && recipients.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < recipients.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(recipients[i].toString());
            }
            toLabel.setText(sb.toString());
        } else {
            toLabel.setText("(Unknown)");
        }
        
        // Date
        Date sentDate = message.getSentDate();
        if (sentDate != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            dateLabel.setText(dateFormat.format(sentDate));
        } else {
            dateLabel.setText("(Unknown)");
        }
        
        // Process content
        attachments = new ArrayList<>();
        processContent(message.getContent(), message.getContentType());
        
        // Update the tabs
        contentTabs.setSelectedIndex(0); // Show message content by default
        contentTabs.setEnabledAt(1, !attachments.isEmpty()); // Enable attachments tab only if there are attachments
        
        // Update attachment panel
        updateAttachmentPanel();
    }

    /**
     * Process the content of the message
     *
     * @param content The content object
     * @param contentType The content type
     * @throws MessagingException If there is an error accessing message content
     * @throws IOException If there is an error reading message content
     */
    private void processContent(Object content, String contentType) throws MessagingException, IOException {
        if (content instanceof String) {
            // Simple text or HTML content
            if (contentType.toLowerCase().contains("text/html")) {
                contentPane.setContentType("text/html");
                contentPane.setText((String) content);
            } else {
                // Plain text - convert to HTML for better display
                contentPane.setContentType("text/html");
                String htmlContent = "<html><body><pre>" + 
                        escapeHtml((String) content) + 
                        "</pre></body></html>";
                contentPane.setText(htmlContent);
            }
        } else if (content instanceof Multipart) {
            // Multipart content
            processMultipart((Multipart) content);
        } else if (content instanceof InputStream) {
            // Input stream - read the content
            InputStream is = (InputStream) content;
            byte[] buffer = new byte[4096];
            StringBuilder sb = new StringBuilder();
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
            }
            
            // Display as plain text
            contentPane.setContentType("text/html");
            String htmlContent = "<html><body><pre>" + 
                    escapeHtml(sb.toString()) + 
                    "</pre></body></html>";
            contentPane.setText(htmlContent);
        } else {
            // Unknown content type
            contentPane.setContentType("text/html");
            contentPane.setText("<html><body><p>Unable to display content of type: " + 
                    contentType + "</p></body></html>");
        }
    }

    /**
     * Process multipart content
     *
     * @param multipart The multipart content
     * @throws MessagingException If there is an error accessing message content
     * @throws IOException If there is an error reading message content
     */
    private void processMultipart(Multipart multipart) throws MessagingException, IOException {
        boolean foundMainContent = false;
        
        for (int i = 0; i < multipart.getCount(); i++) {
            Part part = multipart.getBodyPart(i);
            String disposition = part.getDisposition();
            String contentType = part.getContentType();
            
            // Handle attachments
            if (disposition != null && 
                    (disposition.equalsIgnoreCase(Part.ATTACHMENT) || 
                     disposition.equalsIgnoreCase(Part.INLINE))) {
                // This is an attachment
                String filename = part.getFileName();
                if (filename == null) {
                    filename = "attachment-" + (i + 1);
                }
                
                // Add to attachments list
                attachments.add(new AttachmentInfo(filename, contentType, part));
            } 
            // Handle main content
            else if (!foundMainContent && contentType != null && 
                    (contentType.toLowerCase().contains("text/html") || 
                     contentType.toLowerCase().contains("text/plain"))) {
                // This is the main content
                Object content = part.getContent();
                processContent(content, contentType);
                foundMainContent = true;
            } 
            // Handle nested multipart
            else if (part.getContent() instanceof Multipart) {
                // Recursively process nested multipart
                if (!foundMainContent) {
                    processMultipart((Multipart) part.getContent());
                    foundMainContent = true;
                } else {
                    // If we already found main content, check for attachments in nested multipart
                    processMultipartForAttachments((Multipart) part.getContent());
                }
            }
        }
    }

    /**
     * Process multipart content for attachments only
     *
     * @param multipart The multipart content
     * @throws MessagingException If there is an error accessing message content
     * @throws IOException If there is an error reading message content
     */
    private void processMultipartForAttachments(Multipart multipart) throws MessagingException, IOException {
        for (int i = 0; i < multipart.getCount(); i++) {
            Part part = multipart.getBodyPart(i);
            String disposition = part.getDisposition();
            
            if (disposition != null && 
                    (disposition.equalsIgnoreCase(Part.ATTACHMENT) || 
                     disposition.equalsIgnoreCase(Part.INLINE))) {
                // This is an attachment
                String filename = part.getFileName();
                if (filename == null) {
                    filename = "attachment-" + (i + 1);
                }
                
                // Add to attachments list
                attachments.add(new AttachmentInfo(filename, part.getContentType(), part));
            } else if (part.getContent() instanceof Multipart) {
                // Recursively process nested multipart
                processMultipartForAttachments((Multipart) part.getContent());
            }
        }
    }

    /**
     * Update the attachment panel with buttons for each attachment
     */
    private void updateAttachmentPanel() {
        attachmentPanel.removeAll();
        
        if (attachments.isEmpty()) {
            attachmentPanel.add(new JLabel("No attachments"));
        } else {
            for (AttachmentInfo attachment : attachments) {
                JPanel attachRow = new JPanel(new BorderLayout());
                attachRow.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                
                // File icon and name
                JLabel fileLabel = new JLabel(attachment.filename);
                fileLabel.setIcon(UIManager.getIcon("FileView.fileIcon"));
                attachRow.add(fileLabel, BorderLayout.CENTER);
                
                // Open button
                JButton openButton = new JButton("Open");
                openButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        openAttachment(attachment);
                    }
                });
                
                // Save button
                JButton saveButton = new JButton("Save");
                saveButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        saveAttachment(attachment);
                    }
                });
                
                // Button panel
                JPanel buttonPanel = new JPanel();
                buttonPanel.add(openButton);
                buttonPanel.add(saveButton);
                attachRow.add(buttonPanel, BorderLayout.EAST);
                
                // Add to attachment panel
                attachmentPanel.add(attachRow);
            }
        }
        
        attachmentPanel.revalidate();
        attachmentPanel.repaint();
    }

    /**
     * Open an attachment with the system default application
     *
     * @param attachment The attachment to open
     */
    private void openAttachment(AttachmentInfo attachment) {
        try {
            // Create a temporary file for the attachment
            String filename = attachment.filename;
            int dotIndex = filename.lastIndexOf('.');
            String prefix = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
            String suffix = dotIndex > 0 ? filename.substring(dotIndex) : "";
            
            // Create temp file with appropriate suffix for file type
            Path tempFile = Files.createTempFile(prefix, suffix);
            
            // Write attachment data to the temp file
            try (InputStream in = attachment.part.getInputStream()) {
                Files.copy(in, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            
            // Open with default application
            Desktop.getDesktop().open(tempFile.toFile());
            
            // Add a shutdown hook to delete the temp file on exit
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to delete temp file: " + tempFile, e);
                }
            }));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error opening attachment: " + attachment.filename, e);
            JOptionPane.showMessageDialog(this, 
                    "Error opening attachment: " + e.getMessage(), 
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Save an attachment to a user-selected location
     *
     * @param attachment The attachment to save
     */
    private void saveAttachment(AttachmentInfo attachment) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(attachment.filename));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            try {
                // Write attachment data to the file
                try (InputStream in = attachment.part.getInputStream()) {
                    Files.copy(in, file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                
                JOptionPane.showMessageDialog(this,
                        "Attachment saved to " + file.getPath(),
                        "Attachment Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error saving attachment: " + attachment.filename, e);
                JOptionPane.showMessageDialog(this,
                        "Error saving attachment: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Clear the message display
     */
    private void clearMessage() {
        subjectLabel.setText("");
        fromLabel.setText("");
        toLabel.setText("");
        dateLabel.setText("");
        contentPane.setText("<html><body><p>No message selected</p></body></html>");
        
        attachments = new ArrayList<>();
        updateAttachmentPanel();
        
        contentTabs.setSelectedIndex(0);
        contentTabs.setEnabledAt(1, false);
    }

    /**
     * Escape HTML special characters
     *
     * @param text The text to escape
     * @return The escaped text
     */
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Class to hold attachment information
     */
    private static class AttachmentInfo {
        private final String filename;
        private final String contentType;
        private final Part part;
        
        public AttachmentInfo(String filename, String contentType, Part part) {
            this.filename = filename;
            this.contentType = contentType;
            this.part = part;
        }
    }
}