package application;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class SessionData {
    private static ObservableList<Deck> allDecks = FXCollections.observableArrayList();
    private static ObservableList<Deck> userDecks = FXCollections.observableArrayList();
    private static String currentUser;
    private static String theme = "dark";
    private static int currentScore = 0;
    private static int totalStudied = 0;
    private static final Path SETTINGS_PATH = Paths.get("data/settings.properties");

    static {
        // Load theme from settings file
        loadSettings();
        
        if (Database.init()) {
            ObservableList<Deck> fromDb = Database.loadAllDecks();
            if (fromDb != null && !fromDb.isEmpty()) {
                allDecks.addAll(fromDb);
            }
        }
    }

    public static ObservableList<Deck> getAllDecks() {
        return allDecks;
    }

    public static String getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(String username) {
        currentUser = username;
        currentScore = 0;
        totalStudied = 0;
        if (username != null && Database.isAvailable()) {
            currentScore = Database.getUserScore(username);
            totalStudied = Database.getUserTotalStudied(username);
        }
        // Refresh user decks when user changes
        refreshUserDecks();
    }

    public static int getCurrentScore() {
        return currentScore;
    }

    public static void addScore(int points) {
        currentScore += points;
        if (currentUser != null && Database.isAvailable()) {
            Database.addUserScore(currentUser, points);
        }
    }

    public static int getTotalStudied() {
        return totalStudied;
    }

    public static String getTheme() {
        return theme;
    }

    public static void setTheme(String t) {
        if (t == null || (!t.equals("light") && !t.equals("dark"))) {
            theme = "dark";
        } else {
            theme = t;
        }
        // Save theme to file
        saveSettings();
    }

    public static void logout() {
        currentUser = null;
        currentScore = 0;
        totalStudied = 0;
        userDecks.clear();
    }

    public static void clearSession() {
        logout();
        allDecks.clear();
        userDecks.clear();
    }

    public static ObservableList<Deck> getVisibleDecksForCurrentUser() {
        String currentUser = getCurrentUser();
        ObservableList<Deck> view = FXCollections.observableArrayList();

        for (Deck d : allDecks) {
            // Only show decks that belong to the current user or have been downloaded by them
            if (d.getOwner() != null && d.getOwner().equals(currentUser) && !d.isBanned()) {
                if (!view.contains(d)) view.add(d);
            }
        }
        return view;
    }

    public static Deck createDeck(String name) {
        Deck d = new Deck(name);
        d.setOwner(currentUser);
        allDecks.add(d);
        if (Database.isAvailable()) {
            Database.saveAllDecks(allDecks);
        }
        return d;
    }

    public static void downloadDeck(Deck originalDeck) {
        if (currentUser == null) return;
        
        Deck clone = new Deck(originalDeck.getName());
        clone.setOwner(currentUser);
        clone.setPublic(false);
        clone.setBanned(false);
        
        originalDeck.getCards().forEach(fc -> {
            Flashcard c = new Flashcard(fc.getFront(), fc.getBack());
            c.setImage(fc.getImage());
            clone.getCards().add(c);
        });
        
        allDecks.add(clone);
        if (Database.isAvailable()) {
            Database.saveAllDecks(allDecks);
        }
    }

    public static void reloadDecks() {
        allDecks.clear();
        if (Database.isAvailable()) {
            ObservableList<Deck> fromDb = Database.loadAllDecks();
            if (fromDb != null) {
                allDecks.addAll(fromDb);
            }
        }
        refreshUserDecks();
    }

    private static void refreshUserDecks() {
        userDecks.clear();
        if (currentUser == null) return;
        for (Deck d : allDecks) {
            if (d.getOwner() != null && d.getOwner().equals(currentUser) && !d.isBanned()) {
                userDecks.add(d);
            }
        }
    }

    private static void loadSettings() {
        if (!Files.exists(SETTINGS_PATH)) {
            // Create default settings file
            saveSettings();
            return;
        }
        try {
            java.util.Properties props = new java.util.Properties();
            try (InputStream is = Files.newInputStream(SETTINGS_PATH)) {
                props.load(is);
            }
            String savedTheme = props.getProperty("theme", "dark");
            if (savedTheme.equals("light") || savedTheme.equals("dark")) {
                theme = savedTheme;
            } else {
                theme = "dark";
            }
        } catch (IOException e) {
            theme = "dark";
        }
    }

    private static void saveSettings() {
        try {
            Path parent = SETTINGS_PATH.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            java.util.Properties props = new java.util.Properties();
            props.setProperty("theme", theme);
            try (OutputStream os = Files.newOutputStream(SETTINGS_PATH)) {
                props.store(os, "Flashcard App Settings");
            }
        } catch (IOException e) {
            System.err.println("Could not save settings: " + e.getMessage());
        }
    }
}