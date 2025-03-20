package com.intenovation.invoice;

import java.util.HashSet;
import java.util.Set;

/**
 * Enumeration of cities where properties are located.
 * Used for categorizing invoices by location.
 */
public enum City {
    Hannover("Bödeker"),
    Indio("Riverside", "Indian", "614510020", "050100", "614-510-020", "381-5004757132-04", "5004757132", "********57132"),
    Oakhurst("Madera", "8877-9", "325-4", "065-290-036-000", "2020-0777", "2570053", "5648412", "648412", "emadco"),
    Davenport("Polk", "214 FOGG", "6385", "272610-701300-000680", "128546", "118832", "DWE6316338", "RDIP001634", "1923"),
    Benidorm("Alicante"),
    Timeshares("redweek.com", "899-176-43", "899-125-41", "527-524-039-000"),
    Cupertino("7097-4", "0104"),
    Jose("San Jose", "459-11-126"),
    Unknown;

    private Set<String> synonyms = new HashSet<>();
    
    City(String... synonym) {
        synonyms.add(this.name().toLowerCase());
        for (String s : synonym)
            synonyms.add(s.toLowerCase());
    }
    
    /**
     * Get the set of synonyms for this city
     * @return Set of synonyms
     */
    public Set<String> getSynonyms() {
        return synonyms;
    }
    
    /**
     * Detect city from content
     * @param content The content to check
     * @return Detected city or Unknown if not detected
     */
    public static City detectCity(String content) {
        if (content == null)
            return Unknown;
        
        content = content.toLowerCase();
        for (City city : City.values()) {
            for (String synonym : city.synonyms) {
                if (content.contains(synonym)) {
                    return city;
                }
            }
        }
        
        return Unknown;
    }
}