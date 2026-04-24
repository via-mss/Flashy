package application;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;

public class AdminController {
    @FXML private ListView<String> usersList;
    @FXML private ListView<String> decksList;
    @FXML private Button banButton;
    @FXML private Button unbanButton;
    @FXML private Button deleteUserButton;
    @FXML private Button editDeckButton;
    @FXML private Button deleteDeckButton;
    @FXML private Button refreshButton;
    @FXML private Button showAllButton;
    @FXML private Button showBannedButton;
    @FXML private Button showActiveButton;
    @FXML private Button closeButton;

    private static final Path USERS_PATH = Paths.get("src/application/users.json");

    @FXML
    public void initialize() {
        if (refreshButton != null) refreshButton.setOnAction(e -> refreshAll());
        if (showAllButton != null) showAllButton.setOnAction(e -> { usersList.getSelectionModel().clearSelection(); loadDecks(); });
        if (showBannedButton != null) showBannedButton.setOnAction(e -> loadUsers(true));
        if (showActiveButton != null) showActiveButton.setOnAction(e -> loadUsers(false));
        if (closeButton != null) closeButton.setOnAction(e -> closeWindow());
        if (banButton != null) banButton.setOnAction(e -> handleBan(true));
        if (unbanButton != null) unbanButton.setOnAction(e -> handleBan(false));
        if (deleteUserButton != null) deleteUserButton.setOnAction(e -> handleDeleteUser());
        if (deleteDeckButton != null) deleteDeckButton.setOnAction(e -> handleDeleteDeck());
        if (editDeckButton != null) editDeckButton.setOnAction(e -> handleEditDeck());

        refreshAll();

        // When an admin selects a user, filter the deck list to only that user's packs
        usersList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;
            String username = extractUsernameFromLine(newV);
            filterDecksForUser(username);
        });
    }

    private void refreshAll() {
        loadUsers();
        loadDecks();
    }

    private void loadUsers() { loadUsers((Boolean) null); }

    private void loadUsers(Boolean bannedFilter) {
        usersList.getItems().clear();
        if (Database.isAvailable()) {
            List<Map<String,Object>> list = Database.listUsers();
            for (Map<String,Object> m : list) {
                String u = (String) m.get("username");
                String n = (String) m.get("name");
                boolean banned = Boolean.TRUE.equals(m.get("banned"));
                if (bannedFilter == null || bannedFilter == banned) {
                    usersList.getItems().add(formatUserLine(u, n, banned));
                }
            }
        } else {
            // file-based
            String content = null;
            try {
                if (!Files.exists(USERS_PATH)) return;
                content = new String(Files.readAllBytes(USERS_PATH), StandardCharsets.UTF_8);
            } catch (IOException ex) { showAlert(AlertType.ERROR, "Data error", "Could not read users file: " + ex.getMessage()); return; }
            if (content.trim().isEmpty()) return;
            // naive parse: find objects
            java.util.regex.Pattern objP = java.util.regex.Pattern.compile("\\{[^}]*?\\}");
            java.util.regex.Matcher mo = objP.matcher(content);
            while (mo.find()) {
                String obj = mo.group();
                java.util.regex.Matcher mu = java.util.regex.Pattern.compile("\"username\"\\s*:\\s*\"([^\"]+)\"").matcher(obj);
                String u = null; String n = null; boolean banned = false;
                if (mu.find()) {
                    u = mu.group(1);
                    java.util.regex.Matcher mn = java.util.regex.Pattern.compile("\"name\"\\s*:\\s*\"([^\"]*)\"").matcher(obj);
                    if (mn.find()) n = mn.group(1);
                    if (obj.contains("\"banned\":true") || obj.contains("\"banned\": 1") || obj.contains("\"banned\":1")) banned = true;
                    if (bannedFilter == null || bannedFilter == banned) {
                        usersList.getItems().add(formatUserLine(u, n, banned));
                    }
                }
            }
        }
    }

    private String formatUserLine(String username, String name, boolean banned) {
        String display = username + (name != null && !name.isEmpty() ? " (" + name + ")" : "");
        if (banned) display += " [BANNED]";
        return display;
    }

    private void loadDecks() {
        decksList.getItems().clear();
        List<Deck> all = SessionData.getAllDecks();
        for (Deck d : all) {
            String owner = d.getOwner();
            String line = d.getName() + (owner != null ? " (" + owner + ")" : "");
            if (d.isBanned()) line += " [BANNED]";
            decksList.getItems().add(line);
        }
    }

    private void filterDecksForUser(String username) {
        decksList.getItems().clear();
        for (Deck d : SessionData.getAllDecks()) {
            if (username.equals(d.getOwner())) {
                String line = d.getName() + (d.getOwner() != null ? " (" + d.getOwner() + ")" : "");
                if (d.isBanned()) line += " [BANNED]";
                decksList.getItems().add(line);
            }
        }
    }

    private void handleBan(boolean ban) {
        String sel = usersList.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert(AlertType.ERROR, "Selection", "Please select a user."); return; }
        String username = extractUsernameFromLine(sel);
        boolean ok = false;
        if (Database.isAvailable()) {
            ok = Database.setUserBanned(username, ban);
        } else {
            ok = updateBannedInFile(username, ban);
        }
        if (ok) {
            showAlert(AlertType.INFORMATION, "Success", (ban?"Banned":"Unbanned") + " user " + username);
            // Also apply ban status to all decks owned by this user
            for (Deck d : SessionData.getAllDecks()) {
                if (username.equals(d.getOwner())) {
                    d.setBanned(ban);
                }
            }
            if (Database.isAvailable()) {
                Database.saveAllDecks(SessionData.getAllDecks());
            }
            // Notify listeners that decks have been updated by replacing the backing list contents
            java.util.List<Deck> snapshot = new java.util.ArrayList<>(SessionData.getAllDecks());
            SessionData.getAllDecks().setAll(snapshot);
            refreshAll();
        } else {
            showAlert(AlertType.ERROR, "Error", "Could not update ban status.");
        }
    }

    private void handleDeleteUser() {
        String sel = usersList.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert(AlertType.ERROR, "Selection", "Please select a user."); return; }
        String username = extractUsernameFromLine(sel);
        boolean ok = false;
        if (Database.isAvailable()) {
            ok = Database.deleteUser(username);
            // reload decks from DB
            if (ok) {
                SessionData.getAllDecks().clear();
                SessionData.getAllDecks().addAll(Database.loadAllDecks());
            }
        } else {
            ok = deleteUserFromFile(username);
            if (ok) {
                // remove decks owned by user from in-memory list
                Iterator<Deck> it = SessionData.getAllDecks().iterator();
                while (it.hasNext()) {
                    Deck d = it.next();
                    if (username.equals(d.getOwner())) it.remove();
                }
            }
        }
        if (ok) { showAlert(AlertType.INFORMATION, "Deleted", "User deleted: " + username); refreshAll(); loadDecks(); }
        else showAlert(AlertType.ERROR, "Error", "Could not delete user.");
    }

    private void handleDeleteDeck() {
        String sel = decksList.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert(AlertType.ERROR, "Selection", "Please select a pack."); return; }
        String deckName = extractDeckNameFromLine(sel);
        Deck toRemove = null;
        for (Deck d : SessionData.getAllDecks()) {
            if (d.getName().equals(deckName)) { toRemove = d; break; }
        }
        if (toRemove == null) { showAlert(AlertType.ERROR, "Error", "Could not find selected deck."); return; }
        SessionData.getAllDecks().remove(toRemove);
        boolean saved = true;
        if (Database.isAvailable()) {
            saved = Database.saveAllDecks(SessionData.getAllDecks());
        }
        if (saved) { showAlert(AlertType.INFORMATION, "Deleted", "Pack deleted: " + deckName); loadDecks(); }
        else showAlert(AlertType.ERROR, "Error", "Could not delete pack.");
    }

    private void handleEditDeck() {
        String sel = decksList.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert(AlertType.ERROR, "Selection", "Please select a pack."); return; }
        String deckName = extractDeckNameFromLine(sel);
        Deck toEdit = null;
        for (Deck d : SessionData.getAllDecks()) {
            if (d.getName().equals(deckName)) { toEdit = d; break; }
        }
        if (toEdit == null) { showAlert(AlertType.ERROR, "Error", "Could not find selected deck."); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/DeckEditor.fxml"));
            Parent root = loader.load();
            DeckEditorController c = loader.getController();
            c.setDeck(toEdit);
            Main.switchScene(root, 900, 600);
        } catch (Exception ex) { ex.printStackTrace(); showAlert(AlertType.ERROR, "Error", "Could not open editor: " + ex.getMessage()); }
    }

    private void closeWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/Dashboard.fxml"));
            Parent root = loader.load();
            Main.switchScene(root, 900, 600);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private String extractUsernameFromLine(String line) {
        // username is before any space or '('
        int idx = line.indexOf(' ');
        int idx2 = line.indexOf('(');
        int cut = -1;
        if (idx == -1) cut = (idx2 == -1 ? line.length() : idx2);
        else if (idx2 == -1) cut = idx; else cut = Math.min(idx, idx2);
        return line.substring(0, cut).trim();
    }

    private String extractDeckNameFromLine(String line) {
        int idx = line.lastIndexOf('(');
        if (idx == -1) return line;
        return line.substring(0, idx).trim();
    }

    // file-based helpers
    private boolean updateBannedInFile(String username, boolean banned) {
        try {
            if (!Files.exists(USERS_PATH)) return false;
            String content = new String(Files.readAllBytes(USERS_PATH), StandardCharsets.UTF_8);
            String key = "\"username\":\"" + username + "\"";
            int idx = content.indexOf(key);
            if (idx == -1) return false;
            int objStart = content.lastIndexOf('{', idx);
            int objEnd = content.indexOf('}', idx);
            if (objStart == -1 || objEnd == -1) return false;
            String obj = content.substring(objStart, objEnd + 1);
            String bannedKey = "\"banned\":";
            String newObj;
            if (obj.contains("\"banned\"")) {
                // replace existing
                newObj = obj.replaceAll("\"banned\"\\s*:\\s*(true|false|0|1)", "\"banned\":" + (banned?"true":"false"));
            } else {
                // insert before final }
                newObj = obj.substring(0, obj.length()-1) + ",\"banned\":" + (banned?"true":"false") + "}";
            }
            String updated = content.substring(0, objStart) + newObj + content.substring(objEnd + 1);
            Files.write(USERS_PATH, updated.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException ex) { return false; }
    }

    private boolean deleteUserFromFile(String username) {
        try {
            if (!Files.exists(USERS_PATH)) return false;
            String content = new String(Files.readAllBytes(USERS_PATH), StandardCharsets.UTF_8);
            // find the object and remove it including surrounding comma
            java.util.regex.Pattern objP = java.util.regex.Pattern.compile("\\{[^}]*?\\}");
            java.util.regex.Matcher mo = objP.matcher(content);
            int start = -1, end = -1;
            while (mo.find()) {
                String obj = mo.group();
                java.util.regex.Matcher mu = java.util.regex.Pattern.compile("\"username\"\\s*:\\s*\"" + java.util.regex.Pattern.quote(username) + "\"").matcher(obj);
                if (mu.find()) { start = mo.start(); end = mo.end(); break; }
            }
            if (start == -1) return false;
            // remove comma handling
            String before = content.substring(0, start).trim();
            String after = content.substring(end).trim();
            String updated;
            if (before.endsWith(",")) {
                // remove trailing comma before object
                updated = content.substring(0, start-1) + after;
            } else if (after.startsWith(",")) {
                // remove leading comma after object
                updated = before + after.substring(1);
            } else {
                updated = before + after;
            }
            // ensure valid array brackets
            updated = updated.replaceAll("\\[,\\s*\\]", "[]");
            Files.write(USERS_PATH, updated.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException ex) { return false; }
    }

    private void showAlert(AlertType type, String title, String message) {
        try { Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(message); a.showAndWait(); }
        catch (Exception ex) { System.err.println(title + ": " + message); }
    }
} // end of class