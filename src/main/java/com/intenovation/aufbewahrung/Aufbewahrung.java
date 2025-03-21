package com.intenovation.aufbewahrung;

import com.intenovation.email.reader.LocalMail;
import com.sun.mail.util.BASE64DecoderStream;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.Properties;
import java.util.Scanner;

/**
 * ollama run llama3.2
 */

public class Aufbewahrung {

    public Aufbewahrung() {
    }

    static int downloadedCount = 0;
    static int visitedCount = 0;

    public static void doit(int yearParam) throws MessagingException, IOException {
        Folder folder = null;
        Store store = null;
        System.out.println("find . -type f -name 'invoices.tsv' -delete");
        System.out.println("find . -type f -name 'unparsedMail*.txt' -delete");
        System.out.println(Invoice.header());
        Statistic stat = new Statistic("Total");
        try {
            store = LocalMail.openStore(new File("/Users/jens/src/tex/intenovation/Aufbewahrung/EmailArchive"));
            Folder[] f = store.getDefaultFolder().list("*");
            for (Folder fd : f) {
                //System.out.println(">> " + fd.getName());
                folder = readFolder(fd, stat,yearParam);
                if (folder != null && folder.isOpen()) {
                    folder.close(true);
                }
            }
        } finally {
            if (folder != null && folder.isOpen()) {
                folder.close(true);
            }
            if (store != null) {
                store.close();
            }
        }
        System.out.println("done, downloaded: " + downloadedCount);
        System.out.println(stat.toString(0));
        BufferedWriter writer = new BufferedWriter(new FileWriter("Statistics.txt"));
        writer.write(stat.toString(0));
        writer.close();
    }

    public static double total = 0.0;

    private static Folder readFolder(Folder folder, Statistic statistic,int yearParam) throws MessagingException, IOException {
        // Folder folder;
        // folder = store.getFolder(folderName);
        /*
         * Others GMail folders : [Gmail]/All Mail This folder contains all of
         * your Gmail messages. [Gmail]/Drafts Your drafts. [Gmail]/Sent Mail
         * Messages you sent to other people. [Gmail]/Spam Messages marked as
         * spam. [Gmail]/Starred Starred messages. [Gmail]/Trash Messages
         * deleted from Gmail.
         */
        int statCounter = 0;
        try {
            // System.out.println("Folder : " + folder.getName());
            folder.open(Folder.READ_ONLY);
            Message messages[] = folder.getMessages();
            // System.out.println("No of Messages : " +
            // folder.getMessageCount());
            // System.out.println("No of Unread Messages : " +
            // folder.getUnreadMessageCount());
            for (int i = 0; i < messages.length; ++i) {

                visitedCount++;
                //if (visitedCount>=1000)
                //    continue;
                //check 3 after point
                //recreation.gov remove new line
                // System.out.println("MESSAGE #" + (i + 1) + ":");
                Message msg = messages[i];
                int year = msg.getReceivedDate().getYear() + 1900;
                int month = msg.getReceivedDate().getMonth() + 1;
                int day = msg.getReceivedDate().getDate();
                if (yearParam!=0 && year<yearParam){
                    LocalDateTime localDateTime = LocalDateTime.now();
                    System.out.println(localDateTime.toString()+" Skipping "+year);
                    continue;
                }
                /*
                 * if we don''t want to fetch messages already processed if
                 * (!msg.isSet(Flags.Flag.SEEN)) { String from = "unknown"; ...
                 * }
                 */
                String from = "unknown";
                // Address[] froms = msg.getFrom();
                // String email = froms == null ? null : ((InternetAddress)
                // froms[0]).getAddress();
                if (msg.getReplyTo().length >= 1) {
                    from = ((InternetAddress) msg.getReplyTo()[0]).getAddress();
                } else if (msg.getFrom().length >= 1) {
                    from = ((InternetAddress) msg.getFrom()[0]).getAddress();
                }
                // if (!from.contains("stacart"))
                // continue;
                int atPos = from.indexOf('@');
                if (atPos > 25) {
                    from = "randomized@" + from.substring(atPos + 1);
                    atPos = 10;
                }

                String subject = msg.getSubject();
                System.out.println("Processing ... " + subject + " " + from + " on " + msg.getReceivedDate());
                // you may want to replace the spaces with "_"
                // the TEMP directory is used to store the files
                String filename = subject;
                String folderName = folder.getName();


                String domain = from.substring(atPos + 1);
                while(countLetter(domain,'.')>1){
                    int pos=domain.indexOf('.');
                    domain=domain.substring(pos+1);
                }
                if ((from.contains("gmail.com")||from.contains("me.com")) && atPos < from.length()) {
                    domain = from.substring(0, atPos);
                }

                if (true || !folderName.startsWith("20") || folderName.contains("@")) {


                    folderName = "emails/"+year + "/" + domain + "/"+ from;
/*
                    if (false) {
                        Folder newFolder = store.getFolder("INBOX/" + folderName.replace('/', '-'));
                        if (!newFolder.exists())
                            newFolder.create(Folder.HOLDS_MESSAGES | Folder.READ_WRITE);
                        List<Message> tempList = new ArrayList<Message>();
                        tempList.add(msg);
                        Message[] tempMessageArray = tempList.toArray(new Message[tempList.size()]);
                        folder.copyMessages(tempMessageArray, newFolder); //

                    }
 */

                }
                if (msg.getFileName() != null)
                    filename = msg.getFileName();
                Invoice invoice = new Invoice();
                MimeMessage mm = (MimeMessage)msg;
                invoice.emailnumber=mm.getMessageID();
                if (invoice.emailnumber!=null)
                    invoice.emailnumber=invoice.emailnumber.replace("<","").replace(">","");
                invoice.year = year;
                invoice.month = month;
                invoice.day = day;
                invoice.subject = subject;
                invoice.email = from;
                invoice.type = Type.detectType(subject);
                String number = parseField(subject, false, "Invoice #:", "Rechnungsnummer:", "Billing Period:",
                        "aktuelle Rechnung", "Invoice no.", "INVOICE", "Bill Period    : ", "Receipt #", "Ihre Rechnung Nr.", "Ihre Rechnung", "Invoice");
                if (number != null) {
                    number = findEnd(number, "von");
                    number = findEnd(number, "vom");
                    number = findEnd(number, "from");
                    number = findEnd(number, "/");
                    number = findEnd(number, "Is Available");
                    number = number.replace('\n', ' ');
                    if (number.length() > 70)
                        number = number.substring(0, 70);
                    if (number.length() > 1)
                        invoice.number = number;
                }
                saveParts(invoice, msg.getContent(), filename, folderName, msg.getSentDate());
                if (invoice.number == null || invoice.number.length() == 0) {
                    int hash = Math.abs((invoice.amount + invoice.dueDate + invoice.email).hashCode());
                    invoice.number = "" + hash;
                }


                total += invoice.amount;


                boolean isDupe = statistic.addAmount(invoice.amount, "" + year, domain, invoice.type.name(), "" + invoice.number);
                if (isDupe)
                    invoice.type = Type.Reminder;
                if (invoice.amount > 0.0) {
                    save(invoice, folderName);
                    if (!isDupe)
                        save(invoice, ".");
                    statCounter++;
                    //if (statCounter%20==0) {
                    //    System.out.println(statistic.toString(0));
                    //}

                    if (statCounter % 200 == 0) {
                        BufferedWriter writer = new BufferedWriter(new FileWriter("Statistics.txt"));
                        writer.write(statistic.toString(0));
                        writer.close();
                    }
                }
                // msg.setFlag(Flags.Flag.SEEN,true);
                // to delete the message
                // msg.setFlag(Flags.Flag.DELETED, true);
            }
        } catch (MessagingException e) {
            //e.printStackTrace();
        }

        return folder;

    }

    private static void parseInvoiceText(Invoice invoice, MimeBodyPart msg, String folderName, String filename,
                                         Date date) {
        String content;

        try {
            // System.out.println("parseInvoiceText " + folderName);
            content = msg.getContent().toString();
            boolean success = parseTxt(invoice, folderName, filename, content, date, "text");
            if (success) {
                String datString = (date.getMonth() + 1) + "-" + date.getDate();
                String mailFile = storeUnparseMail(invoice.emailnumber,invoice.type.name(), folderName, content, datString, "text", filename, "txt");
                addFileToInvoice(invoice, mailFile);

            }
            return;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return;

    }

    public static String trimAdvanced(String value) {
        value = value.replaceAll("\u00A0", "");
        Objects.requireNonNull(value);

        int strLength = value.length();
        int len = value.length();
        int st = 0;
        char[] val = value.toCharArray();

        if (strLength == 0) {
            return "";
        }

        while ((st < len) && (val[st] <= ' ') || (val[st] == '\u00A0')) {
            st++;
            if (st == strLength) {
                break;
            }
        }
        while ((st < len) && (val[len - 1] <= ' ') || (val[len - 1] == '\u00A0')) {
            len--;
            if (len == 0) {
                break;
            }
        }

        return (st > len) ? "" : ((st > 0) || (len < strLength)) ? value.substring(st, len) : value;
    }

    private static boolean parseTxt(Invoice invoice, String folderName, String filename, String content, Date date,
                                    String parse) throws IOException {
        String datString = (date.getMonth() + 1) + "-" + date.getDate();
        try {
            if (invoice.type == Type.Letter)
                invoice.type = Type.detectType(content);
            if (invoice.city == City.Unknown)
                invoice.city = City.detectCity(content);
            if (invoice.utility == Utility.Unknown)
                invoice.utility = Utility.detectUtility(content);
            // System.out.println("parseTxt");
            // System.out.println(content);

            content = trimLines(content);
            // content = content.replaceAll("(?m)^[ \t]*\r?\n", "");
            // content = content.replaceAll("(?m)^\\s+$",
            // "").replaceAll("(?m)^\\n", "");

            boolean skipLine = folderName.contains("socalgas")|| folderName.contains("Emadco")|| folderName.contains("pearson");
            String amountString = parseField(content, skipLine, "Gesamtbetrag (brutto)","Gesamtsumme", "Rechnungsbetrag in Höhe von","Total Charges:",
                    "Total Amount Due:","Total Balance Due:", "a payment of", "Total due now:", "Total Due",
                    "Statement Amount:", "Amount due:","Amount Due:", "Rechnungsbetrag:", "Amount:", "Rechnungsbetrag von", "The amount of",
                    "Rechnungsbetrag in Höhe von", "Invoice amount", "invoice total is ", "Total$", "Original Charge",
                    "Total Charged", "Payment amount:", "BALANCE DUE", "Current charges:", "Total (USD):", "Total (USD)","Amount Paid:",
                    "Amount paid:", "Total $","TOTAL $", "Amount Received", "Amount due on this invoice:", "Total", " is $", "Statement balance:",
                    "Statement balance","Amount Charged $","Amount Due","PAY THIS AMOUNT:","outstanding balance of","TOTAL","Amount paid","DETAILS",
                    "for your payment of","Converted From:","invoice for","Zu zahlender Betrag:","You paid","Balance due","Bill Payment Amount $",
                    "Your payment of $","Payment amount","Total charged","Payments","charged to your Bank Account is  $-","*Amount*","Summe ink l. USt",
                    "Endbetrag EUR","Total","received your payment of","Payment Amount:","in the amount of","Your donation total:","Your payment of **$",
                    "SUBTOTAL: $","Automatic payment amount","Amount","SUBTOTAL: $","SUBTOTAL:","Automatic payment amount","Brutto ","payment of","for this period is",
                    "Amount","amount of","Total charged","Balance Due:","outstanding balance of","Bill Payment Amount","Total Payment");
            if (amountString == null || amountString.trim().length() < 2) {
                //String mails="From:"+
                String mailFile = storeUnparseMail(invoice.emailnumber,"unparsed", folderName, content, datString, parse, filename, "txt");
                addFileToInvoice(invoice, mailFile);
                return false;
            }
            if (content.contains("Betrag")  ||content.contains("Rechnungsbetrag") ||content.contains("EUR")|| content.contains("Bestellung") || content.contains("Gesamtsumme")) {
                amountString = amountString.replace('.', '*');
                amountString = amountString.replace(',', '.');
                amountString = amountString.replace('*', ',');
            }

            amountString = findEnd(amountString, "€");
            amountString = findEnd(amountString, "EUR");
            amountString = findEnd(amountString, "with");
            amountString = findEnd(amountString, "ziehen");
            amountString = findEnd(amountString, "We ");
            amountString = findEnd(amountString, "Du ");
            amountString = findEnd(amountString, " and ");
            amountString = findEnd(amountString, "as of");
            amountString = findEnd(amountString, " on ");
            amountString = findEnd(amountString, " for ");
            amountString = findEnd(amountString, " and ");

            if (amountString.contains("/")) {
                //String mails="From:"+
                String mailFile = storeUnparseMail(invoice.emailnumber,"unparsed", folderName, content, datString, parse, filename, "txt");
                addFileToInvoice(invoice, mailFile);
                return false;
            }

            String cleanAmountStr = amountString.replaceAll("[^\\d.]+", "");
            if (cleanAmountStr.endsWith("."))
                cleanAmountStr = cleanAmountStr.substring(0, cleanAmountStr.length() - 1);
            int newEnd = cleanAmountStr.indexOf('.') + 3;
            if (newEnd > 2 && newEnd < cleanAmountStr.length())
                cleanAmountStr = cleanAmountStr.substring(0, newEnd);

            if (cleanAmountStr.length() < 2 || cleanAmountStr.length() > 10) {
                String mailFile = storeUnparseMail(invoice.emailnumber,"unparsed", folderName, content, datString, parse + "No Amount", filename, "txt");
                addFileToInvoice(invoice, mailFile);
                return false;
            }
            if (containsLetter(cleanAmountStr)) {
                String mailFile = storeUnparseMail(invoice.emailnumber,"unparsed", folderName, content, datString, parse + "Letters", filename, "txt");
                addFileToInvoice(invoice, mailFile);
                return false;
            }
            if (countLetter(cleanAmountStr,'.')>1) {
                String mailFile = storeUnparseMail(invoice.emailnumber,"unparsed", folderName, content, datString, parse + "multiple dots", filename, "txt");
                addFileToInvoice(invoice, mailFile);
                return false;
            }

            double amount = 0.0;
            try {
                amount = Double.parseDouble(cleanAmountStr);
                if (amount > 20000.0) {

                    new RuntimeException("large amount "+amount);
                }
                double cents =Math.round(amount*100);
                if ((cents/100)!=amount)
                    new RuntimeException("too many digits after decimal "+amount);

            } catch (Exception e) {
                System.err.println("oldStr" + amountString);
                System.err.println("newStr" + cleanAmountStr);
                System.err.println("folderName" + folderName);
                System.err.println("content" + content);
                e.printStackTrace();
                String mailFile = storeUnparseMail(invoice.emailnumber,"unparsed", folderName, content, datString, parse + e.getMessage(), filename, "txt");
                addFileToInvoice(invoice, mailFile);
                return false;
            }
            if (amount != 0.0) {
                invoice.amount = amount;
            } else {
                String mailFile = storeUnparseMail(invoice.emailnumber,"unparsed", folderName, content, datString, parse + "0.0 amount", filename, "txt");
                addFileToInvoice(invoice, mailFile);
            }

            String billdate = null;
            billdate = parseField(content, skipLine, "Billing Date:", "Date:",
                    "Rechnungsdatum:", "Payment date:", "was placed on","Statement date:");
            billdate = findEnd(billdate, "and");
            if (billdate != null && billdate.length() > 2 && containsDigit(billdate)) {
                invoice.date = billdate;
            }
            String dueDate = parseField(content, skipLine, "Due Date:", "DUE DATE", "is due on","Total Current Charges Due On",
                    "Payment Due Date", "Payment due date:","TRANSACTION DATE", "Auto Pay Date:",
                    "Received:", "delivered on", "DUE", "AUTO DRAFT DATE:", "APS amount to be applied on","will be charged on");
            if (dueDate != null && dueDate.length() > 2 && !dueDate.contains(cleanAmountStr)) {
                invoice.dueDate = dueDate;
            }
            if (invoice.date == null || invoice.date.equals(invoice.dueDate))
                invoice.date = datString;
            String number = parseField(content, false,"Your invoice","Invoice #:", "Order Number #", "Rechnungsnummer:", "Billing Period:","Plan period",
                    "aktuelle Rechnung", "Invoice no.", "Invoice Number:", "INVOICE", "Bill Period    : ", "Receipt #", "Ihre Rechnung", "Order Number","Order #:");
            number = findEnd(number, "von");
            number = findEnd(number, "vom");
            number = findEnd(number, "als");
            number = findEnd(number, " has ");
            number = findEnd(number, " IS ");
            number = findEnd(number, " Is ");
            if ((invoice.number == null || invoice.number.length() == 0) && number != null) {
                number=number.replace('\n',' ' );
                invoice.number = number;
                if (invoice.number.length()>15){
                    invoice.number=invoice.number.substring(0,14);
                }
            }
            String account = "";
            account = parseField(content, false, "Kundennummer:", "Kundennummer", "Kunden Nr", "Account Number: Ending in", "account number:",
                    "Account Number:", "Account:", "Service Address:", "Account number:", "Your order from", "ordered from", "Beleg für", "CF Number:",
                    "for account number ******","Receipt for Your Payment to","Let ","for account :","Customer #:","account ending in ******","Policy Number");
            account = findEnd(account, "was placed");
            account = findEnd(account, "was delivered");
            account = findEnd(account, "(");
            account = findEnd(account, " is ");
            account = findEnd(account, " know ");

            //account = findEnd(account, "Ending in");
            if (account != null && account.length() > 0) {
                account = account.replace('\n', ' ');
                invoice.account = account;
            }
            invoice.parse += parse;
            //if (filename == null)
            //	filename = invoice.number + ".pdf";
            //invoice.fileName += folderName + "/" + filename+ ";";
            //save(invoice, folderName);


            return invoice.amount != 0.0;
        } catch (Exception e) {
            e.printStackTrace();
            String mailFile = storeUnparseMail(invoice.emailnumber,"unparsed", folderName, content, datString, parse + e.getMessage(), filename, "txt");
            addFileToInvoice(invoice, mailFile);
            return false;
        }
    }

    private static boolean containsDigit(String sample) {
        if (sample==null)
            return false;
        char[] chars = sample.toCharArray();
        for (char c : chars) {
            if (Character.isDigit(c)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsLetter(String sample) {
        char[] chars = sample.toCharArray();
        for (char c : chars) {
            if (Character.isLetter(c)) {
                return true;
            }
        }
        return false;
    }
    private static int countLetter(String sample,char needle) {
        int result =0;
        char[] chars = sample.toCharArray();
        for (char c : chars) {
            if (c==needle) {
                result++;
            }
        }
        return result;
    }
    private static void save(Invoice invoice, String folderName) throws IOException {
        System.out.printf("%5s|%s\n", visitedCount, invoice.toTableString());
        if (invoice.amount == 0.0)
            return;


        File invoices = new File(folderName + "/invoices.tsv");
        // System.out.println("Writing" + invoices.getAbsolutePath());
        if (!invoices.getParentFile().exists()) {
            invoices.getParentFile().mkdirs();
        }
        boolean exists = invoices.exists();
        BufferedWriter out = new BufferedWriter(new FileWriter(invoices, true));
        if (!exists /* || !containsText(invoices, Invoice.header()) */) {
            out.write(Invoice.header());
        }
        // Open given file in append mode by creating an
        // object of BufferedWriter class
        // Writing on output stream
        if (!containsText(invoices, invoice.toString())) {
            out.write(invoice.toString());
        }

        // Closing the connection
        out.close();
    }

    private static boolean containsText(File f, String t) {
        try {
            Scanner scanner = new Scanner(f);

            // now read the file line by line...
            int lineNum = 0;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                lineNum++;
                if (t.contains(line)) {
                    // System.out.println("Line already exists " + lineNum +
                    // line);
                    return true;
                }
            }
        } catch (FileNotFoundException e) {
            return false;
        }
        return false;
    }

    private static String trimLines(String content) {
        String contentSplit[] = content.split("\n");
        for (int i = 0; i < contentSplit.length; i++) {
            contentSplit[i] = contentSplit[i].replace(" ", " ");
            contentSplit[i] = contentSplit[i].trim();
            contentSplit[i] = contentSplit[i].replace(" ", " ");
        }
        StringBuilder cirStringBuilder = new StringBuilder("");
        for (String s : contentSplit) {
            if (!s.equals("") && !s.equals(" ") && !s.equals("  ")) {
                cirStringBuilder.append(s).append(System.getProperty("line.separator"));
            }
        }
        content = cirStringBuilder.toString();
        return content;
    }

    private static String findEnd(String oldStr, String terminator) {
        if (oldStr == null)
            return null;
        if (oldStr.contains(terminator)) {
            int end = oldStr.indexOf(terminator);
            oldStr = oldStr.substring(0, end);
        }
        return oldStr.trim();
    }

    private static String storeUnparseMail(String messageId,String parsed, String folderName, String content, String datString, String parse,
                                           String filename, String filetype) throws IOException {
        File unparsedMail = new File(folderName + "/" + parsed+ "."+messageId + "." + datString + parse + "." + filetype);
        if (!unparsedMail.getParentFile().exists()) {
            unparsedMail.getParentFile().mkdirs();
        }
        if (unparsedMail.exists() && unparsedMail.length() > 100) {
            // accelation in case this was not deleted
            return unparsedMail.getName();
        }
        BufferedWriter outUnparsedMail = new BufferedWriter(new FileWriter(unparsedMail, false));
        //outUnparsedMail.write("--------------------------------------------------------------------------");
        //outUnparsedMail.write("" + parse);
        //outUnparsedMail.write("" + filename);
        //outUnparsedMail.write("--------------------------------------------------------------------------");
        outUnparsedMail.write(content);
        // Closing the connection
        outUnparsedMail.close();
        return unparsedMail.getName();
    }

    private static void parseHTMLInvoice(Invoice invoice, MimeBodyPart msg, String folderName, String filename,
                                         Date date) {
        String content;

        try {
            // System.out.println("parseHTMLInvoice " + folderName);
            content = msg.getContent().toString();
            // System.out.println(content);
            String txt = Jsoup.parse(content).wholeText();
            // System.out.println("txt " + txt);
            boolean success = parseTxt(invoice, folderName, filename, txt, date, "Html");

            if (success) {
                String datString = (date.getMonth() + 1) + "-" + date.getDate();
                String mailFile = storeUnparseMail(invoice.emailnumber, invoice.type.name(), folderName, content, datString, "Html", filename, "html");
                addFileToInvoice(invoice, mailFile);
            }
            return;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return;

    }

    public static void saveParts(Invoice invoice, Object content, String filename, String folderName, Date date)
            throws IOException, MessagingException {
        // System.out.println("... saveParts " + filename + content);
        // System.out.println("... saveParts " +
        // content.getClass().getCanonicalName());
        OutputStream out = null;
        InputStream in = null;
        try {
            if (content instanceof Multipart) {
                Multipart multi = ((Multipart) content);
                int parts = multi.getCount();
                if (parts == 0)
                    System.out.println("... no parts " + content);
                for (int j = 0; j < parts; ++j) {
                    MimeBodyPart part = (MimeBodyPart) multi.getBodyPart(j);
                    //System.out.println("...  part " + part);

                    Object content1 = part.getContent();
                    if (content1 instanceof Multipart) {
                        // part-within-a-part, do some recursion...
                        saveParts(invoice, content1, part.getFileName(), folderName, date);
                    } else {
                        String extension = "";
                        if (part.isMimeType("text/html")||(part.isMimeType("text/plain") && part.getContent().toString().toLowerCase().contains("<html"))) {
                            extension = "html";
                            parseHTMLInvoice(invoice, part, folderName, filename, date);

                        } else {
                            // System.out.println("... not html ");
                            if (part.isMimeType("text/plain")) {
                                // ystem.out.println("... txt ");
                                extension = "txt";
                                parseInvoiceText(invoice, part, folderName, filename, date);

                            } else {
                                // System.out.println("... not txt ");
                                // Try to get the name of the attachment
                                String name = part.getDataHandler().getName();
                                if (name == null) {
                                    //System.out.println("... no name " + part);
                                    //System.out.println("... no name " + part.getContentType());
                                    continue;
                                }
                                extension = MimeUtility.decodeText(name);

                                if ("receipt.pdf".equals(extension)|| "statement.pdf".equals(extension) ||!containsDigit(extension)) {
                                    filename =  "receipt." + (date.getMonth() + 1) + "-" + date.getDay();
                                    filename = folderName + "/" + filename.replace(" ", "") + extension;
                                } else {
                                    filename = folderName + "/" + extension;
                                }
                                // System.out.println("... " + filename);
                                File file = new File(filename);
                                file.getParentFile().mkdirs();
                                addFileToInvoice(invoice, file.getName());
                                if (file.exists()) {
                                    //System.out.println("... exists " + filename);

                                } else {
                                    //System.out.println("... saving " + filename);
                                    downloadedCount++;
                                    out = new FileOutputStream(file);
                                    in = part.getInputStream();
                                    int k;
                                    while ((k = in.read()) != -1) {
                                        out.write(k);
                                    }
                                }


                                if (file.getName().toLowerCase().endsWith(".pdf")) {
                                    File parseCache = new File(file.getParentFile(), file.getName() + ".txt");
                                    String txt = null;
                                    if (parseCache.exists()) {
                                        txt = new String(Files.readAllBytes(Paths.get(parseCache.getAbsolutePath())));
                                    } else {
                                        txt = extractPdf(file);
                                        if (txt != null) {
                                            FileWriter myWriter = new FileWriter(parseCache);
                                            myWriter.write(txt);
                                            myWriter.close();
                                        }
                                    }
                                    if (txt != null) {
                                        parseTxt(invoice, folderName, filename, txt, date, "pdf");
                                    }
                                }
                            }

                        }
                    }
                }
            } else {
                if (content instanceof String) {
                    String txt = Jsoup.parse((String) content).wholeText();

                    // System.out.println("txt " + txt);

                    boolean success = parseTxt(invoice, folderName, filename, txt, date, "String");
                    if (success) {
                        String datString = (date.getMonth() + 1) + "-" + date.getDate();
                        String mailFile = storeUnparseMail(invoice.emailnumber,invoice.type.name(), folderName, txt, datString, "String", filename, "txt");
                        addFileToInvoice(invoice, mailFile);
                    }
                } else if (content instanceof BASE64DecoderStream) {
                    System.out.println("content ");
                    BASE64DecoderStream base64DecoderStream = (BASE64DecoderStream) content;
                    (new File(folderName)).mkdirs();
                    FileOutputStream outStrem = new FileOutputStream(folderName + "/" + filename.replace(' ', '-'));
                    addFileToInvoice(invoice, filename.replace(' ', '-'));
                    int buf;
                    while ((buf = base64DecoderStream.read()) != -1) {
                        outStrem.write(buf);
                    }
                    outStrem.close();
                    base64DecoderStream.close();
                } else {
                    System.err.println("Unknown");
                    System.err.println(content.getClass().getCanonicalName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.flush();
                out.close();
            }
        }
        return;
    }

    private static String extractPdf(File file) {
        PDFParser parser = null;
        PDDocument pdDoc = null;
        PDFTextStripper pdfStripper;


        String parsedText = null;
        try {
            String password ="";
            if (file.getName().contains("EBill_"))
                password ="33837";
            pdDoc = PDDocument.load(file,password);
            pdfStripper = new PDFTextStripper();
            parsedText = pdfStripper.getText(pdDoc);

            //System.out.println(parsedText);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (pdDoc != null)
                    pdDoc.close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }

        }
        return parsedText;
    }

    private static void addFileToInvoice(Invoice i, String filename) {
        if (!filename.toLowerCase().endsWith("pdf") && !filename.toLowerCase().endsWith("txt") && !filename.toLowerCase().endsWith("html"))
            return;
        //brew install wkhtmltopdf
        if (i != null) {
            if (containsDigit(filename) && filename.toLowerCase().endsWith("pdf")) {
                i.fileName = filename;
            } else if (i.fileName==null || i.fileName.contains("unparsed")) {
                i.fileName = filename;
            } else {
                if (!i.fileName.toLowerCase().contains("pdf")) {
                    i.fileName += filename;
                    i.fileName += ";";
                }
            }
            if ((i.number == null || i.number.length() == 0) && containsDigit(filename) && filename.toLowerCase().endsWith("pdf")) {
                i.number = filename.substring(0,filename.length()-4);

            }
        }

    }

    public static String parseField(String haystack, boolean skipALine, String... searchStrings) {
        if (haystack == null)
            return null;
        int start = -1;
        String searchString = "";
        for (String s : searchStrings) {
            start = haystack.indexOf(s);
            if (start > -1) {
                searchString = s;
                break;
            }
        }

        if (start > -1) {
            start = start + searchString.length();
            if (start > haystack.length()) {
                return haystack;
            }
            int end = haystack.indexOf("\n", start + 2);
            if (skipALine) {
                start = end;
                end = haystack.indexOf("\n", end + 2);
            }
            String utterance;
            if (end > start) {
                utterance = haystack.substring(start, end);
            } else {
                utterance = haystack.substring(start);
            }
            String trim = utterance.trim();

            if (trim.length() == 0) {

                utterance = "Not found in " + haystack;
            }

            return trim;

        }
        return null;
    }

    public static void main(String args[]) throws Exception {
        System.out.println("This is Aufbewahrung by Intenovation");
        System.out.println("Usage ./bewahreauf.sh 2023");
        try {
            int yearParam = 0;
            if (args.length>0)
            {
                yearParam =Integer.parseInt(args[0]);
                System.out.println("Parsing all Mails starting from year:"+yearParam);
            }
            //OllamaAPI ollamaAPI = new OllamaAPI();

//            boolean isOllamaServerReachable = ollamaAPI.ping();

            //System.out.println("Is Ollama server running: " + isOllamaServerReachable);
            Aufbewahrung.doit(yearParam);
            //OllamaResult result =
              //      ollamaAPI.generate("llama3.2", "Who are you?",false, new OptionsBuilder().build());

            //System.out.println(result.getResponse());
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println((new Date()));
        System.out.println("total:" + total);
    }
}