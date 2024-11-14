package uk.org.tdars.toolbox.surplus;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ResourceBundle;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
    private static final BigDecimal CLUB_SALES_FACTOR = new BigDecimal("0.9");

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

    @FXML private Pane paneRoot;
    @FXML private BorderPane borderPane;
    @FXML private TabPane tabPane;
    @FXML private Tab tabOverview;
    @FXML private Tab tabAuction;
    @FXML private Tab tabReconciliation;
    @FXML private Tab tabSalesOverview;
    @FXML private MenuItem menuSaveItem;
    @FXML private Label lblSaved;

    // Overview tab
    @FXML private DatePicker auctionDatePicker;
    @FXML private TableView<AuditEntry> tableAuditLog;
    @FXML private TableColumn<AuditEntry, OffsetDateTime> tblcolAuditLogMoment;
    @FXML private TableColumn<AuditEntry, String> tblcolAuditLog;

    // Under the Hammer tab
    @FXML private TextField txtAuctionLotNumber;
    @FXML private TextField txtAuctionSeller;
    @FXML private TextField txtAuctionItemDescription;
    @FXML private TextField txtAuctionReservePrice;
    @FXML private TextField txtAuctionBuyer;
    @FXML private TextField txtAuctionHammerPrice;

    // Reconciliation
    @FXML private TextField txtReconciliationCallsign;
    @FXML private Label lblStatus;
    @FXML private TableView<SurplusSaleItem> tableReconciliation;
    @FXML private TableColumn<SurplusSaleItem, String> tblcolReconciliationLotNumber;
    @FXML private TableColumn<SurplusSaleItem, String> tblcolReconciliationItemDescription;
    @FXML private TableColumn<SurplusSaleItem, BigDecimal> tblcolReconciliationBoughtAt;
    @FXML private TableColumn<SurplusSaleItem, BigDecimal> tblcolReconciliationSoldFor;
    @FXML private TableColumn<SurplusSaleItem, BigDecimal> tblcolReconciliationLineTotal;

    // Saves overview
    @FXML private TableView<SurplusSaleItem> tableSalesOverview;
    @FXML private TableColumn<SurplusSaleItem, String> tblcolSalesLotNumber;
    @FXML private TableColumn<SurplusSaleItem, String> tblcolSalesItemDescription;
    @FXML private TableColumn<SurplusSaleItem, BigDecimal> tblcolSalesSoldFor;
    @FXML private TableColumn<SurplusSaleItem, String> tblcolSalesSeller;
    @FXML private TableColumn<SurplusSaleItem, String> tblcolSalesBuyer;
    @FXML private TableColumn<SurplusSaleItem, Boolean> tblcolReconciledSeller;
    @FXML private TableColumn<SurplusSaleItem, Boolean> tblcolReconciledBuyer;

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
        stage.addEventHandler(WindowEvent.WINDOW_HIDING, event -> {
            closeFile();
        });

        borderPane.prefWidthProperty().bind(paneRoot.widthProperty());
        borderPane.prefHeightProperty().bind(paneRoot.heightProperty());

        txtAuctionSeller.focusedProperty().addListener(observable -> {
            auctionSellerChanged();
        });
        txtAuctionBuyer.focusedProperty().addListener(observable -> {
            auctionBuyerChanged();
        });
        txtReconciliationCallsign.focusedProperty().addListener(observable -> {
            reconciliationCallsignChanged();
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
        log.trace("loadInitialFileData");
        auctionDatePicker.setValue(this.datafile.getAuctionDate());
        tabChanged(); // trigger reload of tab if needed
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
            log.warn("Failed to save: {}", e.getLocalizedMessage());
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
        log.trace("auctionSellerChanged");
        val currentlyTypedOrig = txtAuctionSeller.textProperty().get();
        if (currentlyTypedOrig.isBlank()) return;

        // Update text to uppercase callsign
        val split = currentlyTypedOrig.split(" ", -1);
        split[0] = split[0].toUpperCase();
        val currentlyTyped = String.join(" ", split);
        val caretPos = txtAuctionSeller.caretPositionProperty().get();
        txtAuctionSeller.textProperty().set(currentlyTyped);
        txtAuctionSeller.positionCaret(caretPos);

        // Update lot number
        val callsign = currentlyTyped.split(" ")[0];
        val nextNum = this.datafile
            .getItems()
            .keySet()
            .stream()
            .filter(lotNum -> lotNum.startsWith("%s-".formatted(callsign)))
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
        log.trace("auctionBuyerChanged");
        val currentlyTypedOrig = txtAuctionBuyer.textProperty().get();
        if (currentlyTypedOrig.isBlank()) return;

        // Update text to uppercase callsign
        val split = currentlyTypedOrig.split(" ", -1);
        split[0] = split[0].toUpperCase();
        val currentlyTyped = String.join(" ", split);
        val caretPos = txtAuctionBuyer.caretPositionProperty().get();
        txtAuctionBuyer.textProperty().set(currentlyTyped);
        txtAuctionBuyer.positionCaret(caretPos);

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
        log.trace("auctionItemNotSold");
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
        item.setReconciledSeller(true);
        item.setReconciledBuyer(true);
        // Save item and callsigns
        this.datafile.getItems().put(item.getLotNumber(), item);
        if (!this.datafile.getCallsigns().contains(item.getSellerCallsign())) {
            this.datafile.getCallsigns().add(item.getSellerCallsign());
        }
        this.datafile.getAuditLog().add(new AuditEntry("Lot %s (%s, %s) did not sell.".formatted(item.getLotNumber(), item.getItemDescription(), item.getSellerCallsign())));
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
        log.trace("auctionItemSold");
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
        this.datafile.getAuditLog().add(new AuditEntry("Lot %s (%s, %s) sold to %s for %s.".formatted(item.getLotNumber(), item.getItemDescription(), item.getSellerCallsign(), item.getBuyerCallsign(), item.getHammerPrice())));
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

    /**
     * Establish if any UI updates are needed as a result of a tab change
     */
    public void tabChanged() {
        log.trace("tabChanged");
        val selectedTab = tabPane.selectionModelProperty().get().getSelectedItem();
        if (selectedTab.equals(tabSalesOverview)) {
            // Update sales table
            log.debug("Updating sales table");
            tblcolSalesLotNumber.setCellValueFactory(new PropertyValueFactory<SurplusSaleItem, String>("lotNumber"));
            tblcolSalesItemDescription.setCellValueFactory(new PropertyValueFactory<SurplusSaleItem, String>("itemDescription"));
            tblcolSalesSoldFor.setCellValueFactory(new PropertyValueFactory<SurplusSaleItem, BigDecimal>("hammerPrice"));
            tblcolSalesSeller.setCellValueFactory(new PropertyValueFactory<SurplusSaleItem, String>("sellerCallsign"));
            tblcolSalesBuyer.setCellValueFactory(new PropertyValueFactory<SurplusSaleItem, String>("buyerCallsign"));
            tblcolReconciledSeller.setCellValueFactory(new PropertyValueFactory<SurplusSaleItem, Boolean>("reconciledSeller"));
            tblcolReconciledBuyer.setCellValueFactory(new PropertyValueFactory<SurplusSaleItem, Boolean>("reconciledBuyer"));

            tableSalesOverview.setItems(FXCollections.observableList(this.datafile.getItems().values().stream().toList()));
            tableSalesOverview.refresh();
        } else if (selectedTab.equals(tabReconciliation)) {
            // Reset callsign and table
            log.debug("Clearing reconciliation callsign");
            txtReconciliationCallsign.textProperty().set("");
            reconciliationCallsignChanged();
        } else if (selectedTab.equals(tabOverview)) {
            log.debug("Updating audit log");
            tblcolAuditLogMoment.setCellValueFactory(new PropertyValueFactory<AuditEntry, OffsetDateTime>("moment"));
            tblcolAuditLog.setCellValueFactory(new PropertyValueFactory<AuditEntry, String>("entry"));
            if (this.datafile == null) {
                tableAuditLog.setItems(null);
            } else {
                tableAuditLog.setItems(FXCollections.observableList(this.datafile.getAuditLog().stream().toList()));
                tableAuditLog.refresh();
            }
        }
    }

    /**
     * Make suggestions for autocompletion and calculate figure for individual.
     */
    public void reconciliationCallsignChanged() {
        log.trace("reconciliationCallsignChanged");
        val currentlyTypedOrig = txtReconciliationCallsign.textProperty().get();
        if (currentlyTypedOrig.isBlank()) {
            lblStatus.textProperty().set(messageBundle.getString("outcome-nothing"));
            tableReconciliation.setItems(null);
            return;
        }

        // Update text to uppercase callsign
        val split = currentlyTypedOrig.split(" ", -1);
        split[0] = split[0].toUpperCase();
        val currentlyTyped = String.join(" ", split);
        val caretPos = txtReconciliationCallsign.caretPositionProperty().get();
        txtReconciliationCallsign.textProperty().set(currentlyTyped);
        txtReconciliationCallsign.positionCaret(caretPos);

        // find a suggestion
        val maybeSuggestion = this.datafile.getCallsigns().stream().filter(cs -> cs.startsWith(currentlyTyped)).findFirst();
        if (maybeSuggestion.isPresent()) {
            val sel = txtReconciliationCallsign.caretPositionProperty().get();
            txtReconciliationCallsign.textProperty().set(maybeSuggestion.get());
            txtReconciliationCallsign.positionCaret(sel);
            txtReconciliationCallsign.selectEnd();
        }

        // Build table for callsign, excluding lots that are already reconciled
        tblcolReconciliationLotNumber.setCellValueFactory(new PropertyValueFactory<SurplusSaleItem, String>("lotNumber"));
        tblcolReconciliationItemDescription.setCellValueFactory(new PropertyValueFactory<SurplusSaleItem, String>("itemDescription"));
        tblcolReconciliationBoughtAt.setCellValueFactory(item -> new ReadOnlyObjectWrapper<>(item.getValue().getBuyerCallsign().equals(currentlyTyped) ? item.getValue().getHammerPrice() : null));
        tblcolReconciliationSoldFor.setCellValueFactory(item -> new ReadOnlyObjectWrapper<>(item.getValue().getSellerCallsign().equals(currentlyTyped) ? item.getValue().getHammerPrice() : null));
        tblcolReconciliationLineTotal.setCellValueFactory(item -> {
            val i = item.getValue();
            val price = i.getHammerPrice();
            var lineTotal = new BigDecimal("0");
            if (i.getBuyerCallsign().equals(currentlyTyped)) {
                lineTotal = lineTotal.subtract(price);
            }
            if (i.getSellerCallsign().equals(currentlyTyped)) {
                lineTotal = lineTotal.add(price.multiply(CLUB_SALES_FACTOR).setScale(2, RoundingMode.HALF_EVEN));
            }
            return new ReadOnlyObjectWrapper<>(lineTotal);
        });

        log.debug("Updating reconciliation table");
        val items = this.datafile.getItems()
            .values()
            .stream()
            .filter(i ->
                (currentlyTyped.equals(i.getBuyerCallsign()) && !i.isReconciledBuyer())
                    || (i.getSellerCallsign().equals(currentlyTyped) & !i.isReconciledSeller()))
            .toList();
        tableReconciliation.setItems(FXCollections.observableList(items));
        tableReconciliation.refresh();

        // Calculate and update figures
        var callsignTotal = new BigDecimal(0);
        for (var i : items) {
            if (currentlyTyped.equals(i.getBuyerCallsign())) {
                callsignTotal = callsignTotal.subtract(i.getHammerPrice());
            }
            if (i.getSellerCallsign().equals(currentlyTyped)) {
                callsignTotal = callsignTotal.add(i.getHammerPrice().multiply(CLUB_SALES_FACTOR).setScale(2, RoundingMode.HALF_EVEN));
            }
        }
        val cmp = callsignTotal.compareTo(BigDecimal.ZERO);
        log.debug("Calculated callsign total: {}", callsignTotal);
        if (cmp == 0) {
            // Nothing to pay
            lblStatus.textProperty().set(messageBundle.getString("outcome-nothing"));
        } else if (cmp > 0) {
            // Club pays callsign
            lblStatus.textProperty().set(messageBundle.getString("outcome-owed").formatted(callsignTotal.abs()));
        } else {
            // Callsign pays club
            lblStatus.textProperty().set(messageBundle.getString("outcome-owes").formatted(callsignTotal.abs()));
        }
    }

    /**
     * Save details of the payment made to this callsign, and the associated lot
     * numbers it reconciles.
     */
    public void reconcile() {
        log.trace("reconcile");
        val currentlyTyped = txtReconciliationCallsign.textProperty().get();
        val items = this.datafile.getItems()
            .values()
            .stream()
            .filter(i ->
                (currentlyTyped.equals(i.getBuyerCallsign()) && !i.isReconciledBuyer())
                    || (i.getSellerCallsign().equals(currentlyTyped) & !i.isReconciledSeller()))
            .toList();
        for (var i : items) {
            if (currentlyTyped.equals(i.getBuyerCallsign())) {
                i.setReconciledBuyer(true);
                this.datafile.getAuditLog().add(new AuditEntry("Buyer %s has been reconciled against lot number %s.".formatted(currentlyTyped, i.getLotNumber())));
                this.datafile.getItems().replace(i.getLotNumber(), i);
            }
            if (i.getSellerCallsign().equals(currentlyTyped)) {
                i.setReconciledSeller(true);
                this.datafile.getAuditLog().add(new AuditEntry("Seller %s has been reconciled against lot number %s.".formatted(currentlyTyped, i.getLotNumber())));
                this.datafile.getItems().replace(i.getLotNumber(), i);
            }
        }
        this.needsSaving = true;
        txtReconciliationCallsign.textProperty().set("");
        reconciliationCallsignChanged();
    }
}
