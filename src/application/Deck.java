package application;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Deck {
    private int id; // optional persisted id
    private String name;
    private ObservableList<Flashcard> cards = FXCollections.observableArrayList();

    private String owner; // username of owner
    private boolean isPublic; // whether this deck is shared/visible to other users
    private boolean banned; // whether this deck is banned (hidden)

    public Deck(String name) {
        this.name = name;
        this.owner = null;
        this.isPublic = false;
        this.banned = false;
    }

    public Deck(String name, ObservableList<Flashcard> cards) {
        this.name = name;
        this.cards = cards;
        this.owner = null;
        this.isPublic = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ObservableList<Flashcard> getCards() {
        return cards;
    }

    public void setCards(ObservableList<Flashcard> cards) {
        this.cards = cards;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    @Override
    public String toString() {
        return name;
    }
}