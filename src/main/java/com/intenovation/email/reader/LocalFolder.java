package com.intenovation.email.reader;

import javax.mail.*;
import javax.mail.search.SearchTerm;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * A local implementation of the JavaMail Folder class that reads from the filesystem.
 */
class LocalFolder extends Folder {
    private final File directory;
    private final String folderName;
    private boolean isOpen = false;
    private int mode = -1;
    private final List<LocalMessage> messages = new ArrayList<>();

    /**
     * Create a new LocalFolder
     *
     * @param store      The LocalStore
     * @param folderName The folder name
     * @param directory  The directory containing the emails
     */
    public LocalFolder(LocalStore store, String folderName, File directory) {
        super(store);
        this.folderName = folderName;
        this.directory = directory;
    }

    @Override
    public String getName() {
        return directory.getName();
    }

    @Override
    public String getFullName() {
        return folderName != null ? folderName : getName();
    }

    @Override
    public Folder getParent() throws MessagingException {
        if (folderName == null) {
            return null; // This is the root folder
        }

        File parentDir = directory.getParentFile();
        if (parentDir == null || parentDir.equals(((LocalStore) store).getBaseDirectory())) {
            return ((LocalStore) store).getDefaultFolder();
        }

        return new LocalFolder((LocalStore) store, parentDir.getName(), parentDir);
    }

    @Override
    public boolean exists() throws MessagingException {
        return directory.exists() && directory.isDirectory();
    }

    @Override
    public Folder[] list(String pattern) throws MessagingException {
        File[] subdirs = directory.listFiles(File::isDirectory);
        if (subdirs == null) {
            return new Folder[0];
        }

        List<Folder> folders = new ArrayList<>();
        for (File subdir : subdirs) {
            // Skip hidden directories and non-folder directories
            if (subdir.getName().startsWith(".") || subdir.getName().equals("attachments")) {
                continue;
            }

            String subfolder = folderName != null ? folderName + "/" + subdir.getName() : subdir.getName();
            folders.add(new LocalFolder((LocalStore) store, subfolder, subdir));
        }

        return folders.toArray(new Folder[0]);
    }

    @Override
    public Folder[] list() throws MessagingException {
        return list("%");
    }

    @Override
    public char getSeparator() throws MessagingException {
        return '/';
    }

    @Override
    public int getType() throws MessagingException {
        if (folderName == null) {
            return HOLDS_FOLDERS; // Root folder
        }
        return HOLDS_MESSAGES | HOLDS_FOLDERS;
    }

    @Override
    public boolean create(int type) throws MessagingException {
        if (exists()) {
            return false;
        }

        return directory.mkdirs();
    }

    @Override
    public boolean hasNewMessages() throws MessagingException {
        return false; // Not applicable for local store
    }

    @Override
    public Folder getFolder(String name) throws MessagingException {
        File subfolder = new File(directory, name);
        String newFolderName = folderName != null ? folderName + "/" + name : name;
        return new LocalFolder((LocalStore) store, newFolderName, subfolder);
    }

    @Override
    public boolean delete(boolean recurse) throws MessagingException {
        if (!exists()) {
            return false;
        }

        if (recurse) {
            deleteRecursively(directory);
            return true;
        } else {
            return directory.delete();
        }
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }

    @Override
    public boolean renameTo(Folder f) throws MessagingException {
        if (!(f instanceof LocalFolder)) {
            throw new MessagingException("Can only rename to another LocalFolder");
        }

        LocalFolder target = (LocalFolder) f;
        return directory.renameTo(target.directory);
    }

    @Override
    public void open(int mode) throws MessagingException {
        if (isOpen) {
            throw new IllegalStateException("Folder is already open");
        }

        this.mode = mode;
        this.isOpen = true;

        // Load messages
        File[] messageDirs = directory.listFiles(File::isDirectory);
        if (messageDirs != null) {
            for (File messageDir : messageDirs) {
                // Skip hidden directories or non-message directories
                if (messageDir.getName().startsWith(".") || messageDir.getName().equals("attachments")) {
                    continue;
                }

                File propertiesFile = new File(messageDir, "message.properties");
                if (propertiesFile.exists()) {
                    try {
                        LocalMessage message = new LocalMessage(this, messageDir);
                        messages.add(message);
                    } catch (Exception e) {
                        // Log error but continue with other messages
                        System.err.println("Error loading message: " + e.getMessage());
                    }
                }
            }
        }

        // Sort messages by date if possible
        messages.sort(Comparator.comparing(LocalMessage::getReceivedDate, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    @Override
    public void close(boolean expunge) throws MessagingException {
        if (!isOpen) {
            throw new IllegalStateException("Folder is not open");
        }

        messages.clear();
        isOpen = false;
        mode = -1;
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public Flags getPermanentFlags() {
        Flags flags = new Flags();
        flags.add(Flags.Flag.SEEN);
        flags.add(Flags.Flag.DELETED);
        flags.add(Flags.Flag.ANSWERED);
        flags.add(Flags.Flag.FLAGGED);
        return flags;
    }

    @Override
    public int getMessageCount() throws MessagingException {
        if (!isOpen) {
            File[] messageDirs = directory.listFiles(File::isDirectory);
            if (messageDirs == null) {
                return 0;
            }

            int count = 0;
            for (File dir : messageDirs) {
                if (!dir.getName().startsWith(".") && !dir.getName().equals("attachments")) {
                    count++;
                }
            }
            return count;
        }

        return messages.size();
    }

    @Override
    public Message getMessage(int msgnum) throws MessagingException {
        if (!isOpen) {
            throw new IllegalStateException("Folder is not open");
        }

        if (msgnum < 1 || msgnum > messages.size()) {
            throw new IndexOutOfBoundsException("Message number out of range: " + msgnum);
        }

        return messages.get(msgnum - 1);
    }

    @Override
    public Message[] getMessages(int start, int end) throws MessagingException {
        if (!isOpen) {
            throw new IllegalStateException("Folder is not open");
        }

        if (start < 1 || start > messages.size() || end < start || end > messages.size()) {
            throw new IndexOutOfBoundsException("Message range out of bounds");
        }

        Message[] result = new Message[end - start + 1];
        for (int i = start; i <= end; i++) {
            result[i - start] = messages.get(i - 1);
        }
        return result;
    }

    @Override
    public Message[] getMessages() throws MessagingException {
        if (!isOpen) {
            throw new IllegalStateException("Folder is not open");
        }

        return messages.toArray(new Message[0]);
    }

    @Override
    public void appendMessages(Message[] msgs) throws MessagingException {
        throw new MethodNotSupportedException("appendMessages not supported");
    }

    @Override
    public Message[] expunge() throws MessagingException {
        if (!isOpen || mode != READ_WRITE) {
            throw new IllegalStateException("Folder not open in READ_WRITE mode");
        }

        List<Message> expunged = new ArrayList<>();
        Iterator<LocalMessage> iterator = messages.iterator();
        while (iterator.hasNext()) {
            LocalMessage message = iterator.next();
            if (message.getFlags().contains(Flags.Flag.DELETED)) {
                expunged.add(message);
                iterator.remove();

                // Delete the message directory
                File messageDir = message.getMessageDirectory();
                deleteRecursively(messageDir);
            }
        }

        return expunged.toArray(new Message[0]);
    }

    @Override
    public int getMode() {
        return mode;
    }

    @Override
    public Message[] search(SearchTerm term) throws MessagingException {
        if (!isOpen) {
            throw new IllegalStateException("Folder is not open");
        }

        if (term == null) {
            return getMessages();
        }

        List<Message> matches = new ArrayList<>();
        for (LocalMessage message : messages) {
            if (term.match(message)) {
                matches.add(message);
            }
        }

        return matches.toArray(new Message[0]);
    }
}
