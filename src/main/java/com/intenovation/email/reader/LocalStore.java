package com.intenovation.email.reader;

import javax.mail.*;
import java.io.*;
import java.util.*;

/**
 * A local implementation of the JavaMail API that works with downloaded emails.
 * This allows existing code to iterate over locally stored emails using the same
 * API as it would for remote IMAP emails but without the network overhead.
 */
public class LocalStore extends Store {
    public File getBaseDirectory() {
        return baseDirectory;
    }
    private Session session;
    private final File baseDirectory;
    private boolean connected = false;
    private LocalFolder defaultFolder;
    private final Map<String, LocalFolder> folderCache = new HashMap<>();
    
    /**
     * Create a new LocalStore
     * 
     * @param session The mail session
     * @param urlname The URL
     * @param baseDirectory The base directory where emails are stored
     */
    protected LocalStore(Session session, URLName urlname, File baseDirectory) {
        super(session, urlname);
        this.baseDirectory = baseDirectory;
        this.session = session;
    }
    public Session getSession(){
        return this.session = session;
    }
    /**
     * Create a new LocalStore
     * 
     * @param session The mail session
     * @param baseDirectory The base directory where emails are stored
     * @return A new LocalStore
     */
    public static LocalStore getInstance(Session session, File baseDirectory) {
        return new LocalStore(session, null, baseDirectory);
    }
    
    @Override
    protected boolean protocolConnect(String host, int port, String user, String password) throws MessagingException {
        if (!baseDirectory.exists() || !baseDirectory.isDirectory()) {
            throw new MessagingException("Base directory does not exist: " + baseDirectory);
        }
        connected = true;
        return true;
    }
    
    @Override
    public Folder getDefaultFolder() throws MessagingException {
        if (!connected) {
            throw new IllegalStateException("Not connected");
        }
        
        if (defaultFolder == null) {
            defaultFolder = new LocalFolder(this, null, baseDirectory);
        }
        
        return defaultFolder;
    }
    
    @Override
    public Folder getFolder(String name) throws MessagingException {
        if (!connected) {
            throw new IllegalStateException("Not connected");
        }
        
        if (folderCache.containsKey(name)) {
            return folderCache.get(name);
        }
        
        File folderDir = new File(baseDirectory, name);
        if (!folderDir.exists() || !folderDir.isDirectory()) {
            throw new MessagingException("Folder does not exist: " + name);
        }
        
        LocalFolder folder = new LocalFolder(this, name, folderDir);
        folderCache.put(name, folder);
        return folder;
    }
    
    @Override
    public Folder getFolder(URLName url) throws MessagingException {
        return getFolder(url.getFile());
    }
    
    @Override
    public void close() throws MessagingException {
        connected = false;
        folderCache.clear();
        defaultFolder = null;
    }
    
    @Override
    public boolean isConnected() {
        return connected;
    }
}

