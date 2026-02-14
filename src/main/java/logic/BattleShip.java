package logic;

import model.*;

import java.util.ArrayList;

public class BattleShip {
    private static ArrayList<Player> players = new ArrayList<>();
    private Player currentUser;

    private Player player1;
    private Player player2;

    private Difficulty difficulty;
    private Mode gameMode;
    private Player currentTurn;
    private ArrayList<Ship> shipsP1;
    private ArrayList<Ship> shipsP2;
    private boolean placementPhase = true;
    private boolean player1Ready = false;
    private boolean player2Ready = false;


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
        if (placementPhase) {
            System.out.println("No puedes disparar durante la fase de colocación.");
            return null;
        }
        
        Player enemy = getEnemyPlayer();
        CellState currentView = enemy.getEnemyView()[row][col];

        if (currentView != CellState.WATER && currentView != CellState.SHIP) {
            System.out.println("Celda (" + row + "," + col + ") ya fue disparada.");
            return currentView;
        }

        Ship[][] enemyBoard = enemy.getBoard();
        Ship shipAtPosition = enemyBoard[row][col];

        if (shipAtPosition == null) {
            enemy.setEnemyViewCell(row, col, CellState.MISS);
            System.out.println("Disparo en (" + row + "," + col + ") FALLASTE!");
            return CellState.MISS;
        }

        enemy.setEnemyViewCell(row, col, CellState.HIT);
        System.out.println("Disparo en (" + row + "," + col + ") ¡HIT!");
        shipAtPosition.hit();
        System.out.println("Barco tiro: " + shipAtPosition.getCode());
        
        if (shipAtPosition.isSunk()) {
            System.out.println("Barco hundido: " + shipAtPosition.getCode());
            for (int i = 0; i < shipAtPosition.getSize(); i++) {
                int r = shipAtPosition.getRow() + (shipAtPosition.isVertical() ? i : 0);
                int c = shipAtPosition.getCol() + (shipAtPosition.isVertical() ? 0 : i);
                enemy.setEnemyViewCell(r, c, CellState.SUNK);
            }
            return CellState.SUNK;
        }
        return CellState.HIT;
    }

    public void nextTurn(){
        currentTurn = (currentTurn == player1) ? player2 : player1;
        currentUser = currentTurn;
        System.out.println("Turno de: " + currentTurn.getUsername());
    }


    private Player getEnemyPlayer() {
        return (currentTurn == player1) ? player2 : player1;
    }

    public CellState[][] getEnemyBoard() {
        Player enemy = getEnemyPlayer();
        System.out.println("DEBUG getEnemyBoard: currentTurn=" + (currentTurn != null ? currentTurn.getUsername() : "null"));
        System.out.println("DEBUG getEnemyBoard: enemy=" + (enemy != null ? enemy.getUsername() : "null"));
        return enemy != null ? enemy.getEnemyView() : null;
    }

    public String getCurrentTurnUsername() {
        return currentTurn != null ? currentTurn.getUsername() : "";
    }

    public String getNextTurnUsername() {
        Player nextTurn = (currentTurn == player1) ? player2 : player1;
        return nextTurn != null ? nextTurn.getUsername() : "";
    }
    public Player getCurrentUser() {
        return currentUser;
    }

    public boolean isMyTurn() {
        return currentTurn == currentUser;
    }

    public boolean isPlacementPhase() {
        return placementPhase;
    }

    public boolean areBothPlayersReady() {
        return player1Ready && player2Ready;
    }

    public void setPlayerReady(boolean ready) {
        System.out.println("DEBUG setPlayerReady: currentTurn=" + (currentTurn != null ? currentTurn.getUsername() : "null"));
        System.out.println("DEBUG setPlayerReady: player1=" + (player1 != null ? player1.getUsername() : "null"));
        System.out.println("DEBUG setPlayerReady: player2=" + (player2 != null ? player2.getUsername() : "null"));
        
        if (currentTurn == player1) {
            player1Ready = ready;
            System.out.println("DEBUG setPlayerReady: Marcando player1 como ready=" + ready);
        } else if (currentTurn == player2) {
            player2Ready = ready;
            System.out.println("DEBUG setPlayerReady: Marcando player2 como ready=" + ready);
        } else {
            System.out.println("DEBUG setPlayerReady: currentTurn no coincide con player1 ni player2!");
        }
        
        System.out.println("DEBUG setPlayerReady: player1Ready=" + player1Ready + ", player2Ready=" + player2Ready);
        
        if (player1Ready && player2Ready) {
            placementPhase = false;
            currentTurn = player1;
            System.out.println("Fase de colocación terminada. Comienza la batalla!");
            System.out.println("Turno de: " + currentTurn.getUsername());
        }
    }

    public Player getWinner() {
        if (placementPhase) {
            return null;
        }
        return currentTurn;
    }

    public boolean areAllShipsPlaced() {
        if (currentUser == null) {
            System.out.println("DEBUG areAllShipsPlaced: currentUser es null!");
            return false;
        }
        int shipsPlaced = 0;
        Ship[][] board = currentUser.getBoard();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (board[i][j] != null) {
                    shipsPlaced++;
                }
            }
        }
        System.out.println("DEBUG areAllShipsPlaced: shipsPlaced=" + shipsPlaced + " (necesita 14)");
        return shipsPlaced == 14;
    }

    public Player surrender() {
        System.out.println(currentUser.getUsername() + " se ha rendido.");
        Player winner = getEnemyPlayer();
        Player loser = currentUser;
        
        String gameResult = model.GameLog.retiro(loser.getUsername(), winner.getUsername());
        winner.addGameToHistory(gameResult);
        loser.addGameToHistory(gameResult);
        winner.addPoints(10);
        
        return winner;
    }

    public void continueToNextTurn() {
        if (placementPhase) {
            if (areAllShipsPlaced()) {
                boolean bothReadyBefore = player1Ready && player2Ready;
                setPlayerReady(true);
                boolean bothReadyAfter = player1Ready && player2Ready;
                
                if (bothReadyAfter && !bothReadyBefore) {
                    return;
                }
                
                if (placementPhase && !bothReadyAfter) {
                    nextTurn();
                }
            } else {
                System.out.println("Debes colocar todos los barcos antes de continuar.");
            }
        } else {
            nextTurn();
        }
    }

    public void startMatch(String enemyUsername) {
        Player enemy = findPlayer(enemyUsername);
        if (enemy == null) {
            System.out.println("El jugador enemigo no existe");
            return;
        }

        this.player1 = currentUser;
        this.player2 = enemy;
        this.currentTurn = player1;
        this.placementPhase = true;
        this.player1Ready = false;
        this.player2Ready = false;
        this.shipsP1 = new ArrayList<>();
        this.shipsP2 = new ArrayList<>();
    }

    public void resetGame() {
        this.player1 = null;
        this.player2 = null;
        this.currentTurn = null;
        this.placementPhase = false;
        this.player1Ready = false;
        this.player2Ready = false;
        this.shipsP1 = null;
        this.shipsP2 = null;
    }

    public void addShip(String shipCode, int size, int row, int col, boolean vertical) {
        Ship ship = new Ship(shipCode, size, null);
        
        if (currentUser == player1) {
            shipsP1.add(ship);
            currentUser.placeShip(ship, row, col, vertical);
        } else if (currentUser == player2) {
            shipsP2.add(ship);
            currentUser.placeShip(ship, row, col, vertical);
        }
    }

    public void removeShip(String shipCode) {
        if (currentUser == player1) {
            Ship shipToRemove = shipsP1.stream()
                .filter(s -> s.getCode().equals(shipCode))
                .findFirst()
                .orElse(null);
            if (shipToRemove != null) {
                currentUser.removeShip(shipToRemove);
                shipsP1.remove(shipToRemove);
            }
        } else if (currentUser == player2) {
            Ship shipToRemove = shipsP2.stream()
                .filter(s -> s.getCode().equals(shipCode))
                .findFirst()
                .orElse(null);
            if (shipToRemove != null) {
                currentUser.removeShip(shipToRemove);
                shipsP2.remove(shipToRemove);
            }
        }
    }

    public Ship findShipAtPosition(ArrayList<Ship> enemyShips, int row, int col) {

        for (Ship ship : enemyShips) {
            if(ship.isVertical()) {
                for (int i = 0; i < ship.getSize(); i++) {
                    if (ship.getRow() + i == row && ship.getCol() == col) {
                        return ship;
                    }
                }
            } else {
                for (int i = 0; i < ship.getSize(); i++) {
                    if (ship.getRow() == row && ship.getCol() + i == col) {
                        return ship;
                    }
                }
            }
        }
        return null;
    }
}

