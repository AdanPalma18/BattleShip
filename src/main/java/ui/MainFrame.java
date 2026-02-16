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
    public static final String REPORTS = "REPORTS";
    public static final String CONFIG = "CONFIG";
    public static final String PROFILE = "PROFILE";
    CardLayout cardLayout;
    JPanel mainPanel;
    private GamePanel gamePanel;
    private ReportsPanel reportsPanel;
    private ConfigPanel configPanel;
    private ProfilePanel profilePanel;
    private LoginPanel loginPanel;
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
        loginPanel = new LoginPanel(this, game);

        mainPanel.add(loginPanel, LOGIN);
        mainPanel.add(new MenuPanel(this, game), MENU);
        mainPanel.add(gamePanel, GAME);
        reportsPanel = new ReportsPanel(this, game);
        mainPanel.add(reportsPanel, REPORTS);
        configPanel = new ConfigPanel(this, game);
        mainPanel.add(configPanel, CONFIG);
        profilePanel = new ProfilePanel(this, game);
        mainPanel.add(profilePanel, PROFILE);

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
        loginPanel.clearFields(); // Limpiar campos al mostrar el login
        cardLayout.show(mainPanel, LOGIN);
    }

    public void showMenu() {
        cardLayout.show(mainPanel, MENU);
    }

    public void showGame(String enemyUsername) {
        // La validación ya se hizo en MenuPanel, solo mostrar el juego
        gamePanel.startGame();
        cardLayout.show(mainPanel, GAME);
    }

    public void showReports() {
        cardLayout.show(mainPanel, REPORTS);
    }

    public void showConfig() {
        cardLayout.show(mainPanel, CONFIG);
    }

    public void showProfile() {
        cardLayout.show(mainPanel, PROFILE);
    }

}
