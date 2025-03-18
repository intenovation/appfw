# Email Reader Package

A local implementation of the JavaMail API for reading and processing emails stored in the file system.

## Overview

The Email Reader package provides a JavaMail-compatible interface for accessing emails that have been downloaded and stored on the local file system. This allows applications to work with stored emails using the familiar JavaMail API without requiring a network connection to an email server.

## Key Components

- **LocalMail**: Helper class with static methods for creating sessions and opening stores
- **LocalStore**: Implementation of the JavaMail Store class that reads from the file system
- **LocalFolder**: Implementation of the JavaMail Folder class for accessing email folders
- **LocalMessage**: Implementation of the JavaMail Message class for reading message content

## Features

- **JavaMail API Compatibility**: Uses the standard JavaMail interfaces
- **No Network Required**: Works with locally stored emails without a network connection
- **Folder Hierarchy**: Preserves the folder structure from the original mail server
- **Message Properties**: Maintains all message metadata (sender, recipient, date, etc.)
- **Content Access**: Supports reading message content and attachments
- **Search Capability**: Implements the JavaMail search functionality

## Usage

### Opening a Local Mail Store

```java
// Get a session for local mail
Session session = LocalMail.createSession();

// Open a local store pointing to the email directory
File emailDirectory = new File("/path/to/emails");
LocalStore store = LocalMail.openStore(emailDirectory);

// Now you can use the store just like a regular JavaMail store
```

### Working with Folders

```java
// Get the root folder
Folder rootFolder = store.getDefaultFolder();

// List all folders
Folder[] folders = rootFolder.list();
for (Folder folder : folders) {
    System.out.println("Folder: " + folder.getFullName());
}

// Open a specific folder
Folder inbox = store.getFolder("INBOX");
inbox.open(Folder.READ_ONLY);
```

### Accessing Messages

```java
// Get all messages in a folder
Message[] messages = inbox.getMessages();

// Display basic information about each message
for (Message message : messages) {
    System.out.println("Subject: " + message.getSubject());
    System.out.println("From: " + Arrays.toString(message.getFrom()));
    System.out.println("Sent Date: " + message.getSentDate());
}
```

### Searching for Messages

```java
// Create a search term
SearchTerm term = new SubjectTerm("Invoice");

// Search for messages matching the term
Message[] found = inbox.search(term);
System.out.println("Found " + found.length + " messages with 'Invoice' in subject");
```

### Reading Message Content

```java
// Get the content of a message
Message message = inbox.getMessage(1);
Object content = message.getContent();

if (content instanceof String) {
    // Plain text message
    String text = (String) content;
    System.out.println("Content: " + text);
} else if (content instanceof Multipart) {
    // Multipart message
    Multipart multipart = (Multipart) content;
    // Process the parts...
}
```

## File System Structure

The LocalMail components expect emails to be stored in a specific directory structure:

```
/base_directory/
├── INBOX/
│   ├── message-id-1/
│   │   ├── message.properties  (Message metadata)
│   │   ├── content.txt         (Message content)
│   │   └── attachments/        (Optional folder for attachments)
│   └── message-id-2/
│       └── ...
├── Sent/
│   └── ...
└── Other-Folders/
    └── ...
```

Each message directory contains:
- `message.properties`: Message metadata (subject, sender, recipient, date, etc.)
- `content.txt`: The message body content
- `attachments/`: Optional directory containing message attachments

## Integration

The Email Reader package is designed to work with:

- **IMAP Downloader**: For downloading emails to the required directory structure
- **Invoice Analyzer**: For processing invoices found in email messages
- **Any JavaMail consumer**: The API is compatible with any code that uses JavaMail

## Implementation Notes

- Folders are represented as directories in the file system
- Messages are directories containing message files
- Message identification relies on the presence of a `message.properties` file
- Messages are read-only (modifications are not supported)
- Search operations are performed in memory

## Requirements

- Java 20 or higher
- JavaMail API (javax.mail)