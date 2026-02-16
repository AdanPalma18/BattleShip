package model;

public class GameLog {

    public static String win(String winner, String loser, String difficulty) {
        return winner + " hundió todos los barcos de " + loser + " en dificultad " + difficulty;
    }

    public static String retiro(String quitter, String winner) {
        return quitter + " se retiró del juego dejando como ganador a " + winner;
    }
}
