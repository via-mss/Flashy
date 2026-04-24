package application;

public class Flashcard {
    private int id;
    private String front;
    private String back;
    private byte[] image;
    private int familiarity = 0;

    public Flashcard() {}
    
    public Flashcard(String front, String back) {
        this.front = front;
        this.back = back;
    }
    
    public Flashcard(int id, String front, String back, byte[] image) {
        this.id = id;
        this.front = front;
        this.back = back;
        this.image = image;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFront() {
        return front;
    }

    public void setFront(String front) {
        this.front = front;
    }

    public String getBack() {
        return back;
    }

    public void setBack(String back) {
        this.back = back;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public int getFamiliarity() { return familiarity; }

    public void setFamiliarity(int f) { this.familiarity = Math.max(-2, Math.min(2, f)); }

    public void markKnown() { setFamiliarity(familiarity - 1); }
    public void markSomewhat() {  }
    public void markNotKnown() { setFamiliarity(familiarity + 1); }

    @Override
    public String toString() {
        return front + " / " + back;
    }
}