package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;

import model.BoardCellListener;
import model.CellState;

public class BoardPanel extends JPanel {

    private final int ROWS = 8;
    private final int COLS = 8;
    private final BoardCell[][] cells = new BoardCell[ROWS][COLS];
    private int previewShipSize = -1;
    private int previewRow = -1;
    private int previewCol = -1;
    private boolean previewVertical = true;
    private int currentDragSize = -1;
    private boolean currentDragVertical = true;
    private boolean defaultDragVertical = true;
    private boolean isDragging = false;
    private GamePanel gamePanel;
    private String currentDragShipType = null;
    private boolean tutorialMode = false;
    private boolean dragFromSidebar = false; // Indica si el drag viene del sidebar (true) o del tablero (false)
    private int draggedShipOriginalRow = -1; // Posición original del barco cuando se arrastra desde el tablero
    private int draggedShipOriginalCol = -1;
    private boolean draggedShipOriginalVertical = false;
    private boolean showingModal = false; // Bandera para evitar modales infinitos
    
    public boolean isDragging() {
        return isDragging;
    }
    
    public void handleDragEvent(int x, int y) {
        handleDrag(x, y);
    }
    
    public void handleDropEvent(int x, int y) {
        handleDrop(x, y);
    }
    
    public void cancelDrag() {
        // Si el barco venía del tablero y se suelta fuera, devolverlo al sidebar
        if (!dragFromSidebar && currentDragShipType != null && draggedShipOriginalRow >= 0 && draggedShipOriginalCol >= 0) {
            // Devolver el barco al sidebar
            if (gamePanel != null) {
                gamePanel.showShipLabel(currentDragShipType);
            }
        }
        clearDrag();
    }
    
    public boolean isDragFromBoard() {
        return !dragFromSidebar;
    }
    
    public String getCurrentDragShipType() {
        return currentDragShipType;
    }

    public BoardPanel(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
        setLayout(new GridLayout(ROWS, COLS));

        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                BoardCell cell = new BoardCell(i, j);
                cells[i][j] = cell;
                add(cell);
            }
        }

        // Sistema de drag simulado con eventos de mouse
        setFocusable(true);
        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_R && isDragging) {
                    handleRotation();
                } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_R && !isDragging) {
                    defaultDragVertical = !defaultDragVertical;
                }
            }
        });
        
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (isDragging && currentDragSize > 0 && currentDragShipType != null) {
                    handleDrop(e.getX(), e.getY());
                }
            }
        });
        
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (isDragging && currentDragSize > 0) {
                    handleDrag(e.getX(), e.getY());
                }
            }
        });
    }
    
    // Método para iniciar el drag simulado desde GamePanel (sidebar)
    public void startSimulatedDrag(String shipType, int size) {
        // Solo permitir drag durante la fase de colocación
        if (gamePanel != null && gamePanel.getBattleShip().isPlacementPhase()) {
            System.out.println("DEBUG startSimulatedDrag: Iniciando drag desde sidebar - shipType=" + shipType + ", size=" + size);
            isDragging = true;
            currentDragShipType = shipType;
            currentDragSize = size;
            currentDragVertical = defaultDragVertical;
            dragFromSidebar = true; // Viene del sidebar
            requestFocusInWindow();
        } else {
            System.out.println("DEBUG startSimulatedDrag: NO se puede iniciar drag - isPlacementPhase=" + (gamePanel != null && gamePanel.getBattleShip() != null ? gamePanel.getBattleShip().isPlacementPhase() : false));
        }
    }
    
    // Método para iniciar el drag desde el tablero
    public void startSimulatedDragFromBoard(String shipType, int size, int originalRow, int originalCol, boolean originalVertical) {
        // Solo permitir drag durante la fase de colocación
        if (gamePanel != null && gamePanel.getBattleShip().isPlacementPhase()) {
            System.out.println("DEBUG startSimulatedDragFromBoard: Iniciando drag desde tablero - shipType=" + shipType + ", size=" + size + ", row=" + originalRow + ", col=" + originalCol);
            isDragging = true;
            currentDragShipType = shipType;
            currentDragSize = size;
            currentDragVertical = originalVertical; // Mantener la orientación original
            dragFromSidebar = false; // Viene del tablero
            draggedShipOriginalRow = originalRow;
            draggedShipOriginalCol = originalCol;
            draggedShipOriginalVertical = originalVertical;
            requestFocusInWindow();
        } else {
            System.out.println("DEBUG startSimulatedDragFromBoard: NO se puede iniciar drag - isPlacementPhase=" + (gamePanel != null && gamePanel.getBattleShip() != null ? gamePanel.getBattleShip().isPlacementPhase() : false));
        }
    }
    
    private void handleDrag(int x, int y) {
        if (getWidth() <= 0 || getHeight() <= 0) return;
        
        int cellW = getWidth() / COLS;
        int cellH = getHeight() / ROWS;
        
        int col = Math.max(0, Math.min(COLS - 1, x / cellW));
        int row = Math.max(0, Math.min(ROWS - 1, y / cellH));
        
        boolean vertical = currentDragVertical;
        boolean canShowPreview = true;
        
        // Verificar si hay otro barco en esa posición
        for (int i = 0; i < currentDragSize; i++) {
            int r = row + (vertical ? i : 0);
            int c = col + (vertical ? 0 : i);
            if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                if (cells[r][c].hasShip()) {
                    String existingShipType = cells[r][c].getShipType();
                    if (currentDragShipType == null || !existingShipType.equals(currentDragShipType)) {
                        canShowPreview = false;
                        break;
                    }
                }
            }
        }
        
        if (canShowPreview) {
            showPreview(row, col, currentDragSize, vertical);
        } else {
            clearPreview();
        }
    }
    
    private void handleDrop(int x, int y) {
        if (getWidth() <= 0 || getHeight() <= 0) {
            clearDrag();
            return;
        }
        
                    int cellW = getWidth() / COLS;
                    int cellH = getHeight() / ROWS;

        int col = Math.max(0, Math.min(COLS - 1, x / cellW));
        int row = Math.max(0, Math.min(ROWS - 1, y / cellH));
        
        boolean vertical = currentDragVertical;
        boolean fits;
        if (vertical) {
            fits = (row + currentDragSize <= ROWS);
        } else {
            fits = (col + currentDragSize <= COLS);
        }
        
        if (!fits) {
            clearDrag();
            return;
        }
        
        // Verificar si hay otro barco en la posición donde se quiere colocar
        boolean hasOtherShip = false;
        for (int i = 0; i < currentDragSize; i++) {
            int r = row + (vertical ? i : 0);
            int c = col + (vertical ? 0 : i);
            if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                if (cells[r][c].hasShip()) {
                    String existingShipType = cells[r][c].getShipType();
                    // Si es un tipo diferente, no se puede colocar
                    if (currentDragShipType == null || !existingShipType.equals(currentDragShipType)) {
                        hasOtherShip = true;
                        break;
                    }
                    // Si es el mismo tipo:
                    // - Si viene del sidebar en modo EASY: permitir (crear duplicado en otra posición)
                    // - Si viene del tablero: permitir (mover el barco)
                    // - En otros modos: no permitir solapar (pero esto se maneja en placeShip)
                    // Por ahora, permitimos soltar si es el mismo tipo (la validación final se hace en placeShip)
                }
            }
        }
        
        // Solo bloquear si hay un barco de tipo DIFERENTE
        // Si es el mismo tipo, permitir soltar (la validación de límite y duplicados se hace en placeShip)
        if (hasOtherShip) {
            clearDrag();
            return;
        }
        
        // Validar límite ANTES de llamar a placeShip para evitar alertas infinitas
        // SOLO si viene del sidebar (nuevo barco o duplicado)
        if (dragFromSidebar && currentDragShipType != null) {
            logic.BattleShip bs = gamePanel != null ? gamePanel.getBattleShip() : null;
            if (bs != null) {
                model.Difficulty difficulty = bs.getDifficulty();
                if (difficulty == null) {
                    difficulty = model.Difficulty.NORMAL;
                }
                
                int totalShipsPlaced = bs.getCurrentTurnShipsCount();
                int maxShips = difficulty.getShipsAllowed();
                boolean shipExists = checkShipExists(currentDragShipType);
                
                // Contar cuántos barcos de este tipo hay en el tablero visualmente
                int countOfThisType = 0;
                for (int i = 0; i < ROWS; i++) {
                    for (int j = 0; j < COLS; j++) {
                        if (cells[i][j].hasShip() && cells[i][j].getShipType().equals(currentDragShipType)) {
                            BoardCell.ShipInfo info = cells[i][j].getShipInfo();
                            if (info != null) {
                                // Solo contar una vez por barco (usando startRow y startCol)
                                boolean isStart = (info.startRow == i && info.startCol == j);
                                if (isStart) {
                                    countOfThisType++;
                                }
                            }
                        }
                    }
                }
                
                System.out.println("DEBUG handleDrop: totalShipsPlaced=" + totalShipsPlaced + ", maxShips=" + maxShips + ", shipType=" + currentDragShipType + ", shipExists=" + shipExists + ", countOfThisType=" + countOfThisType);
                
                // Si viene del sidebar y no existe el barco, se agregará uno nuevo
                boolean willAddNewShip = !shipExists;
                
                // Validación especial para EASY mode con duplicados
                if (difficulty == model.Difficulty.EASY && shipExists) {
                    // En EASY, si el barco ya existe, se puede crear un duplicado (excepto Destructor)
                    if (currentDragShipType.equals("DT")) {
                        // Destructor solo puede haber 1
                        System.out.println("DEBUG handleDrop: BLOQUEANDO Destructor duplicado");
                        if (!showingModal) {
                            showingModal = true;
                            JOptionPane.showMessageDialog(
                                this,
                                "Solo se permite 1 Destructor, incluso en modo EASY.",
                                "Barco no permitido",
                                JOptionPane.WARNING_MESSAGE
                            );
                            showingModal = false;
                        }
                        clearDrag();
                        return;
                    }
                    // Se puede crear duplicado, pero verificar límite
                    willAddNewShip = true; // Se agregará un nuevo barco (duplicado)
                    System.out.println("DEBUG handleDrop: Permitir duplicado en EASY, willAddNewShip=true");
                }
                
                // Si se va a agregar un barco nuevo y ya hay maxShips, bloquear ANTES de placeShip
                if (willAddNewShip && totalShipsPlaced >= maxShips) {
                    System.out.println("DEBUG handleDrop: BLOQUEANDO - willAddNewShip=" + willAddNewShip + ", totalShipsPlaced=" + totalShipsPlaced + ", maxShips=" + maxShips);
                    // Solo mostrar modal si no se está mostrando uno ya (evitar modales infinitos)
                    if (!showingModal) {
                        showingModal = true;
                        JOptionPane.showMessageDialog(
                            this,
                            "Ya has colocado el máximo de barcos permitidos (" + maxShips + ") para la dificultad " + difficulty.name() + ".\n" +
                            "Barcos colocados: " + totalShipsPlaced + "\n" +
                            "No puedes colocar más barcos.",
                            "Límite alcanzado",
                            JOptionPane.WARNING_MESSAGE
                        );
                        showingModal = false;
                    }
                    clearDrag();
                    return;
                }
                
                System.out.println("DEBUG handleDrop: PERMITIENDO - willAddNewShip=" + willAddNewShip + ", totalShipsPlaced=" + totalShipsPlaced + ", maxShips=" + maxShips);
            }
        }
        
        // Colocar el barco (solo si pasó todas las validaciones)
        if (currentDragShipType != null) {
            placeShip(currentDragShipType, currentDragSize, row, col, vertical);
        }
        
        clearDrag();
    }
    
    private void clearDrag() {
        clearPreview();
        currentDragSize = -1;
        currentDragShipType = null;
        isDragging = false;
        dragFromSidebar = false;
        draggedShipOriginalRow = -1;
        draggedShipOriginalCol = -1;
        draggedShipOriginalVertical = false;
        showingModal = false; // Resetear bandera de modal
    }
    
    private void handleRotation() {
        // Permitir rotación si hay un drag activo, incluso si no hay preview (cuando sales del tablero)
        if (isDragging && currentDragSize > 0) {
            currentDragVertical = !currentDragVertical;
            // Si hay preview activo, actualizarlo con la nueva orientación
            if (previewShipSize > 0 && previewRow >= 0 && previewCol >= 0) {
                showPreview(previewRow, previewCol, previewShipSize, currentDragVertical);
            }
            // Si no hay preview pero hay drag activo, solo cambiar la orientación
            // La próxima vez que el mouse entre al tablero, usará la nueva orientación
                }
            }
    
    public void rotateCurrentPreview() {
        handleRotation();
    }

    public void setCellListener(BoardCellListener listener) {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                cells[i][j].setListener(listener);
            }
        }
    }

    public void updateBoard(CellState[][] board) {
        if (board == null) return;
        for (int i = 0; i < ROWS && i < board.length; i++) {
            for (int j = 0; j < COLS && j < board[i].length; j++) {
                cells[i][j].setState(board[i][j]);
            }
        }
        
        // En modo tutorial, cargar los barcos enemigos
        if (tutorialMode && gamePanel != null) {
            loadEnemyShips();
        } else {
            // En modo ARCADE, limpiar TODAS las imágenes, shipInfo y TransferHandler
            // para evitar interferencias con los disparos
            for (int i = 0; i < ROWS; i++) {
                for (int j = 0; j < COLS; j++) {
                    cells[i][j].clearShipImage();
                    cells[i][j].clearShipInfo();
                    // Asegurarse de que no haya TransferHandler durante la batalla
                    if (gamePanel != null && !gamePanel.getBattleShip().isPlacementPhase()) {
                        cells[i][j].setTransferHandler(null);
                    }
                }
            }
        }
        
        repaint();
    }
    
    /**
     * Carga y muestra los barcos enemigos en el tablero (solo en modo tutorial)
     */
    private void loadEnemyShips() {
        if (gamePanel == null || gamePanel.getBattleShip() == null) return;
        
        logic.BattleShip battleShip = gamePanel.getBattleShip();
        java.util.ArrayList<model.Ship> enemyShips = battleShip.getEnemyShipsList();
        
        if (enemyShips == null) return;
        
        int cellSize = getCellSize();
        if (cellSize <= 0) {
            cellSize = 420 / 8;
        }
        
        // Obtener el enemyView para verificar qué celdas pueden mostrar barcos
        model.CellState[][] enemyView = battleShip.getEnemyBoard();
        
        // Limpiar TODAS las imágenes de barcos primero, sin importar el estado
        // Luego solo colocaremos barcos en celdas que tengan estado WATER o SHIP en el enemyView
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                // Verificar el estado en el enemyView
                model.CellState viewState = (enemyView != null && i < enemyView.length && j < enemyView[i].length) 
                    ? enemyView[i][j] 
                    : model.CellState.WATER;
                
                // Limpiar imagen de barco si la celda tiene estado especial (MISS, HIT, SUNK)
                // O si no tiene estado WATER o SHIP en el enemyView
                if (viewState != model.CellState.WATER && viewState != model.CellState.SHIP) {
                    cells[i][j].clearShipImage();
                    cells[i][j].clearShipInfo();
                } else {
                    // También limpiar si tiene estado WATER o SHIP, para luego colocar correctamente
                    cells[i][j].clearShipImage();
                    cells[i][j].clearShipInfo();
                }
            }
        }
        
        // Cargar los barcos enemigos desde la lista
        for (model.Ship ship : enemyShips) {
            if (ship == null || ship.isSunk()) continue;
            
            int shipRow = ship.getRow();
            int shipCol = ship.getCol();
            boolean vertical = ship.isVertical();
            String shipType = ship.getCode();
            int size = ship.getSize();
            
            // Colocar cada parte del barco
            for (int i = 0; i < size; i++) {
                int r = shipRow + (vertical ? i : 0);
                int c = shipCol + (vertical ? 0 : i);
                
                if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                    // Verificar el estado en el enemyView - solo mostrar barco si es WATER o SHIP
                    // NO mostrar en celdas con MISS, HIT, o SUNK
                    model.CellState viewState = (enemyView != null && r < enemyView.length && c < enemyView[r].length) 
                        ? enemyView[r][c] 
                        : model.CellState.WATER;
                    
                    // Solo mostrar el barco si la celda en el enemyView es WATER o SHIP
                    // No mostrar en celdas con MISS, HIT, o SUNK
                    if (viewState != model.CellState.WATER && viewState != model.CellState.SHIP) {
                        // Esta celda tiene un estado especial (MISS, HIT, SUNK), no mostrar barco aquí
                        continue;
                    }
                    
                    int partNumber = i + 1;
                    String imagePath = "/ships/" + shipType + "_" + partNumber + ".png";
                    
                    java.net.URL resource = getClass().getResource(imagePath);
                    if (resource != null) {
                        ImageIcon icon = new ImageIcon(resource);
                        
                        java.awt.image.BufferedImage originalBuffered = new java.awt.image.BufferedImage(
                            icon.getIconWidth(), icon.getIconHeight(), 
                            java.awt.image.BufferedImage.TYPE_INT_ARGB
                        );
                        java.awt.Graphics2D g = originalBuffered.createGraphics();
                        g.drawImage(icon.getImage(), 0, 0, null);
                        g.dispose();
                        
                        java.awt.image.BufferedImage scaled = new java.awt.image.BufferedImage(
                            cellSize, cellSize, java.awt.image.BufferedImage.TYPE_INT_ARGB
                        );
                        java.awt.Graphics2D g2 = scaled.createGraphics();
                        g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, 
                                           java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g2.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, 
                                           java.awt.RenderingHints.VALUE_RENDER_QUALITY);
                        g2.drawImage(originalBuffered, 0, 0, cellSize, cellSize, null);
                        g2.dispose();
                        
                        Image finalImage = scaled;
                        if (!vertical) {
                            finalImage = rotateImage90(scaled, cellSize);
                            if (finalImage == null) {
                                finalImage = scaled;
                            }
                        }
                        
                        cells[r][c].setShipImage(finalImage, shipType + "_" + partNumber);
                        cells[r][c].setShipInfo(shipType, size, shipRow, shipCol, vertical, i);
                        
                        // Solo establecer estado SHIP si no tiene otro estado (HIT, MISS, SUNK)
                        CellState currentState = cells[r][c].getState();
                        if (currentState == model.CellState.WATER) {
                            cells[r][c].setState(model.CellState.SHIP);
                        }
                    }
                }
            }
        }
    }

    public void setTutorialMode(boolean tutorialMode) {
        this.tutorialMode = tutorialMode;
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                cells[i][j].setTutorialMode(tutorialMode);
            }
        }
    }

    private void showPreview(int row, int col, int size, boolean vertical) {
        if (previewShipSize == size && previewRow == row && previewCol == col && previewVertical == vertical) {
            return;
        }
        
        clearPreview();
        
        previewShipSize = size;
        previewRow = row;
        previewCol = col;
        previewVertical = vertical;

        boolean fits = true;
        if (vertical) {
            if (row + size > ROWS) fits = false;
        } else {
            if (col + size > COLS) fits = false;
        }

        if (!fits) {
            previewShipSize = -1;
            return;
        }

        for (int i = 0; i < size; i++) {
            int r = row + (vertical ? i : 0);
            int c = col + (vertical ? 0 : i);
            if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                cells[r][c].setPreview(true);
            }
        }
        
        repaint();
    }

    public void clearPreview() {
        if (previewShipSize > 0) {
            for (int i = 0; i < previewShipSize; i++) {
                int r = previewRow + (previewVertical ? i : 0);
                int c = previewCol + (previewVertical ? 0 : i);
                if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                    cells[r][c].setPreview(false);
                }
            }
            previewShipSize = -1;
            repaint();
        }
    }

    private void placeShip(String shipType, int size, int row, int col, boolean vertical) {
        // Obtener la dificultad actual
        logic.BattleShip battleShip = gamePanel != null ? gamePanel.getBattleShip() : null;
        model.Difficulty difficulty = battleShip != null ? battleShip.getDifficulty() : null;
        if (difficulty == null) {
            difficulty = model.Difficulty.NORMAL; // Por defecto
        }
        
        // Verificar si ya hay un barco del mismo tipo en el tablero
        boolean shipExists = checkShipExists(shipType);
        
        // Contar barcos totales colocados actualmente (no tipos únicos)
        int totalShipsPlaced = 0;
        if (battleShip != null) {
            totalShipsPlaced = battleShip.getCurrentTurnShipsCount();
        } else {
            // Fallback: contar desde el tablero visual
            totalShipsPlaced = countShipsPlaced();
        }
        
        int maxShips = difficulty.getShipsAllowed();
        
        // Debug: mostrar cuántos barcos hay
        System.out.println("DEBUG placeShip: totalShipsPlaced=" + totalShipsPlaced + ", maxShips=" + maxShips + ", shipType=" + shipType + ", dragFromSidebar=" + dragFromSidebar + ", shipExists=" + shipExists);
        
        // Verificar límite ANTES de colocar
        // Si viene del sidebar y no existe el barco, se agregará uno nuevo (+1)
        // Si viene del tablero o ya existe, solo se mueve (no cambia el conteo)
        boolean willAddNewShip = dragFromSidebar && !shipExists;
        
        // En modo EASY, si viene del sidebar y el barco ya existe, también se agregará uno nuevo (duplicado)
        if (difficulty == model.Difficulty.EASY && dragFromSidebar && shipExists && !shipType.equals("DT")) {
            willAddNewShip = true; // Se agregará un duplicado
        }
        
        // IMPORTANTE: Solo validar si viene del sidebar (nuevo barco o duplicado)
        // Si viene del tablero, es un movimiento y NO debe validarse aquí (ya se validó en handleDrop si era necesario)
        if (dragFromSidebar && willAddNewShip && totalShipsPlaced >= maxShips) {
            // Si se va a agregar un barco nuevo y ya hay maxShips, bloquear
            System.out.println("DEBUG placeShip: BLOQUEANDO - willAddNewShip=" + willAddNewShip + ", totalShipsPlaced=" + totalShipsPlaced + ", maxShips=" + maxShips);
            // NO mostrar modal aquí - ya se mostró en handleDrop() para evitar modales infinitos
            // Solo retornar sin colocar
            return;
        }
        
        // Si viene del tablero o ya existe el barco, es un movimiento, no un nuevo barco
        // Permitir mover incluso si ya hay maxShips (porque no aumenta el conteo)
        
        // Validar según las reglas:
        // - EASY: Permite 5 barcos, puede repetir uno (excepto Destructor que solo puede haber 1)
        // - Otros modos: No permite duplicados
        if (shipExists) {
            // Si viene del tablero, es un movimiento - permitir siempre (no aumenta el conteo)
            if (!dragFromSidebar) {
                // Viene del tablero: mover el barco existente (remover de posición anterior)
                // NO validar límites ni restricciones de duplicados - es solo un movimiento
                // Usar la posición original guardada cuando se inició el drag
                if (draggedShipOriginalRow >= 0 && draggedShipOriginalCol >= 0) {
                    removeShipAtPosition(draggedShipOriginalRow, draggedShipOriginalCol, shipType, size, draggedShipOriginalVertical);
                } else {
                    // Fallback: buscar en la posición destino (comportamiento anterior)
                    removeShipAtPosition(row, col, shipType, size, vertical);
                }
            } else {
                // Viene del sidebar: validar si se puede crear duplicado
                if (difficulty == model.Difficulty.EASY) {
                    // En EASY, se puede repetir un barco, pero Destructor solo puede haber 1
                    if (shipType.equals("DT")) {
                        // Destructor ya existe, no se puede colocar otro
                        JOptionPane.showMessageDialog(
                            this,
                            "Solo se permite 1 Destructor, incluso en modo EASY.",
                            "Barco no permitido",
                            JOptionPane.WARNING_MESSAGE
                        );
                        return;
                    }
                    // Verificar que después de colocar este barco no exceda el límite
                    int totalShipsAfter = totalShipsPlaced + 1; // Nuevo barco desde sidebar
                    
                    if (totalShipsAfter > maxShips) {
                        JOptionPane.showMessageDialog(
                            this,
                            "Ya has colocado el máximo de barcos permitidos (" + maxShips + ").\n" +
                            "Barcos colocados: " + totalShipsPlaced + "\n" +
                            "No puedes colocar más barcos.",
                            "Límite alcanzado",
                            JOptionPane.WARNING_MESSAGE
                        );
                        return;
                    }
                    // Viene del sidebar en EASY: crear duplicado, NO remover el anterior
                    // NO llamar a removeShipWithoutClearingHandlers aquí
                } else {
                    // En otros modos, no se permiten duplicados - remover el anterior y colocar el nuevo (mover)
                    removeShipWithoutClearingHandlers(shipType);
                }
            }
        }
        
        int cellSize = getCellSize();
        if (cellSize <= 0) {
            cellSize = 420 / 8;
        }

        for (int i = 0; i < size; i++) {
            int r = row + (vertical ? i : 0);
            int c = col + (vertical ? 0 : i);
            
            if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                int partNumber = i + 1;
                String imagePath = "/ships/" + shipType + "_" + partNumber + ".png";
                
                java.net.URL resource = getClass().getResource(imagePath);
                if (resource != null) {
                    ImageIcon icon = new ImageIcon(resource);
                    
                    java.awt.image.BufferedImage originalBuffered = new java.awt.image.BufferedImage(
                        icon.getIconWidth(), icon.getIconHeight(), 
                        java.awt.image.BufferedImage.TYPE_INT_ARGB
                    );
                    java.awt.Graphics2D g = originalBuffered.createGraphics();
                    g.drawImage(icon.getImage(), 0, 0, null);
                    g.dispose();
                    
                    java.awt.image.BufferedImage scaled = new java.awt.image.BufferedImage(
                        cellSize, cellSize, java.awt.image.BufferedImage.TYPE_INT_ARGB
                    );
                    java.awt.Graphics2D g2 = scaled.createGraphics();
                    g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, 
                                       java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, 
                                       java.awt.RenderingHints.VALUE_RENDER_QUALITY);
                    g2.drawImage(originalBuffered, 0, 0, cellSize, cellSize, null);
                    g2.dispose();
                    
                    Image finalImage = scaled;
                    if (!vertical) {
                        System.out.println("DEBUG ROTATE: Rotando imagen para barco horizontal - shipType=" + shipType + ", part=" + partNumber);
                        finalImage = rotateImage90(scaled, cellSize);
                        if (finalImage == null) {
                            System.out.println("ERROR: rotateImage90 retornó null, usando imagen sin rotar");
                            finalImage = scaled;
                        } else {
                            System.out.println("DEBUG ROTATE: Imagen rotada exitosamente");
                        }
                    }
                    
                    cells[r][c].setShipImage(finalImage, shipType + "_" + partNumber);
                    cells[r][c].setState(model.CellState.SHIP);
                    cells[r][c].setShipInfo(shipType, size, row, col, vertical, i);
                    
                    makeCellDraggable(cells[r][c], shipType, size);
                } else {
                    System.out.println("No se encontró imagen: " + imagePath);
                }
            }
        }
        
        if (gamePanel != null) {
            // Si viene del tablero, es un movimiento - NO crear duplicado, solo mover
            boolean createDuplicate = false;
            boolean skipRemove = false; // Flag para indicar si ya se removió el barco
            
            if (!dragFromSidebar) {
                // Movimiento desde el tablero: El barco ya fue removido en removeShipAtPosition()
                // NO debemos remover de nuevo, solo agregar en la nueva posición
                skipRemove = true; // Ya se removió el barco específico
                createDuplicate = false; // No es un duplicado, es un movimiento
            } else {
                // Viene del sidebar: determinar si se debe crear duplicado
                if (difficulty == model.Difficulty.EASY && shipExists) {
                    // En modo EASY, si viene del sidebar y ya existe el barco, crear duplicado
                    createDuplicate = true;
                }
            }
            
            // Validar límite ANTES de guardar (para movimientos, esto no debería ser un problema)
            int shipsBefore = battleShip != null ? battleShip.getCurrentTurnShipsCount() : countShipsPlaced();
            
            // Guardar el barco
            if (skipRemove) {
                // El barco ya fue removido, solo agregarlo
                gamePanel.addShipToBattleShip(shipType, size, row, col, vertical);
            } else {
                // Comportamiento normal: puede remover o no según createDuplicate
                gamePanel.saveShipToBattleShip(shipType, size, row, col, vertical, createDuplicate);
            }
            
            // Recontar barcos después de guardar
            int shipsAfter = battleShip != null ? battleShip.getCurrentTurnShipsCount() : countShipsPlaced();
            System.out.println("DEBUG placeShip: shipsBefore=" + shipsBefore + ", shipsAfter=" + shipsAfter + ", maxShips=" + maxShips + ", dragFromSidebar=" + dragFromSidebar + ", createDuplicate=" + createDuplicate);
            
            // Verificar límite después de guardar (doble verificación de seguridad)
            // Solo verificar si se agregó un barco nuevo (no si solo se movió)
            boolean addedNewShip = createDuplicate || (!shipExists && dragFromSidebar);
            if (addedNewShip) {
                // Se agregó un barco nuevo, verificar límite
                if (shipsAfter > maxShips) {
                    System.out.println("ERROR: Se excedió el límite de barcos! shipsAfter=" + shipsAfter + ", maxShips=" + maxShips);
                    // Remover el barco que acabamos de agregar
                    // Buscar y remover el último barco agregado de este tipo
                    if (battleShip != null) {
                        java.util.ArrayList<model.Ship> ships = battleShip.getCurrentTurnShips();
                        if (ships != null) {
                            // Remover el último barco de este tipo que se agregó
                            for (int i = ships.size() - 1; i >= 0; i--) {
                                model.Ship ship = ships.get(i);
                                if (ship != null && ship.getCode().equals(shipType)) {
                                    if (ship.getRow() == row && ship.getCol() == col && ship.isVertical() == vertical) {
                                        battleShip.removeSpecificShip(ship);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    // Remover del tablero visual
                    removeShipFromBoardSafely(shipType);
                    JOptionPane.showMessageDialog(
                        this,
                        "Error: Se excedió el límite de barcos permitidos (" + maxShips + ").\n" +
                        "Barcos colocados: " + shipsAfter + "\n" +
                        "El barco no se ha colocado.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
            }
            
            // Actualizar labels según el tipo de operación
            if (!dragFromSidebar) {
                // Es un movimiento desde el tablero: NO mostrar el label, debe permanecer oculto
                // El barco solo se está moviendo, no se está eliminando
                gamePanel.hideShipLabel(shipType);
            } else {
                // Es una colocación nueva desde el sidebar
                // En modo EASY, si aún no se han colocado 5 barcos, mostrar el barco de nuevo
                // para permitir colocar un duplicado (excepto Destructor)
                if (difficulty == model.Difficulty.EASY && battleShip != null) {
                    // Si aún no se alcanzó el límite y no es Destructor, mostrar el barco de nuevo
                    if (shipsAfter < maxShips && !shipType.equals("DT")) {
                        gamePanel.showShipLabel(shipType);
                    } else {
                        // Si es Destructor o se alcanzó el límite, ocultar el barco
                        gamePanel.hideShipLabel(shipType);
                    }
                } else {
                    // En otros modos, ocultar el barco después de colocarlo
                    gamePanel.hideShipLabel(shipType);
                }
            }
        }
        
        if (shipExists) {
            cleanupOldShipHandlers(shipType);
        }
        
        repaint();
    }

    private void removeShip(String shipType) {
        removeShipFromBoard(shipType);
    }

    private boolean checkShipExists(String shipType) {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (cells[i][j].hasShip() && cells[i][j].getShipType().equals(shipType)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Cuenta cuántos barcos únicos hay colocados en el tablero
     */
    private int countShipsPlaced() {
        java.util.Set<String> uniqueShips = new java.util.HashSet<>();
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (cells[i][j].hasShip()) {
                    String shipType = cells[i][j].getShipType();
                    if (shipType != null && !shipType.isEmpty()) {
                        uniqueShips.add(shipType);
                    }
                }
            }
        }
        return uniqueShips.size();
    }

    private void cleanupOldShipHandlers(String shipType) {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (!cells[i][j].hasShip() && cells[i][j].getTransferHandler() != null) {
                    cells[i][j].setTransferHandler(null);
                }
            }
        }
    }

    /**
     * Remueve un barco específico en una posición dada (para mover barcos)
     */
    private void removeShipAtPosition(int originalRow, int originalCol, String shipType, int size, boolean vertical) {
        // Buscar el barco que está en la posición ORIGEN (donde estaba antes de moverlo)
        java.util.Set<java.awt.Point> cellsToClear = new java.util.HashSet<>();
        model.Ship shipToRemove = null;
        
        // Buscar el barco en la posición original
        for (int k = 0; k < size; k++) {
            int r = originalRow + (vertical ? k : 0);
            int c = originalCol + (vertical ? 0 : k);
            if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                if (cells[r][c].hasShip() && cells[r][c].getShipType().equals(shipType)) {
                    // Esta celda tiene el barco que se está moviendo - obtener toda su información
                    BoardCell.ShipInfo info = cells[r][c].getShipInfo();
                    if (info != null && info.startRow == originalRow && info.startCol == originalCol && info.vertical == vertical) {
                        // Este es el barco correcto - remover todas sus celdas
                        for (int i = 0; i < info.size; i++) {
                            int shipR = info.startRow + (info.vertical ? i : 0);
                            int shipC = info.startCol + (info.vertical ? 0 : i);
                            if (shipR >= 0 && shipR < ROWS && shipC >= 0 && shipC < COLS) {
                                cellsToClear.add(new java.awt.Point(shipR, shipC));
                            }
                        }
                        // Buscar el barco en BattleShip para removerlo específicamente
                        if (gamePanel != null) {
                            logic.BattleShip bs = gamePanel.getBattleShip();
                            if (bs != null) {
                                java.util.ArrayList<model.Ship> ships = bs.getCurrentTurnShips();
                                if (ships != null) {
                                    for (model.Ship ship : ships) {
                                        if (ship != null && ship.getCode().equals(shipType)) {
                                            if (ship.getRow() == info.startRow && 
                                                ship.getCol() == info.startCol && 
                                                ship.isVertical() == info.vertical) {
                                                shipToRemove = ship;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        break; // Ya encontramos el barco, salir
                    }
                }
            }
        }
        
        // Limpiar las celdas del barco que se está moviendo
        for (java.awt.Point p : cellsToClear) {
            int r = p.x;
            int c = p.y;
            cells[r][c].clearShipImage();
            cells[r][c].setState(model.CellState.WATER);
            cells[r][c].clearShipInfo();
            // Eliminar todos los listeners de mouse para limpiar
            for (java.awt.event.MouseListener ml : cells[r][c].getMouseListeners()) {
                cells[r][c].removeMouseListener(ml);
            }
        }
        
        // Remover el barco específico de BattleShip
        if (shipToRemove != null && gamePanel != null) {
            logic.BattleShip bs = gamePanel.getBattleShip();
            if (bs != null) {
                bs.removeSpecificShip(shipToRemove);
            }
        }
    }
    
    private void removeShipWithoutClearingHandlers(String shipType) {
        java.util.Set<java.awt.Point> cellsToClear = new java.util.HashSet<>();
        
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (cells[i][j].hasShip() && cells[i][j].getShipType().equals(shipType)) {
                    BoardCell.ShipInfo info = cells[i][j].getShipInfo();
                    if (info != null) {
                        for (int k = 0; k < info.size; k++) {
                            int r = info.startRow + (info.vertical ? k : 0);
                            int c = info.startCol + (info.vertical ? 0 : k);
                            if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                                cellsToClear.add(new java.awt.Point(r, c));
                            }
                        }
                    }
                }
            }
        }
        
        for (java.awt.Point p : cellsToClear) {
            int r = p.x;
            int c = p.y;
            cells[r][c].clearShipImage();
            cells[r][c].setState(model.CellState.WATER);
            cells[r][c].clearShipInfo();
            // Eliminar todos los listeners de mouse para limpiar
            for (java.awt.event.MouseListener ml : cells[r][c].getMouseListeners()) {
                cells[r][c].removeMouseListener(ml);
            }
        }
        
        // Mostrar el label de nuevo en el sidebar cuando se elimina un barco
        if (gamePanel != null) {
            gamePanel.showShipLabel(shipType);
        }
    }

    public void removeShipFromBoard(String shipType) {
        removeShipFromBoardSafely(shipType);
    }

    public void removeShipFromBoardSafely(String shipType) {
        System.out.println("DEBUG removeShipFromBoardSafely: Removiendo barco " + shipType);
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (cells[i][j].hasShip() && cells[i][j].getShipType().equals(shipType)) {
                    BoardCell.ShipInfo info = cells[i][j].getShipInfo();
                    if (info != null) {
                        // Limpiar las celdas visualmente
                        for (int k = 0; k < info.size; k++) {
                            int r = info.startRow + (info.vertical ? k : 0);
                            int c = info.startCol + (info.vertical ? 0 : k);
                            if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                                cells[r][c].clearShipImage();
                                cells[r][c].setState(model.CellState.WATER);
                                cells[r][c].clearShipInfo();
                                cells[r][c].setTransferHandler(new TransferHandler("text") {
                                    @Override
                                    protected Transferable createTransferable(JComponent c) {
                                        return new StringSelection("");
                                    }
                                    @Override
                                    public int getSourceActions(JComponent c) {
                                        return NONE;
                                    }
                                    @Override
                                    protected void exportDone(JComponent c, Transferable data, int action) {
                                    }
                                });
                            }
                        }
                        // Remover el barco de la lógica de BattleShip y mostrar en sidebar
                        if (gamePanel != null) {
                            gamePanel.removeShipFromBattleShip(shipType);
                            gamePanel.showShipLabel(shipType);
                        }
                        repaint();
                        break;
                    }
                }
            }
        }
    }

    private void makeCellDraggable(BoardCell cell, String shipType, int size) {
        // Sistema de drag simulado: agregar MouseListener para iniciar drag desde el tablero
        // Solo durante la fase de colocación
        // IMPORTANTE: NO consumir el evento para que mouseClicked también funcione
        cell.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                // Solo permitir drag durante la fase de colocación
                if (gamePanel != null && gamePanel.getBattleShip().isPlacementPhase()) {
                    // Obtener el tipo y tamaño del barco desde la celda
                    BoardCell.ShipInfo info = cell.getShipInfo();
                    if (info != null) {
                        // Iniciar drag simulado con el barco que está en esta celda (desde el tablero)
                        startSimulatedDragFromBoard(info.shipType, info.size, info.startRow, info.startCol, info.vertical);
                        // NO consumir el evento - permitir que mouseClicked también funcione si es necesario
                    } else {
                        // Fallback si no hay info - buscar en la celda actual
                        int row = -1, col = -1;
                        boolean vert = false;
                        for (int i = 0; i < ROWS; i++) {
                            for (int j = 0; j < COLS; j++) {
                                if (cells[i][j] == cell && cells[i][j].hasShip()) {
                                    BoardCell.ShipInfo cellInfo = cells[i][j].getShipInfo();
                                    if (cellInfo != null) {
                                        row = cellInfo.startRow;
                                        col = cellInfo.startCol;
                                        vert = cellInfo.vertical;
                                        break;
                                    }
                                }
                            }
                        }
                        if (row >= 0 && col >= 0) {
                            startSimulatedDragFromBoard(shipType, size, row, col, vert);
                        } else {
                            startSimulatedDragFromBoard(shipType, size, -1, -1, false);
                        }
                    }
                }
                // NO consumir el evento - permitir que mouseClicked funcione durante la batalla
            }
        });
    }

    public int getCellSize() {
        int width = getWidth();
        int height = getHeight();
        
        if (width > 0 && height > 0) {
            return Math.min(width / COLS, height / ROWS);
        }
        
        Dimension prefSize = getPreferredSize();
        if (prefSize != null && prefSize.width > 0) {
            return Math.min(prefSize.width / COLS, prefSize.height / ROWS);
        }
        
        return 420 / 8;
    }

    public CellState[][] getBoardState() {
        CellState[][] board = new CellState[ROWS][COLS];
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                board[i][j] = cells[i][j].getState();
            }
        }
        return board;
    }

    public void clearBoard() {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                cells[i][j].clearShipImage();
                cells[i][j].clearShipInfo();
                cells[i][j].setState(model.CellState.WATER);
                cells[i][j].setTransferHandler(null);
            }
        }
        repaint();
    }

    public void loadBoardFromPlayer(CellState[][] board) {
        clearBoard();
        
        if (board != null) {
            for (int i = 0; i < ROWS && i < board.length; i++) {
                for (int j = 0; j < COLS && j < board[i].length; j++) {
                cells[i][j].setState(board[i][j]);
            }
        }
    }
        repaint();
    }
    

    private Image rotateImage90(java.awt.image.BufferedImage image, int size) {
        if (image == null) {
            System.out.println("ERROR: rotateImage90 recibió imagen null");
            return null;
        }
        
        java.awt.image.BufferedImage buffered = new java.awt.image.BufferedImage(
            size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB
        );
        java.awt.Graphics2D g2d = buffered.createGraphics();
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, 
                             java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, 
                             java.awt.RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                             java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.translate(0, size);
        g2d.rotate(-Math.PI / 2);
        g2d.drawImage(image, 0, 0, size, size, null);
        g2d.dispose();
        return buffered;
    }
    
}
