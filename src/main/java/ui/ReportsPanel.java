package ui;

import logic.BattleShip;
import model.Player;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ReportsPanel extends JPanel {

    private MainFrame frame;
    private BattleShip game;
    private CardLayout cardLayout;
    private JPanel reportsContentPanel;
    private JTextArea lastGamesTextArea;
    private JTextArea rankingTextArea;

    public ReportsPanel(MainFrame frame, BattleShip game) {
        this.frame = frame;
        this.game = game;
        setLayout(new BorderLayout());

        // Panel principal con CardLayout para las diferentes vistas
        cardLayout = new CardLayout();
        reportsContentPanel = new JPanel(cardLayout);

        // Panel de menú de reportes
        JPanel menuPanel = buildReportsMenu();
        reportsContentPanel.add(menuPanel, "MENU");

        // Panel de últimos 10 juegos
        JPanel lastGamesPanel = buildLastGamesPanel();
        reportsContentPanel.add(lastGamesPanel, "LAST_GAMES");

        // Panel de ranking
        JPanel rankingPanel = buildRankingPanel();
        reportsContentPanel.add(rankingPanel, "RANKING");

        add(reportsContentPanel, BorderLayout.CENTER);
    }

    private JPanel buildReportsMenu() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("REPORTES", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(title);

        JButton btnLastGames = new JButton("a. Descripción de mis últimos 10 juegos");
        JButton btnRanking = new JButton("b. Ranking de Jugadores");
        JButton btnBack = new JButton("c. Regresar al Menú Principal");

        btnLastGames.addActionListener(e -> {
            // Actualizar el contenido antes de mostrar
            refreshLastGamesPanel();
            cardLayout.show(reportsContentPanel, "LAST_GAMES");
        });
        btnRanking.addActionListener(e -> {
            // Actualizar el contenido antes de mostrar
            refreshRankingPanel();
            cardLayout.show(reportsContentPanel, "RANKING");
        });
        btnBack.addActionListener(e -> frame.showMenu());

        panel.add(btnLastGames);
        panel.add(btnRanking);
        panel.add(btnBack);

        return panel;
    }

    private JPanel buildLastGamesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Mis Últimos 10 Juegos", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(title, BorderLayout.NORTH);

        // Crear área de texto para mostrar los juegos
        lastGamesTextArea = new JTextArea();
        lastGamesTextArea.setEditable(false);
        lastGamesTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        lastGamesTextArea.setBackground(Color.WHITE);

        // Actualizar contenido inicialmente
        refreshLastGamesContent();

        JScrollPane scrollPane = new JScrollPane(lastGamesTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton btnBack = new JButton("Regresar");
        btnBack.addActionListener(e -> cardLayout.show(reportsContentPanel, "MENU"));
        
        // Botón para refrescar
        JButton btnRefresh = new JButton("Actualizar");
        btnRefresh.addActionListener(e -> refreshLastGamesContent());
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(btnRefresh);
        buttonPanel.add(btnBack);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildRankingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Ranking de Jugadores", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(title, BorderLayout.NORTH);

        // Crear área de texto para mostrar el ranking
        rankingTextArea = new JTextArea();
        rankingTextArea.setEditable(false);
        rankingTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        rankingTextArea.setBackground(Color.WHITE);

        // Actualizar contenido inicialmente
        refreshRankingContent();

        JScrollPane scrollPane = new JScrollPane(rankingTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton btnBack = new JButton("Regresar");
        btnBack.addActionListener(e -> cardLayout.show(reportsContentPanel, "MENU"));
        
        // Botón para refrescar
        JButton btnRefresh = new JButton("Actualizar");
        btnRefresh.addActionListener(e -> refreshRankingContent());
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(btnRefresh);
        buttonPanel.add(btnBack);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void refreshLastGamesContent() {
        if (lastGamesTextArea == null) return;
        
        // Obtener los últimos juegos del jugador actual (actualizado cada vez)
        Player currentUser = game.getCurrentUser();
        String[] lastGames = currentUser != null ? currentUser.getLastGames() : new String[10];

        StringBuilder content = new StringBuilder();
        int gameNumber = 1;
        boolean hasGames = false;
        for (String gameStr : lastGames) {
            if (gameStr != null && !gameStr.trim().isEmpty()) {
                content.append(gameNumber).append("- ").append(gameStr).append("\n");
                hasGames = true;
                gameNumber++;
            } else {
                content.append(gameNumber).append("-\n");
                gameNumber++;
            }
        }

        // Si no hay juegos, mostrar mensaje
        if (!hasGames) {
            content.append("No hay juegos registrados aún.");
        }

        lastGamesTextArea.setText(content.toString());
        lastGamesTextArea.setCaretPosition(0); // Scroll al inicio
    }

    private void refreshRankingContent() {
        if (rankingTextArea == null) return;
        
        // Obtener todos los jugadores (actualizado cada vez)
        ArrayList<Player> players = game.getAllPlayers();

        // Ordenar por puntos (de mayor a menor)
        Collections.sort(players, new Comparator<Player>() {
            @Override
            public int compare(Player p1, Player p2) {
                return Integer.compare(p2.getPoints(), p1.getPoints());
            }
        });

        StringBuilder content = new StringBuilder();
        content.append(String.format("%-5s %-20s %-10s %-50s\n", "Pos", "Usuario", "Puntos", "Últimos Juegos"));
        content.append("--------------------------------------------------------------------------------------------------------\n");

        int position = 1;
        for (Player player : players) {
            String[] lastGames = player.getLastGames();
            String gamesInfo = "";
            if (lastGames != null && lastGames.length > 0) {
                int gamesCount = 0;
                for (String game : lastGames) {
                    if (game != null && !game.trim().isEmpty()) {
                        gamesCount++;
                    }
                }
                gamesInfo = gamesCount + " juegos registrados";
                if (gamesCount > 0 && lastGames[0] != null && !lastGames[0].trim().isEmpty()) {
                    // Mostrar el último juego
                    String lastGame = lastGames[0];
                    if (lastGame.length() > 45) {
                        lastGame = lastGame.substring(0, 42) + "...";
                    }
                    gamesInfo = lastGame;
                }
            } else {
                gamesInfo = "Sin juegos";
            }
            
            content.append(String.format("%-5d %-20s %-10d %-50s\n", 
                position, 
                player.getUsername(), 
                player.getPoints(),
                gamesInfo));
            position++;
        }

        if (players.isEmpty()) {
            content.append("No hay jugadores registrados.");
        }

        rankingTextArea.setText(content.toString());
        rankingTextArea.setCaretPosition(0); // Scroll al inicio
    }

    private void refreshLastGamesPanel() {
        refreshLastGamesContent();
    }

    private void refreshRankingPanel() {
        refreshRankingContent();
    }
}
