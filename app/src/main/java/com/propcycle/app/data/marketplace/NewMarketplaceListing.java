package com.propcycle.app.data.marketplace;

/** Validated input used to publish a marketplace listing. */
public final class NewMarketplaceListing {

    private final String title;
    private final String titleNormalized;
    private final String description;
    private final String category;
    private final String condition;
    private final String transactionIntent;
    private final String fulfilmentMethod;
    private final long priceMinor;
    private final String exchangeTerms;
    private final String demoImageKey;

    public NewMarketplaceListing(
            String title,
            String titleNormalized,
            String description,
            String category,
            String condition,
            String transactionIntent,
            String fulfilmentMethod,
            long priceMinor,
            String exchangeTerms) {
        this(title, titleNormalized, description, category, condition, transactionIntent,
                fulfilmentMethod, priceMinor, exchangeTerms, "");
    }

    public NewMarketplaceListing(
            String title,
            String titleNormalized,
            String description,
            String category,
            String condition,
            String transactionIntent,
            String fulfilmentMethod,
            long priceMinor,
            String exchangeTerms,
            String demoImageKey) {
        this.title = title;
        this.titleNormalized = titleNormalized;
        this.description = description;
        this.category = category;
        this.condition = condition;
        this.transactionIntent = transactionIntent;
        this.fulfilmentMethod = fulfilmentMethod;
        this.priceMinor = priceMinor;
        this.exchangeTerms = exchangeTerms;
        this.demoImageKey = demoImageKey;
    }

    public String getTitle() {
        return title;
    }

    public String getTitleNormalized() {
        return titleNormalized;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getCondition() {
        return condition;
    }

    public String getTransactionIntent() {
        return transactionIntent;
    }

    public String getFulfilmentMethod() {
        return fulfilmentMethod;
    }

    public long getPriceMinor() {
        return priceMinor;
    }

    public String getExchangeTerms() {
        return exchangeTerms;
    }

    public String getDemoImageKey() {
        return demoImageKey;
    }
}
