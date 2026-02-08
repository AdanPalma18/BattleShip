package model;

public class Player {

    private String username;
    private String password;
    private int points;
    private String[] lastGames;
    private CellState[][] board;

    public Player(String username, String password) {
        this.username = username;
        this.password = password;
        this.points = 0;
        this.lastGames = new String[10];

        board = new CellState[8][8];
        initBoard();
    }

    private void initBoard() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = CellState.WATER;
            }
        }
    }


    public CellState[][] getBoard() {
        return board;
    }

    public CellState getCell(int row, int col) {
        return board[row][col];
    }

    public void setCell(int row, int col, CellState state) {
        board[row][col] = state;
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
}
