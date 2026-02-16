package ui;

import logic.BattleShip;

import javax.swing.*;
import java.awt.*;

public class LoginPanel extends JPanel {
    private BattleShip game;
    private JTextField txtUser;
    private JPasswordField txtPass;
    private MainFrame frame;

    public LoginPanel(MainFrame frame, BattleShip game) {
        this.game = game;
        this.frame = frame;
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel title = new JLabel("BATTLESHIP");
        title.setFont(new Font("Arial", Font.BOLD, 26));

        txtUser = new JTextField(15);
        txtPass = new JPasswordField(15);

        JButton btnLogin = new JButton("Login");
        JButton btnCreate = new JButton("Crear Player");
        JButton btnExit = new JButton("Salir");

        gbc.insets = new Insets(10,10,10,10);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("Usuario:"), gbc);

        gbc.gridx = 1;
        add(txtUser, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        add(txtPass, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        add(btnLogin, gbc);

        gbc.gridx = 1;
        add(btnCreate, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        add(btnExit, gbc);

        btnLogin.addActionListener(e -> login());
        btnCreate.addActionListener(e -> createPlayer());
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
    
    public void clearFields() {
        txtUser.setText("");
        txtPass.setText("");
    }

    private void login() {
        String user = txtUser.getText().trim();
        String pass = new String(txtPass.getPassword());
        
        // Validar campos vacíos
        if (user.isEmpty()) {
            JOptionPane.showMessageDialog(
                this, 
                "Por favor, ingresa un nombre de usuario.",
                "Campo vacío",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        if (pass.isEmpty()) {
            JOptionPane.showMessageDialog(
                this, 
                "Por favor, ingresa una contraseña.",
                "Campo vacío",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        // Verificar si el usuario existe
        if (!this.game.playerExists(user)) {
            JOptionPane.showMessageDialog(
                this, 
                "El usuario no existe. Por favor, crea una cuenta primero.",
                "Usuario no encontrado",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        // Intentar login
        if (this.game.login(user, pass)) {
            clearFields(); // Limpiar campos después de login exitoso
            frame.showMenu();
        } else {
            JOptionPane.showMessageDialog(
                this, 
                "Contraseña incorrecta. Por favor, intenta de nuevo.",
                "Contraseña incorrecta",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void createPlayer() {
        String user = txtUser.getText().trim();
        String pass = new String(txtPass.getPassword());
        
        // Validar campos vacíos
        if (user.isEmpty()) {
            JOptionPane.showMessageDialog(
                this, 
                "Por favor, ingresa un nombre de usuario.",
                "Campo vacío",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        if (pass.isEmpty()) {
            JOptionPane.showMessageDialog(
                this, 
                "Por favor, ingresa una contraseña.",
                "Campo vacío",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        // Verificar si el usuario ya existe
        if (this.game.playerExists(user)) {
            JOptionPane.showMessageDialog(
                this, 
                "El usuario ya existe. Por favor, elige otro nombre de usuario o inicia sesión.",
                "Usuario existente",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        
        // Intentar registro
        if (this.game.register(user, pass)) {
            clearFields(); // Limpiar campos después de registro exitoso
            JOptionPane.showMessageDialog(
                this, 
                "¡Usuario creado exitosamente!",
                "Registro exitoso",
                JOptionPane.INFORMATION_MESSAGE
            );
            frame.showMenu();
        } else {
            JOptionPane.showMessageDialog(
                this, 
                "Error al crear el usuario. Por favor, intenta de nuevo.",
                "Error de registro",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
