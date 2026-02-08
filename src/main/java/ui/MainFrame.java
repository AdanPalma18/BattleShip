package ui;

import logic.BattleShip;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    public static final String LOGIN = "LOGIN";
    public static final String MENU = "MENU";
    public static final String GAME = "GAME";
    CardLayout cardLayout;
    JPanel mainPanel;
    private GamePanel gamePanel;
    private BattleShip game;
    public MainFrame(BattleShip game) {
        this.game = game;
        setTitle("BattleShip - 22511257");
        setSize(600, 600);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        gamePanel = new GamePanel(this, game);

        mainPanel.add(new LoginPanel(this, game), LOGIN);
        mainPanel.add(new MenuPanel(this, game), MENU);
        mainPanel.add(gamePanel, GAME);

        add(mainPanel);
        showLogin();


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
