package com.intenovation.invoice;

import java.util.HashSet;
import java.util.Set;

/**
 * Enumeration of properties managed by the company.
 * Used for categorizing invoices by property.
 */
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

    private Set<String> synonyms = new HashSet<>();

    Property(String... synonym) {
        synonyms.add(this.name().toLowerCase());
        for (String s : synonym)
            synonyms.add(s.toLowerCase());
    }

    /**
     * Get the set of synonyms for this property
     * @return Set of synonyms
     */
    public Set<String> getSynonyms() {
        return synonyms;
    }

    /**
     * Detect property from content
     * @param content The content to check
     * @return Detected property or Unknown if not detected
     */
    public static Property detectProperty(String content) {
        if (content == null)
            return Unknown;
            
        content = content.toLowerCase();
        for (Property property : Property.values()) {
            for (String synonym : property.synonyms) {
                if (content.contains(synonym)) {
                    return property;
                }
            }
        }

        return Unknown;
    }
}