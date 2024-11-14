package uk.org.tdars.toolbox;

import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class MenuController {
    private final ResourceBundle messageBundle = ResourceBundle.getBundle("uk.org.tdars.toolbox.messages");
    @FXML private Pane paneRoot;
    @FXML private VBox vbox;
    @FXML private Label lblTitle;
    @FXML private Label lblCredits;
    @FXML private Button btnSurplusSale;

    public void initialize() {
        // Update text
        lblTitle.setText(messageBundle.getString("menuTitle"));
        lblCredits.setText(messageBundle.getString("credits"));
        btnSurplusSale.setText(messageBundle.getString("btnSurplusSale"));

        // Bind size of vbox to top pane
        vbox.prefWidthProperty().bind(paneRoot.widthProperty());
        vbox.prefHeightProperty().bind(paneRoot.heightProperty());
    }
}
