module org.comercio.gestionProductos {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires com.almasb.fxgl.all;
    requires static lombok;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires io;
    requires kernel;
    requires layout;
    requires itextpdf;
    requires java.prefs;

    // otras declaraciones
    opens org.comercio.gestionProductos to javafx.fxml;
    opens org.comercio.gestionProductos.models to javafx.base;

    exports org.comercio.gestionProductos;
    exports org.comercio.gestionProductos.models;
    exports org.comercio.gestionProductos.controllers;

}