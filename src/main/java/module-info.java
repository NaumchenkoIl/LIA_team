module scene_master {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    opens scene_master to javafx.graphics, javafx.fxml;
    opens scene_master.model to javafx.base;
    opens scene_master.manager to javafx.base;
    opens scene_master.reader to javafx.base;
    opens scene_master.calculator to javafx.base;
    opens scene_master.util to javafx.base;

    exports scene_master;
    exports scene_master.model;
    exports scene_master.manager;
    exports scene_master.reader;
    exports scene_master.calculator;
    exports scene_master.util;
}