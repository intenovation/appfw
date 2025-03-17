# IMAP Email Downloader Application

This application demonstrates how to use the SystemTrayAppFramework to create a practical utility that downloads emails from an IMAP server and organizes them in a local file system structure.

## Overview

The IMAP Email Downloader runs in the system tray and provides the following features:

1. **Full Email Synchronization**: Downloads all emails from the IMAP server
2. **New Email Check**: Downloads only new emails since the last check
3. **Email Archive Cleanup**: Organizes and cleans up the local email archive
4. **Storage Organization**: Creates a hierarchical folder structure that mirrors the IMAP server's folders

## File System Structure

The application creates a hierarchical file system structure for the emails:

```
EmailArchive/
├── INBOX/
│   ├── message-id-1/
│   │   ├── content.txt         (Message body)
│   │   ├── message.properties  (Metadata)
│   │   ├── attachment1.txt     (Attachments)
│   │   └── attachment2.txt
│   └── message-id-2/
│       └── ...
├── Sent/
│   └── ...
└── Other-Folders/
    └── ...
```

## Key Features

### Email Synchronization

The application provides two synchronization options:

1. **Full Email Sync**: Downloads all emails from the server (runs every 4 hours)
2. **New Emails Only**: Downloads only emails that aren't already in the local archive (runs every 10 minutes)

The synchronization process:
- Connects to the IMAP server
- Retrieves the folder list
- Creates matching local directories
- Downloads messages and their attachments
- Stores message metadata in properties files

### Email Archive Structure

For each email:
- Creates a directory named after the message ID
- Stores the message content in `content.txt`
- Saves metadata (subject, sender, recipients, date) in `message.properties`
- Saves attachments as individual files with their original filenames

### Cleanup and Maintenance

The application includes a cleanup task that:
- Runs once per day
- Removes duplicate messages
- Ensures proper organization of the archive
- Reports on storage usage

## System Tray Integration

The application integrates with the system tray by using the SystemTrayAppFramework:

1. **Progress Reporting**: Shows download progress in both the system tray menu and status dialog
2. **Background Tasks**: Runs synchronization and cleanup tasks in the background
3. **Menu Integration**: Provides quick access to common operations
4. **Status Reporting**: Shows task status and progress

## Menu Structure

- **Email Operations**
    - Configure IMAP Settings
    - Open Email Archive
- **Tasks**
    - Full Email Sync
    - New Emails Only
    - Email Cleanup
- **Tools**
    - Check Server Status
    - View Storage Usage
- **Task Status** (Shows progress of running tasks)

## Implementation Notes

### Actual IMAP Implementation

In a real application, you would replace the simulation code with actual IMAP operations:

```java
// Replace simulation with real IMAP code:
Properties props = new Properties();
props.setProperty("mail.store.protocol", "imaps");
Session session = Session.getInstance(props, null);
Store store = session.getStore("imaps");
store.connect(EMAIL_HOST, EMAIL_USERNAME, EMAIL_PASSWORD);

// Get folders from server
Folder[] folders = store.getDefaultFolder().list();

// For each folder:
Folder folder = store.getFolder(folderName);
folder.open(Folder.READ_ONLY);
Message[] messages = folder.getMessages();

// Process each message:
for (Message message : messages) {
    // Extract message ID, content, attachments, etc.
}
```

### Configuration

In a real application, you would implement a configuration dialog to:
- Set the IMAP server details
- Provide username and password (securely stored)
- Configure synchronization intervals
- Set the email archive location

### Security Considerations

This example uses hardcoded credentials for simplicity. In a real application:
- Use a secure credential storage mechanism
- Support OAuth 2.0 for modern email providers
- Encrypt sensitive data on disk
- Implement proper error handling and retry mechanisms

## Requirements

- Java 8 or higher
- JavaMail API (javax.mail)
- Access to an IMAP server
- Sufficient local storage for email archive

## Usage

1. Configure your IMAP server details (in a real app, this would be through a UI)
2. Start the application
3. The application will appear in your system tray
4. Use the system tray menu to trigger synchronization or view status
5. Background tasks will run automatically according to their schedule