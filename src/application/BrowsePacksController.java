package application;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class BrowsePacksController {
    public ListView<Deck> packsList;
    public Label packNameLabel;
    public Label packOwnerLabel;
    public Label packCountLabel;
    public Button downloadButton;
    public Button closeButton;
    public TableView<Deck> myPacksTable;
    public TableColumn<Deck, String> nameCol;
    public TableColumn<Deck, String> ownerCol;
    public TableColumn<Deck, Integer> cardsCol;
    public Button editMyPackButton;
    public Button deleteMyPackButton;

    private ObservableList<Deck> publicPacks = FXCollections.observableArrayList();
    private ObservableList<Deck> myPacks = FXCollections.observableArrayList();

    public void initialize() {
        refreshPublicPacks();
        refreshMyPacks();

        packsList.setItems(publicPacks);
        myPacksTable.setItems(myPacks);

        packsList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> updateDetails(newV));

        downloadButton.setOnAction(e -> downloadSelectedPack());
        closeButton.setOnAction(e -> closeWindow());

        javafx.scene.control.Button refreshBtn = null;
        try { refreshBtn = (javafx.scene.control.Button) closeButton.getParent().lookup("#refreshButton"); } catch (Exception ignore) {}
        if (refreshBtn != null) refreshBtn.setOnAction(e -> { refreshPublicPacks(); refreshMyPacks(); });

        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        ownerCol.setCellValueFactory(new PropertyValueFactory<>("owner"));
        cardsCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().getCards().size()).asObject());

        editMyPackButton.setOnAction(e -> editSelectedMyPack());
        deleteMyPackButton.setOnAction(e -> deleteSelectedMyPack());

        SessionData.getAllDecks().addListener((javafx.collections.ListChangeListener.Change<? extends Deck> c) -> { 
            refreshPublicPacks(); 
            refreshMyPacks(); 
        });

        updateDetails(null);
    }

    private void updateDetails(Deck d) {
        if (d == null) {
            packNameLabel.setText("Name:");
            packOwnerLabel.setText("Owner:");
            packCountLabel.setText("Cards:");
            downloadButton.setDisable(true);
        } else {
            packNameLabel.setText("Name: " + d.getName());
            packOwnerLabel.setText("Owner: " + (d.getOwner() == null ? "-" : d.getOwner()));
            packCountLabel.setText("Cards: " + d.getCards().size());
            downloadButton.setDisable(false);
        }
    }

    private void downloadSelectedPack() {
        Deck sel = packsList.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        // Use SessionData to download the deck
        SessionData.downloadDeck(sel);
        showSimpleAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Downloaded", "Pack '" + sel.getName() + "' has been added to your packs.");
        refreshPublicPacks();
        refreshMyPacks();
    }

    private void editSelectedMyPack() {
        Deck sel = myPacksTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/DeckEditor.fxml"));
            Parent root = loader.load();
            DeckEditorController c = loader.getController();
            c.setDeck(sel);
            Main.switchScene(root, 900, 600);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void deleteSelectedMyPack() {
        Deck sel = myPacksTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Delete pack"); 
        a.setHeaderText(null); 
        a.setContentText("Delete pack '" + sel.getName() + "'? This cannot be undone.");
        java.util.Optional<ButtonType> res = a.showAndWait();
        if (!res.isPresent() || res.get() != ButtonType.OK) return;
        
        SessionData.getAllDecks().remove(sel);
        boolean saved = true;
        if (Database.isAvailable()) saved = Database.saveAllDecks(SessionData.getAllDecks());
        
        refreshPublicPacks(); 
        refreshMyPacks();
        
        if (!saved) {
            showSimpleAlert(javafx.scene.control.Alert.AlertType.ERROR, "Error", "Could not delete pack.");
        }
    }

    private void refreshPublicPacks() {
        publicPacks.clear();
        String cur = SessionData.getCurrentUser();
        for (Deck d : SessionData.getAllDecks()) {
            // Show public packs that are not owned by the current user and not banned
            if (d.isPublic() && !d.isBanned() && (d.getOwner() == null || !d.getOwner().equals(cur))) {
                publicPacks.add(d);
            }
        }
    }

    private void refreshMyPacks() {
        myPacks.clear();
        String cur = SessionData.getCurrentUser();
        if (cur == null) return;
        for (Deck d : SessionData.getAllDecks()) {
            if (d.getOwner() != null && d.getOwner().equals(cur)) {
                myPacks.add(d);
            }
        }
    }

    private void showSimpleAlert(javafx.scene.control.Alert.AlertType t, String title, String msg) {
        try { 
            Alert a = new Alert(t); 
            a.setTitle(title); 
            a.setHeaderText(null); 
            a.setContentText(msg); 
            a.showAndWait(); 
        }
        catch (Exception ex) { System.err.println(title + ": " + msg); }
    }

    private void closeWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/Dashboard.fxml"));
            Parent root = loader.load();
            Main.switchScene(root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}