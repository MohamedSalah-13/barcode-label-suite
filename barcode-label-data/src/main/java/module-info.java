module com.mohamed.barcodelabel.data {
    requires transitive com.mohamed.barcodelabel.model;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    exports com.mohamed.barcodelabel.data;
}

