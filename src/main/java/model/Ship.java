package model;

import java.awt.Image;

public class Ship {

    private String code;
    private int size;
    private int hits;
    private boolean vertical;
    private int row;
    private int col;
    private Image image;

    public Ship(String code, int size, Image image) {
        this.code = code;
        this.size = size;
        this.image = image;
        this.hits = 0;
        this.vertical = true;
    }

    public void hit() {
        hits++;
    }

    public boolean isSunk() {
        return hits >= size;
    }

    public String getCode() {
        return code;
    }

    public int getSize() {
        return size;
    }

    public int getHits() {
        return hits;
    }

    public boolean isVertical() {
        return vertical;
    }

    public void setVertical(boolean vertical) {
        this.vertical = vertical;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public void setPosition(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public Image getImage() {
        return image;
    }
}
