## Key Features

1. **Complete IMAP Integration**: Connects to any IMAP server using javax.mail to download emails
2. **Hierarchical Storage**: Saves emails in a folder structure that mirrors the IMAP server's folders
3. **Multiple Sync Options**: Supports both full synchronization and incremental new email fetching
4. **Progress Tracking**: Shows detailed progress as it downloads emails
5. **Configuration UI**: Includes dialogs for configuring server settings and sync schedules
6. **Email Organization**: Preserves email content, metadata, and attachments
7. **Duplicate Detection**: Identifies and removes duplicate emails during cleanup
8. **Robust Error Handling**: Catches and logs exceptions at multiple levels

## Implementation Details

1. **Email Storage**:
    - Each IMAP folder becomes a directory
    - Each email gets a directory named after its Message-ID
    - Email content stored in content.txt (and/or content.html)
    - Attachments stored in a separate attachments subdirectory
    - Metadata stored in message.properties

2. **Synchronization Options**:
    - Full sync: Downloads all emails from the server
    - New emails only: Uses last sync date to get only new emails
    - Manually triggered or scheduled based on user preferences

3. **User Interface**:
    - System tray icon with hierarchical menu
    - Status dialog showing current operations
    - Configuration dialogs for server settings
    - Progress bars showing download and cleanup status

4. **Background Tasks**:
    - Email synchronization tasks with progress reporting
    - Cleanup task for organizing and removing duplicates

5. **Security**:
    - Supports SSL/TLS connections
    - Stores credentials (though in a real application, you'd want to use a more secure method)

The framework nicely abstracts away all the UI complexities, allowing the code to focus on the email processing logic.