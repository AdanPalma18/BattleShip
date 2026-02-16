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

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
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
    
    public void clearEnemyView() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                enemyView[i][j] = CellState.WATER;
            }
        }
    }

    public void regenerateBoard(java.util.ArrayList<Ship> ships) {
        // NO limpiar todo el tablero - primero colocar los barcos hundidos en sus posiciones originales
        // Luego regenerar solo los barcos que NO están hundidos
        
        // Separar barcos hundidos de los activos
        java.util.ArrayList<Ship> sunkShips = new java.util.ArrayList<>();
        java.util.ArrayList<Ship> activeShips = new java.util.ArrayList<>();
        
        for (Ship ship : ships) {
            if (ship != null) {
                if (ship.isSunk()) {
                    sunkShips.add(ship);
                } else {
                    activeShips.add(ship);
                }
            }
        }
        
        // Limpiar solo las posiciones de los barcos activos (no hundidos)
        // Primero, guardar las posiciones de los barcos hundidos
        java.util.Set<java.awt.Point> sunkPositions = new java.util.HashSet<>();
        for (Ship sunkShip : sunkShips) {
            int row = sunkShip.getRow();
            int col = sunkShip.getCol();
            boolean vertical = sunkShip.isVertical();
            for (int i = 0; i < sunkShip.getSize(); i++) {
                int r = row + (vertical ? i : 0);
                int c = col + (vertical ? 0 : i);
                if (r >= 0 && r < 8 && c >= 0 && c < 8) {
                    sunkPositions.add(new java.awt.Point(r, c));
                }
            }
        }
        
        // Limpiar el tablero completamente primero
        clearBoard();
        
        // Colocar los barcos hundidos en sus posiciones originales (NO se mueven)
        for (Ship sunkShip : sunkShips) {
            int row = sunkShip.getRow();
            int col = sunkShip.getCol();
            boolean vertical = sunkShip.isVertical();
            // Usar placeShip para colocar el barco hundido
            placeShip(sunkShip, row, col, vertical);
        }
        
        // Ahora regenerar solo los barcos activos (que NO están hundidos)
        java.util.Random rand = new java.util.Random();
        
        // Ordenar barcos activos por tamaño (más grandes primero) para evitar problemas de solapamiento
        activeShips.sort((s1, s2) -> Integer.compare(s2.getSize(), s1.getSize()));
        
        for (Ship ship : activeShips) {
            if (ship != null && !ship.isSunk()) {
                // Intentar colocar el barco en una posición aleatoria
                boolean placed = false;
                int attempts = 0;
                int maxAttempts = 500; // Aumentar intentos para tableros más llenos
                
                // Guardar posición original como fallback
                int originalRow = ship.getRow();
                int originalCol = ship.getCol();
                boolean originalVertical = ship.isVertical();
                
                while (!placed && attempts < maxAttempts) {
                    int row = rand.nextInt(8);
                    int col = rand.nextInt(8);
                    boolean vertical = rand.nextBoolean();
                    
                    if (canPlaceShip(ship, row, col, vertical)) {
                        placeShip(ship, row, col, vertical);
                        placed = true;
                    }
                    attempts++;
                }
                
                // Si no se pudo colocar después de muchos intentos, intentar posición original
                if (!placed) {
                    // Verificar si la posición original está disponible
                    if (canPlaceShip(ship, originalRow, originalCol, originalVertical)) {
                        placeShip(ship, originalRow, originalCol, originalVertical);
                        placed = true;
                    } else {
                        // Si la posición original también está ocupada, intentar todas las posiciones posibles
                        boolean found = false;
                        for (int r = 0; r < 8 && !found; r++) {
                            for (int c = 0; c < 8 && !found; c++) {
                                for (boolean v : new boolean[]{true, false}) {
                                    if (canPlaceShip(ship, r, c, v)) {
                                        placeShip(ship, r, c, v);
                                        found = true;
                                        placed = true;
                                        break;
                                    }
                                }
                            }
                        }
                        
                        // Si aún no se pudo colocar, hay un problema serio (tablero muy lleno)
                        if (!placed) {
                            System.out.println("ERROR: No se pudo colocar el barco " + ship.getCode() + " después de todos los intentos.");
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Verifica si un barco puede ser colocado en la posición especificada
     * No puede solaparse con otros barcos (excepto si es el mismo barco en la misma posición)
     */
    private boolean canPlaceShip(Ship ship, int row, int col, boolean vertical) {
        for (int i = 0; i < ship.getSize(); i++) {
            int r = row + (vertical ? i : 0);
            int c = col + (vertical ? 0 : i);
            
            if (r < 0 || r >= 8 || c < 0 || c >= 8) {
                return false;
            }
            // Verificar si hay otro barco en esta posición (que no sea el mismo barco)
            if (board[r][c] != null && board[r][c] != ship) {
                return false;
            }
        }
        return true;
    }
}
