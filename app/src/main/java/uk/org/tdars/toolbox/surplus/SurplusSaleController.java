package uk.org.tdars.toolbox.surplus;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ResourceBundle;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SurplusSaleController {
    private static Stage stage;
    private static final ResourceBundle messageBundle = ResourceBundle.getBundle("uk.org.tdars.toolbox.surplus.messages");

    /**
     * Switch to the Surplus Sale mode.
     */
    @SneakyThrows(IOException.class)
    public static void switchTo() {
        if (stage != null) {
            stage.close();
            stage = null;
        }

        stage = new Stage();
        Parent root = FXMLLoader.load(SurplusSaleController.class.getResource("surplus.fxml"), messageBundle);
        val scene = new Scene(root);

        stage.setTitle(messageBundle.getString("windowTitle"));
        stage.setScene(scene);
        stage.show();
    }

    @FXML private Label lblStatus;
    @FXML private Tab tabAuction;
    @FXML private Tab tabReconciliation;
    @FXML private Tab tabSalesOverview;
    @FXML private Tab tabPaymentOverview;
    @FXML private MenuItem menuSaveItem;
    @FXML private Label lblSaved;

    // Overview tab
    @FXML private DatePicker auctionDatePicker;

    // Under the Hammer tab
    @FXML private TextField txtAuctionLotNumber;
    @FXML private TextField txtAuctionSeller;
    @FXML private TextField txtAuctionItemDescription;
    @FXML private TextField txtAuctionReservePrice;
    @FXML private TextField txtAuctionBuyer;
    @FXML private TextField txtAuctionHammerPrice;

    /**
     * The file path for the currently opened file.
     */
    private File openFilePath;
    /**
     * The currently loaded datafile, or null.
     */
    private SurplusSaleDatafile datafile = null;
    /**
     * Does this file need saving before continuing?
     */
    private boolean needsSaving = false;

    public void initialize() {
        stage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, event -> {
            closeFile();
        });
        stage.addEventHandler(WindowEvent.WINDOW_HIDING, event -> {
            closeFile();
        });

        txtAuctionSeller.focusedProperty().addListener(observable -> {
            auctionSellerChanged();
        });
    }

    /**
     * Enable or disable the tabs that should only be available once a file is opened.
     * @param fileOpened is there a file currently opened
     */
    private void fileStateEnable(boolean fileOpened) {
        log.debug("fileStateEnabled = {}", fileOpened);
        tabAuction.setDisable(!fileOpened);
        tabReconciliation.setDisable(!fileOpened);
        tabSalesOverview.setDisable(!fileOpened);
        tabPaymentOverview.setDisable(!fileOpened);
        auctionDatePicker.setDisable(!fileOpened);
        menuSaveItem.setDisable(!fileOpened);
    }

    /**
     * Show an error alert, blocking until it is acknowledged
     * @param titleResource the resource for the title
     * @param headerResource the resource for the header
     * @param contentResource the resource for the content
     */
    private void showErrorAlert(@NonNull String titleResource, @NonNull String headerResource, @NonNull String contentResource) {
        val errorAlert = new Alert(AlertType.ERROR);
        errorAlert.initOwner(stage);
        errorAlert.setTitle(messageBundle.getString(titleResource));
        errorAlert.setHeaderText(messageBundle.getString(headerResource));
        errorAlert.setContentText(messageBundle.getString(contentResource));
        errorAlert.showAndWait();
    }

    /**
     * Create a new file. This will show a save file picker and create the blank
     * file, then enable the UI ready for use.
     */
    public void newFile() {
        log.trace("newFile");
        if (!closeFile()) return;

        // Show a save file dialog
        val fileChooser = new FileChooser();
        fileChooser.setTitle(messageBundle.getString("new-file.title"));

        // Optionally, set a default file name and file extension filters
        fileChooser.setInitialFileName("%s.tdarsauction".formatted(messageBundle.getString("new-file.default-name")));
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(messageBundle.getString("file-filter.auction"), "*.tdarsauction"),
            new FileChooser.ExtensionFilter(messageBundle.getString("file-filter.all"), "*.*")
        );

        // Show the dialog and get the chosen file
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                file.createNewFile();
                this.datafile = new SurplusSaleDatafile();
                this.openFilePath = file;
                this.needsSaving = false;
                fileStateEnable(true);
                loadInitialFileData();
            } catch (IOException e) {
                log.error("An error occurred while creating the file.", e);
                showErrorAlert("dialog.failed-save.title", "dialog.failed-save.header", "dialog.failed-save.content");
            }
        }
    }

    /**
     * Open a file that exists. This will show the open file picker, then
     * attempt to load the selected file.
     */
    public void openFile() {
        log.trace("openFile");
        if (!closeFile()) return;

        // Show a save file dialog
        val fileChooser = new FileChooser();
        fileChooser.setTitle(messageBundle.getString("open-file.title"));

        // Optionally, set a default file name and file extension filters
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(messageBundle.getString("file-filter.auction"), "*.tdarsauction"),
            new FileChooser.ExtensionFilter(messageBundle.getString("file-filter.all"), "*.*")
        );

        // Show the dialog and get the chosen file
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                this.datafile = SurplusSaleDatafile.open(file);
                this.openFilePath = file;
                this.needsSaving = false;
                fileStateEnable(true);
                loadInitialFileData();
            } catch (IOException e) {
                log.error("An error occurred while opening the file.", e);
                showErrorAlert("dialog.failed-open.title", "dialog.failed-open.header", "dialog.failed-open.content");
            } catch (NotAnAuctionException e) {
                log.error("The file wasn't an auction file.", e);
                showErrorAlert("dialog.failed-open-wrong-type.title", "dialog.failed-open-wrong-type.header", "dialog.failed-open-wrong-type.content");
            }
        }
    }

    /**
     * Trigger basic data to load from the file into the UI.
     */
    private void loadInitialFileData() {
        auctionDatePicker.setValue(this.datafile.getAuctionDate());
    }

    /**
     * Save the currently opened file.
     */
    public void saveFile() {
        log.trace("saveFile");
        this.datafile.setAuctionDate(auctionDatePicker.getValue());
        try {
            this.datafile.save(openFilePath);
            this.needsSaving = false;
            // Show save label
            val t = new Timeline(
                new KeyFrame(
                    Duration.ZERO,
                    new KeyValue(lblSaved.opacityProperty(), 0.0)
                ),
                new KeyFrame(
                    Duration.seconds(0.4),
                    new KeyValue(lblSaved.opacityProperty(), 1.0)
                ),

                new KeyFrame(
                    Duration.seconds(2.6),
                    new KeyValue(lblSaved.opacityProperty(), 1.0)
                ),
                new KeyFrame(
                    Duration.seconds(3),
                    new KeyValue(lblSaved.opacityProperty(), 0.0)
                )
            );
            t.play();
        } catch (IOException e) {
            log.warn("Failed to save: %s", e.getLocalizedMessage());
            showErrorAlert("dialog.failed-save.title", "dialog.failed-save.header", "dialog.failed-save.content");
        }
    }

    /**
     * Safely close the currently opened file, if any.
     * @return should the triggering flow continue.
     */
    public boolean closeFile() {
        log.trace("closeFile");
        if (this.datafile == null) return true;
        if (!this.needsSaving) {
            this.datafile = null;
            this.openFilePath = null;
            this.needsSaving = false;
            fileStateEnable(false);
            return true;
        }

        log.info("Validating if user wants to save first.");

        // Close safely
        val alert = new Alert(AlertType.CONFIRMATION);
        alert.initOwner(stage);
        alert.setTitle(messageBundle.getString("dialog.save.title"));
        alert.setHeaderText(messageBundle.getString("dialog.save.header"));
        alert.setContentText(messageBundle.getString("dialog.save.content"));

        // Customize button labels for "Save," "Don't Save," and "Cancel"
        val saveButton = new ButtonType(messageBundle.getString("dialog.save.button.save"));
        val dontSaveButton = new ButtonType(messageBundle.getString("dialog.save.button.dont_save"));
        val cancelButton = new ButtonType(messageBundle.getString("dialog.save.button.cancel"));

        alert.getButtonTypes().setAll(saveButton, dontSaveButton, cancelButton);

        val result = alert.showAndWait();
        // Check user's choice
        if (result.isPresent()) {
            val buttonType = result.get();

            if (buttonType == ButtonType.YES) {
                try {
                    datafile.save(openFilePath);
                    this.needsSaving = false;
                } catch (IOException e) {
                    log.warn("Failed to save: %s", e.getLocalizedMessage());
                    showErrorAlert("dialog.failed-save.title", "dialog.failed-save.header", "dialog.failed-save.content");
                    return false;
                }
                this.datafile = null;
                this.openFilePath = null;
                this.needsSaving = false;
                fileStateEnable(false);
            } else if (buttonType == ButtonType.NO) {
                this.datafile = null;
                this.openFilePath = null;
                this.needsSaving = false;
                fileStateEnable(false);
            }
        }
        return false;
    }

    /**
     * Make suggestions for autocompletion and update the lot number as the
     * seller is typed in.
     */
    public void auctionSellerChanged() {
        val currentlyTyped = txtAuctionSeller.textProperty().get();
        if (currentlyTyped.isBlank()) return;

        // Update lot number
        val callsign = currentlyTyped.split(" ")[0];
        val nextNum = this.datafile
            .getItems()
            .keySet()
            .stream()
            .filter(lotNum -> lotNum.startsWith(callsign))
            .count() + 1;
        txtAuctionLotNumber.textProperty().set("%s-%d".formatted(callsign, nextNum));

        // find a suggestion
        val maybeSuggestion = this.datafile.getCallsigns().stream().filter(cs -> cs.startsWith(currentlyTyped)).findFirst();
        if (maybeSuggestion.isPresent()) {
            val sel = txtAuctionSeller.caretPositionProperty().get();
            txtAuctionSeller.textProperty().set(maybeSuggestion.get());
            txtAuctionSeller.positionCaret(sel);
            txtAuctionSeller.selectEnd();
        }
    }

    /**
     * Make suggestions for autocompletion.
     */
    public void auctionBuyerChanged() {
        val currentlyTyped = txtAuctionBuyer.textProperty().get();
        if (currentlyTyped.isBlank()) return;

        // find a suggestion
        val maybeSuggestion = this.datafile.getCallsigns().stream().filter(cs -> cs.startsWith(currentlyTyped)).findFirst();
        if (maybeSuggestion.isPresent()) {
            val sel = txtAuctionBuyer.caretPositionProperty().get();
            txtAuctionBuyer.textProperty().set(maybeSuggestion.get());
            txtAuctionBuyer.positionCaret(sel);
            txtAuctionBuyer.selectEnd();
        }
    }

    /**
     * Save the item as not sold and clear fields
     */
    public void auctionItemNotSold() {
        val item = new SurplusSaleItem();
        item.setLotNumber(txtAuctionLotNumber.textProperty().get());
        item.setSellerCallsign(txtAuctionSeller.textProperty().get());
        item.setItemDescription(txtAuctionItemDescription.textProperty().get());
        try {
            val resv = txtAuctionReservePrice.textProperty().get();
            if (!resv.isBlank()) item.setReservePrice(new BigDecimal(resv));
        } catch (NumberFormatException e) {
            log.warn("invalid reserve price");
        }
        // Save item and callsigns
        this.datafile.getItems().put(item.getLotNumber(), item);
        if (!this.datafile.getCallsigns().contains(item.getSellerCallsign())) {
            this.datafile.getCallsigns().add(item.getSellerCallsign());
        }
        this.needsSaving = true;

        // Clear fields
        txtAuctionLotNumber.textProperty().set("");
        txtAuctionSeller.textProperty().set("");
        txtAuctionItemDescription.textProperty().set("");
        txtAuctionReservePrice.textProperty().set("");
    }

    /**
     * Save the item as sold and clear fields
     */
    public void auctionItemSold() {
        val item = new SurplusSaleItem();
        item.setLotNumber(txtAuctionLotNumber.textProperty().get());
        item.setSellerCallsign(txtAuctionSeller.textProperty().get());
        item.setItemDescription(txtAuctionItemDescription.textProperty().get());
        try {
            val resv = txtAuctionReservePrice.textProperty().get();
            if (!resv.isBlank()) item.setReservePrice(new BigDecimal(resv));
        } catch (NumberFormatException e) {
            log.warn("invalid reserve price");
        }

        item.setBuyerCallsign(txtAuctionBuyer.textProperty().get());
        try {
            val hammer = txtAuctionHammerPrice.textProperty().get();
            if (hammer.isBlank()) throw new NumberFormatException();
            item.setHammerPrice(new BigDecimal(hammer));
        } catch (NumberFormatException e) {
            showErrorAlert("dialog.invalid-hammer-price.title", "dialog.invalid-hammer-price.header", "dialog.invalid-hammer-price.content");
            return;
        }

        // Save item and callsigns
        this.datafile.getItems().put(item.getLotNumber(), item);
        if (!this.datafile.getCallsigns().contains(item.getSellerCallsign())) {
            this.datafile.getCallsigns().add(item.getSellerCallsign());
        }
        if (!this.datafile.getCallsigns().contains(item.getBuyerCallsign())) {
            this.datafile.getCallsigns().add(item.getBuyerCallsign());
        }
        this.needsSaving = true;

        // Clear fields
        txtAuctionLotNumber.textProperty().set("");
        txtAuctionSeller.textProperty().set("");
        txtAuctionItemDescription.textProperty().set("");
        txtAuctionReservePrice.textProperty().set("");
        txtAuctionBuyer.textProperty().set("");
        txtAuctionHammerPrice.textProperty().set("");
    }
}
