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
	String company = "";
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

	public String getEmailId() {
		return emailId;
	}

	public void setEmailId(String emailId) {
		this.emailId = emailId;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getUtility() {
		return utility;
	}

	public void setUtility(String utility) {
		this.utility = utility;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public int getMonth() {
		return month;
	}

	public void setMonth(int month) {
		this.month = month;
	}

	public int getDay() {
		return day;
	}

	public void setDay(int day) {
		this.day = day;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getDueDate() {
		return dueDate;
	}

	public void setDueDate(String dueDate) {
		this.dueDate = dueDate;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public String getParse() {
		return parse;
	}

	public void setParse(String parse) {
		this.parse = parse;
	}

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
