package com.intenovation.email.ui;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panel that displays a list of emails in a selected folder.
 */
public class EmailListPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(EmailListPanel.class.getName());

    // Data
    private Folder currentFolder;
    private Message[] messages;
    
    // UI components
    private final JTable messageTable;
    private final MessageTableModel tableModel;
    private final JLabel folderInfoLabel;
    
    // Listeners
    private final List<MessageSelectionListener> messageSelectionListeners = new ArrayList<>();

    /**
     * Create a new email list panel
     */
    public EmailListPanel() {
        // Configure panel
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Messages"));
        
        // Create the table model
        tableModel = new MessageTableModel();
        
        // Create the message table
        messageTable = new JTable(tableModel);
        messageTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        messageTable.setAutoCreateRowSorter(true);
        messageTable.setShowGrid(false);
        messageTable.setIntercellSpacing(new Dimension(0, 0));
        
        // Set column widths
        messageTable.getColumnModel().getColumn(0).setMaxWidth(24); // Flag
        messageTable.getColumnModel().getColumn(0).setMinWidth(24);
        messageTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Date
        messageTable.getColumnModel().getColumn(2).setPreferredWidth(200); // From
        messageTable.getColumnModel().getColumn(3).setPreferredWidth(350); // Subject
        
        // Set custom renderers
        messageTable.getColumnModel().getColumn(0).setCellRenderer(new FlagColumnRenderer());
        messageTable.getColumnModel().getColumn(1).setCellRenderer(new DateColumnRenderer());
        
        // Add selection listener
        messageTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int row = messageTable.getSelectedRow();
                    if (row >= 0) {
                        int modelRow = messageTable.convertRowIndexToModel(row);
                        notifyMessageSelected(messages[modelRow]);
                    }
                }
            }
        });
        
        // Add table to scroll pane
        JScrollPane scrollPane = new JScrollPane(messageTable);
        add(scrollPane, BorderLayout.CENTER);
        
        // Add folder info label
        folderInfoLabel = new JLabel("No folder selected");
        folderInfoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(folderInfoLabel, BorderLayout.NORTH);
    }

    /**
     * Set the folder to display messages from
     *
     * @param folder The folder to display messages from
     * @throws MessagingException If there is an error accessing messages
     */
    public void setFolder(Folder folder) throws MessagingException {
        // Close the current folder if open
        if (currentFolder != null && currentFolder.isOpen()) {
            currentFolder.close(false);
        }
        
        currentFolder = folder;
        
        // Open the folder
        if (folder != null) {
            if (!folder.isOpen()) {
                folder.open(Folder.READ_ONLY);
            }
            
            // Get messages
            messages = folder.getMessages();
            
            // Update folder info label
            int messageCount = folder.getMessageCount();
            folderInfoLabel.setText(folder.getFullName() + " (" + messageCount + " messages)");
        } else {
            // No folder selected
            messages = new Message[0];
            folderInfoLabel.setText("No folder selected");
        }
        
        // Update table model
        tableModel.fireTableDataChanged();
        
        // Auto-select first message if available
        if (messages.length > 0) {
            messageTable.getSelectionModel().setSelectionInterval(0, 0);
        }
    }

    /**
     * Add a message selection listener
     *
     * @param listener The listener to add
     */
    public void addMessageSelectionListener(MessageSelectionListener listener) {
        messageSelectionListeners.add(listener);
    }

    /**
     * Remove a message selection listener
     *
     * @param listener The listener to remove
     */
    public void removeMessageSelectionListener(MessageSelectionListener listener) {
        messageSelectionListeners.remove(listener);
    }

    /**
     * Notify all message selection listeners that a message was selected
     *
     * @param message The selected message
     */
    private void notifyMessageSelected(Message message) {
        for (MessageSelectionListener listener : messageSelectionListeners) {
            listener.messageSelected(message);
        }
    }

    /**
     * Table model for the message list
     */
    private class MessageTableModel extends AbstractTableModel {
        private final String[] columnNames = {"", "Date", "From", "Subject"};
        
        @Override
        public int getRowCount() {
            return messages != null ? messages.length : 0;
        }
        
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (messages == null || rowIndex >= messages.length) {
                return null;
            }
            
            try {
                Message message = messages[rowIndex];
                
                switch (columnIndex) {
                    case 0: // Flag column - indicates if message has been read
                        return message.isSet(Flags.Flag.SEEN);
                    case 1: // Date column
                        Date date = message.getSentDate();
                        if (date == null) {
                            date = message.getReceivedDate();
                        }
                        return date;
                    case 2: // From column
                        if (message.getFrom() != null && message.getFrom().length > 0) {
                            return message.getFrom()[0].toString();
                        } else {
                            return "";
                        }
                    case 3: // Subject column
                        return message.getSubject() != null ? message.getSubject() : "(No Subject)";
                    default:
                        return null;
                }
            } catch (MessagingException e) {
                LOGGER.log(Level.WARNING, "Error getting message data", e);
                return "Error";
            }
        }
        
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0: return Boolean.class;
                case 1: return Date.class;
                default: return String.class;
            }
        }
    }

    /**
     * Custom renderer for the flag column
     */
    private class FlagColumnRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
            
            setHorizontalAlignment(JLabel.CENTER);
            
            Boolean seen = (Boolean) value;
            if (seen != null && !seen) {
                setText("●"); // Bullet character for unread messages
                setFont(getFont().deriveFont(Font.BOLD));
            } else {
                setText("");
            }
            
            return this;
        }
    }

    /**
     * Custom renderer for the date column
     */
    private class DateColumnRenderer extends DefaultTableCellRenderer {
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            if (value instanceof Date) {
                value = dateFormat.format((Date) value);
            }
            
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    /**
     * Interface for message selection listeners
     */
    public interface MessageSelectionListener {
        void messageSelected(Message message);
    }
}