package application;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.*;

public class DeckEditorController {
    public TextField deckNameField;
    public Label ownerLabel;
    public CheckBox publicCheckBox;
    public TableView<Flashcard> cardsTable;
    public TableColumn<Flashcard, String> frontColumn;
    public TableColumn<Flashcard, String> backColumn;
    public TableColumn<Flashcard, String> pictureColumn;
    public TextField frontInput;
    public TextField backInput;
    public Button attachImageButton;
    public Button addCardButton;
    public Button deleteCardButton;
    public ImageView imagePreview;
    public Button returnButton;

    private Deck deck;
    private byte[] stagedImage = null;

    public void initialize() {
        frontColumn.setCellValueFactory(new PropertyValueFactory<>("front"));
        backColumn.setCellValueFactory(new PropertyValueFactory<>("back"));
        
        // Picture column shows "Yes" or "No" based on whether the card has an image
        pictureColumn.setCellValueFactory(cellData -> {
            Flashcard card = cellData.getValue();
            if (card != null && card.getImage() != null && card.getImage().length > 0) {
                return new javafx.beans.property.SimpleStringProperty("Yes");
            } else {
                return new javafx.beans.property.SimpleStringProperty("No");
            }
        });

        cardsTable.setEditable(true);
        frontColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        backColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        
        // Picture column cell factory - shows a small image icon or text
        pictureColumn.setCellFactory(col -> new TableCell<Flashcard, String>() {
            private final ImageView imageView = new ImageView();
            private final Label label = new Label();
            
            {
                imageView.setFitWidth(40);
                imageView.setFitHeight(30);
                imageView.setPreserveRatio(true);
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else if ("Yes".equals(item)) {
                    // Try to show the actual image as thumbnail
                    Flashcard card = getTableView().getItems().get(getIndex());
                    if (card != null && card.getImage() != null) {
                        try {
                            Image img = new Image(new ByteArrayInputStream(card.getImage()), 40, 30, true, true);
                            imageView.setImage(img);
                            setGraphic(imageView);
                            setText(null);
                        } catch (Exception e) {
                            setGraphic(null);
                            setText("Yes");
                        }
                    } else {
                        setGraphic(null);
                        setText("Yes");
                    }
                } else {
                    setGraphic(null);
                    setText("No");
                }
            }
        });

        // Click on picture column to change image
        pictureColumn.setOnEditStart(e -> {
            // Not using edit - handled by row selection + attach button
        });

        frontColumn.setOnEditCommit(e -> {
            Flashcard card = e.getRowValue();
            if (card != null) {
                card.setFront(e.getNewValue());
                saveIfPossible();
            }
        });

        backColumn.setOnEditCommit(e -> {
            Flashcard card = e.getRowValue();
            if (card != null) {
                card.setBack(e.getNewValue());
                saveIfPossible();
            }
        });

        // When a row is selected, show its image in the preview
        cardsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getImage() != null) {
                try {
                    Image img = new Image(new ByteArrayInputStream(newVal.getImage()));
                    imagePreview.setImage(img);
                } catch (Exception e) {
                    imagePreview.setImage(null);
                }
            } else {
                imagePreview.setImage(null);
            }
        });

        addCardButton.setOnAction(e -> addCard());
        deleteCardButton.setOnAction(e -> deleteSelectedCard());
        returnButton.setOnAction(e -> returnToDashboard());
        attachImageButton.setOnAction(e -> openAttachImageChooser());

        // Set placeholder for empty table
        cardsTable.setPlaceholder(new Label("No cards yet. Add one below."));

        // Set column resize policy to fill available space
        cardsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    public void setDeck(Deck deck) {
        this.deck = deck;
        if (deck == null) return;

        deckNameField.setText(deck.getName());
        cardsTable.setItems(deck.getCards());
        if (publicCheckBox != null) {
            publicCheckBox.setSelected(deck.isPublic());
            publicCheckBox.selectedProperty().addListener((obs, oldV, newV) -> {
                deck.setPublic(newV);
                saveIfPossible();
                if (deck.getOwner() == null) deck.setOwner(SessionData.getCurrentUser());
            });
        }

        String owner = deck.getOwner();
        ownerLabel.setText(owner == null ? "Owner: -" : "Owner: " + owner);

        deckNameField.textProperty().addListener((obs, oldV, newV) -> {
            deck.setName(newV);
            saveIfPossible();
        });
    }

    private void addCard() {
        if (deck == null) return;
        String f = frontInput.getText() != null ? frontInput.getText().trim() : "";
        String b = backInput.getText() != null ? backInput.getText().trim() : "";
        if (f.isEmpty() && b.isEmpty() && stagedImage == null) return;
        
        Flashcard card = new Flashcard(f, b);
        if (stagedImage != null) {
            card.setImage(stagedImage);
            stagedImage = null;
            if (imagePreview != null) imagePreview.setImage(null);
        }
        deck.getCards().add(card);
        frontInput.clear();
        backInput.clear();
        saveIfPossible();
        cardsTable.refresh();
    }

    private void deleteSelectedCard() {
        if (deck == null) return;
        Flashcard sel = cardsTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        deck.getCards().remove(sel);
        saveIfPossible();
        cardsTable.refresh();
    }

    private void returnToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/Dashboard.fxml"));
            Parent root = loader.load();
            Main.switchScene(root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openAttachImageChooser() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        File f = chooser.showOpenDialog(null);
        if (f != null && f.exists()) {
            try (InputStream is = new FileInputStream(f);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = is.read(buf)) != -1) baos.write(buf, 0, r);
                byte[] imgData = baos.toByteArray();
                
                // If a card is selected, attach image to it directly
                Flashcard sel = cardsTable.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    sel.setImage(imgData);
                    saveIfPossible();
                    cardsTable.refresh();
                    // Show preview
                    Image img = new Image(new ByteArrayInputStream(imgData));
                    if (imagePreview != null) imagePreview.setImage(img);
                } else {
                    // Stage image for new card
                    stagedImage = imgData;
                    Image img = new Image(new ByteArrayInputStream(stagedImage));
                    if (imagePreview != null) imagePreview.setImage(img);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void saveIfPossible() {
        if (Database.isAvailable()) {
            Database.saveAllDecks(SessionData.getAllDecks());
        }
    }
}