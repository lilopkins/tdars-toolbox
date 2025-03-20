module uk.org.tdars.toolbox {
    // Export your main package for external use
    exports uk.org.tdars.toolbox;

    // Java
    requires java.desktop;

    // JavaFX modules required by your project
    requires transitive javafx.controls;
    requires javafx.fxml;

    // Open all packages for JavaFX FXML controllers
    opens uk.org.tdars.toolbox;
    opens uk.org.tdars.toolbox.surplus;

    // Lombok
    requires static lombok;

    // SLF4J and logging libraries
    requires org.slf4j;
    requires ch.qos.reload4j;

    // JOptSimple
    requires joptsimple;

    // Apache POI
    requires org.apache.poi.ooxml;
}
