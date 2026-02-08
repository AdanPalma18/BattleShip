package ui;

import logic.BattleShip;

import javax.swing.*;
import java.awt.*;

public class MenuPanel extends JPanel {

    private MainFrame frame;

    public MenuPanel(MainFrame frame, BattleShip game) {
        this.frame = frame;
        setLayout(new GridLayout(6,1,10,10));

        add(new JLabel("MENÚ PRINCIPAL", SwingConstants.CENTER));

        JButton btnPlay = new JButton("1. Jugar Battleship");
        JButton btnConfig = new JButton("2. Configuración");
        JButton btnReports = new JButton("3. Reportes");
        JButton btnProfile = new JButton("4. Mi Perfil");
        JButton btnLogout = new JButton("5. Cerrar Sesión");

        add(btnPlay);
        add(btnConfig);
        add(btnReports);
        add(btnProfile);
        add(btnLogout);
        btnPlay.addActionListener(e -> {
            String enemy = JOptionPane.showInputDialog(this, "Ingrese el username del jugador 2:");
            if(enemy != null && !enemy.isBlank()){
                frame.showGame(enemy);
            }
        });

        btnLogout.addActionListener(e -> frame.showLogin());
    }
}
