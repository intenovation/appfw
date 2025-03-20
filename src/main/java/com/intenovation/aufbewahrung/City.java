package com.intenovation.aufbewahrung;

import java.util.HashSet;
import java.util.Set;

public enum City {
    Hannover("Bödeker"),
    Indio("Riverside","Indian","614510020","050100","614-510-020","381-5004757132-04","5004757132","********57132"),


    Oakhurst("Madera","8877-9","325-4","065-290-036-000","2020-0777","2570053","5648412","648412","emadco"),
    Davenport("Polk","214 FOGG","6385","272610-701300-000680","128546","118832","DWE6316338","RDIP001634","1923"),
    Benidorm("Alicante"),
    Timeshares("redweek.com","899-176-43","899-125-41","527-524-039-000"),
    Cupertino("7097-4","0104"),
    Jose("San Jose","459-11-126"),
    Unknown;


    Set<String> synonyms =new HashSet<String>();
    City(String... synonym) {
        synonyms.add(this.name().toLowerCase());
        for (String s:synonym)
            synonyms.add(s.toLowerCase());

    }
    static City detectCity(String content){
        if (content==null)
            return Unknown;
        content=content.toLowerCase();
        for (City city:City.values()){
            for (String synonym: city.synonyms) {
                if (content.contains(synonym))
                { return city;}
            }
        }

        return Unknown;
    }

}
