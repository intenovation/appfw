package com.intenovation.email.reader;

import com.intenovation.email.utils.EmailAddressSanitizer;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A local implementation of the JavaMail Message class that reads from the filesystem.
 */
class LocalMessage extends MimeMessage {
    private static final Logger LOGGER = Logger.getLogger(LocalMessage.class.getName());

    private final File messageDirectory;
    private Properties properties;
    private Date receivedDate;
    private Date sentDate;
    private String subject;
    private String from;
    private String[] to;
    private String[] cc;
    private Flags flags;
    private String content;
    private boolean loaded = false;
    private String replyTo;

    /**
     * Create a new LocalMessage
     *
     * @param folder           The folder containing this message
     * @param messageDirectory The directory containing the message files
     * @throws MessagingException If there is an error loading the message
     */
    public LocalMessage(LocalFolder folder, File messageDirectory) throws MessagingException {
        super(((LocalStore) folder.getStore()).getSession());
        this.messageDirectory = messageDirectory;
        this.flags = new Flags();
        this.properties = new Properties();

        // Load basic properties
        File propertiesFile = new File(messageDirectory, "message.properties");
        if (propertiesFile.exists()) {
            try (FileInputStream fis = new FileInputStream(propertiesFile)) {
                properties.load(fis);

                // Parse dates
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String sentDateStr = properties.getProperty("sent.date");
                if (sentDateStr != null) {
                    try {
                        this.sentDate = sdf.parse(sentDateStr);
                    } catch (ParseException e) {
                        LOGGER.log(Level.WARNING, "Error parsing sent date: " + sentDateStr, e);
                    }
                }

                String receivedDateStr = properties.getProperty("received.date");
                if (receivedDateStr != null) {
                    try {
                        this.receivedDate = sdf.parse(receivedDateStr);
                    } catch (ParseException e) {
                        LOGGER.log(Level.WARNING, "Error parsing received date: " + receivedDateStr, e);
                    }
                }

                // Get basic fields
                this.subject = properties.getProperty("subject");
                this.from = properties.getProperty("from");
                this.replyTo = properties.getProperty("reply.to");

                String toStr = properties.getProperty("to");
                if (toStr != null) {
                    this.to = toStr.split(",\\s*");
                }

                String ccStr = properties.getProperty("cc");
                if (ccStr != null) {
                    this.cc = ccStr.split(",\\s*");
                }
            } catch (IOException e) {
                throw new MessagingException("Error loading message properties", e);
            }
        }
    }

    /**
     * Get the message directory
     *
     * @return The directory containing the message files
     */
    public File getMessageDirectory() {
        return messageDirectory;
    }

    /**
     * Load the full message content
     */
    private void loadContent() throws MessagingException {
        if (loaded) {
            return;
        }

        try {
            // Load content
            File contentFile = new File(messageDirectory, "content.txt");
            if (contentFile.exists()) {
                this.content = new String(Files.readAllBytes(contentFile.toPath()), StandardCharsets.UTF_8);
            } else {
                // Try to find any other text content file that might exist
                File[] files = messageDirectory.listFiles((dir, name) -> name.endsWith(".txt"));
                if (files != null && files.length > 0) {
                    this.content = new String(Files.readAllBytes(files[0].toPath()), StandardCharsets.UTF_8);
                }
            }

            loaded = true;
        } catch (IOException e) {
            throw new MessagingException("Error loading message content", e);
        }
    }

    @Override
    public int getSize() throws MessagingException {
        loadContent();
        return content != null ? content.length() : 0;
    }

    @Override
    public String getSubject() throws MessagingException {
        return subject;
    }

    @Override
    public Date getSentDate() throws MessagingException {
        return sentDate;
    }

    @Override
    public Date getReceivedDate() {
        return receivedDate;
    }

    @Override
    public Object getContent() throws MessagingException, IOException {
        loadContent();
        return content;
    }

    @Override
    public void setFlags(Flags flag, boolean set) throws MessagingException {
        if (set) {
            flags.add(flag);
        } else {
            flags.remove(flag);
        }
    }

    @Override
    public Flags getFlags() throws MessagingException {
        return flags;
    }

    @Override
    public boolean isSet(Flags.Flag flag) throws MessagingException {
        return flags.contains(flag);
    }

    @Override
    public void setFlag(Flags.Flag flag, boolean set) throws MessagingException {
        if (set) {
            flags.add(flag);
        } else {
            flags.remove(flag);
        }
    }

    @Override
    public Message reply(boolean replyToAll) throws MessagingException {
        throw new MethodNotSupportedException("reply not supported");
    }

    @Override
    public void saveChanges() throws MessagingException {
        throw new MethodNotSupportedException("saveChanges not supported");
    }

    @Override
    public Address[] getFrom() throws MessagingException {
        if (from == null) {
            return null;
        }

        try {
            // Sanitize the email address before processing
            String sanitizedFrom = EmailAddressSanitizer.sanitizeEmailAddress(from);
            if (sanitizedFrom != null) {
                return new Address[]{new InternetAddress(sanitizedFrom)};
            } else {
                // If sanitization failed completely, log warning and return fallback
                LOGGER.log(Level.WARNING, "Unable to sanitize from address: " + from);
                return new Address[]{new InternetAddress("unknown@domain.com")};
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error creating InternetAddress after sanitization: " + from, e);
            // Create a fallback address as last resort
            return new Address[]{new InternetAddress("unknown@domain.com")};
        }
    }


    public Address[] getRecipients(RecipientType type) throws MessagingException {
        if (type == RecipientType.TO && to != null) {
            Address[] addresses = new Address[to.length];
            for (int i = 0; i < to.length; i++) {
                // Sanitize each recipient address
                String sanitized = EmailAddressSanitizer.sanitizeEmailAddress(to[i]);
                try {
                    addresses[i] = new InternetAddress(sanitized != null ? sanitized : "recipient@example.com");
                } catch (Exception e) {
                    addresses[i] = new InternetAddress("recipient@example.com");
                }
            }
            return addresses;
        } else if (type == RecipientType.CC && cc != null) {
            Address[] addresses = new Address[cc.length];
            for (int i = 0; i < cc.length; i++) {
                // Sanitize each CC address
                String sanitized = EmailAddressSanitizer.sanitizeEmailAddress(cc[i]);
                try {
                    addresses[i] = new InternetAddress(sanitized != null ? sanitized : "cc@example.com");
                } catch (Exception e) {
                    addresses[i] = new InternetAddress("cc@example.com");
                }
            }
            return addresses;
        }

        return null;
    }

    @Override
    public String[] getHeader(String name) throws MessagingException {
        String value = properties.getProperty(name.toLowerCase());
        return value != null ? new String[]{value} : null;
    }

    @Override
    public InputStream getInputStream() throws IOException, MessagingException {
        loadContent();
        return content != null ? new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)) : null;
    }

    @Override
    public Address[] getReplyTo() throws MessagingException {
        if (replyTo == null) {
            return super.getReplyTo(); // Fall back to default implementation
        }

        try {
            // Try to parse as a comma-separated list of addresses
            String[] replyToAddresses = replyTo.split(",\\s*");
            Address[] addresses = new Address[replyToAddresses.length];

            for (int i = 0; i < replyToAddresses.length; i++) {
                // Sanitize each reply-to address
                String sanitized = EmailAddressSanitizer.sanitizeEmailAddress(replyToAddresses[i]);
                if (sanitized != null) {
                    addresses[i] = new InternetAddress(sanitized);
                } else {
                    LOGGER.log(Level.WARNING, "Unable to sanitize reply-to address: " + replyToAddresses[i]);
                    addresses[i] = new InternetAddress("replyto@example.com");
                }
            }
            return addresses;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error processing reply-to addresses", e);
            return null;
        }
    }
}