package uk.org.tdars.toolbox;

import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import uk.org.tdars.toolbox.surplus.SurplusSaleController;

public class MenuController {
    @FXML private Pane paneRoot;
    @FXML private VBox vbox;

    public void initialize() {
        // Bind size of vbox to top pane
        vbox.prefWidthProperty().bind(paneRoot.widthProperty());
        vbox.prefHeightProperty().bind(paneRoot.heightProperty());
    }

    public void openSurplusSale() {
        SurplusSaleController.switchTo();
    }
}
