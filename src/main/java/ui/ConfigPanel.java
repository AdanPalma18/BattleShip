package ui;

import logic.BattleShip;
import model.Difficulty;
import model.Mode;

import javax.swing.*;
import java.awt.*;

public class ConfigPanel extends JPanel {

    private MainFrame frame;
    private BattleShip game;
    private CardLayout cardLayout;
    private JPanel configContentPanel;

    public ConfigPanel(MainFrame frame, BattleShip game) {
        this.frame = frame;
        this.game = game;
        setLayout(new BorderLayout());

        // Panel principal con CardLayout para las diferentes vistas
        cardLayout = new CardLayout();
        configContentPanel = new JPanel(cardLayout);

        // Panel de menú de configuración
        JPanel menuPanel = buildConfigMenu();
        configContentPanel.add(menuPanel, "MENU");

        // Panel de dificultad
        JPanel difficultyPanel = buildDifficultyPanel();
        configContentPanel.add(difficultyPanel, "DIFFICULTY");

        // Panel de modo de juego
        JPanel modePanel = buildModePanel();
        configContentPanel.add(modePanel, "MODE");

        add(configContentPanel, BorderLayout.CENTER);
    }

    private JPanel buildConfigMenu() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("CONFIGURACIÓN", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(title);

        JButton btnDifficulty = new JButton("a. Dificultad");
        JButton btnMode = new JButton("b. Modo de Juego");
        JButton btnBack = new JButton("c. Regresar al Menú Principal");

        btnDifficulty.addActionListener(e -> {
            refreshDifficultyPanel();
            cardLayout.show(configContentPanel, "DIFFICULTY");
        });
        btnMode.addActionListener(e -> {
            refreshModePanel();
            cardLayout.show(configContentPanel, "MODE");
        });
        btnBack.addActionListener(e -> frame.showMenu());

        panel.add(btnDifficulty);
        panel.add(btnMode);
        panel.add(btnBack);

        return panel;
    }

    private JPanel buildDifficultyPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Dificultad", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(title, BorderLayout.NORTH);

        // Panel de opciones
        JPanel optionsPanel = new JPanel(new GridLayout(5, 1, 10, 10));
        
        // Mostrar dificultad actual
        Difficulty currentDifficulty = game.getDifficulty();
        String currentText = currentDifficulty != null ? 
            "Dificultad actual: " + currentDifficulty.name() + " (" + currentDifficulty.getShipsAllowed() + " barcos)" :
            "Dificultad actual: NORMAL (4 barcos) - Por defecto";
        
        JLabel currentLabel = new JLabel(currentText, SwingConstants.CENTER);
        currentLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        optionsPanel.add(currentLabel);

        // Botones para cada dificultad
        JButton btnEasy = new JButton("EASY - 5 barcos");
        JButton btnNormal = new JButton("NORMAL - 4 barcos");
        JButton btnExpert = new JButton("EXPERT - 2 barcos");
        JButton btnGenius = new JButton("GENIUS - 1 barco");

        btnEasy.addActionListener(e -> {
            game.setDifficulty(Difficulty.EASY);
            JOptionPane.showMessageDialog(this, "Dificultad cambiada a EASY (5 barcos)", 
                "Configuración", JOptionPane.INFORMATION_MESSAGE);
            refreshDifficultyPanel();
        });

        btnNormal.addActionListener(e -> {
            game.setDifficulty(Difficulty.NORMAL);
            JOptionPane.showMessageDialog(this, "Dificultad cambiada a NORMAL (4 barcos)", 
                "Configuración", JOptionPane.INFORMATION_MESSAGE);
            refreshDifficultyPanel();
        });

        btnExpert.addActionListener(e -> {
            game.setDifficulty(Difficulty.EXPERT);
            JOptionPane.showMessageDialog(this, "Dificultad cambiada a EXPERT (2 barcos)", 
                "Configuración", JOptionPane.INFORMATION_MESSAGE);
            refreshDifficultyPanel();
        });

        btnGenius.addActionListener(e -> {
            game.setDifficulty(Difficulty.GENIUS);
            JOptionPane.showMessageDialog(this, "Dificultad cambiada a GENIUS (1 barco)", 
                "Configuración", JOptionPane.INFORMATION_MESSAGE);
            refreshDifficultyPanel();
        });

        optionsPanel.add(btnEasy);
        optionsPanel.add(btnNormal);
        optionsPanel.add(btnExpert);
        optionsPanel.add(btnGenius);

        panel.add(optionsPanel, BorderLayout.CENTER);

        JButton btnBack = new JButton("Regresar");
        btnBack.addActionListener(e -> cardLayout.show(configContentPanel, "MENU"));
        panel.add(btnBack, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildModePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Modo de Juego", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(title, BorderLayout.NORTH);

        // Panel de opciones
        JPanel optionsPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        
        // Mostrar modo actual
        Mode currentMode = game.getMode();
        String currentText = "Modo actual: " + (currentMode != null ? currentMode.name() : "TUTORIAL - Por defecto");
        
        JLabel currentLabel = new JLabel(currentText, SwingConstants.CENTER);
        currentLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        optionsPanel.add(currentLabel);

        // Botones para cada modo
        JButton btnArcade = new JButton("ARCADE - Barcos ocultos");
        JButton btnTutorial = new JButton("TUTORIAL - Barcos visibles");

        btnArcade.addActionListener(e -> {
            game.setMode(Mode.ARCADE);
            JOptionPane.showMessageDialog(this, "Modo cambiado a ARCADE (barcos ocultos)", 
                "Configuración", JOptionPane.INFORMATION_MESSAGE);
            refreshModePanel();
        });

        btnTutorial.addActionListener(e -> {
            game.setMode(Mode.TUTORIAL);
            JOptionPane.showMessageDialog(this, "Modo cambiado a TUTORIAL (barcos visibles)", 
                "Configuración", JOptionPane.INFORMATION_MESSAGE);
            refreshModePanel();
        });

        optionsPanel.add(btnArcade);
        optionsPanel.add(btnTutorial);

        panel.add(optionsPanel, BorderLayout.CENTER);

        JButton btnBack = new JButton("Regresar");
        btnBack.addActionListener(e -> cardLayout.show(configContentPanel, "MENU"));
        panel.add(btnBack, BorderLayout.SOUTH);

        return panel;
    }

    private void refreshDifficultyPanel() {
        // Reconstruir el panel de dificultad para actualizar la información
        configContentPanel.removeAll();
        JPanel menuPanel = buildConfigMenu();
        JPanel difficultyPanel = buildDifficultyPanel();
        JPanel modePanel = buildModePanel();
        configContentPanel.add(menuPanel, "MENU");
        configContentPanel.add(difficultyPanel, "DIFFICULTY");
        configContentPanel.add(modePanel, "MODE");
        cardLayout.show(configContentPanel, "DIFFICULTY");
        revalidate();
        repaint();
    }

    private void refreshModePanel() {
        // Reconstruir el panel de modo para actualizar la información
        configContentPanel.removeAll();
        JPanel menuPanel = buildConfigMenu();
        JPanel difficultyPanel = buildDifficultyPanel();
        JPanel modePanel = buildModePanel();
        configContentPanel.add(menuPanel, "MENU");
        configContentPanel.add(difficultyPanel, "DIFFICULTY");
        configContentPanel.add(modePanel, "MODE");
        cardLayout.show(configContentPanel, "MODE");
        revalidate();
        repaint();
    }
}
