package com.intenovation.invoice;

import java.util.HashSet;
import java.util.Set;

public enum Type {
    Statement,
    Donation("Spendenquittung"),
    Receipt("Beleg","recent payment","Payment confirmation","posted payment","Confirmation"),
    Letter("Brief"),
    Invoice("Rechnung","Bill","Billing"),
    Reminder("Lastschrift fehlgeschlagen"),
    Estimate("Schätzung","Angebot");



    Set<String> synonyms =new HashSet<String>();
    Type(String... synonym) {
        synonyms.add(this.name().toLowerCase());
        for (String s:synonym)
        synonyms.add(s.toLowerCase());

    }
    static Type detectType(String content){
        if (content==null)
            return Letter;
        content=content.toLowerCase();
        for (Type type:Type.values()){
            for (String synonym: type.synonyms) {
                if (content.contains(synonym))
                { return type;}
            }
        }

        return Letter;
    }
}
