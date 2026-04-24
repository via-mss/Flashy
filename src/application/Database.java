package application;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Database {
    private static final String DB_FOLDER = "data";
    private static final String DB_FILE = "data/app.db";
    private static String jdbcUrl;
    private static volatile boolean initialized = false;

    public static boolean init() {
        if (initialized) return true;
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            initialized = false;
            return false;
        }
        Path dbFolder = Paths.get(DB_FOLDER);
        if (!Files.exists(dbFolder)) {
            try { Files.createDirectories(dbFolder); } catch (IOException ignore) {}
        }
        jdbcUrl = "jdbc:sqlite:" + DB_FILE.replace('/', java.io.File.separatorChar);
        try (Connection c = DriverManager.getConnection(jdbcUrl)) {
            try (Statement s = c.createStatement()) {
                try { s.execute("PRAGMA foreign_keys = ON"); } catch (Exception ignore) {}
                
                s.executeUpdate("CREATE TABLE IF NOT EXISTS users (username TEXT PRIMARY KEY, password TEXT NOT NULL, name TEXT)");
                s.executeUpdate("CREATE TABLE IF NOT EXISTS decks (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL)");
                s.executeUpdate("CREATE TABLE IF NOT EXISTS cards (id INTEGER PRIMARY KEY AUTOINCREMENT, deck_id INTEGER NOT NULL, front TEXT, back TEXT, FOREIGN KEY(deck_id) REFERENCES decks(id) ON DELETE CASCADE)");
                
                // Add banned column if missing
                boolean hasBanned = false;
                try (ResultSet urs = s.executeQuery("PRAGMA table_info('users')")) {
                    while (urs.next()) {
                        String col = urs.getString("name");
                        if ("banned".equalsIgnoreCase(col)) { hasBanned = true; break; }
                    }
                }
                if (!hasBanned) {
                    try { s.executeUpdate("ALTER TABLE users ADD COLUMN banned INTEGER DEFAULT 0"); } catch (Exception ignore) {}
                }
                
                // Add score column if missing
                boolean hasScore = false;
                try (ResultSet urs = s.executeQuery("PRAGMA table_info('users')")) {
                    while (urs.next()) {
                        String col = urs.getString("name");
                        if ("score".equalsIgnoreCase(col)) { hasScore = true; break; }
                    }
                }
                if (!hasScore) {
                    try { s.executeUpdate("ALTER TABLE users ADD COLUMN score INTEGER DEFAULT 0"); } catch (Exception ignore) {}
                }
                
                // Add total_studied column if missing
                boolean hasTotalStudied = false;
                try (ResultSet urs = s.executeQuery("PRAGMA table_info('users')")) {
                    while (urs.next()) {
                        String col = urs.getString("name");
                        if ("total_studied".equalsIgnoreCase(col)) { hasTotalStudied = true; break; }
                    }
                }
                if (!hasTotalStudied) {
                    try { s.executeUpdate("ALTER TABLE users ADD COLUMN total_studied INTEGER DEFAULT 0"); } catch (Exception ignore) {}
                }
                
                // Add owner, is_public, banned columns to decks if missing
                boolean hasOwner = false, hasIsPublic = false;
                try (ResultSet prs = s.executeQuery("PRAGMA table_info('decks')")) {
                    while (prs.next()) {
                        String col = prs.getString("name");
                        if ("owner".equalsIgnoreCase(col)) hasOwner = true;
                        if ("is_public".equalsIgnoreCase(col)) hasIsPublic = true;
                        if ("banned".equalsIgnoreCase(col)) hasBanned = true;
                    }
                }
                if (!hasOwner) {
                    try { s.executeUpdate("ALTER TABLE decks ADD COLUMN owner TEXT"); } catch (Exception ignore) {}
                }
                if (!hasIsPublic) {
                    try { s.executeUpdate("ALTER TABLE decks ADD COLUMN is_public INTEGER DEFAULT 0"); } catch (Exception ignore) {}
                }
                if (!hasBanned) {
                    try { s.executeUpdate("ALTER TABLE decks ADD COLUMN banned INTEGER DEFAULT 0"); } catch (Exception ignore) {}
                }
                
                // Add image column to cards if missing
                boolean hasImage = false;
                try (ResultSet prs2 = s.executeQuery("PRAGMA table_info('cards')")) {
                    while (prs2.next()) {
                        String col = prs2.getString("name");
                        if ("image".equalsIgnoreCase(col)) { hasImage = true; break; }
                    }
                }
                if (!hasImage) {
                    try { s.executeUpdate("ALTER TABLE cards ADD COLUMN image BLOB"); } catch (Exception ignore) {}
                }
            }
        } catch (SQLException e) {
            initialized = false;
            return false;
        }
        
        // Migrate from JSON if needed
        try (Connection c2 = DriverManager.getConnection(jdbcUrl)) {
            try (Statement s2 = c2.createStatement()) {
                try (ResultSet urs = s2.executeQuery("PRAGMA table_info('users')")) {
                    boolean hasBannedCol = false;
                    while (urs.next()) {
                        if ("banned".equalsIgnoreCase(urs.getString("name"))) { hasBannedCol = true; break; }
                    }
                    if (!hasBannedCol) {
                        try { s2.executeUpdate("ALTER TABLE users ADD COLUMN banned INTEGER DEFAULT 0"); } catch (Exception ignore) {}
                    }
                }
                
                try (Statement s3 = c2.createStatement(); ResultSet urs2 = s3.executeQuery("SELECT COUNT(*) as c FROM users")) {
                    if (urs2.next()) {
                        int c = urs2.getInt("c");
                        if (c == 0) {
                            Path usersPath = Paths.get("src/application/users.json");
                            if (Files.exists(usersPath)) {
                                try {
                                    Path bak = Paths.get(usersPath.toString() + ".bak_" + System.currentTimeMillis());
                                    Files.copy(usersPath, bak);
                                    
                                    String content = new String(Files.readAllBytes(usersPath), java.nio.charset.StandardCharsets.UTF_8);
                                    java.util.regex.Pattern objP = java.util.regex.Pattern.compile("\\{[^}]*?\\}");
                                    java.util.regex.Matcher mo = objP.matcher(content);
                                    try (PreparedStatement ins = c2.prepareStatement("INSERT OR IGNORE INTO users(username,password,name,banned,score,total_studied) VALUES(?,?,?,?,0,0)")) {
                                        while (mo.find()) {
                                            String obj = mo.group();
                                            java.util.regex.Matcher mu = java.util.regex.Pattern.compile("\\\"username\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(obj);
                                            if (!mu.find()) continue;
                                            String uname = mu.group(1);
                                            java.util.regex.Matcher mp = java.util.regex.Pattern.compile("\\\"password\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(obj);
                                            String pwd = "";
                                            if (mp.find()) pwd = mp.group(1);
                                            java.util.regex.Matcher mn = java.util.regex.Pattern.compile("\\\"name\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(obj);
                                            String name = "";
                                            if (mn.find()) name = mn.group(1);
                                            boolean banned = obj.contains("\"banned\":true") || obj.contains("\"banned\": 1") || obj.contains("\"banned\":1");
                                            ins.setString(1, uname);
                                            ins.setString(2, pwd == null ? "" : pwd);
                                            ins.setString(3, name);
                                            ins.setInt(4, banned ? 1 : 0);
                                            try { ins.executeUpdate(); } catch (Exception ex) { /* ignore per-user errors */ }
                                        }
                                    }
                                } catch (IOException ex) { /* ignore migration errors */ }
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            // ignore migration errors
        }
        
        initialized = true;
        return true;
    }

    public static boolean isAvailable() {
        return initialized && jdbcUrl != null;
    }

    public static String getPasswordHash(String username) {
        if (!isAvailable()) return null;
        String sql = "SELECT password FROM users WHERE username = ?";
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("password");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public static boolean isUserBanned(String username) {
        if (!isAvailable()) return false;
        String sql = "SELECT banned FROM users WHERE username = ?";
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("banned") == 1;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static boolean setUserBanned(String username, boolean banned) {
        if (!isAvailable()) return false;
        String sql = "UPDATE users SET banned = ? WHERE username = ?";
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, banned ? 1 : 0);
            ps.setString(2, username);
            int changed = ps.executeUpdate();
            return changed > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static boolean deleteUser(String username) {
        if (!isAvailable()) return false;
        try (Connection c = DriverManager.getConnection(jdbcUrl)) {
            try (Statement fk = c.createStatement()) { try { fk.execute("PRAGMA foreign_keys = ON"); } catch (Exception ignore) {} }
            c.setAutoCommit(false);
            try (PreparedStatement delDecks = c.prepareStatement("DELETE FROM decks WHERE owner = ?")) {
                delDecks.setString(1, username);
                delDecks.executeUpdate();
            }
            try (PreparedStatement delUser = c.prepareStatement("DELETE FROM users WHERE username = ?")) {
                delUser.setString(1, username);
                int changed = delUser.executeUpdate();
                c.commit();
                return changed > 0;
            } catch (SQLException e) {
                try { c.rollback(); } catch (SQLException ignore) {}
                e.printStackTrace();
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static java.util.List<java.util.Map<String,Object>> listUsers() {
        java.util.List<java.util.Map<String,Object>> out = new java.util.ArrayList<>();
        if (!isAvailable()) return out;
        String sql = "SELECT username, name, banned, score, total_studied FROM users";
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                java.util.Map<String,Object> m = new java.util.HashMap<>();
                m.put("username", rs.getString("username"));
                m.put("name", rs.getString("name"));
                m.put("banned", rs.getInt("banned") == 1);
                m.put("score", rs.getInt("score"));
                m.put("total_studied", rs.getInt("total_studied"));
                out.add(m);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return out;
    }

    public static String getName(String username) {
        if (!isAvailable()) return null;
        String sql = "SELECT name FROM users WHERE username = ?";
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("name");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public static boolean userExists(String username) {
        return getPasswordHash(username) != null;
    }

    public static boolean createUser(String username, String passwordHash, String name) {
        if (!isAvailable()) return false;
        String sql = "INSERT INTO users(username, password, name, banned, score, total_studied) VALUES(?, ?, ?, 0, 0, 0)";
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, name);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static boolean updatePassword(String username, String newHash) {
        if (!isAvailable()) return false;
        String sql = "UPDATE users SET password = ? WHERE username = ?";
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static boolean updateName(String username, String name) {
        if (!isAvailable()) return false;
        String sql = "UPDATE users SET name = ? WHERE username = ?";
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    // SCORING SYSTEM METHODS
    
    public static int getUserScore(String username) {
        if (!isAvailable()) return 0;
        String sql = "SELECT score FROM users WHERE username = ?";
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("score");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }
    
    public static int getUserTotalStudied(String username) {
        if (!isAvailable()) return 0;
        String sql = "SELECT total_studied FROM users WHERE username = ?";
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("total_studied");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }
    
    public static boolean addUserScore(String username, int points) {
        if (!isAvailable()) return false;
        String sql = "UPDATE users SET score = score + ?, total_studied = total_studied + 1 WHERE username = ?";
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, points);
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }
    
    public static java.util.List<java.util.Map<String,Object>> getLeaderboard(int limit) {
        java.util.List<java.util.Map<String,Object>> out = new java.util.ArrayList<>();
        if (!isAvailable()) return out;
        String sql = "SELECT username, name, score, total_studied FROM users WHERE banned = 0 ORDER BY score DESC LIMIT ?";
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.util.Map<String,Object> m = new java.util.HashMap<>();
                    m.put("username", rs.getString("username"));
                    m.put("name", rs.getString("name"));
                    m.put("score", rs.getInt("score"));
                    m.put("total_studied", rs.getInt("total_studied"));
                    out.add(m);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return out;
    }

    // DECK LOAD/SAVE
    
    public static ObservableList<Deck> loadAllDecks() {
        ObservableList<Deck> decks = FXCollections.observableArrayList();
        if (!isAvailable()) return decks;
        String deckSql = "SELECT id, name, owner, is_public, banned FROM decks";
        String cardSql = "SELECT id, front, back, image FROM cards WHERE deck_id = ?";
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             PreparedStatement deckPs = c.prepareStatement(deckSql);
             PreparedStatement cardPs = c.prepareStatement(cardSql)) {
            try (ResultSet drs = deckPs.executeQuery()) {
                while (drs.next()) {
                    int deckId = drs.getInt("id");
                    String name = drs.getString("name");
                    String owner = null;
                    try { owner = drs.getString("owner"); } catch (Exception ex) { owner = null; }
                    boolean isPublic = false;
                    try { isPublic = drs.getInt("is_public") == 1; } catch (Exception ex) { isPublic = false; }
                    Deck d = new Deck(name);
                    d.setId(deckId);
                    d.setOwner(owner);
                    d.setPublic(isPublic);
                    try { boolean banned = drs.getInt("banned") == 1; d.setBanned(banned); } catch (Exception ex) { d.setBanned(false); }
                    
                    cardPs.setInt(1, deckId);
                    try (ResultSet crs = cardPs.executeQuery()) {
                        while (crs.next()) {
                            int cardId = crs.getInt("id");
                            String front = crs.getString("front");
                            String back = crs.getString("back");
                            byte[] img = null;
                            try { img = crs.getBytes("image"); } catch (Exception ex) { img = null; }
                            Flashcard fc = new Flashcard(cardId, front, back, img);
                            d.getCards().add(fc);
                        }
                    }
                    decks.add(d);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return decks;
    }

    public static boolean saveAllDecks(ObservableList<Deck> decks) {
        if (!isAvailable()) return false;
        String deleteCards = "DELETE FROM cards";
        String deleteDecks = "DELETE FROM decks";
        String insertDeck = "INSERT INTO decks(name, owner, is_public, banned) VALUES(?, ?, ?, ?)";
        String insertCard = "INSERT INTO cards(deck_id, front, back, image) VALUES(?, ?, ?, ?)";
        try (Connection c = DriverManager.getConnection(jdbcUrl)) {
            c.setAutoCommit(false);
            try (Statement s = c.createStatement()) {
                s.executeUpdate(deleteCards);
                s.executeUpdate(deleteDecks);
            }
            try (PreparedStatement deckPs = c.prepareStatement(insertDeck, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement cardPs = c.prepareStatement(insertCard)) {
                for (Deck d : decks) {
                    deckPs.setString(1, d.getName());
                    deckPs.setString(2, d.getOwner());
                    deckPs.setInt(3, d.isPublic() ? 1 : 0);
                    deckPs.setInt(4, d.isBanned() ? 1 : 0);
                    deckPs.executeUpdate();
                    try (ResultSet gk = deckPs.getGeneratedKeys()) {
                        if (gk.next()) {
                            int newDeckId = gk.getInt(1);
                            d.setId(newDeckId);
                            for (Flashcard fc : d.getCards()) {
                                cardPs.setInt(1, newDeckId);
                                cardPs.setString(2, fc.getFront());
                                cardPs.setString(3, fc.getBack());
                                if (fc.getImage() != null) {
                                    cardPs.setBytes(4, fc.getImage());
                                } else {
                                    cardPs.setNull(4, java.sql.Types.BLOB);
                                }
                                cardPs.executeUpdate();
                                try (ResultSet cgk = cardPs.getGeneratedKeys()) {
                                    if (cgk.next()) {
                                        fc.setId(cgk.getInt(1));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            c.commit();
            c.setAutoCommit(true);
            return true;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }
}