package com.mohamed.barcodelabel.model.page;

import com.mohamed.barcodelabel.model.geometry.DimensionsMm;
import com.mohamed.barcodelabel.model.geometry.InsetsMm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PageDefinitionTest {
    @Test
    void acceptsAValidA4Sheet() {
        var layout = new SheetLayout(3, 7, new DimensionsMm(63.5, 38.1), 2.5, 0);
        var page = PageDefinition.a4Sheet(layout, new InsetsMm(15.15, 7.25, 15.15, 7.25));
        assertEquals(21, page.sheetLayout().labelsPerPage());
    }

    @Test
    void rejectsLayoutThatDoesNotFit() {
        var layout = new SheetLayout(3, 7, new DimensionsMm(70, 40), 3, 2);
        assertThrows(IllegalArgumentException.class,
                () -> PageDefinition.a4Sheet(layout, new InsetsMm(10, 10, 10, 10)));
    }
}

