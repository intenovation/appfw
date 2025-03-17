package com.intenovation.invoice;

public class Invoice {
	String emailId="";
	String city;
	String utility;
	Type type=Type.Letter;
	/*
	Sender of the invoice
	 */
	String email="";
	double amount = 0.0;
	String fileName = "";
	String subject = "";
	int year=0;
	int month=0;
	int day=0;

	String date;
	String dueDate;
	String number = "";
	String account = "";
	String parse = "";

	public static String header() {
		return "ID\tYear\tMonth\tDay\tcity\tutility\ttype\temail\tDate\tDue Date\tSubject\tFile\tInvoice #\tAmount\tAccount\tParse\n";
	}

	public String toTableString() {
		return String.format("%4s|%2s|%2s|%3.3s|%3.3s|%8.8s|%30.30s|%9.9s|%8.8s|%52.52s|%30.30s|%20.20s|%10.2f|%8.8s",year,month,day,city.toString(), utility.toString(),type.toString(),email, date, dueDate,subject, fileName, number, amount, account);
	}

	@Override
	public String toString() {
		return  ""+emailId + "\t" +year + "\t" +month + "\t" +day +"\t" + city +"\t" + utility + "\t" + type + "\t" + email+ "\t" + date + "\t" + dueDate + "\t" +subject + "\t" +fileName + "\t" + number + "\t" + amount + "\t" + account + "\t" + parse
				+ "\n";
	}

}
