package model;

import java.util.ArrayList;
import java.util.Random;

public class Board {

    private Ship[][] grid;
    private ArrayList<Ship> ships;

    public Board() {
        grid = new Ship[8][8];
        ships = new ArrayList<>();
    }

    public boolean placeShip(Ship ship, int row, int col, boolean vertical) {
        if (!canPlace(ship, row, col, vertical)) return false;

        ship.setPosition(row, col);
        ship.setVertical(vertical);

        for (int i = 0; i < ship.getSize(); i++) {
            int r = row + (vertical ? i : 0);
            int c = col + (vertical ? 0 : i);
            grid[r][c] = ship;
        }

        ships.add(ship);
        return true;
    }

    private boolean canPlace(Ship ship, int row, int col, boolean vertical) {
        for (int i = 0; i < ship.getSize(); i++) {
            int r = row + (vertical ? i : 0);
            int c = col + (vertical ? 0 : i);

            if (r < 0 || r >= 8 || c < 0 || c >= 8) return false;
            if (grid[r][c] != null) return false;
        }
        return true;
    }

    public Ship bomb(int row, int col) {
        return grid[row][col];
    }

    public void clearBoard() {
        grid = new Ship[8][8];
    }

    public void regenerate() {
        clearBoard();
        Random rand = new Random();

        for (Ship ship : ships) {
            boolean placed = false;
            while (!placed) {
                int row = rand.nextInt(8);
                int col = rand.nextInt(8);
                boolean vertical = rand.nextBoolean();
                placed = placeShip(ship, row, col, vertical);
            }
        }
    }

    public int remainingShips() {
        int count = 0;
        for (Ship s : ships) {
            if (!s.isSunk()) count++;
        }
        return count;
    }

    public Ship[][] getGrid() {
        return grid;
    }

    public ArrayList<Ship> getShips() {
        return ships;
    }
}
