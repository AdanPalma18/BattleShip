package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import model.BoardCellListener;
import model.CellState;

public class BoardCell extends JPanel {
    private int row;
    private int col;
    private CellState state;
    private boolean isPreview;
    private Image shipImage;
    private String shipPart;
    private ShipInfo shipInfo;
    private boolean isTutorialMode = false;

    private BoardCellListener listener;

    // Clase interna para guardar información del barco
    public static class ShipInfo {
        public String shipType;
        public int size;
        public int startRow;
        public int startCol;
        public boolean vertical;
        public int partIndex; // Índice de esta parte en el barco (0-based)

        public ShipInfo(String shipType, int size, int startRow, int startCol, boolean vertical, int partIndex) {
            this.shipType = shipType;
            this.size = size;
            this.startRow = startRow;
            this.startCol = startCol;
            this.vertical = vertical;
            this.partIndex = partIndex;
        }
    }

    public BoardCell(int row, int col) {
        this.row = row;
        this.col = col;
        this.state = CellState.WATER;

        setOpaque(true);
        setBorder(BorderFactory.createLineBorder(Color.BLACK));
        updateColor();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Asegurarse de que el click siempre se procese
                if(listener != null){
                    listener.cellClicked(row, col);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                // Durante la batalla, NO hacer nada - solo permitir que mouseClicked funcione
                // Solo permitir drag durante la fase de colocación
                // Verificar si hay un TransferHandler válido
                if (hasShip() && getTransferHandler() != null) {
                    // Verificar si estamos en fase de colocación antes de permitir drag
                    // Si no hay TransferHandler válido (NONE), no hacer nada
                    if (getTransferHandler().getSourceActions(BoardCell.this) != TransferHandler.NONE) {
                        // Solo iniciar drag si realmente hay un TransferHandler válido
                        // NO consumir el evento para que mouseClicked también se dispare
                        getTransferHandler().exportAsDrag(BoardCell.this, e, TransferHandler.COPY);
                    }
                }
                // IMPORTANTE: NO consumir el evento - permitir que mouseClicked se dispare siempre
            }
        });
    }

    public void setState(CellState newState) {
        System.out.println("Acutalizando color: " + newState);
        this.state = newState;
        
        // Si el estado cambia a MISS, HIT, o SUNK, limpiar la imagen del barco
        // porque estos estados no deben mostrar barcos
        if (newState == CellState.MISS || newState == CellState.HIT || newState == CellState.SUNK) {
            clearShipImage();
            clearShipInfo();
        }
        
        updateColor();
    }

    public CellState getState() {
        return state;
    }

    private void updateColor() {
        if (isPreview) {
            setBackground(new Color(100, 200, 100, 150));
            return;
        }
        switch(state){
            case WATER -> setBackground(new Color(180,220,255));
            case HIT -> setBackground(Color.RED);
            case MISS -> setBackground(Color.LIGHT_GRAY);
            case SUNK -> setBackground(Color.DARK_GRAY);
            case SHIP -> {
                if (isTutorialMode) {
                    setBackground(Color.GRAY);
                } else {
                    setBackground(new Color(180,220,255));
                }
            }
        }
    }

    public void setTutorialMode(boolean tutorialMode) {
        this.isTutorialMode = tutorialMode;
        updateColor();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (shipImage != null && !isPreview) {
            int cellWidth = getWidth();
            int cellHeight = getHeight();
            g.drawImage(shipImage, 0, 0, cellWidth, cellHeight, this);
        }
    }

    public void setShipImage(Image image, String part) {
        this.shipImage = image;
        this.shipPart = part;
        repaint();
    }

    public void clearShipImage() {
        this.shipImage = null;
        this.shipPart = null;
        repaint();
    }

    public void setPreview(boolean preview) {
        this.isPreview = preview;
        updateColor();
    }

    public void setListener(BoardCellListener listener){
        this.listener = listener;
    }

    public void setShipInfo(String shipType, int size, int startRow, int startCol, boolean vertical, int partIndex) {
        this.shipInfo = new ShipInfo(shipType, size, startRow, startCol, vertical, partIndex);
    }

    public ShipInfo getShipInfo() {
        return shipInfo;
    }

    public void clearShipInfo() {
        this.shipInfo = null;
    }

    public boolean hasShip() {
        return shipImage != null && shipInfo != null;
    }

    public String getShipType() {
        return shipInfo != null ? shipInfo.shipType : null;
    }

    public int getShipPartIndex() {
        return shipInfo != null ? shipInfo.partIndex : -1;
    }
}
