package model;

public class Player {

    private String username;
    private String password;
    private int points;
    private String[] lastGames;
    private CellState[][] enemyView;
    private Ship[][] board;


    public Player(String username, String password) {
        this.username = username;
        this.password = password;
        this.points = 0;
        this.lastGames = new String[10];

        enemyView = new CellState[8][8];
        board = new Ship[8][8];
        initBoard();
    }

    private void initBoard() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                enemyView[i][j] = CellState.WATER;
            }
        }
    }


    public CellState[][] getEnemyView() {
        return enemyView;
    }

    public Ship[][] getBoard() {
        return board;
    }

    public CellState getEnemyViewCell(int row, int col) {
        return enemyView[row][col];
    }

    public void setEnemyViewCell(int row, int col, CellState state) {
        enemyView[row][col] = state;
    }

    public String getUsername() {
        return username;
    }

    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    public int getPoints() {
        return points;
    }

    public void addPoints(int p) {
        points += p;
    }

    public void addGameToHistory(String gameResult) {
        for (int i = lastGames.length - 1; i > 0; i--) {
            lastGames[i] = lastGames[i - 1];
        }
        lastGames[0] = gameResult;
    }

    public String[] getLastGames() {
        return lastGames;
    }

    public void placeShip(Ship ship, int row, int col, boolean vertical) {
        ship.setPosition(row, col);
        ship.setVertical(vertical);
        
        for (int i = 0; i < ship.getSize(); i++) {
            int r = row + (vertical ? i : 0);
            int c = col + (vertical ? 0 : i);
            if (r >= 0 && r < 8 && c >= 0 && c < 8) {
                board[r][c] = ship;
            }
        }
    }

    public void removeShip(Ship ship) {
        if (ship == null) return;
        int startRow = ship.getRow();
        int startCol = ship.getCol();
        boolean vertical = ship.isVertical();
        
        for (int i = 0; i < ship.getSize(); i++) {
            int r = startRow + (vertical ? i : 0);
            int c = startCol + (vertical ? 0 : i);
            if (r >= 0 && r < 8 && c >= 0 && c < 8) {
                if (board[r][c] == ship) {
                    board[r][c] = null;
                }
            }
        }
    }

    public void clearBoard() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = null;
            }
        }
    }

    public void regenerateBoard(java.util.ArrayList<Ship> ships) {
        clearBoard();
        for (Ship ship : ships) {
            if (ship != null && !ship.isSunk()) {
                placeShip(ship, ship.getRow(), ship.getCol(), ship.isVertical());
            }
        }
    }
}
