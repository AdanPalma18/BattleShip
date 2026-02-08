import logic.BattleShip;
import ui.MainFrame;

public class Main {
    public static void main(String[] args) {
        BattleShip game = new BattleShip();
        javax.swing.SwingUtilities.invokeLater(() -> {
            new MainFrame(game).setVisible(true);
        });
    }
}