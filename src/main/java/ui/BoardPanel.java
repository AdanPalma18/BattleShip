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
    private GamePanel gamePanel; // Referencia al GamePanel para acceder a los métodos

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

        setDropTarget(new DropTarget(this, DnDConstants.ACTION_COPY, new DropTargetAdapter() {

            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                System.out.println("dragEnter: DataFlavors disponibles: " + java.util.Arrays.toString(dtde.getCurrentDataFlavors()));
                
                // Intentar encontrar cualquier DataFlavor que represente String
                DataFlavor stringFlavor = null;
                for (DataFlavor flavor : dtde.getCurrentDataFlavors()) {
                    // Verificar si es String directamente o si la clase de representación es String
                    if (flavor.isFlavorTextType() || 
                        flavor.equals(DataFlavor.stringFlavor) ||
                        String.class.equals(flavor.getRepresentationClass())) {
                        stringFlavor = flavor;
                        System.out.println("dragEnter: Encontrado DataFlavor: " + flavor);
                        break;
                    }
                }
                
                if (stringFlavor == null && dtde.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    stringFlavor = DataFlavor.stringFlavor;
                }
                
                if (stringFlavor != null) {
                    try {
                        String data = (String) dtde.getTransferable().getTransferData(stringFlavor);
                        System.out.println("dragEnter: Datos recibidos: " + data);
                        String[] parts = data.split(":");
                        if (parts.length >= 2) {
                            currentDragSize = Integer.parseInt(parts[1]);
                            System.out.println("dragEnter: Tamaño del barco: " + currentDragSize);
                            dtde.acceptDrag(DnDConstants.ACTION_COPY);
                        } else {
                            System.out.println("dragEnter: Formato de datos inválido");
                            currentDragSize = -1;
                            dtde.rejectDrag();
                        }
                    } catch (Exception e) {
                        System.out.println("dragEnter error: " + e.getMessage());
                        e.printStackTrace();
                        currentDragSize = -1;
                        dtde.rejectDrag();
                    }
                } else {
                    System.out.println("dragEnter: No se encontró DataFlavor de string");
                    dtde.rejectDrag();
                }
            }

            @Override
            public void dragOver(DropTargetDragEvent dtde) {
                // Verificar si hay algún DataFlavor que represente String
                boolean hasStringFlavor = false;
                for (DataFlavor flavor : dtde.getCurrentDataFlavors()) {
                    if (flavor.isFlavorTextType() || 
                        flavor.equals(DataFlavor.stringFlavor) ||
                        String.class.equals(flavor.getRepresentationClass())) {
                        hasStringFlavor = true;
                        break;
                    }
                }
                
                if (!hasStringFlavor && !dtde.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    // No mostrar mensaje repetitivo, solo limpiar preview
                    clearPreview();
                    dtde.rejectDrag();
                    return;
                }
                
                if (currentDragSize <= 0) {
                    clearPreview();
                    dtde.rejectDrag();
                    return;
                }

                try {
                    Point p = dtde.getLocation();
                    int cellW = getWidth() / COLS;
                    int cellH = getHeight() / ROWS;

                    int col = Math.max(0, Math.min(COLS - 1, p.x / cellW));
                    int row = Math.max(0, Math.min(ROWS - 1, p.y / cellH));

                    // Usar orientación vertical fija
                    boolean vertical = true;

                    // Verificar que no haya otros barcos en las celdas del preview
                    // (permitir si es el mismo barco que se está moviendo)
                    boolean canShowPreview = true;
                    String draggedShipType = null;
                    
                    // Intentar obtener el tipo del barco que se está arrastrando
                    DataFlavor dragStringFlavor = null;
                    for (DataFlavor flavor : dtde.getCurrentDataFlavors()) {
                        if (flavor.isFlavorTextType() || 
                            flavor.equals(DataFlavor.stringFlavor) ||
                            String.class.equals(flavor.getRepresentationClass())) {
                            dragStringFlavor = flavor;
                            break;
                        }
                    }
                    
                    if (dragStringFlavor == null && dtde.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        dragStringFlavor = DataFlavor.stringFlavor;
                    }
                    
                    if (dragStringFlavor != null) {
                        try {
                            String data = (String) dtde.getTransferable().getTransferData(dragStringFlavor);
                            String[] parts = data.split(":");
                            if (parts.length >= 1) {
                                draggedShipType = parts[0];
                            }
                        } catch (Exception e) {
                            // Si no se puede obtener, continuar sin validación de tipo
                        }
                    }

                    for (int i = 0; i < currentDragSize; i++) {
                        int r = row + (vertical ? i : 0);
                        int c = col + (vertical ? 0 : i);
                        if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                            if (cells[r][c].hasShip()) {
                                String existingShipType = cells[r][c].getShipType();
                                // Permitir preview si es el mismo barco (está moviéndolo)
                                if (draggedShipType == null || !existingShipType.equals(draggedShipType)) {
                                    canShowPreview = false;
                                    break;
                                }
                            }
                        }
                    }

                    if (canShowPreview) {
                        showPreview(row, col, currentDragSize, vertical);
                        dtde.acceptDrag(DnDConstants.ACTION_COPY);
                    } else {
                        clearPreview();
                        dtde.rejectDrag();
                    }

                } catch (Exception e) {
                    System.out.println("dragOver error: " + e.getMessage());
                    clearPreview();
                    dtde.rejectDrag();
                }
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
                clearPreview();
                currentDragSize = -1;
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                System.out.println("Drop event recibido");
                System.out.println("Drop: DataFlavors disponibles: " + java.util.Arrays.toString(dtde.getCurrentDataFlavors()));
                
                // Intentar encontrar cualquier DataFlavor que represente String
                DataFlavor stringFlavor = null;
                for (DataFlavor flavor : dtde.getCurrentDataFlavors()) {
                    if (flavor.isFlavorTextType() || 
                        flavor.equals(DataFlavor.stringFlavor) ||
                        String.class.equals(flavor.getRepresentationClass())) {
                        stringFlavor = flavor;
                        System.out.println("Drop: Encontrado DataFlavor: " + flavor);
                        break;
                    }
                }
                
                if (stringFlavor == null && dtde.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    stringFlavor = DataFlavor.stringFlavor;
                }
                
                if (stringFlavor == null) {
                    System.out.println("Drop: No se encontró DataFlavor de string");
                    dtde.rejectDrop();
                    clearPreview();
                    return;
                }

                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    System.out.println("Drop aceptado");

                    String data = (String) dtde.getTransferable().getTransferData(stringFlavor);
                    System.out.println("Datos recibidos: " + data);

                    String[] parts = data.split(":");
                    if (parts.length < 2) {
                        System.out.println("Formato de datos inválido");
                        dtde.dropComplete(false);
                        clearPreview();
                        return;
                    }

                    String shipType = parts[0];
                    int size = Integer.parseInt(parts[1]);

                    Point p = dtde.getLocation();
                    if (getWidth() <= 0 || getHeight() <= 0) {
                        System.out.println("Panel sin tamaño");
                        dtde.dropComplete(false);
                        clearPreview();
                        return;
                    }

                    int cellW = getWidth() / COLS;
                    int cellH = getHeight() / ROWS;

                    int col = Math.max(0, Math.min(COLS - 1, p.x / cellW));
                    int row = Math.max(0, Math.min(ROWS - 1, p.y / cellH));

                    // Verificar si cabe en la posición
                    boolean vertical = true;
                    boolean fits = (row + size <= ROWS);

                    if (!fits) {
                        System.out.println(
                                "No se puede colocar " + shipType +
                                " (tamaño " + size + ") en (" + row + "," + col +
                                ") - No cabe en el tablero"
                        );
                        clearPreview();
                        dtde.dropComplete(false);
                        return;
                    }

                    // Verificar que no haya otros barcos en las celdas donde se va a colocar
                    // (excepto el mismo barco si se está moviendo)
                    boolean hasOtherShip = false;
                    for (int i = 0; i < size; i++) {
                        int r = row + (vertical ? i : 0);
                        int c = col + (vertical ? 0 : i);
                        if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                            if (cells[r][c].hasShip()) {
                                String existingShipType = cells[r][c].getShipType();
                                // Permitir si es el mismo barco (está moviéndolo)
                                if (!existingShipType.equals(shipType)) {
                                    hasOtherShip = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (hasOtherShip) {
                        System.out.println(
                                "No se puede colocar " + shipType +
                                " (tamaño " + size + ") en (" + row + "," + col +
                                ") - Ya hay otro barco en esa posición"
                        );
                        clearPreview();
                        dtde.dropComplete(false);
                        return;
                    }

                    // Mostrar las casillas que ocupa
                    StringBuilder casillas = new StringBuilder();
                    for (int i = 0; i < size; i++) {
                        int r = row + (vertical ? i : 0);
                        int c = col + (vertical ? 0 : i);
                        if (i > 0) casillas.append(", ");
                        casillas.append("(").append(r).append(",").append(c).append(")");
                    }

                    System.out.println(
                            "Barco " + shipType + " (tamaño " + size + ") soltado en casilla inicial: (" + row + "," + col + ")"
                    );
                    System.out.println(
                            "Ocupa las casillas: " + casillas.toString()
                    );

                    // Guardar si el barco ya existe antes de hacer el drop
                    final boolean shipExists = checkShipExists(shipType);
                    
                    // Marcar que el drop se completó primero
                    dtde.dropComplete(true);
                    
                    // Usar SwingUtilities.invokeLater para diferir la colocación del barco
                    // hasta que el drag termine completamente
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        // Colocar el barco visualmente en el tablero
                        placeShip(shipType, size, row, col, vertical);
                        
                        // Limpiar los TransferHandlers del barco anterior si existía
                        // Solo después de que el drag haya terminado completamente
                        if (shipExists) {
                            javax.swing.SwingUtilities.invokeLater(() -> {
                                javax.swing.SwingUtilities.invokeLater(() -> {
                                    cleanupOldShipHandlers(shipType);
                                });
                            });
                        }
                    });

                    clearPreview();

                } catch (Exception e) {
                    System.out.println("Error al soltar: " + e.getMessage());
                    e.printStackTrace();
                    clearPreview();
                    dtde.dropComplete(false);
                }
            }
        }));
    }

    public void setCellListener(BoardCellListener listener) {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                cells[i][j].setListener(listener);
            }
        }
    }

    public void updateBoard(CellState[][] board) {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                cells[i][j].setState(board[i][j]);
            }
        }
    }

    private void showPreview(int row, int col, int size, boolean vertical) {
        clearPreview();
        
        previewShipSize = size;
        previewRow = row;
        previewCol = col;
        previewVertical = vertical;

        // Verificar si cabe
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

    private void clearPreview() {
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
        // Guardar si el barco ya existe para quitarlo después de colocar el nuevo
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
        
        // Si el barco ya existe, quitarlo primero (pero guardar los TransferHandlers temporalmente)
        if (shipExists) {
            removeShipWithoutClearingHandlers(shipType);
        }
        
        int cellSize = getCellSize();
        if (cellSize <= 0) {
            cellSize = 420 / 8; // Fallback
        }

        for (int i = 0; i < size; i++) {
            int r = row + (vertical ? i : 0);
            int c = col + (vertical ? 0 : i);
            
            if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                // Determinar qué parte del barco va en esta celda
                int partNumber = i + 1; // Las partes van de 1 a size
                String imagePath = "/ships/" + shipType + "_" + partNumber + ".png";
                
                // Cargar la imagen
                java.net.URL resource = getClass().getResource(imagePath);
                if (resource != null) {
                    ImageIcon icon = new ImageIcon(resource);
                    Image original = icon.getImage();
                    
                    // Escalar la imagen al tamaño de la celda
                    Image scaled = original.getScaledInstance(
                            cellSize,
                            cellSize,
                            Image.SCALE_SMOOTH
                    );
                    
                    // Colocar la imagen en la celda y guardar información del barco
                    cells[r][c].setShipImage(scaled, shipType + "_" + partNumber);
                    cells[r][c].setState(model.CellState.SHIP);
                    cells[r][c].setShipInfo(shipType, size, row, col, vertical, i);
                    
                    // Hacer la celda arrastrable
                    makeCellDraggable(cells[r][c], shipType, size);
                } else {
                    System.out.println("No se encontró imagen: " + imagePath);
                }
            }
        }
        
        // Ocultar el label del sidebar
        if (gamePanel != null) {
            gamePanel.hideShipLabel(shipType);
        }
        
        // Limpiar los handlers antiguos después de múltiples invocaciones
        // para asegurar que el drag haya terminado completamente
        if (shipExists) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        cleanupOldShipHandlers(shipType);
                    });
                });
            });
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
        // Limpiar TransferHandlers de celdas que ya no tienen barco pero aún tienen handler
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (!cells[i][j].hasShip() && cells[i][j].getTransferHandler() != null) {
                    // Verificar si el handler es de este tipo de barco
                    cells[i][j].setTransferHandler(null);
                }
            }
        }
    }

    private void removeShipWithoutClearingHandlers(String shipType) {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (cells[i][j].hasShip() && cells[i][j].getShipType().equals(shipType)) {
                    // Encontrar todas las celdas del barco y limpiarlas (pero mantener handlers)
                    BoardCell.ShipInfo info = cells[i][j].getShipInfo();
                    if (info != null) {
                        for (int k = 0; k < info.size; k++) {
                            int r = info.startRow + (info.vertical ? k : 0);
                            int c = info.startCol + (info.vertical ? 0 : k);
                            if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                                cells[r][c].clearShipImage();
                                cells[r][c].setState(model.CellState.WATER);
                                cells[r][c].clearShipInfo();
                                // Reemplazar el handler con uno vacío en lugar de eliminarlo
                                // Esto evita el NullPointerException cuando Swing llama a exportDone
                                cells[r][c].setTransferHandler(new TransferHandler("text") {
                                    @Override
                                    protected Transferable createTransferable(JComponent c) {
                                        return new StringSelection("");
                                    }
                                    @Override
                                    public int getSourceActions(JComponent c) {
                                        return NONE; // No permitir drag
                                    }
                                    @Override
                                    protected void exportDone(JComponent c, Transferable data, int action) {
                                        // No hacer nada - handler vacío
                                    }
                                });
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    public void removeShipFromBoard(String shipType) {
        removeShipFromBoardSafely(shipType);
    }

    public void removeShipFromBoardSafely(String shipType) {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (cells[i][j].hasShip() && cells[i][j].getShipType().equals(shipType)) {
                    // Encontrar todas las celdas del barco y limpiarlas
                    BoardCell.ShipInfo info = cells[i][j].getShipInfo();
                    if (info != null) {
                        for (int k = 0; k < info.size; k++) {
                            int r = info.startRow + (info.vertical ? k : 0);
                            int c = info.startCol + (info.vertical ? 0 : k);
                            if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                                cells[r][c].clearShipImage();
                                cells[r][c].setState(model.CellState.WATER);
                                cells[r][c].clearShipInfo();
                                // Reemplazar el handler con uno vacío en lugar de eliminarlo
                                // Esto evita el NullPointerException cuando Swing llama a exportDone
                                cells[r][c].setTransferHandler(new TransferHandler("text") {
                                    @Override
                                    protected Transferable createTransferable(JComponent c) {
                                        return new StringSelection("");
                                    }
                                    @Override
                                    public int getSourceActions(JComponent c) {
                                        return NONE; // No permitir drag
                                    }
                                    @Override
                                    protected void exportDone(JComponent c, Transferable data, int action) {
                                        // No hacer nada - handler vacío
                                    }
                                });
                            }
                        }
                        repaint();
                        break;
                    }
                }
            }
        }
    }

    private void makeCellDraggable(BoardCell cell, String shipType, int size) {
        // Hacer todas las celdas del barco arrastrables
        // Todas apuntan a la primera celda del barco para obtener la información
        cell.setTransferHandler(new TransferHandler("text") {
            @Override
            protected Transferable createTransferable(JComponent c) {
                BoardCell boardCell = (BoardCell) c;
                BoardCell.ShipInfo info = boardCell.getShipInfo();
                if (info != null) {
                    // Obtener la primera celda del barco para la información completa
                    int firstRow = info.startRow;
                    int firstCol = info.startCol;
                    if (firstRow >= 0 && firstRow < ROWS && firstCol >= 0 && firstCol < COLS) {
                        return new StringSelection(info.shipType + ":" + info.size);
                    }
                }
                return new StringSelection(shipType + ":" + size);
            }

            @Override
            public int getSourceActions(JComponent c) {
                return COPY;
            }

            @Override
            protected void exportDone(JComponent c, Transferable data, int action) {
                // Sobrescribir para evitar NullPointerException si el handler fue eliminado
                // durante el drag. No hacer nada aquí ya que la limpieza se hace después.
                super.exportDone(c, data, action);
            }
        });
    }

    public int getCellSize() {
        int width = getWidth();
        int height = getHeight();
        
        if (width > 0 && height > 0) {
            return Math.min(width / COLS, height / ROWS);
        }
        
        // Si el panel aún no tiene tamaño, usar el tamaño preferido
        Dimension prefSize = getPreferredSize();
        if (prefSize != null && prefSize.width > 0) {
            return Math.min(prefSize.width / COLS, prefSize.height / ROWS);
        }
        
        // Fallback
        return 420 / 8;
    }
    
}
