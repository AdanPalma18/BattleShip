package model;

import java.awt.Image;
import java.util.HashSet;
import java.util.Set;

public class Ship {

    private String code;
    private int size;
    private int hits;
    private boolean vertical;
    private int row;
    private int col;
    private Image image;
    // Rastrear qué partes específicas del barco han sido golpeadas (índices 0 a size-1)
    private Set<Integer> hitParts;

    public Ship(String code, int size, Image image) {
        this.code = code;
        this.size = size;
        this.image = image;
        this.hits = 0;
        this.vertical = true;
        this.hitParts = new HashSet<>();
    }

    /**
     * Marca una parte específica del barco como golpeada
     * @param partIndex El índice de la parte (0 a size-1)
     * @return true si la parte no había sido golpeada antes, false si ya estaba golpeada
     */
    public boolean hitPart(int partIndex) {
        if (partIndex < 0 || partIndex >= size) {
            return false;
        }
        if (hitParts.contains(partIndex)) {
            // Esta parte ya fue golpeada antes
            return false;
        }
        hitParts.add(partIndex);
        hits++;
        return true;
    }

    public void hit() {
        // Método legacy - no usar directamente, usar hitPart() en su lugar
        hits++;
    }

    public boolean isSunk() {
        return hits >= size;
    }
    
    /**
     * Obtiene el conjunto de partes golpeadas
     */
    public Set<Integer> getHitParts() {
        return new HashSet<>(hitParts);
    }
    
    /**
     * Verifica si una parte específica ha sido golpeada
     */
    public boolean isPartHit(int partIndex) {
        return hitParts.contains(partIndex);
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
