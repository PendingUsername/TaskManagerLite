module com.example.taskmanagerproject {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;  // Add this for FontAwesome icons

    // Allow reflection for JavaFX
    opens com.example.taskmanagerproject to javafx.fxml;

    // Export the main package
    exports com.example.taskmanagerproject;
}
