package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;

import logic.BattleShip;
import model.Ship;
import model.CellState;
import model.Player;

public class GamePanel extends JPanel {

    private static final int BOARD_SIZE = 420;

    private final BattleShip battleShip;
    private final BoardPanel boardPanel;
    private final JLabel turnLabel;
    private final JLabel placementInfoLabel;
    private final java.util.Map<String, JLabel> shipLabels = new java.util.HashMap<>();
    private final MainFrame mainFrame;
    private AWTEventListener globalMouseListener;
    private JButton btnContinue;
    private JButton btnExit;

    public GamePanel(MainFrame frame, BattleShip battleShip) {
        this.mainFrame = frame;
        this.battleShip = battleShip;
        setLayout(new BorderLayout());

        turnLabel = new JLabel("Turno de: -");
        turnLabel.setFont(new Font("Arial", Font.BOLD, 16));
        
        placementInfoLabel = new JLabel("");
        placementInfoLabel.setFont(new Font("Arial", Font.BOLD, 14));
        placementInfoLabel.setForeground(new Color(0, 100, 180));
        placementInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel topPanel = new JPanel(new BorderLayout());
        
        JPanel labelPanel = new JPanel(new GridLayout(2, 1));
        labelPanel.add(turnLabel);
        labelPanel.add(placementInfoLabel);
        
        topPanel.add(labelPanel, BorderLayout.WEST);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnContinue = new JButton("Continuar");
        JButton btnSurrender = new JButton("Rendirse");
        btnExit = new JButton("Salir");
        
        btnContinue.addActionListener(e -> handleContinue());
        btnSurrender.addActionListener(e -> handleSurrender());
        btnExit.addActionListener(e -> handleExit());
        
        buttonPanel.add(btnContinue);
        buttonPanel.add(btnSurrender);
        buttonPanel.add(btnExit);
        topPanel.add(buttonPanel, BorderLayout.EAST);
        
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
        
        // Registrar listener global de mouse para capturar drag desde cualquier lugar
        setupGlobalMouseListener();
    }
    
    private void setupGlobalMouseListener() {
        globalMouseListener = new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                // Solo procesar eventos de drag durante la fase de colocación
                if (event instanceof MouseEvent && battleShip.isPlacementPhase() && boardPanel.isDragging()) {
                    MouseEvent me = (MouseEvent) event;
                    
                    // Convertir coordenadas de pantalla a coordenadas del BoardPanel
                    Point screenPoint = me.getLocationOnScreen();
                    Point panelPoint = new Point(screenPoint);
                    SwingUtilities.convertPointFromScreen(panelPoint, boardPanel);
                    
                    if (me.getID() == MouseEvent.MOUSE_DRAGGED) {
                        // Verificar si el punto está dentro del BoardPanel
                        if (boardPanel.contains(panelPoint)) {
                            boardPanel.handleDragEvent(panelPoint.x, panelPoint.y);
                        } else {
                            // Si está fuera, limpiar preview pero mantener el drag activo
                            // NO limpiar isDragging ni currentDragSize para que R funcione
                            boardPanel.clearPreview();
                        }
                    } else if (me.getID() == MouseEvent.MOUSE_RELEASED) {
                        if (boardPanel.isDragging()) {
                            if (boardPanel.contains(panelPoint)) {
                                boardPanel.handleDropEvent(panelPoint.x, panelPoint.y);
                            } else {
                                // Si se suelta fuera del panel, verificar si viene del tablero
                                if (boardPanel.isDragFromBoard()) {
                                    String shipType = boardPanel.getCurrentDragShipType();
                                    if (shipType != null) {
                                        // Devolver el barco al sidebar
                                        showShipLabel(shipType);
                                        boardPanel.removeShipFromBoardSafely(shipType);
                                    }
                                }
                                // Cancelar el drag
                                boardPanel.cancelDrag();
                            }
                        }
                    }
                }
            }
        };
        
        Toolkit.getDefaultToolkit().addAWTEventListener(
            globalMouseListener, 
            AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK
        );
    }
    
    public void cleanup() {
        if (globalMouseListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(globalMouseListener);
        }
    }
    
    public BoardPanel getBoardPanel() {
        return boardPanel;
    }
    
    public BattleShip getBattleShip() {
        return battleShip;
    }

    private void handleCellClick(int row, int col) {
        if (battleShip.isPlacementPhase()) {
            return;
        }

        // Verificar si la casilla ya fue disparada antes de intentar disparar
        CellState[][] currentEnemyBoard = battleShip.getEnemyBoard();
        if (currentEnemyBoard != null && row >= 0 && row < currentEnemyBoard.length && 
            col >= 0 && col < currentEnemyBoard[row].length) {
            CellState currentState = currentEnemyBoard[row][col];
            // Si la casilla no es WATER ni SHIP, ya fue disparada
            if (currentState != CellState.WATER && currentState != CellState.SHIP) {
                JOptionPane.showMessageDialog(
                    this,
                    "Ya has disparado en esta casilla.",
                    "Casilla ya disparada",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }
        }

        CellState result = battleShip.shoot(row, col);
        if (result == null) return;

        String message;
        String title;
        int messageType;
        boolean shouldChangeTurn = false;
        
        if (result == CellState.HIT) {
            // Obtener el barco golpeado para mostrar su nombre
            Ship hitShip = battleShip.getLastHitShip();
            String shipName = hitShip != null ? logic.BattleShip.getShipName(hitShip.getCode()) : "";
            
            // Verificar si el tablero se regeneró o si fue un disparo a una parte ya golpeada
            if (battleShip.wasLastShotRegenerated()) {
                message = "¡SE HA BOMBARDEADO UN " + shipName + "!\n" +
                          "El tablero enemigo se ha regenerado.\n" +
                          "¡Sigue tu turno!";
            } else {
                message = "Esta parte del " + shipName + " ya ha sido disparada anteriormente.\n" +
                          "El tablero no se regenera porque no fue un impacto nuevo.";
            }
            title = "¡Impacto!";
            messageType = JOptionPane.INFORMATION_MESSAGE;
            shouldChangeTurn = false;

        } else if (result == CellState.MISS) {
            message = "Agua. Has fallado en (" + row + ", " + col + ")\nSigue el turno de " + battleShip.getNextTurnUsername();
            title = "Agua";
            messageType = JOptionPane.INFORMATION_MESSAGE;
            shouldChangeTurn = true;
        } else if (result == CellState.SUNK) {
            // Verificar si el juego terminó (todos los barcos hundidos)
            if (battleShip.getWinner() != null) {
                // El juego terminó - el jugador actual ganó
                Player winner = battleShip.getWinner();
                Player loser = battleShip.getEnemyPlayerPublic();
                
                // Actualizar puntos (ganador recibe 3 puntos según las especificaciones)
                winner.addPoints(3);
                
                // Crear resultado del juego usando la dificultad, no el modo
                String difficultyStr = battleShip.getDifficulty() != null ? battleShip.getDifficulty().toString() : "NORMAL";
                String gameResult = model.GameLog.win(winner.getUsername(), loser.getUsername(), difficultyStr);
                winner.addGameToHistory(gameResult);
                loser.addGameToHistory(gameResult);
                
                message = "¡" + winner.getUsername() + " ha ganado!\n" +
                          "Has hundido todos los barcos enemigos.\n" +
                          "Puntos obtenidos: 3";
                title = "¡Victoria!";
                messageType = JOptionPane.INFORMATION_MESSAGE;
                shouldChangeTurn = false;
                
                // Mostrar mensaje de victoria
                JOptionPane.showMessageDialog(this, message, title, messageType);
                
                // Resetear el juego y volver al menú
                battleShip.resetGame();
                clearGamePanel();
                mainFrame.showMenu();
                return; // Salir temprano, no continuar con el flujo normal
            } else {
                // Solo un barco hundido, el juego continúa
                // Obtener el barco hundido para mostrar su nombre
                Ship sunkShip = battleShip.getLastHitShip();
                String shipName = sunkShip != null ? logic.BattleShip.getShipName(sunkShip.getCode()) : "BARCO";
                Player enemy = battleShip.getEnemyPlayerPublic();
                String enemyName = enemy != null ? enemy.getUsername() : "Jugador";
                
                message = "¡SE HUNDIO EL " + shipName + "!\n" +
                          "Del " + enemyName;
                title = "Barco Hundido!";
                messageType = JOptionPane.INFORMATION_MESSAGE;
                shouldChangeTurn = false;
            }
        } else {
            message = "Ya has disparado en esta casilla.";
            title = "Casilla ya disparada";
            messageType = JOptionPane.WARNING_MESSAGE;
            shouldChangeTurn = false;
        }

        
        JOptionPane.showMessageDialog(this, message, title, messageType);

        // Actualizar el tablero después del disparo (después de regeneración si fue HIT)
        SwingUtilities.invokeLater(() -> {
            CellState[][] enemyBoard = battleShip.getEnemyBoard();
            if (enemyBoard != null) {
                boardPanel.updateBoard(enemyBoard);
            }
        });

        if (shouldChangeTurn) {
            battleShip.nextTurn();
            
            SwingUtilities.invokeLater(() -> {
                CellState[][] newEnemyBoard = battleShip.getEnemyBoard();
                System.out.println("DEBUG: Después de nextTurn, currentTurn=" + battleShip.getCurrentTurnUsername());
                System.out.println("DEBUG: newEnemyBoard=" + (newEnemyBoard != null ? "no null" : "null"));
                if (newEnemyBoard != null) {
                    boardPanel.updateBoard(newEnemyBoard);
                }
                updateTurnLabel();
            });
        } else {
            updateTurnLabel();
        }
    }

    private void handleContinue() {
        if (battleShip.isPlacementPhase()) {
            if (areAllShipsPlacedOnBoard()) {
                boolean wasPlacementPhase = battleShip.isPlacementPhase();
                boolean bothReadyBefore = battleShip.areBothPlayersReady();
                
                battleShip.continueToNextTurn();
                
                boolean isPlacementPhaseNow = battleShip.isPlacementPhase();
                boolean bothReadyAfter = battleShip.areBothPlayersReady();
                
                System.out.println("DEBUG: wasPlacementPhase=" + wasPlacementPhase + ", isPlacementPhaseNow=" + isPlacementPhaseNow);
                System.out.println("DEBUG: bothReadyBefore=" + bothReadyBefore + ", bothReadyAfter=" + bothReadyAfter);
                
                if (!isPlacementPhaseNow) {
                    System.out.println("DEBUG: Iniciando batalla");
                    JOptionPane.showMessageDialog(
                        this,
                        "¡Comienza la batalla! Es el turno de " + battleShip.getCurrentTurnUsername(),
                        "Fase de batalla iniciada",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                    switchToBattlePhase();
                } else {
                    System.out.println("DEBUG: Cambiando al siguiente jugador");
                    switchToNextPlayer();
                }
                updateTurnLabel();
                updatePlacementStatus();
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "Debes colocar todos los barcos antes de continuar.",
                    "Barcos faltantes",
                    JOptionPane.WARNING_MESSAGE
                );
            }
        } else {
            battleShip.continueToNextTurn();
            updateTurnLabel();
        }
    }

    private void switchToBattlePhase() {
        boardPanel.clearBoard();
        hideShipLabel("PA");
        hideShipLabel("AZ");
        hideShipLabel("SM");
        hideShipLabel("DT");
        
        // Deshabilitar el botón Continuar durante la batalla
        if (btnContinue != null) {
            btnContinue.setEnabled(false);
        }
        
        // Deshabilitar el botón Salir durante la batalla
        if (btnExit != null) {
            btnExit.setEnabled(false);
        }
        
        boolean isTutorial = battleShip.getMode() != null && battleShip.getMode() == model.Mode.TUTORIAL;
        boardPanel.setTutorialMode(isTutorial);
        
        CellState[][] enemyBoard = battleShip.getEnemyBoard();
        if (enemyBoard != null) {
            boardPanel.updateBoard(enemyBoard);
        }
    }

    public void saveBoardStateToPlayer() {
        // Ya no es necesario - los barcos se guardan directamente en el board con addShip()
    }

    public void saveShipToBattleShip(String shipCode, int size, int row, int col, boolean vertical) {
        // Método legacy: siempre remueve primero (mover barco)
        saveShipToBattleShip(shipCode, size, row, col, vertical, false);
    }
    
    /**
     * Guarda un barco en BattleShip, con opción de crear duplicado (no remover el anterior)
     * @param shipCode Código del barco
     * @param size Tamaño del barco
     * @param row Fila
     * @param col Columna
     * @param vertical Si es vertical
     * @param createDuplicate Si es true, NO remueve el barco anterior (crea duplicado). Si es false, remueve el anterior (mueve).
     */
    public void saveShipToBattleShip(String shipCode, int size, int row, int col, boolean vertical, boolean createDuplicate) {
        if (!createDuplicate) {
            // Remover el barco anterior (mover)
            battleShip.removeShip(shipCode);
        }
        // Agregar el nuevo barco
        battleShip.addShip(shipCode, size, row, col, vertical);
    }
    
    /**
     * Agrega un barco a BattleShip sin remover ninguno (para movimientos donde ya se removió)
     */
    public void addShipToBattleShip(String shipCode, int size, int row, int col, boolean vertical) {
        battleShip.addShip(shipCode, size, row, col, vertical);
    }

    public void removeShipFromBattleShip(String shipCode) {
        battleShip.removeShip(shipCode);
    }

    private void switchToNextPlayer() {
        boardPanel.clearBoard();
        
        if (battleShip.isPlacementPhase()) {
            showShipLabel("PA");
            showShipLabel("AZ");
            showShipLabel("SM");
            showShipLabel("DT");
        }
    }

    private boolean areAllShipsPlacedOnBoard() {
        return battleShip.areAllShipsPlaced();
    }

    private void handleSurrender() {
        int option = JOptionPane.showConfirmDialog(
            this,
            "¿Estás seguro de que quieres rendirte?",
            "Rendirse",
            JOptionPane.YES_NO_OPTION
        );
        if (option == JOptionPane.YES_OPTION) {
            model.Player winner = battleShip.surrender();
            if (winner != null) {
                String winnerName = winner.getUsername();
                String message = "¡" + winnerName + " ha ganado la partida!\n" +
                               "Puntos obtenidos: 3\n" +
                               "El historial ha sido guardado.";
                JOptionPane.showMessageDialog(
                    this,
                    message,
                    "Juego terminado",
                    JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "Error al procesar la rendición. Por favor, inténtalo de nuevo.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
            battleShip.resetGame();
            clearGamePanel();
            mainFrame.showMenu();
        }
    }
    
    private void handleExit() {
        // Solo permitir salir durante la fase de colocación
        if (!battleShip.isPlacementPhase()) {
            JOptionPane.showMessageDialog(
                this,
                "No puedes salir durante la batalla. Usa 'Rendirse' si quieres terminar la partida.",
                "No permitido",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        int option = JOptionPane.showConfirmDialog(
            this,
            "¿Estás seguro de que quieres salir? Se perderá el progreso de la partida actual.",
            "Salir del juego",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (option == JOptionPane.YES_OPTION) {
            battleShip.resetGame();
            clearGamePanel();
            mainFrame.showMenu();
        }
    }

    private void clearGamePanel() {
        boardPanel.clearBoard();
        hideShipLabel("PA");
        hideShipLabel("AZ");
        hideShipLabel("SM");
        hideShipLabel("DT");
        turnLabel.setText("Turno de: -");
        placementInfoLabel.setText(""); // Limpiar el label de información
        // Habilitar el botón Continuar cuando se limpia el panel (nuevo juego)
        if (btnContinue != null) {
            btnContinue.setEnabled(true);
        }
        // Habilitar el botón Salir cuando se limpia el panel (nuevo juego)
        if (btnExit != null) {
            btnExit.setEnabled(true);
        }
    }

    private void updatePlacementStatus() {
        if (battleShip.getCurrentUser() == null || battleShip.getCurrentTurnUsername() == null || battleShip.getCurrentTurnUsername().isBlank()) {
            turnLabel.setText("Turno de: -");
            placementInfoLabel.setText("");
            return;
        }
        if (battleShip.isPlacementPhase()) {
            turnLabel.setText("Coloca tus barcos - " + battleShip.getCurrentTurnUsername());
            updatePlacementInfoLabel();
        } else {
            updateTurnLabel();
            placementInfoLabel.setText(""); // Ocultar durante la batalla
        }
    }
    
    private void updatePlacementInfoLabel() {
        if (battleShip.isPlacementPhase()) {
            model.Mode mode = battleShip.getMode();
            model.Difficulty difficulty = battleShip.getDifficulty();
            
            String modeName = mode != null ? mode.name() : "TUTORIAL";
            int shipsRequired = difficulty != null ? difficulty.getShipsAllowed() : 4;
            
            placementInfoLabel.setText("MODO " + modeName + ", COLOCA " + shipsRequired + " BARCOS");
        } else {
            placementInfoLabel.setText("");
        }
    }

    public void startGame() {
        boolean isTutorial = battleShip.getMode() != null && battleShip.getMode() == model.Mode.TUTORIAL;
        boardPanel.setTutorialMode(isTutorial);
        
        if (battleShip.isPlacementPhase()) {
            boardPanel.clearBoard();
            showShipLabel("PA");
            showShipLabel("AZ");
            showShipLabel("SM");
            showShipLabel("DT");
            // Habilitar el botón Continuar durante la fase de colocación
            if (btnContinue != null) {
                btnContinue.setEnabled(true);
            }
            // Habilitar el botón Salir durante la fase de colocación
            if (btnExit != null) {
                btnExit.setEnabled(true);
            }
        } else {
            // Deshabilitar el botón Continuar durante la batalla
            if (btnContinue != null) {
                btnContinue.setEnabled(false);
            }
            // Deshabilitar el botón Salir durante la batalla
            if (btnExit != null) {
                btnExit.setEnabled(false);
            }
            CellState[][] enemyBoard = battleShip.getEnemyBoard();
            if (enemyBoard != null) {
                boardPanel.updateBoard(enemyBoard);
            }
        }
        updatePlacementStatus();
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
        
        panel.setDropTarget(new java.awt.dnd.DropTarget(panel, java.awt.dnd.DnDConstants.ACTION_COPY, 
            new java.awt.dnd.DropTargetAdapter() {
                @Override
                public void drop(java.awt.dnd.DropTargetDropEvent dtde) {
                    try {
                        dtde.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY);
                        
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
                                
                                dtde.dropComplete(true);
                                
                                javax.swing.SwingUtilities.invokeLater(() -> {
                                    showShipLabel(shipType);
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

        URL resource = getClass().getResource(resourcePath);
        if (resource == null) return label;

        Image original = new ImageIcon(resource).getImage();

        final ImageIcon[] sidebarIcon = { null };

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

        // Sistema de drag simulado
        label.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                // Solo permitir drag durante la fase de colocación
                if (battleShip.isPlacementPhase()) {
                    // Iniciar drag simulado
                    boardPanel.startSimulatedDrag(shipCode, shipSize);
                }
            }
        });
        
        label.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                // El drag se maneja en BoardPanel con sus propios listeners
                // Solo necesitamos iniciar el drag cuando se presiona
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
