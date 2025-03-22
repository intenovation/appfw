package com.intenovation.email.ui;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panel that displays a tree of email folders.
 */
public class FolderTreePanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(FolderTreePanel.class.getName());

    // Data
    private final Store emailStore;
    
    // UI components
    private final JTree folderTree;
    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode rootNode;
    
    // Listeners
    private final List<FolderSelectionListener> folderSelectionListeners = new ArrayList<>();

    /**
     * Create a new folder tree panel
     *
     * @param emailStore The email store to display folders from
     */
    public FolderTreePanel(Store emailStore) {
        this.emailStore = emailStore;
        
        // Configure panel
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Folders"));
        
        // Create folder tree
        rootNode = new DefaultMutableTreeNode("Email Store");
        treeModel = new DefaultTreeModel(rootNode);
        folderTree = new JTree(treeModel);
        folderTree.setRootVisible(true);
        folderTree.setShowsRootHandles(true);
        
        // Set custom renderer
        folderTree.setCellRenderer(new FolderTreeCellRenderer());
        
        // Add selection listener
        folderTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                TreePath path = e.getNewLeadSelectionPath();
                if (path != null) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    if (node.getUserObject() instanceof FolderNode) {
                        FolderNode folderNode = (FolderNode) node.getUserObject();
                        notifyFolderSelected(folderNode.getFolder());
                    }
                }
            }
        });
        
        // Add tree to scroll pane
        JScrollPane scrollPane = new JScrollPane(folderTree);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Refresh the folder tree
     *
     * @throws MessagingException If there is an error accessing folders
     */
    public void refreshFolders() throws MessagingException {
        // Clear the tree
        rootNode.removeAllChildren();
        
        // Add the default folder and its subfolders
        Folder defaultFolder = emailStore.getDefaultFolder();
        addFolderToTree(defaultFolder, rootNode);
        
        // Expand the root node
        treeModel.reload();
        folderTree.expandPath(new TreePath(rootNode.getPath()));
    }

    /**
     * Add a folder and its subfolders to the tree
     *
     * @param folder The folder to add
     * @param parentNode The parent node to add to
     * @throws MessagingException If there is an error accessing folders
     */
    private void addFolderToTree(Folder folder, DefaultMutableTreeNode parentNode) throws MessagingException {
        FolderNode folderNode = new FolderNode(folder);
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(folderNode);
        parentNode.add(node);
        
        // Add subfolders if this folder holds folders
        if ((folder.getType() & Folder.HOLDS_FOLDERS) != 0) {
            Folder[] subfolders = folder.list();
            for (Folder subfolder : subfolders) {
                addFolderToTree(subfolder, node);
            }
        }
    }

    /**
     * Add a folder selection listener
     *
     * @param listener The listener to add
     */
    public void addFolderSelectionListener(FolderSelectionListener listener) {
        folderSelectionListeners.add(listener);
    }

    /**
     * Remove a folder selection listener
     *
     * @param listener The listener to remove
     */
    public void removeFolderSelectionListener(FolderSelectionListener listener) {
        folderSelectionListeners.remove(listener);
    }

    /**
     * Notify all folder selection listeners that a folder was selected
     *
     * @param folder The selected folder
     */
    private void notifyFolderSelected(Folder folder) {
        for (FolderSelectionListener listener : folderSelectionListeners) {
            listener.folderSelected(folder);
        }
    }

    /**
     * Custom tree cell renderer for folders
     */
    private class FolderTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                      boolean expanded, boolean leaf, int row,
                                                      boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object userObject = node.getUserObject();
            
            if (userObject instanceof FolderNode) {
                FolderNode folderNode = (FolderNode) userObject;
                setText(folderNode.toString());
                
                try {
                    Folder folder = folderNode.getFolder();
                    
                    // Set different icons based on folder type
                    if (folder.getName() == null) {
                        // Root folder
                        setIcon(UIManager.getIcon("FileView.computerIcon"));
                    } else if ((folder.getType() & Folder.HOLDS_MESSAGES) != 0) {
                        // Folder with messages
                        if (folder.getUnreadMessageCount() > 0) {
                            // Has unread messages
                            setIcon(UIManager.getIcon("FileView.directoryIcon"));
                        } else {
                            // No unread messages
                            setIcon(UIManager.getIcon("FileView.directoryIcon"));
                        }
                    } else {
                        // Folder without messages
                        setIcon(UIManager.getIcon("FileView.directoryIcon"));
                    }
                    
                } catch (MessagingException e) {
                    LOGGER.log(Level.WARNING, "Error getting folder info", e);
                }
            }
            
            return this;
        }
    }

    /**
     * Wrapper class for folders in the tree
     */
    private static class FolderNode {
        private final Folder folder;
        
        public FolderNode(Folder folder) {
            this.folder = folder;
        }
        
        public Folder getFolder() {
            return folder;
        }
        
        @Override
        public String toString() {
            try {
                String name = folder.getName();
                if (name == null) {
                    return folder.getFullName();
                }
                
                if ((folder.getType() & Folder.HOLDS_MESSAGES) != 0) {
                    int total = folder.getMessageCount();
                    if (total >= 0) {
                        return name + " (" + total + ")";
                    }
                }
                
                return name;
            } catch (MessagingException e) {
                LOGGER.log(Level.WARNING, "Error getting folder name", e);
                return "Error";
            }
        }
    }

    /**
     * Interface for folder selection listeners
     */
    public interface FolderSelectionListener {
        void folderSelected(Folder folder);
    }
}