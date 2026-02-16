package logic;

import model.*;

import java.util.ArrayList;

public class BattleShip {
    private static ArrayList<Player> players = new ArrayList<>();
    private Player currentUser;

    private Player player1;
    private Player player2;

    private Difficulty difficulty = Difficulty.NORMAL; // Por defecto NORMAL según instrucciones
    private Mode gameMode = Mode.TUTORIAL; // Modo tutorial por defecto según instrucciones
    private Player currentTurn;
    private ArrayList<Ship> shipsP1;
    private ArrayList<Ship> shipsP2;
    private boolean placementPhase = true;
    private boolean player1Ready = false;
    private boolean player2Ready = false;
    private boolean lastShotRegenerated = false; // Indica si el último disparo regeneró el tablero
    private Ship lastHitShip = null; // Barco que fue golpeado en el último disparo
    private boolean lastShipWasSunk = false; // Indica si el último barco golpeado se hundió


    public boolean login(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        Player player = findPlayer(username.trim());
        if (player == null) {
            // Usuario no existe
            return false;
        }
        
        // Usuario existe, verificar contraseña
        if (player.checkPassword(password)) {
            currentUser = player;
            return true;
        }
        
        // Contraseña incorrecta
        return false;
    }
    
    /**
     * Verifica si un usuario existe en el sistema
     */
    public boolean playerExists(String username) {
        return findPlayer(username) != null;
    }

    public boolean register(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        
        if (findPlayer(username.trim()) != null) {
            return false; // Usuario ya existe
        }
        
        Player newPlayer = new Player(username.trim(), password.trim());
        players.add(newPlayer);
        currentUser = newPlayer;
        return true;
    }
    
    public void logout() {
        currentUser = null;
        resetGame(); // Resetear el juego al cerrar sesión
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
            lastShotRegenerated = false; // No se regenera en un MISS
            lastHitShip = null; // No hay barco en un MISS
            lastShipWasSunk = false;
            return CellState.MISS;
        }

        // Calcular qué parte del barco fue golpeada
        int shipRow = shipAtPosition.getRow();
        int shipCol = shipAtPosition.getCol();
        boolean vertical = shipAtPosition.isVertical();
        int partIndex;
        
        if (vertical) {
            partIndex = row - shipRow;
        } else {
            partIndex = col - shipCol;
        }
        
        // Verificar si esta parte ya fue golpeada antes
        if (shipAtPosition.isPartHit(partIndex)) {
            System.out.println("Disparo en (" + row + "," + col + ") - Esta parte del barco " + shipAtPosition.getCode() + " ya fue golpeada antes. No se regenera el tablero.");
            // Marcar como HIT (ya está golpeado) pero NO regenerar
            enemy.setEnemyViewCell(row, col, CellState.HIT);
            lastShotRegenerated = false; // No se regeneró porque ya estaba golpeado
            lastHitShip = shipAtPosition; // Guardar el barco para mostrar el mensaje
            lastShipWasSunk = shipAtPosition.isSunk(); // Verificar si ya estaba hundido
            // No contar como nuevo hit, no regenerar tablero
            return CellState.HIT;
        }

        // Marcar HIT temporalmente (se limpiará después de regenerar)
        enemy.setEnemyViewCell(row, col, CellState.HIT);
        System.out.println("Disparo en (" + row + "," + col + ") ¡HIT en parte " + partIndex + " del barco " + shipAtPosition.getCode() + "!");
        
        // Guardar el barco golpeado antes de regenerar
        lastHitShip = shipAtPosition;
        
        // Marcar esta parte específica como golpeada
        boolean newHit = shipAtPosition.hitPart(partIndex);
        if (!newHit) {
            System.out.println("ADVERTENCIA: La parte ya estaba golpeada, pero se intentó golpear de nuevo.");
        }
        
        boolean wasSunk = shipAtPosition.isSunk();
        lastShipWasSunk = wasSunk;
        
        if (wasSunk) {
            System.out.println("Barco hundido: " + shipAtPosition.getCode());
            for (int i = 0; i < shipAtPosition.getSize(); i++) {
                int r = shipAtPosition.getRow() + (shipAtPosition.isVertical() ? i : 0);
                int c = shipAtPosition.getCol() + (shipAtPosition.isVertical() ? 0 : i);
                enemy.setEnemyViewCell(r, c, CellState.SUNK);
            }
        }
        
        // Regenerar tablero después de HIT (Battleship Dinámico)
        // Esto limpiará el HIT temporal y reposicionará los barcos
        regenerateEnemyBoardAfterHit(enemy);
        lastShotRegenerated = true; // Se regeneró porque fue un hit nuevo
        
        // Verificar si el juego terminó (todos los barcos del enemigo hundidos)
        if (areAllEnemyShipsSunk()) {
            System.out.println("¡" + currentTurn.getUsername() + " ha ganado! Todos los barcos enemigos hundidos.");
            return CellState.SUNK; // Retornar SUNK para indicar que el juego terminó
        }
        
        if (wasSunk) {
            return CellState.SUNK;
        }
        return CellState.HIT;
    }
    
    /**
     * Regenera el tablero del enemigo después de un HIT, preservando el daño persistente.
     * Los barcos se reposicionan aleatoriamente, pero mantienen sus hits.
     * Solo se preservan los SUNK (barcos hundidos). Los MISS se regeneran porque los barcos cambian de posición.
     */
    private void regenerateEnemyBoardAfterHit(Player enemy) {
        // Obtener lista de barcos del enemigo
        ArrayList<Ship> enemyShips = (currentTurn == player1) ? shipsP2 : shipsP1;
        
        // Guardar solo los SUNK (barcos hundidos) - estos NO se regeneran
        CellState[][] savedEnemyView = new CellState[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (enemy.getEnemyViewCell(i, j) == CellState.SUNK) {
                    savedEnemyView[i][j] = CellState.SUNK;
                }
            }
        }
        
        // Limpiar TODO el enemyView (incluyendo MISS y HIT)
        // Los MISS se regeneran porque los barcos cambian de posición
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                enemy.setEnemyViewCell(i, j, CellState.WATER);
            }
        }
        
        // Restaurar solo los SUNK (barcos hundidos)
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (savedEnemyView[i][j] == CellState.SUNK) {
                    enemy.setEnemyViewCell(i, j, CellState.SUNK);
                }
            }
        }
        
        // Regenerar el tablero físico (esto reposiciona los barcos que no están hundidos)
        // Los barcos mantienen sus hits (daño persistente) pero cambian de posición
        enemy.regenerateBoard(enemyShips);
        
        // IMPORTANTE: NO marcar las casillas HIT después de regenerar
        // Esto revelaría la posición de los barcos. Solo mantener:
        // - SUNK: barcos hundidos (ya preservados arriba)
        // Los MISS se regeneran (vuelven a WATER) porque los barcos ya no están en esas posiciones
        // Los barcos mantienen sus hits (daño persistente) pero NO se muestran en el enemyView
        // El jugador solo verá los SUNK, los MISS anteriores desaparecen
        
        System.out.println("Tablero regenerado después de HIT. Daño persistente aplicado (oculto al jugador).");
        System.out.println("MISS regenerados (limpiados). Solo SUNK preservados.");
    }

    public void nextTurn(){
        currentTurn = (currentTurn == player1) ? player2 : player1;
        // NO cambiar currentUser aquí - currentUser es la sesión del usuario logueado
        // currentTurn es solo para el turno del juego
        System.out.println("Turno de: " + currentTurn.getUsername());
    }


    private Player getEnemyPlayer() {
        return (currentTurn == player1) ? player2 : player1;
    }
    
    /**
     * Obtiene el jugador enemigo del jugador actual (público para uso en UI)
     */
    public Player getEnemyPlayerPublic() {
        return getEnemyPlayer();
    }
    
    /**
     * Obtiene los barcos del enemigo para mostrar en modo tutorial
     */
    public Ship[][] getEnemyShipsBoard() {
        Player enemy = getEnemyPlayer();
        return enemy != null ? enemy.getBoard() : null;
    }
    
    /**
     * Obtiene la lista de barcos del enemigo
     */
    public ArrayList<Ship> getEnemyShipsList() {
        return (currentTurn == player1) ? shipsP2 : shipsP1;
    }
    
    /**
     * Obtiene la lista de barcos del jugador actual (currentTurn)
     */
    public ArrayList<Ship> getCurrentTurnShips() {
        if (currentTurn == null) return new ArrayList<>();
        ArrayList<Ship> ships = (currentTurn == player1) ? shipsP1 : shipsP2;
        return ships != null ? ships : new ArrayList<>();
    }
    
    /**
     * Cuenta cuántos barcos tiene el jugador actual
     */
    public int getCurrentTurnShipsCount() {
        ArrayList<Ship> ships = getCurrentTurnShips();
        return ships != null ? ships.size() : 0;
    }
    
    /**
     * Indica si el último disparo regeneró el tablero
     */
    public boolean wasLastShotRegenerated() {
        return lastShotRegenerated;
    }

    /**
     * Obtiene el barco que fue golpeado en el último disparo
     */
    public Ship getLastHitShip() {
        return lastHitShip;
    }

    /**
     * Indica si el último barco golpeado se hundió
     */
    public boolean wasLastShipSunk() {
        return lastShipWasSunk;
    }

    /**
     * Convierte el código del barco a su nombre completo
     */
    public static String getShipName(String code) {
        if (code == null) return "";
        switch (code.toUpperCase()) {
            case "PA": return "PORTAVIONES";
            case "AZ": return "ACORAZADO";
            case "SM": return "SUBMARINO";
            case "DT": return "DESTRUCTOR";
            default: return code;
        }
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
    
    /**
     * Obtiene todos los jugadores registrados (para ranking)
     */
    public ArrayList<Player> getAllPlayers() {
        return new ArrayList<>(players);
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

    /**
     * Verifica si todos los barcos del enemigo están hundidos
     */
    public boolean areAllEnemyShipsSunk() {
        if (placementPhase) {
            return false;
        }
        
        ArrayList<Ship> enemyShips = (currentTurn == player1) ? shipsP2 : shipsP1;
        
        for (Ship ship : enemyShips) {
            if (ship != null && !ship.isSunk()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Obtiene el ganador del juego (el jugador que hundió todos los barcos del enemigo)
     */
    public Player getWinner() {
        if (placementPhase) {
            return null;
        }
        
        // Si todos los barcos del enemigo están hundidos, el jugador actual es el ganador
        if (areAllEnemyShipsSunk()) {
            return currentTurn;
        }
        
        return null;
    }

    public boolean areAllShipsPlaced() {
        // Usar currentTurn para la lógica del juego, no currentUser (sesión)
        if (currentTurn == null) {
            System.out.println("DEBUG areAllShipsPlaced: currentTurn es null!");
            return false;
        }
        
        // Contar barcos colocados según la lista de barcos, no las celdas del tablero
        ArrayList<Ship> ships = (currentTurn == player1) ? shipsP1 : shipsP2;
        int shipsPlaced = ships != null ? ships.size() : 0;
        
        // Obtener el número requerido según la dificultad
        int requiredShips = difficulty != null ? difficulty.getShipsAllowed() : 4; // Por defecto NORMAL
        
        System.out.println("DEBUG areAllShipsPlaced: shipsPlaced=" + shipsPlaced + ", requiredShips=" + requiredShips + ", difficulty=" + (difficulty != null ? difficulty.name() : "null"));
        
        return shipsPlaced >= requiredShips;
    }

    public Player surrender() {
        System.out.println("=== INICIO SURRENDER ===");
        System.out.println("currentUser (sesión)=" + (currentUser != null ? currentUser.getUsername() : "null"));
        System.out.println("player1=" + (player1 != null ? player1.getUsername() : "null"));
        System.out.println("player2=" + (player2 != null ? player2.getUsername() : "null"));
        System.out.println("currentTurn=" + (currentTurn != null ? currentTurn.getUsername() : "null"));
        
        // Usar currentTurn para determinar quién se rinde (no currentUser que es la sesión)
        if (currentTurn == null) {
            System.out.println("ERROR: currentTurn es null, no se puede rendir");
            return null;
        }
        
        System.out.println(currentTurn.getUsername() + " se ha rendido.");
        Player loser = currentTurn;
        
        // Determinar el ganador basándose en quién es el oponente del currentTurn
        Player winner = getEnemyPlayer();
        System.out.println("DEBUG: currentTurn es " + (currentTurn == player1 ? "player1" : "player2") + ", ganador es " + (winner == player1 ? "player1" : "player2"));
        
        // Verificar que winner y loser sean diferentes (no null)
        if (winner == null || loser == null) {
            System.out.println("ERROR: winner o loser es null en surrender()");
            System.out.println("DEBUG: winner=" + (winner != null ? winner.getUsername() : "null"));
            System.out.println("DEBUG: loser=" + (loser != null ? loser.getUsername() : "null"));
            return winner;
        }
        
        if (winner == loser) {
            System.out.println("ERROR: winner y loser son el mismo jugador!");
            System.out.println("DEBUG: winner=" + winner.getUsername());
            System.out.println("DEBUG: loser=" + loser.getUsername());
            return winner;
        }
        
        String gameResult = model.GameLog.retiro(loser.getUsername(), winner.getUsername());
        System.out.println("DEBUG: gameResult=" + gameResult);
        
        System.out.println("DEBUG: Guardando historial para winner: " + winner.getUsername());
        winner.addGameToHistory(gameResult);
        System.out.println("DEBUG: Guardando historial para loser: " + loser.getUsername());
        loser.addGameToHistory(gameResult);
        
        System.out.println("DEBUG: Puntos de winner antes: " + winner.getPoints());
        // El ganador recibe 3 puntos según las especificaciones (no 10)
        winner.addPoints(3);
        System.out.println("DEBUG: Puntos de winner después: " + winner.getPoints());
        
        System.out.println("DEBUG surrender: " + loser.getUsername() + " se rindió, ganador: " + winner.getUsername());
        System.out.println("=== FIN SURRENDER ===");
        
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

    /**
     * Inicia una partida con un jugador enemigo
     * @param enemyUsername Username del jugador enemigo
     * @return true si la partida se inició correctamente, false si hubo un error
     */
    public boolean startMatch(String enemyUsername) {
        System.out.println("=== INICIO startMatch ===");
        System.out.println("currentUser=" + (currentUser != null ? currentUser.getUsername() : "null"));
        System.out.println("enemyUsername=" + enemyUsername);
        
        // Validar que haya un usuario logueado
        if (currentUser == null) {
            System.out.println("ERROR: No hay usuario logueado");
            return false;
        }
        
        // Validar que se proporcionó un username
        if (enemyUsername == null || enemyUsername.trim().isEmpty()) {
            System.out.println("ERROR: Username del enemigo vacío");
            return false;
        }
        
        Player enemy = findPlayer(enemyUsername.trim());
        if (enemy == null) {
            System.out.println("El jugador enemigo no existe");
            return false;
        }
        
        System.out.println("enemy encontrado=" + enemy.getUsername());
        
        // Validar que el enemigo no sea el mismo que el jugador actual
        if (enemy == currentUser) {
            System.out.println("ERROR: No puedes jugar contra ti mismo!");
            return false;
        }

        // Asegurar que el modo esté en TUTORIAL por defecto
        if (this.gameMode == null) {
            this.gameMode = Mode.TUTORIAL;
        }

        this.player1 = currentUser;
        this.player2 = enemy;
        this.currentTurn = player1;
        this.placementPhase = true;
        this.player1Ready = false;
        this.player2Ready = false;
        this.shipsP1 = new ArrayList<>();
        this.shipsP2 = new ArrayList<>();
        
        // Limpiar los tableros y enemyViews de ambos jugadores para empezar fresh
        player1.clearBoard();
        player1.clearEnemyView();
        player2.clearBoard();
        player2.clearEnemyView();
        
        System.out.println("player1 asignado=" + (player1 != null ? player1.getUsername() : "null"));
        System.out.println("player2 asignado=" + (player2 != null ? player2.getUsername() : "null"));
        System.out.println("=== FIN startMatch ===");
        return true;
    }

    public void resetGame() {
        // Limpiar tableros de jugadores
        if (player1 != null) {
            player1.clearBoard();
            player1.clearEnemyView();
        }
        if (player2 != null) {
            player2.clearBoard();
            player2.clearEnemyView();
        }
        
        // Resetear todas las variables de estado
        this.player1 = null;
        this.player2 = null;
        this.currentTurn = null;
        this.placementPhase = true; // Volver a fase de colocación
        this.player1Ready = false;
        this.player2Ready = false;
        this.shipsP1 = null;
        this.shipsP2 = null;
        this.lastShotRegenerated = false;
        this.lastHitShip = null;
        this.lastShipWasSunk = false;
    }

    public void addShip(String shipCode, int size, int row, int col, boolean vertical) {
        Ship ship = new Ship(shipCode, size, null);
        
        // Usar currentTurn para la lógica del juego, no currentUser (sesión)
        if (currentTurn == player1) {
            shipsP1.add(ship);
            currentTurn.placeShip(ship, row, col, vertical);
        } else if (currentTurn == player2) {
            shipsP2.add(ship);
            currentTurn.placeShip(ship, row, col, vertical);
        }
    }

    public void removeShip(String shipCode) {
        // Usar currentTurn para la lógica del juego, no currentUser (sesión)
        // Remueve el PRIMER barco del tipo encontrado (para compatibilidad)
        if (currentTurn == player1) {
            Ship shipToRemove = shipsP1.stream()
                .filter(s -> s.getCode().equals(shipCode))
                .findFirst()
                .orElse(null);
            if (shipToRemove != null) {
                currentTurn.removeShip(shipToRemove);
                shipsP1.remove(shipToRemove);
            }
        } else if (currentTurn == player2) {
            Ship shipToRemove = shipsP2.stream()
                .filter(s -> s.getCode().equals(shipCode))
                .findFirst()
                .orElse(null);
            if (shipToRemove != null) {
                currentTurn.removeShip(shipToRemove);
                shipsP2.remove(shipToRemove);
            }
        }
    }
    
    /**
     * Remueve un barco específico de la lista (para mover barcos sin afectar duplicados)
     */
    public void removeSpecificShip(Ship shipToRemove) {
        if (shipToRemove == null) return;
        
        if (currentTurn == player1) {
            if (shipsP1 != null && shipsP1.contains(shipToRemove)) {
                currentTurn.removeShip(shipToRemove);
                shipsP1.remove(shipToRemove);
            }
        } else if (currentTurn == player2) {
            if (shipsP2 != null && shipsP2.contains(shipToRemove)) {
                currentTurn.removeShip(shipToRemove);
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

    /**
     * Modifica los datos del jugador actual (username y/o password)
     * @param newUsername Nuevo username (puede ser vacío si no se quiere cambiar)
     * @param newPassword Nuevo password (puede ser vacío si no se quiere cambiar)
     * @return true si se modificó exitosamente, false si hubo error (username ya existe)
     */
    public boolean modifyPlayerData(String newUsername, String newPassword) {
        if (currentUser == null) {
            return false;
        }

        // Si se quiere cambiar el username, verificar que no esté en uso
        if (newUsername != null && !newUsername.trim().isEmpty()) {
            // Verificar que el nuevo username no esté en uso por otro jugador
            Player existingPlayer = findPlayer(newUsername.trim());
            if (existingPlayer != null && existingPlayer != currentUser) {
                return false; // El username ya está en uso
            }
            currentUser.setUsername(newUsername.trim());
        }

        // Si se quiere cambiar el password
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            currentUser.setPassword(newPassword.trim());
        }

        return true;
    }

    /**
     * Elimina la cuenta del jugador actual de la colección de jugadores
     * @return true si se eliminó exitosamente, false si hubo error
     */
    public boolean deleteCurrentUser() {
        if (currentUser == null) {
            return false;
        }

        // Remover el jugador de la colección
        boolean removed = players.remove(currentUser);
        
        // Limpiar la sesión
        currentUser = null;
        
        return removed;
    }
}

