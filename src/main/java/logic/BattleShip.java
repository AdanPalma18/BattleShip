package logic;

import model.*;

import java.util.ArrayList;

public class BattleShip {
    private static ArrayList<Player> players = new ArrayList<>();
    private Player currentUser;
    private CellState[][] player1Board;
    private CellState[][] player2Board;

    private Player player1;
    private Player player2;

    private Difficulty difficulty;
    private Mode gameMode;
    private Player currentTurn;
    ArrayList<Ship> shipsP1;
    ArrayList<Ship> shipsP2;


    public boolean login(String username, String password) {
        for (Player player: players) {
            if (player.getUsername().equals(username) && player.checkPassword(password)) {
                currentUser = player;
                return true;
            }
        }
        return false;
    }

    public boolean register(String username, String password) {
        if (findPlayer(username) != null) return false;
        Player newPlayer = new Player(username, password);
        players.add(newPlayer);
        currentUser = newPlayer;
        return true;
    }

    private Player findPlayer(String username) {
        for (Player p : players) {
            if (p.getUsername().equals(username)) return p;
        }
        return null;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Mode getMode() {
        return gameMode;
    }

    public void setMode(Mode mode) {
        this.gameMode = mode;
    }

    public CellState shoot(int row, int col) {
        Player enemy = getEnemyPlayer();          // apuntamos al enemigo
        CellState current = enemy.getCell(row, col); // revisamos celda

        if (current == CellState.WATER) {
            enemy.setCell(row, col, CellState.MISS);
            System.out.println("Disparo en (" + row + "," + col + ") FALLASTE!");
            return CellState.MISS;
        }

        if (current == CellState.SHIP) {
            enemy.setCell(row, col, CellState.HIT);
            System.out.println("Disparo en (" + row + "," + col + ") ¡HIT!");
            return CellState.HIT;
        }

        System.out.println("Celda (" + row + "," + col + ") ya fue disparada.");
        return current;
    }


    public void nextTurn(){
        currentTurn = (currentTurn == player1) ? player2 : player1;
        System.out.println("Turno de: " + currentTurn.getUsername());
    }


    private Player getEnemyPlayer() {
        return (currentTurn == player1) ? player2 : player1;
    }

    public CellState[][] getEnemyBoard() {
        Player enemy = getEnemyPlayer();
        return enemy != null ? enemy.getBoard() : null;
    }

    public String getCurrentTurnUsername() {
        return currentTurn != null ? currentTurn.getUsername() : "";
    }
    public Player getCurrentUser() {
        return currentUser;
    }

    public boolean isMyTurn() {
        return currentTurn == currentUser;
    }

    public void startMatch(String enemyUsername) {
        Player enemy = findPlayer(enemyUsername);
        if (enemy == null) {
            System.out.println("El jugador enemigo no existe");
            return;
        }

        this.player1 = currentUser;  // jugador logueado
        this.player2 = enemy;        // jugador enemigo

        this.currentTurn = player1;  // empieza el jugador logueado
    }


}

