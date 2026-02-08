package model;

public enum Difficulty {
    EASY(5),
    NORMAL(4),
    EXPERT(2),
    GENIUS(1);

    private int shipsAllowed;

    Difficulty(int shipsAllowed) {
        this.shipsAllowed = shipsAllowed;
    }

    public int getShipsAllowed() {
        return shipsAllowed;
    }
}
