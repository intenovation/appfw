package com.intenovation.email.reader;

import javax.mail.MessagingException;
import javax.mail.Session;
import java.io.File;
import java.util.Properties;

/**
 * Helper class for creating and working with local mail stores
 */
public class LocalMail {
    /**
     * Create a Session for use with LocalStore
     * 
     * @return A new mail session
     */
    public static Session createSession() {
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "local");
        return Session.getInstance(props);
    }
    
    /**
     * Open a local store for the given directory
     * 
     * @param baseDirectory The directory containing downloaded emails
     * @return A connected LocalStore
     * @throws MessagingException If there is an error connecting to the store
     */
    public static LocalStore openStore(File baseDirectory) throws MessagingException {
        Session session = createSession();
        LocalStore store = LocalStore.getInstance(session, baseDirectory);
        store.connect();
        return store;
    }
}
