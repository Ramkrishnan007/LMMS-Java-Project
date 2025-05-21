import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class LoginPage extends JFrame {
    private JTextField userIdField;
    private JPasswordField passwordField;
    private JButton loginButton, signUpButton;

    public LoginPage() {
        super("Login Page");
        // Set Nimbus LookAndFeel for a modern UI
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()){
                if ("Nimbus".equals(info.getName())){
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch(Exception e) { /* Fall back if Nimbus not available */ }
        
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 300);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("Library Management System - Login", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 18));
        panel.add(titleLabel, gbc);

        // User ID
        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("User ID:"), gbc);
        gbc.gridx = 1;
        userIdField = new JTextField(20);
        userIdField.setFont(new Font("Georgia", Font.PLAIN, 14));
        panel.add(userIdField, gbc);

        // Password
        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Georgia", Font.PLAIN, 14));
        panel.add(passwordField, gbc);

        // Buttons
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        loginButton = new JButton("Login");
        signUpButton = new JButton("Sign Up");
        btnPanel.add(loginButton);
        btnPanel.add(signUpButton);
        panel.add(btnPanel, gbc);

        add(panel, BorderLayout.CENTER);

        loginButton.addActionListener(e -> performLogin());
        signUpButton.addActionListener(e -> {
            new SignUpPage().setVisible(true);
            dispose();
        });
    }

    private void performLogin() {
        String userIdStr = userIdField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        if(userIdStr.isEmpty() || password.isEmpty()){
            JOptionPane.showMessageDialog(this, "Both User ID and Password must be filled!", 
                                          "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int userId;
        try {
            userId = Integer.parseInt(userIdStr);
        } catch(NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid User ID.", 
                                          "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String role = authenticate(userId, password);
        if(role != null){
            JOptionPane.showMessageDialog(this, "Login successful! Role: " + role, 
                                          "Success", JOptionPane.INFORMATION_MESSAGE);
            openUserPage(userId, role);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Invalid User ID or Password!", 
                                          "Authentication Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Returns role if authentication succeeds; otherwise null.
    private String authenticate(int userId, String password) {
        try (Connection conn = DBConnection.getConnection()){
            if(conn == null) return null;
            // Use the correct column names: U_Password and U_Role
            String query = "SELECT U_Password, U_Role FROM Login_Table WHERE Login_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                String storedPassword = rs.getString("U_Password").trim();
                String role = rs.getString("U_Role").trim();
                // For demo purposes, passwords are compared as plain text.
                if(password.equals(storedPassword)){
                    return role;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            return null;
        }
    }



    private void openUserPage(int userId, String U_role) {
        if(U_role.equalsIgnoreCase("Student")){
            new StudentPage(userId).setVisible(true);
        } else if(U_role.equalsIgnoreCase("Faculty")){
            new FacultyPage(userId).setVisible(true);
        } else if(U_role.equalsIgnoreCase("Admin")){
            new AdminPage(userId).setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Unknown role: " + U_role, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginPage().setVisible(true));
    }
}

/*import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class LoginPage extends JFrame {
    private JTextField userIdField;
    private JPasswordField passwordField;
    private JButton loginButton, signUpButton;

    public LoginPage() {
        super("Login Page");
        // Set Nimbus LookAndFeel if available
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()){
                if ("Nimbus".equals(info.getName())){
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch(Exception e) { /* fall back */ /*}

       /* initUI();
    }

   /* private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 300);
        setLocationRelativeTo(null);

        // Use a panel with GridBagLayout for better control
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("Library Management System - Login", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 18));
        panel.add(titleLabel, gbc);

        // User ID
        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("User ID:"), gbc);
        gbc.gridx = 1;
        userIdField = new JTextField(20);
        userIdField.setFont(new Font("Georgia", Font.PLAIN, 14));
        panel.add(userIdField, gbc);

        // Password
        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Georgia", Font.PLAIN, 14));
        panel.add(passwordField, gbc);

        // Buttons
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        loginButton = new JButton("Login");
        signUpButton = new JButton("Sign Up");
        buttonPanel.add(loginButton);
        buttonPanel.add(signUpButton);
        panel.add(buttonPanel, gbc);

        add(panel, BorderLayout.CENTER);

        // Action listeners
        loginButton.addActionListener(e -> performLogin());
        signUpButton.addActionListener(e -> {
            new SignUpPage().setVisible(true);
            dispose();
        });
    }

    private void performLogin() {
        String userIdStr = userIdField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        if(userIdStr.isEmpty() || password.isEmpty()){
            JOptionPane.showMessageDialog(this, "Both User ID and Password must be filled!", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int userId;
        try {
            userId = Integer.parseInt(userIdStr);
        } catch(NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "User ID must be a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String role = authenticate(userId, password);
        if(role != null){
            JOptionPane.showMessageDialog(this, "Login successful! Role: " + role, "Success", JOptionPane.INFORMATION_MESSAGE);
            openUserPage(userId, role);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Invalid User ID or Password!", "Authentication Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Authenticates the user using Login_Table.
     * @param userId integer user id.
     * @param password plain text password.
     * @return the role if authentication succeeds; otherwise null.
     */
   /* private String authenticate(int userId, String password) {
        try (Connection conn = DBConnection.getConnection()){
            if(conn == null) return null;
            String query = "SELECT U_Password, Role FROM Login_Table WHERE Login_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                String storedPassword = rs.getString("U_Password").trim();
                String role = rs.getString("Role").trim();
                // For demo, using plain text comparison; use hashing in production
                if(password.equals(storedPassword)){
                    return role;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            return null;
        }
    }

    private void openUserPage(int userId, String role) {
        if(role.equalsIgnoreCase("Student")){
            new StudentPage(userId).setVisible(true);
        } else if(role.equalsIgnoreCase("Faculty")){
            new FacultyPage(userId).setVisible(true);
        } else if(role.equalsIgnoreCase("Admin")){
            new AdminPage(userId).setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Unknown role: " + role, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginPage().setVisible(true));
    }
}

/*import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class LoginPage extends JFrame {
    private JTextField userIdField;
    private JPasswordField passwordField;
    private JButton loginButton, signUpButton;

    public LoginPage() {
        super("Login Page");
        // Set Nimbus LookAndFeel (if available)
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch(Exception e) {
            // fallback to default
        }
        
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 300);
        setLocationRelativeTo(null);
        
        // Create a main panel with GridBagLayout
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Row 0: Title Label
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("Library Management System - Login", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 18));
        mainPanel.add(titleLabel, gbc);
        
        // Row 1: User ID Label and Field
        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("User ID:"), gbc);
        gbc.gridx = 1;
        userIdField = new JTextField(20);
        userIdField.setFont(new Font("Georgia", Font.PLAIN, 14));
        mainPanel.add(userIdField, gbc);
        
        // Row 2: Password Label and Field
        gbc.gridy++;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Georgia", Font.PLAIN, 14));
        mainPanel.add(passwordField, gbc);
        
        // Row 3: Buttons Panel
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        loginButton = new JButton("Login");
        signUpButton = new JButton("Sign Up");
        buttonPanel.add(loginButton);
        buttonPanel.add(signUpButton);
        mainPanel.add(buttonPanel, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Action Listeners
        loginButton.addActionListener(e -> performLogin());
        signUpButton.addActionListener(e -> {
            new SignUpPage().setVisible(true);
            dispose();
        });
    }
    
    private void performLogin() {
        String userId = userIdField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        if(userId.isEmpty() || password.isEmpty()){
            JOptionPane.showMessageDialog(this, "Both User ID and Password must be filled!", 
                                          "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String role = authenticate(userId, password);
        if(role != null){
            JOptionPane.showMessageDialog(this, "Login successful! Role: " + role, 
                                          "Success", JOptionPane.INFORMATION_MESSAGE);
            openUserPage(userId, role);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Invalid User ID or Password!", 
                                          "Authentication Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Authenticates against your database (adjust as needed)
    private String authenticate(String userId, String password) {
        try (Connection conn = DBConnection.getConnection()){
            if(conn == null) return null;
            String query = "SELECT * FROM Login_Table WHERE Login_ID = ? AND U_Password = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, userId);
            stmt.setString(2, password); // Use hashing in production!
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                return rs.getString("Role");
            } else {
                return null;
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            return null;
        }
    }
    
    private void openUserPage(String userId, String role) {
        if(role.equalsIgnoreCase("Student")){
            new StudentPage(userId).setVisible(true);
        } else if(role.equalsIgnoreCase("Faculty")){
            new FacultyPage(userId).setVisible(true);
        } else if(role.equalsIgnoreCase("Admin")){
            new AdminPage(userId).setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Unknown role: " + role, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginPage().setVisible(true));
    }
}

/*import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class LoginPage extends JFrame {
    private JTextField userIdField;
    private JPasswordField passwordField;
    private JButton loginButton, signUpButton;

    public LoginPage() {
        setTitle("Login Page");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new FlowLayout());

        userIdField = new JTextField(20);
        passwordField = new JPasswordField(20);
        loginButton = new JButton("Login");
        signUpButton = new JButton("Sign Up");

        add(new JLabel("User ID:"));
        add(userIdField);
        add(new JLabel("Password:"));
        add(passwordField);
        add(loginButton);
        add(signUpButton);

        loginButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                String userId = userIdField.getText().trim();
                String password = new String(passwordField.getPassword()).trim();
                if(userId.isEmpty() || password.isEmpty()){
                    JOptionPane.showMessageDialog(LoginPage.this, "Both User ID and Password must be filled!");
                    return;
                }
                String role = authenticate(userId, password);
                if(role != null){
                    JOptionPane.showMessageDialog(LoginPage.this, "Login successful! Role: " + role);
                    openUserPage(userId, role);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(LoginPage.this, "Invalid User ID or Password!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        signUpButton.addActionListener(e -> {
            new SignUpPage().setVisible(true);
            dispose();
        });
    }

    // Returns role on successful authentication; otherwise null.
    private String authenticate(String userId, String password) {
        try (Connection conn = DBConnection.getConnection()){
            if(conn == null) return null;
            String query = "SELECT * FROM Login_Table WHERE Login_ID = ? AND U_Password = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, userId);
            stmt.setString(2, password); // using plain text comparison for demo
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                return rs.getString("Role");
            } else {
                return null;
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            return null;
        }
    }

    // Opens the page corresponding to the user role.
    private void openUserPage(String userId, String role) {
        if(role.equalsIgnoreCase("Student")){
            new StudentPage(userId).setVisible(true);
        } else if(role.equalsIgnoreCase("Faculty")){
            new FacultyPage(userId).setVisible(true);
        } else if(role.equalsIgnoreCase("Admin")){
			new AdminPage(userId).setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Unknown role: " + role);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginPage().setVisible(true));
    }
}
/*import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.regex.Pattern;

public class LoginPage extends JFrame {
    private JTextField userIdField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton signUpButton;

    public LoginPage() {
        setTitle("Login Page");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window
        setLayout(new FlowLayout());

        // Create components for Login
        userIdField = new JTextField(20);
        passwordField = new JPasswordField(20);
        loginButton = new JButton("Login");
        signUpButton = new JButton("Sign Up");

        add(new JLabel("User ID:"));
        add(userIdField);
        add(new JLabel("Password:"));
        add(passwordField);
        add(loginButton);
        add(signUpButton);

        // Login Button Action
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String userId = userIdField.getText().trim();
                String password = new String(passwordField.getPassword()).trim();

                // Validate input (simple check)
                if (userId.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(LoginPage.this, "Both User ID and Password must be filled!");
                    return;
                }

                // Authenticate credentials using DBConnection
                String errorMessage = authenticate(userId, password);
                if (errorMessage == null) {
                    JOptionPane.showMessageDialog(LoginPage.this, "Login successful!");
                    // Open the StudentPage (or another page based on role if needed)
                    new StudentPage(userId).setVisible(true);
                    setVisible(false); // Hide the login page
                } else {
                    JOptionPane.showMessageDialog(LoginPage.this, errorMessage);
                }
            }
        });

        // Sign Up Button Action
        signUpButton.addActionListener(e -> {
            new SignUpPage().setVisible(true);
            setVisible(false); // Hide the login page
        });
    }

   
    private String authenticate(String userId, String password) {
        try (Connection conn = DBConnection.getConnection()) {
            if (conn == null) {
                return "Database connection failed!";
            }

            String query = "SELECT * FROM Login WHERE User_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("Password"); // For demo, plain text comparison
                // You can later hash and compare passwords
                if (password.equals(storedPassword)) {
                    return null; // No error, login successful
                } else {
                    return "Incorrect password!";
                }
            } else {
                return "Invalid User ID or Password!";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "An error occurred. Please try again later.";
        }
    }

    public static void main(String[] args) {
        // Launch the UI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new LoginPage().setVisible(true));
    }
}
*/
