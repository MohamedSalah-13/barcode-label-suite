package com.mohamed.barcodelabel.batch;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColumnMatcherTest {
    private static final List<String> FIELDS = List.of("product_name", "sku", "price");

    @Test
    void theUserGuidesSampleFileMapsWithoutHelp() {
        // sample-products-quantities.csv: the guide used to tell the user to map these by hand.
        ColumnMapping mapping = ColumnMapping.automatic(FIELDS, List.of("Name", "Code", "Retail Price", "Qty"));
        assertEquals(Map.of("product_name", "Name", "sku", "Code", "price", "Retail Price"), mapping.fields());
        assertEquals("Qty", mapping.quantityColumn());
    }

    @Test
    void arabicHeadersMatchWithOrWithoutTheArticleAndHamza() {
        ColumnMapping mapping = ColumnMapping.automatic(FIELDS, List.of("الصنف", "الباركود", "سعر البيع", "الكمية"));
        assertEquals(Map.of("product_name", "الصنف", "sku", "الباركود", "price", "سعر البيع"), mapping.fields());
        assertEquals("الكمية", mapping.quantityColumn());

        ColumnMapping spelled = ColumnMapping.automatic(FIELDS, List.of("إسم المنتج", "كود", "السِّعر", "كمية"));
        assertEquals(Map.of("product_name", "إسم المنتج", "sku", "كود", "price", "السِّعر"), spelled.fields());
        assertEquals("كمية", spelled.quantityColumn());
    }

    @Test
    void anIdenticalNameBeatsASynonym() {
        // "Code" means sku too, but the column literally called SKU is the better choice.
        assertEquals("SKU", ColumnMapping.automatic(List.of("sku"), List.of("Code", "SKU")).fields().get("sku"));
        assertEquals("Product Name", ColumnMapping.automatic(List.of("product_name"),
                List.of("Name", "Product Name")).fields().get("product_name"));
    }

    @Test
    void sharedWordsMatchFieldsTheSynonymsDoNotKnow() {
        assertEquals(Map.of("expiry_date", "Expiry Date (dd/mm)"),
                ColumnMapping.automatic(List.of("expiry_date"), List.of("Notes", "Expiry Date (dd/mm)")).fields());
        assertEquals(Map.of("customerName", "customer_name"),
                ColumnMapping.automatic(List.of("customerName"), List.of("customer_name")).fields());
    }

    @Test
    void eachColumnFillsOneFieldAtMost() {
        // Both fields mean "name"; the identical one gets the column and the other is left for the user.
        Map<String, String> fields = ColumnMapping.automatic(List.of("product_name", "name"), List.of("Name")).fields();
        assertEquals(Map.of("name", "Name"), fields);
    }

    @Test
    void unrelatedColumnsAreNotGuessed() {
        ColumnMapping mapping = ColumnMapping.automatic(FIELDS, List.of("Column A", "Column B", "Notes"));
        assertTrue(mapping.fields().isEmpty());
        assertNull(mapping.quantityColumn());
    }

    @Test
    void wordsIgnoreCaseSeparatorsCamelCaseAndArabicVariants() {
        assertEquals(List.of("retail", "price"), ColumnMatcher.words("retailPrice"));
        assertEquals(List.of("retail", "price"), ColumnMatcher.words("Retail_Price"));
        assertEquals(List.of("retail", "price"), ColumnMatcher.words(" RETAIL - price "));
        assertEquals(ColumnMatcher.words("اسم"), ColumnMatcher.words("الإسم"));
        assertEquals(ColumnMatcher.words("كميه"), ColumnMatcher.words("الكمية"));
        assertEquals(ColumnMatcher.words("سعر"), ColumnMatcher.words("ســعر"), "tatweel");
        assertEquals(ColumnMatcher.words("مبنى"), ColumnMatcher.words("مبني"));
        assertTrue(ColumnMatcher.words("___").isEmpty());
        assertEquals(0, ColumnMatcher.score("___", "Name"));
    }
}
