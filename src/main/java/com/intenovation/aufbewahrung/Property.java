package com.intenovation.aufbewahrung;

import java.util.HashSet;
import java.util.Set;


public enum Property {
    LakeTahoe1("527-524-039-000"),
    Newport1("899-176-43"),
    Newport2("899-125-41"),
    KoOlina1,
    KoOlina2,
    Boedeker1,
    Boedeker2,
    Emscherweg,
    Rehwinkel,
    Benidorm1,
    Davenport1,
    Oakhurst1,
    Indio1,
    Wellington,
    Unknown;

    Set<String> synonyms = new HashSet<String>();

    Property(String... synonym) {
        synonyms.add(this.name().toLowerCase());
        for (String s : synonym)
            synonyms.add(s.toLowerCase());

    }

    static Property detectCity(String content) {
        if (content == null)
            return Unknown;
        content = content.toLowerCase();
        for (Property city : Property.values()) {
            for (String synonym : city.synonyms) {
                if (content.contains(synonym)) {
                    return city;
                }
            }
        }

        return Unknown;
    }
}
