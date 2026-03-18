package de.flori.exotico.data;

public class PriceObject {
    public String itemId;
    public String hex;
    public long price;
    public String priceFormatted;
    public Metadata metadata;

    public static class Metadata {
        public int confidence;
        public String volatility;
        public int sources;
    }
}