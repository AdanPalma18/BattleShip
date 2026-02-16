package ui;

import logic.BattleShip;

import javax.swing.*;
import java.awt.*;

public class MenuPanel extends JPanel {

    private MainFrame frame;

    public MenuPanel(MainFrame frame, BattleShip game) {
        this.frame = frame;
        setLayout(new GridLayout(7,1,10,10));

        add(new JLabel("MENÚ PRINCIPAL", SwingConstants.CENTER));

        JButton btnPlay = new JButton("1. Jugar Battleship");
        JButton btnConfig = new JButton("2. Configuración");
        JButton btnReports = new JButton("3. Reportes");
        JButton btnProfile = new JButton("4. Mi Perfil");
        JButton btnLogout = new JButton("5. Cerrar Sesión");
        JButton btnExit = new JButton("6. Salir");

        add(btnPlay);
        add(btnConfig);
        add(btnReports);
        add(btnProfile);
        add(btnLogout);
        add(btnExit);
        btnPlay.addActionListener(e -> {
            String enemy = JOptionPane.showInputDialog(this, "Ingrese el username del jugador 2:");
            if(enemy != null && !enemy.isBlank()){
                String enemyTrimmed = enemy.trim();
                
                // Validar que el enemigo no sea el mismo que el jugador actual
                if(enemyTrimmed.equalsIgnoreCase(game.getCurrentUser().getUsername())){
                    JOptionPane.showMessageDialog(
                        this,
                        "No puedes jugar contra ti mismo. Por favor, ingresa un jugador diferente.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
                
                // Validar que el jugador enemigo existe
                if(!game.playerExists(enemyTrimmed)){
                    JOptionPane.showMessageDialog(
                        this,
                        "El jugador '" + enemyTrimmed + "' no existe. Por favor, ingresa un jugador registrado.",
                        "Jugador no encontrado",
                        JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
                
                // Intentar iniciar la partida
                if(game.startMatch(enemyTrimmed)){
                    frame.showGame(enemyTrimmed);
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        "Error al iniciar la partida. Por favor, intenta de nuevo.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        });

        btnReports.addActionListener(e -> frame.showReports());

        btnConfig.addActionListener(e -> frame.showConfig());

        btnProfile.addActionListener(e -> frame.showProfile());

        btnLogout.addActionListener(e -> {
            game.logout(); // Cerrar sesión en el juego
            frame.showLogin();
        });
        
        btnExit.addActionListener(e -> exitApplication());
    }
    
    private void exitApplication() {
        int option = JOptionPane.showConfirmDialog(
            this,
            "¿Estás seguro de que quieres salir?",
            "Salir",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (option == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }
}
