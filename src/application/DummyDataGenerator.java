package application;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple utility to populate the application database with dummy users, decks and cards.
 * Run this from your IDE (run as Java application) or from command line with the project's classpath.
 *
 * Behavior:
 * - If SQLite JDBC driver is available, the tool will back up `data/app.db` (if present),
 *   clear `users`, `decks`, and `cards` tables and insert example users and decks.
 * - If driver is missing, it will write `src/application/users.json` with example users (backing up first) but decks require DB.
 */
public class DummyDataGenerator {
    public static void main(String[] args) {
        try {
            Path dbPath = Paths.get("data", "app.db");
            boolean hasDriver = true;
            try { Class.forName("org.sqlite.JDBC"); } catch (ClassNotFoundException e) { hasDriver = false; }

            if (!hasDriver) {
                System.out.println("SQLite JDBC driver not found on classpath. Falling back to file-mode users.json update.");
                writeUsersJsonFallback();
                System.out.println("Wrote users.json with example accounts. Note: decks are not persisted without a DB.");
                return;
            }

            // ensure folder exists
            if (!Files.exists(dbPath.getParent())) Files.createDirectories(dbPath.getParent());

            // backup existing DB
            if (Files.exists(dbPath)) {
                Path bak = dbPath.resolveSibling(dbPath.getFileName().toString() + ".bak_" + System.currentTimeMillis());
                Files.copy(dbPath, bak);
                System.out.println("Backed up DB to " + bak.toString());
            }

            String jdbc = "jdbc:sqlite:" + dbPath.toString();
            try (Connection c = DriverManager.getConnection(jdbc)) {
                try (Statement s = c.createStatement()) {
                    // basic schema ensure
                    s.executeUpdate("PRAGMA foreign_keys = ON");
                    s.executeUpdate("CREATE TABLE IF NOT EXISTS users (username TEXT PRIMARY KEY, password TEXT NOT NULL, name TEXT)");
                    s.executeUpdate("CREATE TABLE IF NOT EXISTS decks (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL)");
                    s.executeUpdate("CREATE TABLE IF NOT EXISTS cards (id INTEGER PRIMARY KEY AUTOINCREMENT, deck_id INTEGER NOT NULL, front TEXT, back TEXT, FOREIGN KEY(deck_id) REFERENCES decks(id) ON DELETE CASCADE)");
                    // try add owner/is_public columns if missing
                    try (ResultSet prs = s.executeQuery("PRAGMA table_info('decks')")) {
                        boolean hasOwner = false, hasIsPublic = false;
                        while (prs.next()) {
                            String col = prs.getString("name");
                            if ("owner".equalsIgnoreCase(col)) hasOwner = true;
                            if ("is_public".equalsIgnoreCase(col)) hasIsPublic = true;
                        }
                        if (!hasOwner) try { s.executeUpdate("ALTER TABLE decks ADD COLUMN owner TEXT"); } catch (Exception ignore) {}
                        if (!hasIsPublic) try { s.executeUpdate("ALTER TABLE decks ADD COLUMN is_public INTEGER DEFAULT 0"); } catch (Exception ignore) {}
                    }
                    // add image column to cards if missing
                    try (ResultSet prs2 = s.executeQuery("PRAGMA table_info('cards')")) {
                        boolean hasImage = false;
                        while (prs2.next()) {
                            String col = prs2.getString("name");
                            if ("image".equalsIgnoreCase(col)) { hasImage = true; break; }
                        }
                        if (!hasImage) try { s.executeUpdate("ALTER TABLE cards ADD COLUMN image BLOB"); } catch (Exception ignore) {}
                    }
                    // add banned to users if missing
                    try (ResultSet urs = s.executeQuery("PRAGMA table_info('users')")) {
                        boolean hasBanned = false;
                        while (urs.next()) {
                            String col = urs.getString("name");
                            if ("banned".equalsIgnoreCase(col)) { hasBanned = true; break; }
                        }
                        if (!hasBanned) try { s.executeUpdate("ALTER TABLE users ADD COLUMN banned INTEGER DEFAULT 0"); } catch (Exception ignore) {}
                    }
                }

                c.setAutoCommit(false);
                try (Statement s = c.createStatement()) {
                    // clear tables
                    s.executeUpdate("DELETE FROM cards");
                    s.executeUpdate("DELETE FROM decks");
                    s.executeUpdate("DELETE FROM users");
                }

                // create users (at least 5)
                String[] usernames = new String[] {"admin", "alice", "bob", "carol", "dave", "eve"};
                String[] names = new String[] {"Administrator", "Alice Doe", "Bob Smith", "Carol Lee", "Dave Jones", "Eve Adams"};
                String[] passwords = new String[] {"admin123", "password", "password", "password", "password", "password"};

                String insertUser = "INSERT INTO users(username,password,name,banned) VALUES(?,?,?,?)";
                try (PreparedStatement ps = c.prepareStatement(insertUser)) {
                    for (int i = 0; i < usernames.length; i++) {
                        String user = usernames[i];
                        String pass = passwords[i];
                        String hash = PasswordUtils.createHash(pass);
                        ps.setString(1, user);
                        ps.setString(2, hash);
                        ps.setString(3, names[i]);
                        ps.setInt(4, 0);
                        ps.executeUpdate();
                    }
                }

                // create decks and cards
                class Card { String f; String b; Card(String f, String b){this.f=f;this.b=b;} }
                class DeckSpec { String name; String owner; boolean pub; List<Card> cards = new ArrayList<>(); DeckSpec(String n,String o,boolean p){name=n;owner=o;pub=p;} }
                List<DeckSpec> dlist = new ArrayList<>();
                DeckSpec d1 = new DeckSpec("Java Basics", "alice", false);
                d1.cards.add(new Card("What keyword declares a class?","class"));
                d1.cards.add(new Card("Primitive boolean literals?","true/false"));
                dlist.add(d1);
                DeckSpec d2 = new DeckSpec("World Capitals", "bob", true);
                d2.cards.add(new Card("Capital of France?","Paris"));
                d2.cards.add(new Card("Capital of Japan?","Tokyo"));
                dlist.add(d2);
                DeckSpec d3 = new DeckSpec("Math Terms", null, true);
                d3.cards.add(new Card("Derivative of x^2?","2x"));
                dlist.add(d3);
                DeckSpec d4 = new DeckSpec("Programming Trivia", "carol", true);
                d4.cards.add(new Card("What does JVM stand for?","Java Virtual Machine"));
                d4.cards.add(new Card("Immutable string class in Java?","String"));
                dlist.add(d4);
                DeckSpec d5 = new DeckSpec("Science Facts", "dave", true);
                d5.cards.add(new Card("Water chemical formula?","H2O"));
                d5.cards.add(new Card("Planet known as Red Planet?","Mars"));
                dlist.add(d5);
                DeckSpec d6 = new DeckSpec("History Dates", null, true);
                d6.cards.add(new Card("Year Columbus reached Americas?","1492"));
                d6.cards.add(new Card("Year WWII ended?","1945"));
                dlist.add(d6);

                String insertDeck = "INSERT INTO decks(name, owner, is_public, banned) VALUES(?,?,?,?)";
                String insertCard = "INSERT INTO cards(deck_id, front, back, image) VALUES(?,?,?,?)";
                try (PreparedStatement dsp = c.prepareStatement(insertDeck, Statement.RETURN_GENERATED_KEYS);
                     PreparedStatement csp = c.prepareStatement(insertCard)) {
                    for (DeckSpec ds : dlist) {
                        // pad each deck to have at least 40 cards with simple filler content
                        int padIndex = ds.cards.size() + 1;
                        while (ds.cards.size() < 40) {
                            ds.cards.add(new Card("Sample Q " + padIndex + " for " + ds.name + "?", "Answer " + padIndex));
                            padIndex++;
                        }
                        dsp.setString(1, ds.name);
                        dsp.setString(2, ds.owner);
                        dsp.setInt(3, ds.pub ? 1 : 0);
                        dsp.setInt(4, 0); // not banned by default
                        dsp.executeUpdate();
                        try (ResultSet gk = dsp.getGeneratedKeys()) {
                            int deckId = -1;
                            if (gk.next()) deckId = gk.getInt(1);
                            for (Card cd : ds.cards) {
                                csp.setInt(1, deckId);
                                csp.setString(2, cd.f);
                                csp.setString(3, cd.b);
                                csp.setNull(4, java.sql.Types.BLOB);
                                csp.executeUpdate();
                            }
                        }
                    }
                }

                c.commit();
                c.setAutoCommit(true);
                System.out.println("Database populated with dummy users and decks.");
                System.out.println("Admin credentials: username=admin password=admin123");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to populate DB: " + e.getMessage());
        }
    }

    private static void writeUsersJsonFallback() {
        try {
            Path users = Paths.get("src/application/users.json");
            if (!Files.exists(users.getParent())) Files.createDirectories(users.getParent());
            Path bak = users.resolveSibling(users.getFileName().toString() + ".bak_" + System.currentTimeMillis());
            if (Files.exists(users)) Files.copy(users, bak);

            StringBuilder sb = new StringBuilder();
            sb.append('[');
            String[] u = new String[] {"admin","alice","bob","carol","dave","eve"};
            String[] n = new String[] {"Administrator","Alice Doe","Bob Smith","Carol Lee","Dave Jones","Eve Adams"};
            String[] p = new String[] {"admin123","password","password","password","password","password"};
            for (int i=0;i<u.length;i++) {
                if (i>0) sb.append(',');
                String hash = PasswordUtils.createHash(p[i]);
                sb.append('{')
                  .append("\"username\":\"").append(escapeJson(u[i])).append("\"")
                  .append(',').append("\"password\":\"").append(escapeJson(hash)).append("\"")
                  .append(',').append("\"name\":\"").append(escapeJson(n[i])).append("\"")
                  .append('}');
            }
            sb.append(']');
            Files.write(users, sb.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("Wrote users.json and backed up original to " + bak.toString());
            System.out.println("Admin credentials: username=admin password=admin123");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static String escapeJson(String s) { if (s == null) return ""; return s.replace("\\","\\\\").replace("\"","\\\""); }
}
