package com.propcycle.app.data.marketplace;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.propcycle.app.data.media.DemoImagePolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Pure-Java validation and stable-ID mapping for the create-listing form. */
public final class MarketplaceListingValidator {

    public static final int MAX_TITLE_LENGTH = 100;
    public static final int MAX_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_EXCHANGE_TERMS_LENGTH = 500;
    public static final long MAX_PRICE_MINOR = 100_000_000L;

    private static final Set<String> CATEGORIES =
            immutableSet(
                    "banner",
                    "decoration",
                    "fabric",
                    "stationery",
                    "craft",
                    "cosplay",
                    "toys",
                    "wood",
                    "electronic",
                    "packaging",
                    "other");
    private static final Set<String> CONDITIONS =
            immutableSet("new", "like_new", "good", "fair", "poor");
    private static final Set<String> TRANSACTION_INTENTS =
            immutableSet("sale", "donation", "exchange");

    private MarketplaceListingValidator() {
    }

    @NonNull
    public static ValidationResult validate(
            @Nullable String rawTitle,
            @Nullable String rawCategory,
            @Nullable String rawCondition,
            @Nullable String rawTransactionIntent,
            @Nullable String rawFulfilmentMethod,
            @Nullable String rawPrice,
            @Nullable String rawExchangeTerms,
            @Nullable String rawDescription) {
        return validate(rawTitle, rawCategory, rawCondition, rawTransactionIntent,
                rawFulfilmentMethod, rawPrice, rawExchangeTerms, rawDescription, "");
    }

    @NonNull
    public static ValidationResult validate(
            @Nullable String rawTitle,
            @Nullable String rawCategory,
            @Nullable String rawCondition,
            @Nullable String rawTransactionIntent,
            @Nullable String rawFulfilmentMethod,
            @Nullable String rawPrice,
            @Nullable String rawExchangeTerms,
            @Nullable String rawDescription,
            @Nullable String rawDemoImageKey) {
        String title = trim(rawTitle);
        String description = trim(rawDescription);
        String category = stableCategoryId(rawCategory);
        String condition = stableConditionId(rawCondition);
        String transactionIntent = stableTransactionIntentId(rawTransactionIntent);
        String fulfilmentMethod = stableFulfilmentMethodId(rawFulfilmentMethod);
        String exchangeTerms = trim(rawExchangeTerms);
        String demoImageKey = DemoImagePolicy.normalize(rawDemoImageKey);

        if (title.length() < 3) {
            return ValidationResult.error("Enter an item name with at least 3 characters.");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            return ValidationResult.error("Item name must be 100 characters or fewer.");
        }
        if (!CATEGORIES.contains(category)) {
            return ValidationResult.error("Choose a valid category.");
        }
        if (!CONDITIONS.contains(condition)) {
            return ValidationResult.error("Choose a valid condition.");
        }
        if (!TRANSACTION_INTENTS.contains(transactionIntent)) {
            return ValidationResult.error("Choose sale, donation, or exchange.");
        }
        if (!"pickup".equals(fulfilmentMethod) && !"meetup".equals(fulfilmentMethod)) {
            return ValidationResult.error("Choose pickup or meet-up.");
        }
        if (description.isEmpty()) {
            return ValidationResult.error("Add a short description.");
        }
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            return ValidationResult.error("Description must be 1000 characters or fewer.");
        }
        if (!DemoImagePolicy.isValid(demoImageKey)) {
            return ValidationResult.error("Choose a valid built-in demo image.");
        }

        long priceMinor = 0L;
        if ("sale".equals(transactionIntent)) {
            PriceResult price = parsePriceMinor(rawPrice);
            if (!price.valid || price.value <= 0L) {
                return ValidationResult.error("Enter a sale price greater than RM 0.00.");
            }
            priceMinor = price.value;
            exchangeTerms = "";
        } else if ("exchange".equals(transactionIntent)) {
            if (exchangeTerms.isEmpty()) {
                return ValidationResult.error("Describe what you would accept in exchange.");
            }
            if (exchangeTerms.length() > MAX_EXCHANGE_TERMS_LENGTH) {
                return ValidationResult.error("Exchange terms must be 500 characters or fewer.");
            }
        } else {
            exchangeTerms = "";
        }

        NewMarketplaceListing listing = new NewMarketplaceListing(
                title,
                normalizeSearchText(title),
                description,
                category,
                condition,
                transactionIntent,
                fulfilmentMethod,
                priceMinor,
                exchangeTerms,
                demoImageKey);
        return ValidationResult.valid(listing);
    }

    @NonNull
    public static String normalizeSearchText(@Nullable String value) {
        return trim(value).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    @NonNull
    public static String stableCategoryId(@Nullable String value) {
        String normalized = normalizeChoice(value);
        return switch (normalized) {
            case "decor", "decorations" -> "decoration";
            case "electronics", "electrical" -> "electronic";
            case "toy" -> "toys";
            default -> normalized;
        };
    }

    @NonNull
    public static String stableConditionId(@Nullable String value) {
        String normalized = normalizeChoice(value);
        return "likenew".equals(normalized) ? "like_new" : normalized;
    }

    @NonNull
    public static String stableTransactionIntentId(@Nullable String value) {
        String normalized = normalizeChoice(value);
        return "donate".equals(normalized) ? "donation" : normalized;
    }

    @NonNull
    public static String stableFulfilmentMethodId(@Nullable String value) {
        String normalized = normalizeChoice(value);
        return switch (normalized) {
            case "meet_up", "meeting", "meet" -> "meetup";
            case "pick_up", "collection" -> "pickup";
            default -> normalized;
        };
    }

    @NonNull
    public static String displayLabel(@Nullable String stableId) {
        String id = trim(stableId);
        if (id.isEmpty()) {
            return "Not specified";
        }
        String spaced = switch (id) {
            case "meetup" -> "meet-up";
            case "electronic" -> "electronics";
            default -> id.replace('_', ' ');
        };
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private static PriceResult parsePriceMinor(@Nullable String rawPrice) {
        String value = trim(rawPrice).replace("RM", "").replace(",", "").trim();
        if (value.isEmpty()) {
            return PriceResult.invalid();
        }
        try {
            BigDecimal amount = new BigDecimal(value).setScale(2, RoundingMode.UNNECESSARY);
            long minor = amount.movePointRight(2).longValueExact();
            return minor >= 0L && minor <= MAX_PRICE_MINOR
                    ? PriceResult.valid(minor)
                    : PriceResult.invalid();
        } catch (ArithmeticException | NumberFormatException invalidPrice) {
            return PriceResult.invalid();
        }
    }

    private static String normalizeChoice(@Nullable String value) {
        return normalizeSearchText(value)
                .replace('-', '_')
                .replace(' ', '_');
    }

    private static String trim(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    private static Set<String> immutableSet(String... values) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(values)));
    }

    private static final class PriceResult {
        private final boolean valid;
        private final long value;

        private PriceResult(boolean valid, long value) {
            this.valid = valid;
            this.value = value;
        }

        private static PriceResult valid(long value) {
            return new PriceResult(true, value);
        }

        private static PriceResult invalid() {
            return new PriceResult(false, 0L);
        }
    }

    public static final class ValidationResult {
        private final NewMarketplaceListing listing;
        private final String errorMessage;

        private ValidationResult(
                @Nullable NewMarketplaceListing listing,
                @Nullable String errorMessage) {
            this.listing = listing;
            this.errorMessage = errorMessage;
        }

        private static ValidationResult valid(@NonNull NewMarketplaceListing listing) {
            return new ValidationResult(listing, null);
        }

        private static ValidationResult error(@NonNull String message) {
            return new ValidationResult(null, message);
        }

        public boolean isValid() {
            return listing != null;
        }

        @Nullable
        public NewMarketplaceListing getListing() {
            return listing;
        }

        @Nullable
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
