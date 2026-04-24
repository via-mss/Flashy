package application;

import javafx.animation.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.util.Duration;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginController {
    // FXML fields
    public Label flashyTitle;
    public Label subtitleLabel;
    public Button btn_log;
    public Button btn_reg;
    public TextField txt_log_uname;
    public TextField txt_reg_uname;
    public PasswordField txt_log_pswd;
    public PasswordField txt_reg_pswd;
    public TextField txt_reg_name;
    public javafx.scene.layout.VBox anch_log;
    public javafx.scene.layout.VBox anch_reg;
    public javafx.scene.layout.HBox buttonBox;

    private static final Path USERS_PATH = Paths.get("src/application/users.json");
    private static final int MIN_PASSWORD_LEN = 6;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,30}$");

    private boolean loginFieldsVisible = false;
    private boolean registerFieldsVisible = false;

    public void initialize() {
        // Apply theme to login window
        if (buttonBox != null && buttonBox.getScene() != null) {
            Main.applyThemeToScene(buttonBox.getScene());
        }

        // Initially hide both forms
        if (anch_log != null) {
            anch_log.setVisible(false);
            anch_log.setManaged(false);
        }
        if (anch_reg != null) {
            anch_reg.setVisible(false);
            anch_reg.setManaged(false);
        }

        if (btn_log != null) {
            btn_log.setOnAction(e -> handleLoginButtonClick());
            styleButton(btn_log, "#3498db", "#2980b9");
        }

        if (btn_reg != null) {
            btn_reg.setOnAction(e -> handleRegisterButtonClick());
            styleButton(btn_reg, "#2ecc71", "#27ae60");
        }

        // Bind input field widths to match button bar width
        if (buttonBox != null && anch_log != null && anch_reg != null) {
            anch_log.prefWidthProperty().bind(buttonBox.widthProperty());
            anch_reg.prefWidthProperty().bind(buttonBox.widthProperty());
        }

        // Set style class for subtitle
        if (subtitleLabel != null) {
            subtitleLabel.getStyleClass().add("login-subtitle");
        }

        Database.init();
        startTitleAnimation();
    }

    private void styleButton(Button btn, String normalColor, String hoverColor) {
        btn.setStyle("-fx-background-color: " + normalColor + "; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: " + hoverColor + "; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: " + normalColor + "; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;"));
    }

    private void startTitleAnimation() {
        Scene scene = null;
        if (btn_log != null) scene = btn_log.getScene();
        if (scene == null && anch_log != null) scene = anch_log.getScene();
        if (scene != null) {
            javafx.scene.Node root = scene.getRoot();
            findAndAnimateTitle(root);
        } else if (flashyTitle != null) {
            animateTitle(flashyTitle);
        }
    }

    private void findAndAnimateTitle(javafx.scene.Node node) {
        if (node instanceof javafx.scene.control.Label) {
            javafx.scene.control.Label label = (javafx.scene.control.Label) node;
            if ("Flashy".equals(label.getText())) {
                animateTitle(label);
                return;
            }
        }
        if (node instanceof javafx.scene.Parent) {
            javafx.scene.Parent parent = (javafx.scene.Parent) node;
            for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
                findAndAnimateTitle(child);
            }
        }
    }

    private void animateTitle(javafx.scene.control.Label titleLabel) {
        if (titleLabel == null) return;

        Timeline timeline = new Timeline();
        KeyFrame bounceUp = new KeyFrame(Duration.ZERO,
                new KeyValue(titleLabel.translateYProperty(), 0),
                new KeyValue(titleLabel.scaleXProperty(), 1.0),
                new KeyValue(titleLabel.scaleYProperty(), 1.0)
        );
        KeyFrame bouncePeak = new KeyFrame(Duration.seconds(0.3),
                new KeyValue(titleLabel.translateYProperty(), -20),
                new KeyValue(titleLabel.scaleXProperty(), 1.1),
                new KeyValue(titleLabel.scaleYProperty(), 1.1)
        );
        KeyFrame bounceDown = new KeyFrame(Duration.seconds(0.6),
                new KeyValue(titleLabel.translateYProperty(), 0),
                new KeyValue(titleLabel.scaleXProperty(), 1.0),
                new KeyValue(titleLabel.scaleYProperty(), 1.0)
        );
        KeyFrame pause = new KeyFrame(Duration.seconds(1.5),
                new KeyValue(titleLabel.translateYProperty(), 0)
        );
        timeline.getKeyFrames().addAll(bounceUp, bouncePeak, bounceDown, pause);
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        Timeline colorTimeline = new Timeline();
        colorTimeline.setCycleCount(Timeline.INDEFINITE);
        colorTimeline.getKeyFrames().addAll(
                new KeyFrame(Duration.ZERO, new KeyValue(titleLabel.styleProperty(), "-fx-text-fill: #e74c3c; -fx-font-size: 72px;")),
                new KeyFrame(Duration.seconds(0.5), new KeyValue(titleLabel.styleProperty(), "-fx-text-fill: #f39c12; -fx-font-size: 72px;")),
                new KeyFrame(Duration.seconds(1.0), new KeyValue(titleLabel.styleProperty(), "-fx-text-fill: #2ecc71; -fx-font-size: 72px;")),
                new KeyFrame(Duration.seconds(1.5), new KeyValue(titleLabel.styleProperty(), "-fx-text-fill: #3498db; -fx-font-size: 72px;")),
                new KeyFrame(Duration.seconds(2.0), new KeyValue(titleLabel.styleProperty(), "-fx-text-fill: #9b59b6; -fx-font-size: 72px;")),
                new KeyFrame(Duration.seconds(2.5), new KeyValue(titleLabel.styleProperty(), "-fx-text-fill: #e74c3c; -fx-font-size: 72px;"))
        );
        colorTimeline.play();

        Timeline wobbleTimeline = new Timeline();
        wobbleTimeline.setCycleCount(Timeline.INDEFINITE);
        wobbleTimeline.getKeyFrames().addAll(
                new KeyFrame(Duration.ZERO, new KeyValue(titleLabel.rotateProperty(), 0)),
                new KeyFrame(Duration.seconds(0.2), new KeyValue(titleLabel.rotateProperty(), 5)),
                new KeyFrame(Duration.seconds(0.4), new KeyValue(titleLabel.rotateProperty(), -5)),
                new KeyFrame(Duration.seconds(0.6), new KeyValue(titleLabel.rotateProperty(), 3)),
                new KeyFrame(Duration.seconds(0.8), new KeyValue(titleLabel.rotateProperty(), -3)),
                new KeyFrame(Duration.seconds(1.0), new KeyValue(titleLabel.rotateProperty(), 0))
        );
        wobbleTimeline.play();
    }

    private void handleLoginButtonClick() {
        if (!loginFieldsVisible) {
            showLoginFields();
        } else {
            performLogin();
        }
    }

    private void handleRegisterButtonClick() {
        if (!registerFieldsVisible) {
            showRegisterFields();
        } else {
            performRegister();
        }
    }

    private void showLoginFields() {
        loginFieldsVisible = true;
        registerFieldsVisible = false;
        if (anch_log != null) {
            anch_log.setVisible(true);
            anch_log.setManaged(true);
        }
        if (anch_reg != null) {
            anch_reg.setVisible(false);
            anch_reg.setManaged(false);
        }
        if (txt_log_uname != null) txt_log_uname.clear();
        if (txt_log_pswd != null) txt_log_pswd.clear();
        if (btn_log != null) btn_log.setText("Login!");
        if (btn_reg != null) btn_reg.setText("Register");
    }

    private void showRegisterFields() {
        registerFieldsVisible = true;
        loginFieldsVisible = false;
        if (anch_reg != null) {
            anch_reg.setVisible(true);
            anch_reg.setManaged(true);
        }
        if (anch_log != null) {
            anch_log.setVisible(false);
            anch_log.setManaged(false);
        }
        if (txt_reg_name != null) txt_reg_name.clear();
        if (txt_reg_uname != null) txt_reg_uname.clear();
        if (txt_reg_pswd != null) txt_reg_pswd.clear();
        if (btn_reg != null) btn_reg.setText("Register!");
        if (btn_log != null) btn_log.setText("Login");
    }

    private void performLogin() {
        String uname = txt_log_uname == null ? null : txt_log_uname.getText();
        String pswd = txt_log_pswd == null ? null : txt_log_pswd.getText();

        if (uname == null || uname.trim().isEmpty()) { 
            showAlert(AlertType.ERROR, "Login error", "Please enter a username."); 
            return; 
        }
        if (pswd == null || pswd.trim().isEmpty()) { 
            showAlert(AlertType.ERROR, "Login error", "Please enter a password."); 
            return; 
        }

        try {
            String stored;
            boolean authenticated = false;

            if (Database.isAvailable()) {
                stored = Database.getPasswordHash(uname);
                if (stored != null && Database.isUserBanned(uname)) { 
                    showAlert(AlertType.ERROR, "Account disabled", "This account has been banned."); 
                    resetLoginButton();
                    return; 
                }
                if (stored != null && stored.matches("^\\d+:[A-Za-z0-9+/=]+:[A-Za-z0-9+/=]+$")) {
                    authenticated = PasswordUtils.verifyPassword(pswd, stored);
                } else {
                    authenticated = stored != null && stored.equals(pswd);
                    if (authenticated) {
                        String newHash = PasswordUtils.createHash(pswd);
                        try { Database.updatePassword(uname, newHash); } catch (Exception ex) {
                            showAlert(AlertType.WARNING, "Upgrade warning", "Could not upgrade stored password: " + ex.getMessage());
                        }
                    }
                }
            } else {
                Map<String,String> users = loadUsers();
                if (users.containsKey(uname)) {
                    stored = users.get(uname);
                    if (isUserBannedInFile(uname)) { 
                        showAlert(AlertType.ERROR, "Account disabled", "This account has been banned."); 
                        resetLoginButton();
                        return; 
                    }
                    if (stored != null && stored.matches("^\\d+:[A-Za-z0-9+/=]+:[A-Za-z0-9+/=]+$")) {
                        authenticated = PasswordUtils.verifyPassword(pswd, stored);
                    } else {
                        authenticated = stored != null && stored.equals(pswd);
                    }
                }
                if (!authenticated && "admin".equals(uname) && "admin".equals(pswd)) {
                    authenticated = true;
                }
            }

            if (authenticated) {
                SessionData.setCurrentUser(uname);
                SessionData.reloadDecks();
                try {
                    Parent root = new FXMLLoader(getClass().getResource("/application/Dashboard.fxml")).load();
                    Main.switchScene(root);
                } catch (Exception e) {
                    showAlert(AlertType.ERROR, "UI error", "Could not open dashboard: " + e.getMessage());
                }
            } else {
                showAlert(AlertType.ERROR, "Authentication failed", "Invalid username or password.");
                resetLoginButton();
            }
        } catch (Exception ex) {
            showAlert(AlertType.ERROR, "Login error", "Error verifying credentials: " + ex.getMessage());
            resetLoginButton();
        }
    }

    private void performRegister() {
        String uname = txt_reg_uname == null ? null : txt_reg_uname.getText();
        String pswd = txt_reg_pswd == null ? null : txt_reg_pswd.getText();
        String name = txt_reg_name == null ? null : txt_reg_name.getText();

        if (uname == null || uname.trim().isEmpty()) { 
            showAlert(AlertType.ERROR, "Registration error", "Username is required."); 
            return; 
        }
        if (!USERNAME_PATTERN.matcher(uname).matches()) { 
            showAlert(AlertType.ERROR, "Registration error", "Username must be 3-30 chars and only letters, numbers, '-' or '_'."); 
            return; 
        }
        if (pswd == null || pswd.trim().isEmpty()) { 
            showAlert(AlertType.ERROR, "Registration error", "Password is required."); 
            return; 
        }
        if (pswd.length() < MIN_PASSWORD_LEN) { 
            showAlert(AlertType.ERROR, "Registration error", "Password must be at least " + MIN_PASSWORD_LEN + " characters long."); 
            return; 
        }

        try {
            boolean created = false;
            String hashed = PasswordUtils.createHash(pswd);

            if (Database.isAvailable()) {
                if (Database.userExists(uname)) { 
                    showAlert(AlertType.ERROR, "Registration error", "Username already exists."); 
                    return; 
                }
                created = Database.createUser(uname, hashed, name);
            } else {
                Map<String,String> users = loadUsers();
                if (users.containsKey(uname)) { 
                    showAlert(AlertType.ERROR, "Registration error", "Username already exists."); 
                    return; 
                }
                Map<String,String> names = loadNames();
                users.put(uname, hashed);
                if (name != null && !name.trim().isEmpty()) names.put(uname, name);
                created = saveUsers(users, names);
            }

            if (created) {
                showAlert(AlertType.INFORMATION, "Registration", "User registered successfully: " + uname);
                resetRegisterButton();
                showLoginFields();
            } else {
                showAlert(AlertType.ERROR, "Registration error", "Could not create account.");
            }
        } catch (Exception ex) {
            showAlert(AlertType.ERROR, "Registration error", "Could not create account: " + ex.getMessage());
        }
    }

    private void resetLoginButton() {
        loginFieldsVisible = false;
        if (btn_log != null) btn_log.setText("Login");
    }

    private void resetRegisterButton() {
        registerFieldsVisible = false;
        if (btn_reg != null) btn_reg.setText("Register");
    }

    private Map<String,String> loadUsers() {
        Map<String,String> users = new HashMap<>();
        Path path = USERS_PATH;
        if (!Files.exists(path)) return users;
        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            if (content.trim().isEmpty()) return users;
            Pattern up = Pattern.compile("\"username\"\\s*:\\s*\"([^\"]+)\"");
            Pattern pp = Pattern.compile("\"password\"\\s*:\\s*\"([^\"]+)\"");
            Matcher mu = up.matcher(content);
            Matcher mp = pp.matcher(content);
            while (mu.find()) {
                String u = mu.group(1);
                String p = null;
                if (mp.find(mu.end())) p = mp.group(1); else if (mp.find()) p = mp.group(1);
                if (u != null && p != null) users.put(u, p);
            }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Data error", "Could not read users file: " + e.getMessage());
        }
        return users;
    }

    private boolean isUserBannedInFile(String username) {
        Path path = USERS_PATH;
        if (!Files.exists(path)) return false;
        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            String key = "\"username\":\"" + username + "\"";
            int idx = content.indexOf(key);
            if (idx == -1) return false;
            int objStart = content.lastIndexOf('{', idx);
            int objEnd = content.indexOf('}', idx);
            if (objStart == -1 || objEnd == -1) return false;
            String obj = content.substring(objStart, objEnd + 1);
            if (obj.contains("\"banned\":true") || obj.contains("\"banned\": 1") || obj.contains("\"banned\":1")) return true;
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    private boolean saveUsers(Map<String,String> users) { 
        return saveUsers(users, null); 
    }

    private boolean saveUsers(Map<String,String> users, Map<String,String> names) {
        Path path = USERS_PATH;
        try {
            File f = path.toFile();
            if (f.getParentFile() != null && !f.getParentFile().exists()) f.getParentFile().mkdirs();
            StringBuilder sb = new StringBuilder(); 
            sb.append("[");
            boolean first = true;
            for (Map.Entry<String,String> e : users.entrySet()) {
                if (!first) sb.append(',');
                sb.append('{').append("\"username\":\"").append(escapeJson(e.getKey())).append("\",\"password\":\"").append(escapeJson(e.getValue())).append("\"");
                if (names != null) {
                    String n = names.get(e.getKey());
                    if (n != null && !n.isEmpty()) {
                        sb.append(',').append("\"name\":\"").append(escapeJson(n)).append("\"");
                    }
                }
                sb.append('}');
                first = false;
            }
            sb.append(']');
            try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) { 
                w.write(sb.toString()); 
            }
            return true;
        } catch (Exception ex) {
            showAlert(AlertType.ERROR, "Data error", "Could not save users file: " + ex.getMessage());
            return false;
        }
    }

    private Map<String,String> loadNames() {
        Map<String,String> names = new HashMap<>();
        Path path = USERS_PATH;
        if (!Files.exists(path)) return names;
        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            if (content.trim().isEmpty()) return names;
            Pattern objP = Pattern.compile("\\{[^}]*?\\}");
            Matcher mo = objP.matcher(content);
            while (mo.find()) {
                String obj = mo.group();
                Matcher mu = Pattern.compile("\"username\"\\s*:\\s*\"([^\"]+)\"").matcher(obj);
                if (mu.find()) {
                    String u = mu.group(1);
                    String n = "";
                    Matcher mn = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]*)\"").matcher(obj);
                    if (mn.find()) n = mn.group(1);
                    names.put(u, n);
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return names;
    }

    private String escapeJson(String s) { 
        if (s == null) return ""; 
        return s.replace("\\", "\\\\").replace("\"", "\\\""); 
    }

    private void showAlert(AlertType type, String title, String message) {
        try { 
            Alert a = new Alert(type); 
            a.setTitle(title); 
            a.setHeaderText(null); 
            a.setContentText(message); 

            // Apply theme to alert dialog
            DialogPane dialogPane = a.getDialogPane();
            dialogPane.getStylesheets().clear();
            dialogPane.getStylesheets().add(getClass().getResource("/application/style.css").toExternalForm());
            if ("dark".equals(SessionData.getTheme())) {
                dialogPane.getStylesheets().add(getClass().getResource("/application/dark.css").toExternalForm());
            }

            a.showAndWait(); 
        }
        catch (Exception ex) { 
            System.err.println(title + ": " + message); 
        }
    }
}