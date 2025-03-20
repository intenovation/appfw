package com.intenovation.aufbewahrung;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Statistic {
    Statistic(String name){
        this.name=name;
    }
    private String name;
    private int dupeCount=0;
    private int actualCount=0;
    private int unparsedCount=0;
    private int highparsedCount=0;
    private BigDecimal amount = BigDecimal.ZERO;
    //private double amountDouble = 0.0;
    private List<Statistic> subStat = new ArrayList<Statistic>();
    //private static HashSet<String> dedupe=new HashSet<String>();
    public boolean addAmount(double amount,String... names) {
        if (amount>30000.0) {
            System.err.println("Crazy Spending" + amount);
            highparsedCount++;
            return true;
        }

        /*if (names.length>4){
            String invoicenumber=names[4];
            if (dedupe.contains(invoicenumber)) {
                System.err.println("Dupe " + invoicenumber);
                return;
            }
            else {
                dedupe.add(invoicenumber);
            }
        }*/
        if (names.length==0 && actualCount==1){
             //System.err.println("Dupe " + name);
            dupeCount++;
             return true;
        }
        if (amount==0.0){
            unparsedCount++;
            return true;
        }
        //amountDouble+=amount;
        //this.amount+=amount;
        if (names.length==0) {
            count(amount);
            return false;
        }
        //if (names[0].equals("32476")){
        //    (new RuntimeException(toString(0))).printStackTrace();
        //}

        String[] subArray= Arrays.copyOfRange(names, 1, names.length);
        for (Statistic sub:subStat){
            if (sub.name.equals(names[0])){
                boolean isDupe =sub.addAmount(amount,subArray);
                if (!isDupe){
                    count(amount);
                }
                return isDupe;
            }
        }
        count(amount);
        Statistic sub=new Statistic(names[0]);
        subStat.add(sub);

        sub.addAmount(amount,subArray);
        return false;
    }

    private void count(double amount) {
        this.actualCount++;
        this.amount=this.amount.add(new BigDecimal(amount));
    }

    public String toString(int indent){
        String result=String.format("Act: %5d Dup:%5d Unp:%5d Hi:%5d Amt:%10.2f ",actualCount,dupeCount,unparsedCount,highparsedCount, amount);
        for (int i=0;i<indent;i++){
            result+="...";
        }

        //result+=String.format("%10.10s",name);
        result+=name;
        result+="\n";
        for (Statistic sub:subStat){
            result+=sub.toString(indent+1);
        }
        return result;
    }
}
