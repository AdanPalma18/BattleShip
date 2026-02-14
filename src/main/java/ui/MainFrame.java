package ui;

import logic.BattleShip;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainFrame extends JFrame {
    public static final String LOGIN = "LOGIN";
    public static final String MENU = "MENU";
    public static final String GAME = "GAME";
    CardLayout cardLayout;
    JPanel mainPanel;
    private GamePanel gamePanel;
    private BattleShip game;
    private java.awt.KeyEventDispatcher keyDispatcher;
    
    public MainFrame(BattleShip game) {
        this.game = game;
        setTitle("BattleShip - 22511257");
        setSize(600, 600);
        setFocusable(true);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        gamePanel = new GamePanel(this, game);

        mainPanel.add(new LoginPanel(this, game), LOGIN);
        mainPanel.add(new MenuPanel(this, game), MENU);
        mainPanel.add(gamePanel, GAME);

        add(mainPanel);
        
        // Registrar KeyBinding cuando la ventana se hace visible
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                registerKeyBinding();
            }
            
            @Override
            public void windowActivated(WindowEvent e) {
                registerKeyBinding();
            }
        });
        
        // Registrar KeyEventDispatcher como respaldo (funciona durante drag)
        keyDispatcher = new java.awt.KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getID() == KeyEvent.KEY_PRESSED && 
                    (e.getKeyCode() == KeyEvent.VK_R || Character.toLowerCase(e.getKeyChar()) == 'r')) {
                    System.out.println("========== KeyEventDispatcher: TECLA R DETECTADA ==========");
                    if (gamePanel != null) {
                        gamePanel.getBoardPanel().rotateCurrentPreview();
                        return true;
                    } else {
                        System.out.println(">>> gamePanel es null!");
                    }
                }
                return false;
            }
        };
        java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyDispatcher);
        
        showLogin();
    }
    
    private void registerKeyBinding() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            javax.swing.JRootPane root = getRootPane();
            if (root != null) {
                javax.swing.InputMap im = root.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
                javax.swing.ActionMap am = root.getActionMap();
                
                im.put(javax.swing.KeyStroke.getKeyStroke("R"), "rotateShipGlobal");
                
                am.put("rotateShipGlobal", new javax.swing.AbstractAction() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        System.out.println("========== KeyBinding ActionMap: TECLA R DETECTADA ==========");
                        if (gamePanel != null) {
                            gamePanel.getBoardPanel().rotateCurrentPreview();
                        } else {
                            System.out.println(">>> gamePanel es null en ActionMap!");
                        }
                    }
                });
            }
        });
    }
    
    @Override
    public void dispose() {
        if (keyDispatcher != null) {
            java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyDispatcher);
        }
        super.dispose();
    }


    public void showLogin() {
        cardLayout.show(mainPanel, LOGIN);
    }

    public void showMenu() {
        cardLayout.show(mainPanel, MENU);
    }

    public void showGame(String enemyUsername) {
        game.startMatch(enemyUsername);
        gamePanel.startGame();
        cardLayout.show(mainPanel, GAME);
    }


}
