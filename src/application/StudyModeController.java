package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.io.ByteArrayInputStream;
import java.util.*;

public class StudyModeController {
    @FXML private StackPane cardPane;
    @FXML private Label cardLabel;
    @FXML private Label progressLabel;
    @FXML private ImageView cardImageView;
    @FXML private Button knownButton, somewhatButton, notKnownButton, endButton;
    @FXML private VBox cardContentVBox;

    private Deck deck;
    private List<Flashcard> remainingPool = new ArrayList<>();
    private Set<Flashcard> studiedCards = new HashSet<>();
    private Flashcard currentCard;
    private int desiredCount = 15;
    private boolean showingBack = false;
    private boolean sessionEnded = false;
    private int timesReviewed = 0;

    public void initialize() {
        cardPane.setPickOnBounds(true);
        cardPane.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> flipCard());
        
        knownButton.setPickOnBounds(true);
        somewhatButton.setPickOnBounds(true);
        notKnownButton.setPickOnBounds(true);
        endButton.setPickOnBounds(true);
        
        knownButton.setOnAction(e -> markKnown());
        somewhatButton.setOnAction(e -> markSomewhat());
        notKnownButton.setOnAction(e -> markNotKnown());
        endButton.setOnAction(e -> endStudy());
        
        cardLabel.setPickOnBounds(true);
    }

    public void setDeck(Deck deck) {
        this.deck = deck;
        int total = deck == null ? 0 : deck.getCards().size();
        if (total == 0) {
            desiredCount = 0;
            prepareStudySession();
            updateView();
            return;
        }
        
        TextInputDialog d = new TextInputDialog(Integer.toString(Math.min(15, total)));
        d.setTitle("Study options");
        d.setHeaderText("Choose number of cards to study");
        d.setContentText("How many cards? (1 - " + total + ")");
        Optional<String> res = d.showAndWait();
        if (res.isPresent()) {
            try { 
                desiredCount = Integer.parseInt(res.get().trim()); 
            } catch (NumberFormatException ex) { 
                desiredCount = Math.min(15, total); 
            }
            // Clamp between 1 and total
            desiredCount = Math.max(1, Math.min(desiredCount, total));
        }
        prepareStudySession();
        updateView();
    }

    private void prepareStudySession() {
        remainingPool.clear();
        studiedCards.clear();
        currentCard = null;
        sessionEnded = false;
        showingBack = false;
        timesReviewed = 0;
        
        if (deck == null || deck.getCards().isEmpty()) return;
        
        int total = deck.getCards().size();
        int m = Math.max(1, Math.min(desiredCount, total));
        
        // Shuffle all cards and pick exactly the requested amount
        List<Flashcard> shuffled = new ArrayList<>(deck.getCards());
        Collections.shuffle(shuffled);
        
        for (int i = 0; i < m; i++) {
            Flashcard fc = shuffled.get(i);
            fc.setFamiliarity(0);
            remainingPool.add(fc);
        }
        
        selectNextCard();
    }

    private void selectNextCard() {
        if (remainingPool.isEmpty() || sessionEnded) {
            currentCard = null;
            return;
        }
        
        // Pick a random card from remaining pool (no repeats)
        int idx = new Random().nextInt(remainingPool.size());
        currentCard = remainingPool.get(idx);
        showingBack = false;
    }

    private void updateView() {
        if (deck == null || deck.getCards().isEmpty() || currentCard == null) {
            cardLabel.setText("No cards in this deck.");
            progressLabel.setText("0/0");
            cardImageView.setImage(null);
            return;
        }
        
        cardLabel.setText(showingBack ? currentCard.getBack() : currentCard.getFront());
        
        int studiedCount = studiedCards.size();
        progressLabel.setText(String.format("Card %d/%d", studiedCount, desiredCount));
        
        if (currentCard.getImage() != null) {
            Image img = new Image(new ByteArrayInputStream(currentCard.getImage()));
            cardImageView.setImage(img);
        } else {
            cardImageView.setImage(null);
        }
    }

    private void flipCard() {
        if (currentCard == null || sessionEnded) return;
        showingBack = !showingBack;
        if (showingBack) {
            cardLabel.setText(currentCard.getBack());
        } else {
            cardLabel.setText(currentCard.getFront());
        }
    }

    private void markKnown() {
        if (currentCard == null || sessionEnded) return;
        currentCard.markKnown();
        moveToNextCard();
    }

    private void markSomewhat() {
        if (currentCard == null || sessionEnded) return;
        currentCard.markSomewhat();
        moveToNextCard();
    }

    private void markNotKnown() {
        if (currentCard == null || sessionEnded) return;
        currentCard.markNotKnown();
        moveToNextCard();
    }
    
    private void moveToNextCard() {
        if (currentCard == null || sessionEnded) return;
        
        // Add current card to studied set
        studiedCards.add(currentCard);
        
        // Remove from remaining pool (no repeats)
        remainingPool.remove(currentCard);
        
        // Update progress before selecting next card
        updateView();
        
        // Check if we've studied enough cards
        if (studiedCards.size() >= desiredCount || remainingPool.isEmpty()) {
            sessionEnded = true;
            javafx.application.Platform.runLater(() -> showStudyComplete());
            return;
        }
        
        // Select next card
        selectNextCard();
        updateView();
    }
    
    private void showStudyComplete() {
        int score = calculateSessionScore();
        SessionData.addScore(score);
        
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Study Complete!");
        a.setHeaderText("Great job!");
        a.setContentText(String.format(
            "You've completed this study session!\n\n" +
            "Cards studied: %d\n" +
            "Session score: +%d points\n" +
            "Total score: %d points",
            studiedCards.size(), score, SessionData.getCurrentScore()
        ));
        a.showAndWait();
        endStudy();
    }
    
    private int calculateSessionScore() {
        int score = 0;
        for (Flashcard fc : studiedCards) {
            int fam = fc.getFamiliarity();
            if (fam <= -1) score += 10;
            else if (fam == 0) score += 5;
            else score += 2;
        }
        return score;
    }

    private void endStudy() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/Dashboard.fxml"));
            Parent root = loader.load();
            Main.switchScene(root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}