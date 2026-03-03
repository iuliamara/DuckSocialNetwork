module com.ubb {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.ubb to javafx.fxml, javafx.base;
    exports com.ubb;
    exports com.ubb.gui;
    exports com.ubb.domain;
    exports com.ubb.repository;
    exports com.ubb.service;
    opens com.ubb.utils.dto to javafx.base;
    opens com.ubb.gui to javafx.base, javafx.fxml;
    exports com.ubb.ui;
    opens com.ubb.domain to javafx.base, javafx.fxml;
}