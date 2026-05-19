package com.smartdx.property.support;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
public class PropertyLedgerRow {

    private Integer rowNo;

    private String propertyKey;

    private Integer version;

    private String area;

    private String address;

    private String propertyType;

    private Long priceJpy;

    private String layout;

    private BigDecimal areaSqm;

    private Integer stationWalkMin;

    private String builtYearMonth;

    private LocalDate listedDate;

    private Integer listedYear;

    private String priorityRank;

    private String title;

    private String description;

    private String mainPhoto;

    private String subPhotos;

    private String layoutPhoto;

    private String docs;

    private final List<String> errors = new ArrayList<>();

    public static PropertyLedgerRow from(int rowNo, Map<String, String> values) {
        PropertyLedgerRow row = new PropertyLedgerRow();
        row.rowNo = rowNo;
        row.propertyKey = trim(values.get("listingId"));
        if (row.propertyKey == null) {
            row.propertyKey = UUID.randomUUID().toString();
        }
        row.version = parseInteger(values.get("version"), "INVALID_VERSION", row.errors);
        row.area = trim(values.get("area"));
        row.address = trim(values.get("address"));
        row.propertyType = trim(values.get("propertyType"));
        row.priceJpy = parseLong(values.get("priceJpy"), "INVALID_PRICE", row.errors);
        row.layout = trim(values.get("layout"));
        row.areaSqm = parseDecimal(values.get("areaSqm"), "INVALID_AREA_SQM", row.errors);
        row.stationWalkMin = parseInteger(values.get("stationWalkMin"), "INVALID_STATION_WALK_MIN", row.errors);
        row.builtYearMonth = trim(values.get("builtYearMonth"));
        row.listedDate = parseDate(values.get("listedDate"), "INVALID_LISTED_DATE", row.errors);
        row.listedYear = parseInteger(values.get("listedYear"), "INVALID_LISTED_YEAR", row.errors);
        row.priorityRank = trim(values.get("priorityRank"));
        row.title = trim(values.get("title"));
        row.description = trim(values.get("description"));
        row.mainPhoto = trim(values.get("mainPhoto"));
        row.subPhotos = trim(values.get("subPhotos"));
        row.layoutPhoto = trim(values.get("layoutPhoto"));
        row.docs = trim(values.get("docs"));
        row.validateRequired();
        return row;
    }

    public List<String> mediaRefs() {
        List<String> refs = new ArrayList<>();
        addRef(refs, mainPhoto);
        splitRefs(subPhotos).forEach(refs::add);
        addRef(refs, layoutPhoto);
        splitRefs(docs).forEach(refs::add);
        return refs;
    }

    private void validateRequired() {
        if (isBlank(area)) {
            errors.add("REQUIRED_AREA");
        }
        if (isBlank(address)) {
            errors.add("REQUIRED_ADDRESS");
        }
        if (isBlank(propertyType)) {
            errors.add("REQUIRED_PROPERTY_TYPE");
        } else if (!List.of("mansion", "house", "land").contains(propertyType)) {
            errors.add("INVALID_PROPERTY_TYPE");
        }
        if (priceJpy == null) {
            errors.add("REQUIRED_PRICE");
        }
        if (isBlank(mainPhoto)) {
            errors.add("REQUIRED_MAIN_PHOTO");
        }
        if (priorityRank != null && !List.of("S", "A", "B", "C").contains(priorityRank)) {
            errors.add("INVALID_PRIORITY_RANK");
        }
    }

    public static List<String> splitRefs(String value) {
        List<String> refs = new ArrayList<>();
        if (isBlank(value)) {
            return refs;
        }
        for (String ref : value.split("[|;]")) {
            String normalized = normalizeDocRef(ref);
            if (!isBlank(normalized)) {
                refs.add(normalized);
            }
        }
        return refs;
    }

    public static String normalizeDocRef(String ref) {
        String trimmed = trim(ref);
        if (trimmed == null) {
            return null;
        }
        int metaIndex = trimmed.indexOf(':');
        return metaIndex >= 0 ? trim(trimmed.substring(0, metaIndex)) : trimmed;
    }

    private static void addRef(List<String> refs, String value) {
        String normalized = normalizeDocRef(value);
        if (!isBlank(normalized)) {
            refs.add(normalized);
        }
    }

    private static Integer parseInteger(String value, String code, List<String> errors) {
        String trimmed = trim(value);
        if (trimmed == null) {
            return null;
        }
        try {
            return Integer.valueOf(trimmed);
        } catch (NumberFormatException e) {
            errors.add(code);
            return null;
        }
    }

    private static Long parseLong(String value, String code, List<String> errors) {
        String trimmed = trim(value);
        if (trimmed == null) {
            return null;
        }
        try {
            return Long.valueOf(trimmed);
        } catch (NumberFormatException e) {
            errors.add(code);
            return null;
        }
    }

    private static BigDecimal parseDecimal(String value, String code, List<String> errors) {
        String trimmed = trim(value);
        if (trimmed == null) {
            return null;
        }
        try {
            return new BigDecimal(trimmed);
        } catch (NumberFormatException e) {
            errors.add(code);
            return null;
        }
    }

    private static LocalDate parseDate(String value, String code, List<String> errors) {
        String trimmed = trim(value);
        if (trimmed == null) {
            return null;
        }
        try {
            return LocalDate.parse(trimmed);
        } catch (RuntimeException e) {
            errors.add(code);
            return null;
        }
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
