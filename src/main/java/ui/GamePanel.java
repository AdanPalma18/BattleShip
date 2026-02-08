package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

import logic.BattleShip;
import model.CellState;

public class GamePanel extends JPanel {

    private static final int BOARD_SIZE = 420;

    private final BattleShip battleShip;
    private final BoardPanel boardPanel;
    private final JLabel turnLabel;
    private final java.util.Map<String, JLabel> shipLabels = new java.util.HashMap<>();

    public GamePanel(MainFrame frame, BattleShip battleShip) {
        this.battleShip = battleShip;
        setLayout(new BorderLayout());

        turnLabel = new JLabel("Turno de: -");
        turnLabel.setFont(new Font("Arial", Font.BOLD, 16));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(turnLabel);
        add(topPanel, BorderLayout.NORTH);

        boardPanel = new BoardPanel(this);
        boardPanel.setPreferredSize(new Dimension(BOARD_SIZE, BOARD_SIZE));

        JPanel shipsPanel = buildShipsPanel();

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                boardPanel,
                shipsPanel
        );
        splitPane.setResizeWeight(0.75);
        splitPane.setDividerSize(5);
        splitPane.setEnabled(false);

        add(splitPane, BorderLayout.CENTER);

        boardPanel.setCellListener(this::handleCellClick);
    }

    private void handleCellClick(int row, int col) {
        if (!battleShip.isMyTurn()) return;

        CellState result = battleShip.shoot(row, col);

        CellState[][] enemyBoard = battleShip.getEnemyBoard();
        if (enemyBoard != null) {
            boardPanel.updateBoard(enemyBoard);
        }

        battleShip.nextTurn();
        updateTurnLabel();
    }

    public void startGame() {
        CellState[][] enemyBoard = battleShip.getEnemyBoard();
        if (enemyBoard != null) {
            boardPanel.updateBoard(enemyBoard);
        }
        updateTurnLabel();
    }

    private void updateTurnLabel() {
        String name = battleShip.getCurrentTurnUsername();
        turnLabel.setText(
                (name == null || name.isBlank())
                        ? "Turno de: -"
                        : "Turno de: " + name
        );
    }

    private JPanel buildShipsPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.setBackground(new Color(245, 245, 245));
        panel.setPreferredSize(new Dimension(220, BOARD_SIZE));
        
        // Hacer el panel aceptar drops para devolver barcos
        panel.setDropTarget(new java.awt.dnd.DropTarget(panel, java.awt.dnd.DnDConstants.ACTION_COPY, 
            new java.awt.dnd.DropTargetAdapter() {
                @Override
                public void drop(java.awt.dnd.DropTargetDropEvent dtde) {
                    try {
                        dtde.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY);
                        
                        // Buscar cualquier DataFlavor que represente String
                        java.awt.datatransfer.DataFlavor stringFlavor = null;
                        for (java.awt.datatransfer.DataFlavor flavor : dtde.getCurrentDataFlavors()) {
                            if (flavor.isFlavorTextType() || 
                                flavor.equals(java.awt.datatransfer.DataFlavor.stringFlavor) ||
                                String.class.equals(flavor.getRepresentationClass())) {
                                stringFlavor = flavor;
                                break;
                            }
                        }
                        
                        if (stringFlavor != null) {
                            String data = (String) dtde.getTransferable().getTransferData(stringFlavor);
                            String[] parts = data.split(":");
                            if (parts.length >= 2) {
                                String shipType = parts[0];
                                
                                // Marcar que el drop se completó primero
                                dtde.dropComplete(true);
                                
                                // Usar invokeLater para diferir la limpieza hasta que el drag termine
                                javax.swing.SwingUtilities.invokeLater(() -> {
                                    // Mostrar el label del sidebar de nuevo
                                    showShipLabel(shipType);
                                    // Quitar el barco del tablero (pero reemplazar handlers en lugar de eliminarlos)
                                    boardPanel.removeShipFromBoardSafely(shipType);
                                });
                                return;
                            }
                        }
                        
                        dtde.dropComplete(true);
                    } catch (Exception e) {
                        System.out.println("Error al devolver barco al sidebar: " + e.getMessage());
                        dtde.dropComplete(false);
                    }
                }
            }));

        JLabel paLabel = buildShipLabel("PA (5)", "/ships/PA_FULL.png", "PA", 5);
        JLabel azLabel = buildShipLabel("AZ (4)", "/ships/AZ_FULL.png", "AZ", 4);
        JLabel smLabel = buildShipLabel("SM (3)", "/ships/SM_FULL.png", "SM", 3);
        JLabel dtLabel = buildShipLabel("DT (2)", "/ships/DT_FULL.png", "DT", 2);
        
        shipLabels.put("PA", paLabel);
        shipLabels.put("AZ", azLabel);
        shipLabels.put("SM", smLabel);
        shipLabels.put("DT", dtLabel);
        
        panel.add(paLabel);
        panel.add(azLabel);
        panel.add(smLabel);
        panel.add(dtLabel);


        return panel;
    }

    private JLabel buildShipLabel(
            String text,
            String resourcePath,
            String shipCode,
            int shipSize
    ) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 13));
        label.setHorizontalTextPosition(SwingConstants.CENTER);
        label.setVerticalTextPosition(SwingConstants.BOTTOM);
        label.setIconTextGap(8);

        label.setText(shipCode + ":" + shipSize);
        label.setTransferHandler(new TransferHandler("text"));

        URL resource = getClass().getResource(resourcePath);
        if (resource == null) return label;

        Image original = new ImageIcon(resource).getImage();

        final ImageIcon[] sidebarIcon = { null };
        final ImageIcon[] dragIcon = { null };

        // ===== ESCALADO NORMAL (sidebar) =====
        label.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int w = label.getWidth() - 20;
                int h = label.getHeight() - 35;
                if (w <= 0 || h <= 0) return;

                double scale = Math.min(
                        (double) w / original.getWidth(null),
                        (double) h / original.getHeight(null)
                );

                Image scaled = original.getScaledInstance(
                        (int) (original.getWidth(null) * scale),
                        (int) (original.getHeight(null) * scale),
                        Image.SCALE_SMOOTH
                );

                sidebarIcon[0] = new ImageIcon(scaled);
                label.setIcon(sidebarIcon[0]);
            }
        });

        // ===== DRAG CON TAMAÑO REAL =====
        label.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                int cellSize = boardPanel.getCellSize();
                
                // Si el panel aún no tiene tamaño, usar el tamaño preferido
                if (cellSize <= 0) {
                    Dimension prefSize = boardPanel.getPreferredSize();
                    if (prefSize != null && prefSize.width > 0) {
                        cellSize = Math.min(prefSize.width / 8, prefSize.height / 8);
                    } else {
                        cellSize = 420 / 8; // Fallback
                    }
                }

                int targetWidth = cellSize;
                int targetHeight = shipSize * cellSize;

                double scaleHeight = (double) targetHeight / original.getHeight(null);
                double scaleWidth = (double) targetWidth / original.getWidth(null);
                
                double scale = scaleHeight * 1.6;
                
                int resultingWidth = (int) (original.getWidth(null) * scale);
                if (resultingWidth > targetWidth * 2.0) {
                    scale = Math.min(scaleHeight * 1.1, scaleWidth * 1.3);
                }

                // Calcular dimensiones directamente
                int originalWidth = original.getWidth(null);
                int originalHeight = original.getHeight(null);
                
                if (originalWidth <= 0 || originalHeight <= 0) {
                    // Si la imagen original no está lista, usar valores por defecto
                    originalWidth = 100;
                    originalHeight = 100;
                }
                
                int scaledWidth = Math.max(1, (int) (originalWidth * scale));
                int scaledHeight = Math.max(1, (int) (originalHeight * scale));

                Image scaledDrag = original.getScaledInstance(
                        scaledWidth,
                        scaledHeight,
                        Image.SCALE_SMOOTH
                );

                dragIcon[0] = new ImageIcon(scaledDrag);

                TransferHandler handler = label.getTransferHandler();
                handler.setDragImage(dragIcon[0].getImage());
                handler.setDragImageOffset(
                        new Point(
                                Math.min(20, dragIcon[0].getIconWidth() / 4),
                                Math.min(20, dragIcon[0].getIconHeight() / 4)
                        )
                );

                handler.exportAsDrag(label, e, TransferHandler.COPY);
            }
        });

        return label;
    }

    public void hideShipLabel(String shipType) {
        JLabel label = shipLabels.get(shipType);
        if (label != null) {
            label.setVisible(false);
        }
    }

    public void showShipLabel(String shipType) {
        JLabel label = shipLabels.get(shipType);
        if (label != null) {
            label.setVisible(true);
        }
    }

    public JPanel getShipsPanel() {
        return (JPanel) ((JSplitPane) getComponent(1)).getRightComponent();
    }

}
