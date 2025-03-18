# Invoice Processing Framework

A robust framework for automatically detecting, extracting, and analyzing invoice information from email archives.

## Overview

The Invoice Processing Framework provides tools for identifying invoices in email archives, extracting key financial data, and generating reports. It integrates with the Intenovation Application Framework (appfw) to provide a system tray application with background processing capabilities.

## Key Components

- **Invoice**: Data model for representing invoice information
- **Type**: Enumeration that classifies document types (Invoice, Receipt, Statement, etc.)
- **InvoiceProcessor**: Background task that scans emails and extracts invoice data
- **InvoiceAnalyzerApp**: Main application class with UI integration

## Features

- **Email Integration**: Processes emails from local archives created by the IMAP Downloader
- **Document Classification**: Automatically identifies different types of financial documents
- **Data Extraction**: Uses pattern matching to extract key invoice fields:
    - Invoice number
    - Amount
    - Account number
    - Issue date & due date
    - Sender information
- **Reporting**: Generates CSV and summary reports of extracted invoice data
- **System Tray Integration**: Runs in the background with progress monitoring
- **Logging**: Comprehensive logging of process steps and results

## Configuration

The framework stores its configuration in the user's home directory:
```
~/.invoice-analyzer.properties
```

Key settings include:
- **email.directory**: Location of downloaded email archives
- **output.directory**: Location for generated reports

## Usage

### As a Library

```java
// Create an invoice processor
File emailDir = new File("/path/to/emails");
File outputDir = new File("/path/to/reports");
InvoiceProcessor processor = new InvoiceProcessor(emailDir, outputDir);

// Create a progress callback
ProgressStatusCallback callback = new ProgressStatusCallback() {
    @Override
    public void update(int percent, String message) {
        System.out.println(percent + "% - " + message);
    }
};

// Process invoices
String result = processor.execute(callback);
System.out.println("Result: " + result);
```

### As a Desktop Application

1. Launch the InvoiceAnalyzerApp
2. Configure email and output directories through the system tray menu
3. Trigger invoice processing manually or let the scheduled task run automatically
4. View generated reports in the configured output directory

## Integration

The framework is designed to work with:
- **IMAP Downloader**: For downloading and organizing email archives
- **Email Reader**: For accessing downloaded emails
- **Application Framework**: For system tray and background processing functionality

## Requirements

- Java 20 or higher
- Intenovation Application Framework (appfw)
- Downloaded email archives in a structured format