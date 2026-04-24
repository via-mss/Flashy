package application;

import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettingsController {
    public ComboBox<String> themeCombo;
    public TextField displayNameField;
    public Button saveNameButton;
    public PasswordField currentPasswordField;
    public PasswordField newPasswordField;
    public PasswordField confirmPasswordField;
    public Button updatePasswordButton;
    public Button closeButton;
    public Button logoutButton;

    private static final Path USERS_PATH = Paths.get("src/application/users.json");

    public void initialize() {
        // Setup theme combo
        themeCombo.getItems().clear();
        themeCombo.getItems().addAll("dark", "light");

        String currentTheme = SessionData.getTheme();
        if (currentTheme == null || (!currentTheme.equals("light") && !currentTheme.equals("dark"))) {
            currentTheme = "dark";
            SessionData.setTheme("dark");
        }
        themeCombo.setValue(currentTheme);

        themeCombo.setEditable(false);

        themeCombo.setOnAction(e -> {
            String selected = themeCombo.getSelectionModel().getSelectedItem();
            if (selected != null && (selected.equals("light") || selected.equals("dark"))) {
                SessionData.setTheme(selected);
                Main.applyThemeToMainStage();
            }
        });

        logoutButton.setOnAction(e -> {
            try {
                SessionData.clearSession();
                Stage stage = (Stage) logoutButton.getScene().getWindow();
                stage.close();
                Parent loginRoot = FXMLLoader.load(getClass().getResource("/application/LoginWindow.fxml"));
                Main.switchScene(loginRoot, 650, 500);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        closeButton.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/Dashboard.fxml"));
                Parent root = loader.load();
                Main.switchScene(root, 900, 600);
            } catch (Exception ex) { 
                ex.printStackTrace(); 
            }
        });

        saveNameButton.setOnAction(e -> handleSaveName());
        updatePasswordButton.setOnAction(e -> handleChangePassword());

        // Load current display name
        String cur = SessionData.getCurrentUser();
        String name = null;
        if (cur != null) {
            if (Database.isAvailable()) {
                try { name = Database.getName(cur); } catch (Exception ex) { name = null; }
            } else {
                name = loadNameFromFile(cur);
            }
        }
        displayNameField.setText(name == null ? "" : name);
    }

    private void handleSaveName() {
        String cur = SessionData.getCurrentUser();
        if (cur == null) { showAlert(AlertType.ERROR, "Not logged in", "No user is currently logged in."); return; }
        String newName = displayNameField == null ? null : displayNameField.getText();
        boolean ok = false;
        try {
            if (Database.isAvailable()) {
                ok = Database.updateName(cur, newName);
            } else {
                ok = updateNameInFile(cur, newName);
            }
        } catch (Exception ex) {
            showAlert(AlertType.ERROR, "Error", "Could not update name: " + ex.getMessage());
            return;
        }
        if (ok) showAlert(AlertType.INFORMATION, "Saved", "Display name updated.");
        else showAlert(AlertType.ERROR, "Save failed", "Could not update display name.");
    }

    private void handleChangePassword() {
        String curUser = SessionData.getCurrentUser();
        if (curUser == null) { showAlert(AlertType.ERROR, "Not logged in", "No user is currently logged in."); return; }
        String current = currentPasswordField == null ? null : currentPasswordField.getText();
        String np = newPasswordField == null ? null : newPasswordField.getText();
        String conf = confirmPasswordField == null ? null : confirmPasswordField.getText();
        if (current == null || current.isEmpty()) { showAlert(AlertType.ERROR, "Password error", "Please enter your current password."); return; }
        if (np == null || np.length() < 6) { showAlert(AlertType.ERROR, "Password error", "New password must be at least 6 characters."); return; }
        if (!np.equals(conf)) { showAlert(AlertType.ERROR, "Password error", "New password and confirmation do not match."); return; }

        try {
            String stored;
            if (Database.isAvailable()) stored = Database.getPasswordHash(curUser);
            else stored = loadPasswordFromFile(curUser);

            boolean okVerify = false;
            if (stored != null && stored.matches("^\\d+:[A-Za-z0-9+/=]+:[A-Za-z0-9+/=]+$")) {
                okVerify = PasswordUtils.verifyPassword(current, stored);
            } else {
                okVerify = (stored != null && stored.equals(current));
            }

            if (!okVerify) { showAlert(AlertType.ERROR, "Authentication failed", "Current password is incorrect."); return; }

            String newHash = PasswordUtils.createHash(np);
            boolean updated = false;
            if (Database.isAvailable()) {
                updated = Database.updatePassword(curUser, newHash);
            } else {
                updated = updatePasswordInFile(curUser, newHash);
            }

            if (updated) showAlert(AlertType.INFORMATION, "Password updated", "Your password was updated successfully.");
            else showAlert(AlertType.ERROR, "Update failed", "Could not update password.");
        } catch (Exception ex) {
            showAlert(AlertType.ERROR, "Error", "Could not hash new password: " + ex.getMessage());
        }
    }

    private String loadNameFromFile(String username) {
        if (!Files.exists(USERS_PATH)) return null;
        try {
            String content = new String(Files.readAllBytes(USERS_PATH), StandardCharsets.UTF_8);
            int idx = content.indexOf("\"username\":\"" + username + "\"");
            if (idx == -1) return null;
            int objStart = content.lastIndexOf('{', idx);
            int objEnd = content.indexOf('}', idx);
            if (objStart == -1 || objEnd == -1) return null;
            String obj = content.substring(objStart, objEnd + 1);
            String nameKey = "\"name\":\"";
            int ni = obj.indexOf(nameKey);
            if (ni == -1) return null;
            int vstart = ni + nameKey.length();
            int vend = obj.indexOf('"', vstart);
            if (vend == -1) return null;
            return unescapeJson(obj.substring(vstart, vend));
        } catch (Exception e) {
            return null;
        }
    }

    private String loadPasswordFromFile(String username) {
        if (!Files.exists(USERS_PATH)) return null;
        try {
            String content = new String(Files.readAllBytes(USERS_PATH), StandardCharsets.UTF_8);
            int idx = content.indexOf("\"username\":\"" + username + "\"");
            if (idx == -1) return null;
            int objStart = content.lastIndexOf('{', idx);
            int objEnd = content.indexOf('}', idx);
            if (objStart == -1 || objEnd == -1) return null;
            String obj = content.substring(objStart, objEnd + 1);
            String passKey = "\"password\":\"";
            int pi = obj.indexOf(passKey);
            if (pi == -1) return null;
            int vstart = pi + passKey.length();
            int vend = obj.indexOf('"', vstart);
            if (vend == -1) return null;
            return unescapeJson(obj.substring(vstart, vend));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean updateNameInFile(String username, String newName) {
        if (!Files.exists(USERS_PATH)) return false;
        try {
            String content = new String(Files.readAllBytes(USERS_PATH), StandardCharsets.UTF_8);
            int idx = content.indexOf("\"username\":\"" + username + "\"");
            if (idx == -1) return false;
            int objStart = content.lastIndexOf('{', idx);
            int objEnd = content.indexOf('}', idx);
            if (objStart == -1 || objEnd == -1) return false;
            String obj = content.substring(objStart, objEnd + 1);
            String newObj;
            String nameKey = "\"name\":\"";
            int ni = obj.indexOf(nameKey);
            if (ni != -1) {
                int vstart = ni + nameKey.length();
                int vend = obj.indexOf('"', vstart);
                newObj = obj.substring(0, vstart) + escapeJson(newName) + obj.substring(vend);
            } else {
                newObj = obj.substring(0, obj.length()-1) + ",\"name\":\"" + escapeJson(newName) + "\"}";
            }
            String updated = content.substring(0, objStart) + newObj + content.substring(objEnd + 1);
            Files.write(USERS_PATH, updated.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean updatePasswordInFile(String username, String newHash) {
        if (!Files.exists(USERS_PATH)) return false;
        try {
            String content = new String(Files.readAllBytes(USERS_PATH), StandardCharsets.UTF_8);
            int idx = content.indexOf("\"username\":\"" + username + "\"");
            if (idx == -1) return false;
            int objStart = content.lastIndexOf('{', idx);
            int objEnd = content.indexOf('}', idx);
            if (objStart == -1 || objEnd == -1) return false;
            String obj = content.substring(objStart, objEnd + 1);
            String passKey = "\"password\":\"";
            int pi = obj.indexOf(passKey);
            if (pi == -1) return false;
            int vstart = pi + passKey.length();
            int vend = obj.indexOf('"', vstart);
            if (vend == -1) return false;
            String newObj = obj.substring(0, vstart) + escapeJson(newHash) + obj.substring(vend);
            String updated = content.substring(0, objStart) + newObj + content.substring(objEnd + 1);
            Files.write(USERS_PATH, updated.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String escapeJson(String s) { if (s == null) return ""; return s.replace("\\","\\\\").replace("\"","\\\""); }
    private String unescapeJson(String s) { if (s == null) return null; return s.replace("\\\"","\"").replace("\\\\","\\"); }

    private void showAlert(AlertType type, String title, String message) {
        try { Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(message); a.showAndWait(); }
        catch (Exception ex) { System.err.println(title + ": " + message); }
    }
}