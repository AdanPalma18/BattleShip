package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.*;
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
        clearDrag();
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
    
    // Método para iniciar el drag simulado desde GamePanel
    public void startSimulatedDrag(String shipType, int size) {
        // Solo permitir drag durante la fase de colocación
        if (gamePanel != null && gamePanel.getBattleShip().isPlacementPhase()) {
            isDragging = true;
            currentDragShipType = shipType;
            currentDragSize = size;
            currentDragVertical = defaultDragVertical;
            requestFocusInWindow();
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
        
        // Verificar si hay otro barco
        boolean hasOtherShip = false;
        for (int i = 0; i < currentDragSize; i++) {
            int r = row + (vertical ? i : 0);
            int c = col + (vertical ? 0 : i);
            if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                if (cells[r][c].hasShip()) {
                    String existingShipType = cells[r][c].getShipType();
                    if (currentDragShipType == null || !existingShipType.equals(currentDragShipType)) {
                        hasOtherShip = true;
                        break;
                    }
                }
            }
        }
        
        if (hasOtherShip) {
            clearDrag();
            return;
        }
        
        // Colocar el barco
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
        repaint();
    }

    public void setTutorialMode(boolean tutorialMode) {
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
        boolean shipExists = false;
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (cells[i][j].hasShip() && cells[i][j].getShipType().equals(shipType)) {
                    shipExists = true;
                    break;
                }
            }
            if (shipExists) break;
        }
        
        if (shipExists) {
            removeShipWithoutClearingHandlers(shipType);
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
            gamePanel.hideShipLabel(shipType);
            gamePanel.saveShipToBattleShip(shipType, size, row, col, vertical);
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

    private void cleanupOldShipHandlers(String shipType) {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (!cells[i][j].hasShip() && cells[i][j].getTransferHandler() != null) {
                    cells[i][j].setTransferHandler(null);
                }
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
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (cells[i][j].hasShip() && cells[i][j].getShipType().equals(shipType)) {
                    BoardCell.ShipInfo info = cells[i][j].getShipInfo();
                    if (info != null) {
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
        cell.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                // Solo permitir drag durante la fase de colocación
                if (gamePanel != null && gamePanel.getBattleShip().isPlacementPhase()) {
                    // Obtener el tipo y tamaño del barco desde la celda
                    BoardCell.ShipInfo info = cell.getShipInfo();
                    if (info != null) {
                        // Iniciar drag simulado con el barco que está en esta celda
                        startSimulatedDrag(info.shipType, info.size);
                    } else {
                        // Fallback si no hay info
                        startSimulatedDrag(shipType, size);
                    }
                }
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
