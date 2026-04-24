package application;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.geometry.Pos;

import java.util.List;

public class DashboardController {
    public HBox cardsContainer;
    public Button prevDecksButton;
    public Button nextDecksButton;
    public Button studyButton;
    public Button editButton;
    public Button createButton;
    public Button browseButton;
    public Button settingsButton;
    public Button adminButton;

    private int offset = 0;
    private Deck selectedDeck = null;
    private static final int CARDS_PER_PAGE = 3;

    public void initialize() {
        prevDecksButton.setOnAction(e -> scrollPrevious());
        nextDecksButton.setOnAction(e -> scrollNext());

        studyButton.setOnAction(e -> openStudyForSelected());
        editButton.setOnAction(e -> openEditorForSelected());
        createButton.setOnAction(e -> createNewDeck());
        browseButton.setOnAction(e -> openBrowsePacks());
        settingsButton.setOnAction(e -> openSettings());

        String cur = SessionData.getCurrentUser();
        adminButton.setVisible(cur != null && "admin".equals(cur));
        adminButton.setManaged(cur != null && "admin".equals(cur));
        adminButton.setOnAction(e -> openAdmin());

        updateActionButtons();

        SessionData.getAllDecks().addListener((javafx.collections.ListChangeListener.Change<? extends Deck> c) -> refreshCards());

        refreshCards();
    }

    private void refreshCards() {
        cardsContainer.getChildren().clear();
        List<Deck> visible = SessionData.getVisibleDecksForCurrentUser();
        int total = visible.size();

        if (total == 0) {
            Label empty = new Label("You have no packs yet - browse or create one");
            empty.getStyleClass().add("empty-message");
            empty.setPrefWidth(400);
            cardsContainer.getChildren().add(empty);
            prevDecksButton.setDisable(true);
            nextDecksButton.setDisable(true);
            selectedDeck = null;
            updateActionButtons();
            return;
        }

        if (offset > Math.max(0, total - 1)) offset = Math.max(0, total - 1);

        int cardsToShow = Math.min(CARDS_PER_PAGE, total - offset);

        for (int i = 0; i < cardsToShow; i++) {
            int idx = offset + i;
            Deck d = visible.get(idx);
            Node card = createDeckCard(d, idx);
            cardsContainer.getChildren().add(card);
        }

        prevDecksButton.setDisable(offset <= 0);
        nextDecksButton.setDisable(offset + cardsToShow >= total);

        // Auto-select first deck if nothing selected
        if (selectedDeck == null && !visible.isEmpty()) {
            int firstIndex = offset;
            if (firstIndex < visible.size()) {
                Deck first = visible.get(firstIndex);
                for (Node n : cardsContainer.getChildren()) {
                    if (n instanceof VBox) {
                        VBox v = (VBox) n;
                        if (v.getChildren().size() > 0 && v.getChildren().get(0) instanceof Label) {
                            Label lbl = (Label) v.getChildren().get(0);
                            if (lbl.getText().equals(first.getName())) {
                                setSelectedDeck(first, v);
                                break;
                            }
                        }
                    }
                }
            }
        }
        updateActionButtons();
    }

    private Node createDeckCard(Deck deck, int index) {
        VBox card = new VBox();
        card.setSpacing(4);
        card.setPrefWidth(220);
        card.setMinWidth(150);
        card.setPrefHeight(100);
        card.setMaxHeight(100);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("deck-card");
        card.setPickOnBounds(true);

        Label name = new Label(deck.getName());
        name.getStyleClass().add("card-label");
        name.setWrapText(true);
        name.setStyle("-fx-font-size: 16px;");

        Label count = new Label(String.format("%d cards", deck.getCards().size()));
        count.getStyleClass().add("progress-label");

        String cur = SessionData.getCurrentUser();
        String owner = deck.getOwner();
        Label ownerLabel = new Label("Shared by: " + (owner == null ? "-" : (owner.equals(cur) ? "You" : owner)));
        ownerLabel.getStyleClass().add("owner-label");
        ownerLabel.setWrapText(true);
        ownerLabel.setStyle("-fx-font-size: 11px;");

        card.getChildren().addAll(name, count, ownerLabel);

        card.setOnMouseClicked((MouseEvent e) -> {
            setSelectedDeck(deck, card);
            System.out.println("Card clicked: " + deck.getName());
            if (e.getClickCount() == 2) {
                openStudyForSelected();
            }
        });

        if (deck.equals(selectedDeck)) {
            card.getStyleClass().add("selected-card");
        }

        return card;
    }

    private void setSelectedDeck(Deck deck, Node cardNode) {
        this.selectedDeck = deck;
        cardsContainer.getChildren().forEach(n -> n.getStyleClass().removeAll("selected-card"));
        if (cardNode != null && !cardNode.getStyleClass().contains("selected-card")) {
            cardNode.getStyleClass().add("selected-card");
        }
        updateActionButtons();
    }

    private void updateActionButtons() {
        boolean hasSelection = selectedDeck != null;
        studyButton.setDisable(!hasSelection);

        boolean canEdit = false;
        if (hasSelection) {
            String cur = SessionData.getCurrentUser();
            canEdit = (selectedDeck.getOwner() != null && selectedDeck.getOwner().equals(cur)) || "admin".equals(cur);
        }
        editButton.setDisable(!canEdit);
    }

    private Deck getSelectedDeck() {
        return selectedDeck;
    }

    private void openStudyForSelected() {
        Deck selected = getSelectedDeck();
        if (selected == null || selected.getCards().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No cards", "This deck has no cards to study.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/StudyMode.fxml"));
            Parent root = loader.load();
            StudyModeController c = loader.getController();
            c.setDeck(selected);
            Main.switchScene(root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openEditorForSelected() {
        Deck selected = getSelectedDeck();
        if (selected == null) return;
        openEditorForDeck(selected);
    }

    private void openEditorForDeck(Deck deck) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/DeckEditor.fxml"));
            Parent root = loader.load();
            DeckEditorController c = loader.getController();
            c.setDeck(deck);
            Main.switchScene(root, 900, 600);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void createNewDeck() {
        try {
            Deck newDeck = SessionData.createDeck("New Deck");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/DeckEditor.fxml"));
            Parent root = loader.load();
            DeckEditorController c = loader.getController();
            c.setDeck(newDeck);
            Main.switchScene(root, 900, 600);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openBrowsePacks() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/BrowsePacks.fxml"));
            Parent root = loader.load();
            Main.switchScene(root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/Settings.fxml"));
            Parent root = loader.load();
            Main.switchScene(root, 800, 500);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/Admin.fxml"));
            Parent root = loader.load();
            Main.switchScene(root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void scrollNext() {
        offset += CARDS_PER_PAGE;
        refreshCards();
    }

    private void scrollPrevious() {
        offset = Math.max(0, offset - CARDS_PER_PAGE);
        refreshCards();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        try {
            Alert a = new Alert(type);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(msg);
            a.showAndWait();
        } catch (Exception ex) {
            System.err.println(title + ": " + msg);
        }
    }
}