package com.intenovation.invoice;

import java.util.HashSet;
import java.util.Set;

/**
 * Enumeration of utility and service types for invoices.
 * Used for categorizing invoices by type of service.
 */
public enum Utility {
    Solar,
    Childcare(),
    Furniture(),
    HOA(),
    Gas("5648412", "648412"),
    Electric("8877-9", "7097-4"),
    Internet("6385", "0104", "Sierratel", "Comcast"),
    Water("404259-109298", "325-4"),
    HomeOwnerInsurance("5004757132", "********57132", "RDIP001634"),
    SpecialRiskInsurance("Fire", "Earthquake", "2570053"),
    PestControl("pest", "Pest"),
    Pool("1923"),
    Trash(),
    StrTax("Avalara"),
    License("128546", "050100", "2020-0777"),
    PropertyTax("Property Tax", "Parcel", "065-290-036-000", "272610-701300-000680", "614510020", "459-11-126", "614-510-020"),
    Gardener("landscaping"),
    WaterFilter("Haugue", "hallswater", "Culligan"),
    Umbrella("A332964758"),
    Boat("********87466"),
    Unknown;

    private Set<String> synonyms = new HashSet<>();
    
    Utility(String... synonym) {
        synonyms.add(this.name().toLowerCase());
        for (String s : synonym)
            synonyms.add(s.toLowerCase());
    }
    
    /**
     * Get the set of synonyms for this utility
     * @return Set of synonyms
     */
    public Set<String> getSynonyms() {
        return synonyms;
    }
    
    /**
     * Detect utility type from content
     * @param content The content to check
     * @return Detected utility or Unknown if not detected
     */
    public static Utility detectUtility(String content) {
        if (content == null)
            return Unknown;
            
        content = content.toLowerCase();
        for (Utility utility : Utility.values()) {
            for (String synonym : utility.synonyms) {
                if (content.contains(synonym)) {
                    return utility;
                }
            }
        }

        return Unknown;
    }
}