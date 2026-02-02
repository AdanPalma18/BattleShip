package model;

public class Player {

    private String username;
    private String password;
    private int points;
    private String[] lastGames;
    private int logIndex;
    private Board board;

    public Player(String username, String password) {
        this.username = username;
        this.password = password;
        this.points = 0;
        this.lastGames = new String[10];
        this.logIndex = 0;
        this.board = new Board();
    }

    public void addGameLog(String log) {
        for (int i = lastGames.length - 1; i > 0; i--) {
            lastGames[i] = lastGames[i - 1];
        }
        lastGames[0] = log;
    }

    public String[] getLastGames() {
        return lastGames;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (!username.isEmpty()) {
            this.username = username;
        }
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (password.length() >= 4) {
            this.password = password;
        }
    }

    public int getPoints() {
        return points;
    }

    public void addPoints(int p) {
        this.points += p;
    }

    public Board getBoard() {
        return board;
    }
}
