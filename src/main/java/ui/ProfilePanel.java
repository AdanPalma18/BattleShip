package ui;

import logic.BattleShip;
import model.Player;

import javax.swing.*;
import java.awt.*;

public class ProfilePanel extends JPanel {

    private MainFrame frame;
    private BattleShip game;
    private CardLayout cardLayout;
    private JPanel profileContentPanel;
    private JTextArea profileDataTextArea;

    public ProfilePanel(MainFrame frame, BattleShip game) {
        this.frame = frame;
        this.game = game;
        setLayout(new BorderLayout());

        // Panel principal con CardLayout para las diferentes vistas
        cardLayout = new CardLayout();
        profileContentPanel = new JPanel(cardLayout);

        // Panel de menú de perfil
        JPanel menuPanel = buildProfileMenu();
        profileContentPanel.add(menuPanel, "MENU");

        // Panel de ver datos
        JPanel viewDataPanel = buildViewDataPanel();
        profileContentPanel.add(viewDataPanel, "VIEW_DATA");

        // Panel de modificar datos
        JPanel modifyDataPanel = buildModifyDataPanel();
        profileContentPanel.add(modifyDataPanel, "MODIFY_DATA");

        add(profileContentPanel, BorderLayout.CENTER);
    }

    private JPanel buildProfileMenu() {
        JPanel panel = new JPanel(new GridLayout(5, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("MI PERFIL", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        panel.add(title);

        JButton btnViewData = new JButton("a. Ver mis Datos");
        JButton btnModifyData = new JButton("b. Modificar mis Datos");
        JButton btnDeleteAccount = new JButton("c. Eliminar mi Cuenta");
        JButton btnBack = new JButton("d. Regresar al Menú Principal");

        btnViewData.addActionListener(e -> {
            refreshViewDataPanel();
            cardLayout.show(profileContentPanel, "VIEW_DATA");
        });
        btnModifyData.addActionListener(e -> cardLayout.show(profileContentPanel, "MODIFY_DATA"));
        btnDeleteAccount.addActionListener(e -> handleDeleteAccount());
        btnBack.addActionListener(e -> frame.showMenu());

        panel.add(btnViewData);
        panel.add(btnModifyData);
        panel.add(btnDeleteAccount);
        panel.add(btnBack);

        return panel;
    }

    private JPanel buildViewDataPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Mis Datos", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(title, BorderLayout.NORTH);

        // Crear área de texto para mostrar los datos
        profileDataTextArea = new JTextArea();
        profileDataTextArea.setEditable(false);
        profileDataTextArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        profileDataTextArea.setBackground(Color.WHITE);

        // Actualizar contenido inicialmente
        refreshViewDataContent();

        JScrollPane scrollPane = new JScrollPane(profileDataTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton btnBack = new JButton("Regresar");
        btnBack.addActionListener(e -> cardLayout.show(profileContentPanel, "MENU"));
        
        // Botón para refrescar
        JButton btnRefresh = new JButton("Actualizar");
        btnRefresh.addActionListener(e -> refreshViewDataContent());
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(btnRefresh);
        buttonPanel.add(btnBack);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildModifyDataPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel title = new JLabel("Modificar Mis Datos", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;

        // Username
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Nuevo Username:"), gbc);
        gbc.gridx = 1;
        JTextField usernameField = new JTextField(20);
        panel.add(usernameField, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Nuevo Password:"), gbc);
        gbc.gridx = 1;
        JPasswordField passwordField = new JPasswordField(20);
        panel.add(passwordField, gbc);

        // Botones
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton btnSave = new JButton("Guardar Cambios");
        btnSave.addActionListener(e -> {
            String newUsername = usernameField.getText().trim();
            String newPassword = new String(passwordField.getPassword());
            
            if (newUsername.isEmpty() && newPassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                "Debe ingresar al menos un campo para modificar.", 
                "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            boolean success = game.modifyPlayerData(newUsername, newPassword);
            if (success) {
                JOptionPane.showMessageDialog(this, 
                "Datos modificados exitosamente.", 
                "Éxito", JOptionPane.INFORMATION_MESSAGE);
                usernameField.setText("");
                passwordField.setText("");
                // Actualizar el panel de ver datos si existe
                refreshViewDataPanel();
            } else {
                JOptionPane.showMessageDialog(this, 
                "Error al modificar los datos. Verifique que el username no esté en uso.", 
                "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        JButton btnBack = new JButton("Regresar");
        btnBack.addActionListener(e -> {
            usernameField.setText("");
            passwordField.setText("");
            cardLayout.show(profileContentPanel, "MENU");
        });
        
        buttonPanel.add(btnSave);
        buttonPanel.add(btnBack);
        panel.add(buttonPanel, gbc);

        return panel;
    }

    private void refreshViewDataContent() {
        if (profileDataTextArea == null) return;
        
        Player currentUser = game.getCurrentUser();
        if (currentUser == null) {
            profileDataTextArea.setText("No hay usuario logueado.");
            return;
        }

        StringBuilder content = new StringBuilder();
        content.append("=== DATOS DEL PERFIL ===\n\n");
        content.append("Username: ").append(currentUser.getUsername()).append("\n");
        content.append("Puntos: ").append(currentUser.getPoints()).append("\n");
        content.append("\n=== ÚLTIMOS 10 JUEGOS ===\n");
        
        String[] lastGames = currentUser.getLastGames();
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
        
        if (!hasGames) {
            content.append("No hay juegos registrados aún.\n");
        }

        profileDataTextArea.setText(content.toString());
        profileDataTextArea.setCaretPosition(0);
    }

    private void refreshViewDataPanel() {
        refreshViewDataContent();
    }

    private void handleDeleteAccount() {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "¿Está seguro de que desea eliminar su cuenta?\nEsta acción no se puede deshacer.",
            "Confirmar Eliminación",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = game.deleteCurrentUser();
            if (success) {
                JOptionPane.showMessageDialog(this, 
                "Cuenta eliminada exitosamente.", 
                "Éxito", JOptionPane.INFORMATION_MESSAGE);
                frame.showLogin();
            } else {
                JOptionPane.showMessageDialog(this, 
                "Error al eliminar la cuenta.", 
                "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
