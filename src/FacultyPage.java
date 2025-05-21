import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.util.Vector;

public class FacultyPage extends JFrame {
    private int userId;
    private JTable bookTable;
    private DefaultTableModel tableModel;
    private JButton requestButton, logoutButton, searchButton;
    private JTextField searchField;
    
    // Upload panel components (Assuming B_ID is auto-generated in DB)
    private JTextField bTitleField, authorField;
    private JComboBox<String> bTypeComboBox; // Dropdown for "PDF" or "Textbook"
    private JButton chooseFileButton, uploadButton;
    private JLabel filePathLabel;
    private File selectedFile; // holds chosen file (if any)

    public FacultyPage(int userId) {
        super("Faculty Page");
        this.userId = userId;
        initUI();
    }
    
    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        // Title at top
        JLabel titleLabel = new JLabel("Welcome, Faculty " + userId, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);
        
        // Split pane: left for book list and request, right for upload form.
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(550);
        
        // Left Panel: Book list with search and request buttons
        JPanel listPanel = new JPanel(new BorderLayout());
        // Search panel at top
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(20);
        searchButton = new JButton("Search");
        searchButton.addActionListener(e -> loadBooks());
        searchPanel.add(new JLabel("Search (Title/Author):"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        listPanel.add(searchPanel, BorderLayout.NORTH);
        
        // Table for books
        tableModel = new DefaultTableModel(new Object[]{"B_ID", "B_Title", "B_Type", "Author_Name", "Status"}, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        bookTable = new JTable(tableModel);
        // Custom renderer to color rows based on "Status" column
        bookTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = (String) table.getModel().getValueAt(row, 4);
                if (!isSelected) {
                    if ("Available".equalsIgnoreCase(status)) {
                        c.setBackground(new Color(144,238,144)); // light green
                    } else if ("Requested".equalsIgnoreCase(status)) {
                        c.setBackground(new Color(255,255,153)); // light yellow
                    } else if ("Held".equalsIgnoreCase(status)) {
                        c.setBackground(new Color(255,102,102)); // light red
                    } else {
                        c.setBackground(Color.white);
                    }
                } else {
                    c.setBackground(table.getSelectionBackground());
                }
                return c;
            }
        });
        loadBooks();
        listPanel.add(new JScrollPane(bookTable), BorderLayout.CENTER);
        
        // Bottom panel with Request and Logout buttons
        JPanel listBottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        requestButton = new JButton("Request Book");
        requestButton.addActionListener(e -> requestSelectedBook());
        logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            new LoginPage().setVisible(true);
            dispose();
        });
        listBottom.add(requestButton);
        listBottom.add(logoutButton);
        listPanel.add(listBottom, BorderLayout.SOUTH);
        splitPane.setLeftComponent(listPanel);
        
        // Right Panel: Book upload form
        JPanel uploadPanel = new JPanel(new GridBagLayout());
        uploadPanel.setBorder(BorderFactory.createTitledBorder("Upload New Book"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Book Title
        gbc.gridx = 0; gbc.gridy = 0;
        uploadPanel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        bTitleField = new JTextField(20);
        uploadPanel.add(bTitleField, gbc);
        
        // Book Type as dropdown
        gbc.gridx = 0; gbc.gridy++;
        uploadPanel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        String[] types = {"PDF", "Textbook"};
        bTypeComboBox = new JComboBox<>(types);
        uploadPanel.add(bTypeComboBox, gbc);
        
        // Author
        gbc.gridx = 0; gbc.gridy++;
        uploadPanel.add(new JLabel("Author:"), gbc);
        gbc.gridx = 1;
        authorField = new JTextField(20);
        uploadPanel.add(authorField, gbc);
        
        // Choose file button (only applicable for PDF books)
        gbc.gridx = 0; gbc.gridy++;
        chooseFileButton = new JButton("Choose PDF File");
        chooseFileButton.addActionListener(e -> choosePdfFile());
        uploadPanel.add(chooseFileButton, gbc);
        gbc.gridx = 1;
        filePathLabel = new JLabel("No file selected");
        uploadPanel.add(filePathLabel, gbc);
        
        // Upload button
        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        uploadButton = new JButton("Upload Book");
        uploadButton.addActionListener(e -> uploadBook());
        uploadPanel.add(uploadButton, gbc);
        
        splitPane.setRightComponent(uploadPanel);
        add(splitPane, BorderLayout.CENTER);
    }
    
    // Loads available books with computed status.
    private void loadBooks() {
        try (Connection conn = DBConnection.getConnection()){
            String searchTerm = "%" + searchField.getText().trim() + "%";
            // Query that checks if a different user has an approved request (Held) or if current user has a pending request (Requested)
            String query = "SELECT B.B_ID, B.B_Title, B.B_Type, B.Author_Name, " +
                           "CASE " +
                           "  WHEN EXISTS (SELECT 1 FROM Borrow_Requests BR WHERE BR.B_Title = B.B_Title " +
                           "         AND BR.Status = 'Approved' AND DATEDIFF(day, BR.Request_Date, GETDATE()) < 2 " +
                           "         AND BR.Login_ID <> ?) THEN 'Held' " +
                           "  WHEN EXISTS (SELECT 1 FROM Borrow_Requests BR WHERE BR.B_Title = B.B_Title " +
                           "         AND BR.Status = 'Pending' AND BR.Login_ID = ?) THEN 'Requested' " +
                           "  ELSE 'Available' " +
                           "END as BookStatus " +
                           "FROM Book B " +
                           "WHERE (B.B_Title LIKE ? OR B.Author_Name LIKE ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setString(3, searchTerm);
            stmt.setString(4, searchTerm);
            ResultSet rs = stmt.executeQuery();
            tableModel.setRowCount(0);
            while(rs.next()){
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("B_ID"));
                row.add(rs.getString("B_Title"));
                row.add(rs.getString("B_Type"));
                row.add(rs.getString("Author_Name"));
                row.add(rs.getString("BookStatus"));
                tableModel.addRow(row);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading available books: " + ex.getMessage(), 
                                          "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Student requests a book (after checking that they hold less than 3 books)
    private void requestSelectedBook() {
        int heldBooks = countHeldBooks();
        if (heldBooks >= 3) {
            JOptionPane.showMessageDialog(this, "You already hold 3 books. Please return a book before requesting a new one.",
                                          "Limit Reached", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int selectedRow = bookTable.getSelectedRow();
        if(selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book to request.");
            return;
        }
        String bType = tableModel.getValueAt(selectedRow, 2).toString();
        if ("PDF".equalsIgnoreCase(bType)) {
            JOptionPane.showMessageDialog(this, "PDF books cannot be requested. Use the Download button instead.");
            return;
        }
        String bTitle = tableModel.getValueAt(selectedRow, 1).toString();
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Borrow_Requests (Login_ID, B_Title, Request_Date, Status) " +
                           "VALUES (?, ?, GETDATE(), 'Pending')";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setString(2, bTitle);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Book request submitted successfully!");
                loadBooks();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to submit request.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error processing request: " + ex.getMessage(),
                                          "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Count the number of books this faculty currently holds or has pending
    private int countHeldBooks() {
        int count = 0;
        try (Connection conn = DBConnection.getConnection()){
            String query = "SELECT COUNT(*) AS total FROM Borrow_Requests " +
                           "WHERE Login_ID = ? AND (Status = 'Approved' OR Status = 'Pending')";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                count = rs.getInt("total");
            }
        } catch(SQLException ex) {
            ex.printStackTrace();
        }
        return count;
    }
    
    // Opens a file chooser to select a PDF file
    private void choosePdfFile() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showOpenDialog(this);
        if(option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".pdf")) {
                JOptionPane.showMessageDialog(this, "Please select a PDF file.", "Invalid File", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (file.length() > 2 * 1024 * 1024) { // 2 MB limit
                JOptionPane.showMessageDialog(this, "File size exceeds 2MB.", "File Too Large", JOptionPane.ERROR_MESSAGE);
                return;
            }
            selectedFile = file;
            filePathLabel.setText(file.getAbsolutePath());
        }
    }
    
    // Uploads a new book (PDF files are stored as BLOBs, textbooks only store details)
    private void uploadBook() {
        String title = bTitleField.getText().trim();
        String type = bTypeComboBox.getSelectedItem().toString().trim();
        String author = authorField.getText().trim();
        if(title.isEmpty() || type.isEmpty() || author.isEmpty()){
            JOptionPane.showMessageDialog(this, "Please fill in all upload fields.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try (Connection conn = DBConnection.getConnection()){
            if("PDF".equalsIgnoreCase(type)) {
                if(selectedFile == null) {
                    JOptionPane.showMessageDialog(this, "Please choose a PDF file to upload.", "File Required", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                byte[] fileData;
                try (FileInputStream fis = new FileInputStream(selectedFile)) {
                    fileData = fis.readAllBytes();
                }
                String query = "INSERT INTO Book (B_Title, B_Type, Author_Name, PDF_File) VALUES (?, ?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, title);
                stmt.setString(2, type);
                stmt.setString(3, author);
                stmt.setBytes(4, fileData);
                int rows = stmt.executeUpdate();
                if(rows > 0) {
                    JOptionPane.showMessageDialog(this, "PDF book uploaded successfully!");
                    loadBooks();
                    clearUploadFields();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to upload PDF book.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                // For textbooks (non-PDF), insert only details.
                String query = "INSERT INTO Book (B_Title, B_Type, Author_Name) VALUES (?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, title);
                stmt.setString(2, type);
                stmt.setString(3, author);
                int rows = stmt.executeUpdate();
                if(rows > 0) {
                    JOptionPane.showMessageDialog(this, "Book uploaded successfully!");
                    loadBooks();
                    clearUploadFields();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to upload book.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error uploading book: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch(IOException ioe) {
            ioe.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error reading file: " + ioe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Clears the upload form fields after successful upload.
    private void clearUploadFields() {
        bTitleField.setText("");
        bTypeComboBox.setSelectedIndex(0);
        authorField.setText("");
        selectedFile = null;
        filePathLabel.setText("No file selected");
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FacultyPage(101).setVisible(true));
    }
}

/*import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;

public class FacultyPage extends JFrame {
    private int userId;
    private JTable bookTable;
    private DefaultTableModel tableModel;
    private JButton requestButton, logoutButton;
    
    // Upload panel components (B_ID removed – auto-incremented in DB)
    private JTextField bTitleField, authorField;
    private JComboBox<String> bTypeComboBox; // Dropdown for "PDF" or "Textbook"
    private JButton chooseFileButton, uploadButton;
    private JLabel filePathLabel;
    private File selectedFile; // holds the chosen file (if any)
    
    public FacultyPage(int userId) {
        super("Faculty Page");
        this.userId = userId;
        initUI();
    }
    
    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        // Title at top
        JLabel titleLabel = new JLabel("Welcome, Faculty " + userId, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);
        
        // Split pane: left for book list (and request) and right for upload form.
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(550);
        
        // Left panel: Book list and Request Book & Logout buttons
        JPanel listPanel = new JPanel(new BorderLayout());
        tableModel = new DefaultTableModel(new Object[]{"B_ID", "B_Title", "B_Type", "Author_Name"}, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        bookTable = new JTable(tableModel);
        loadBooks();
        listPanel.add(new JScrollPane(bookTable), BorderLayout.CENTER);
        
        JPanel listBottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        requestButton = new JButton("Request Book");
        requestButton.setEnabled(true);
        requestButton.addActionListener(e -> requestSelectedBook());
        logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            new LoginPage().setVisible(true);
            dispose();
        });
        listBottom.add(requestButton);
        listBottom.add(logoutButton);
        listPanel.add(listBottom, BorderLayout.SOUTH);
        splitPane.setLeftComponent(listPanel);
        
        // Right panel: Book upload form
        JPanel uploadPanel = new JPanel(new GridBagLayout());
        uploadPanel.setBorder(BorderFactory.createTitledBorder("Upload New Book"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Book Title
        gbc.gridx = 0; gbc.gridy = 0;
        uploadPanel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        bTitleField = new JTextField(20);
        uploadPanel.add(bTitleField, gbc);
        
        // Book Type as dropdown
        gbc.gridx = 0; gbc.gridy++;
        uploadPanel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        String[] types = {"PDF", "Textbook"};
        bTypeComboBox = new JComboBox<>(types);
        uploadPanel.add(bTypeComboBox, gbc);
        
        // Author
        gbc.gridx = 0; gbc.gridy++;
        uploadPanel.add(new JLabel("Author:"), gbc);
        gbc.gridx = 1;
        authorField = new JTextField(20);
        uploadPanel.add(authorField, gbc);
        
        // For PDF books: Choose file button and file path label
        gbc.gridx = 0; gbc.gridy++;
        chooseFileButton = new JButton("Choose PDF File");
        chooseFileButton.addActionListener(e -> choosePdfFile());
        uploadPanel.add(chooseFileButton, gbc);
        gbc.gridx = 1;
        filePathLabel = new JLabel("No file selected");
        uploadPanel.add(filePathLabel, gbc);
        
        // Upload button
        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        uploadButton = new JButton("Upload Book");
        uploadPanel.add(uploadButton, gbc);
        uploadButton.addActionListener(e -> uploadBook());
        
        splitPane.setRightComponent(uploadPanel);
        add(splitPane, BorderLayout.CENTER);
    }
    
    
    // Loads all books from the Book table
    private void loadBooks() {
        try (Connection conn = DBConnection.getConnection()){
            String query = "SELECT B_ID, B_Title, B_Type, Author_Name FROM Book";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            tableModel.setRowCount(0);
            while(rs.next()){
                Object[] row = {
                    rs.getInt("B_ID"),
                    rs.getString("B_Title"),
                    rs.getString("B_Type"),
                    rs.getString("Author_Name")
                };
                tableModel.addRow(row);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading books: " + ex.getMessage(),
                                          "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // When a faculty selects a book and clicks "Request Book" (if needed)
    private void requestSelectedBook() {
        int selectedRow = bookTable.getSelectedRow();
        if(selectedRow == -1){
            JOptionPane.showMessageDialog(this, "Please select a book to request.");
            return;
        }
        String bTitle = tableModel.getValueAt(selectedRow, 1).toString();
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Borrow_Requests (Login_ID, B_Title, Request_Date, Status) " +
                           "VALUES (?, ?, GETDATE(), ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setString(2, bTitle);
            stmt.setString(3, "Pending");
            int rows = stmt.executeUpdate();
            if(rows > 0){
                JOptionPane.showMessageDialog(this, "Book request sent!");
                loadBooks();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to send request.");
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
                                          "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Opens a file chooser to select a PDF file; checks extension and size.
    private void choosePdfFile() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showOpenDialog(this);
        if(option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if(!file.getName().toLowerCase().endsWith(".pdf")) {
                JOptionPane.showMessageDialog(this, "Please select a PDF file.",
                                              "Invalid File", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(file.length() > 2 * 1024 * 1024) {
                JOptionPane.showMessageDialog(this, "File size exceeds 2MB.",
                                              "File Too Large", JOptionPane.ERROR_MESSAGE);
                return;
            }
            selectedFile = file;
            filePathLabel.setText(file.getAbsolutePath());
        }
    }
    
    // Uploads a new book.
    // For PDF books, the file is read and stored as a BLOB (PDF_File).
    // For non-PDF books, no file is uploaded.
    private void uploadBook() {
        try (Connection conn = DBConnection.getConnection()){
            String title = bTitleField.getText().trim();
            String type = bTypeComboBox.getSelectedItem().toString().trim();
            String author = authorField.getText().trim();
            
            if(title.isEmpty() || type.isEmpty() || author.isEmpty()){
                JOptionPane.showMessageDialog(this, "Please fill in all fields.",
                                              "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if("pdf".equalsIgnoreCase(type)) {
                if(selectedFile == null) {
                    JOptionPane.showMessageDialog(this, "Please choose a PDF file to upload.",
                                                  "File Required", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                byte[] fileData;
                try (FileInputStream fis = new FileInputStream(selectedFile)) {
                    fileData = fis.readAllBytes();
                }
                String query = "INSERT INTO Book (B_Title, B_Type, Author_Name, PDF_File) VALUES (?, ?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, title);
                stmt.setString(2, type);
                stmt.setString(3, author);
                stmt.setBytes(4, fileData);
                int rows = stmt.executeUpdate();
                if(rows > 0) {
                    JOptionPane.showMessageDialog(this, "PDF book uploaded successfully!");
                    loadBooks();
                    clearUploadFields();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to upload book.");
                }
            } else {
                // For non-PDF (e.g. Textbook)
                String query = "INSERT INTO Book (B_Title, B_Type, Author_Name) VALUES (?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, title);
                stmt.setString(2, type);
                stmt.setString(3, author);
                int rows = stmt.executeUpdate();
                if(rows > 0){
                    JOptionPane.showMessageDialog(this, "Book uploaded successfully!");
                    loadBooks();
                    clearUploadFields();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to upload book.");
                }
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
                                          "Error", JOptionPane.ERROR_MESSAGE);
        } catch(IOException ioe) {
            ioe.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error reading file: " + ioe.getMessage(),
                                          "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Clears the upload form fields after a successful upload.
    private void clearUploadFields() {
        bTitleField.setText("");
        bTypeComboBox.setSelectedIndex(0);
        authorField.setText("");
        selectedFile = null;
        filePathLabel.setText("No file selected");
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FacultyPage(101).setVisible(true));
    }
}

/*import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;

public class FacultyPage extends JFrame {
    private int userId;
    private JTable bookTable;
    private DefaultTableModel tableModel;
    private JButton requestButton, logoutButton;
    
    // Upload panel components
    private JTextField bIdField, bTitleField, bTypeField, authorField;
    private JButton chooseFileButton, uploadButton;
    private JLabel filePathLabel;
    private File selectedFile; // to hold chosen file
    
    public FacultyPage(int userId) {
        super("Faculty Page");
        this.userId = userId;
        initUI();
    }
    
    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        // Title at top
        JLabel titleLabel = new JLabel("Welcome, Faculty " + userId, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);
        
        // Split pane: left for book list (and request) and right for upload form.
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(550);
        
        // Left panel: Book list and Request Book & Logout buttons
        JPanel listPanel = new JPanel(new BorderLayout());
        tableModel = new DefaultTableModel(new Object[]{"B_ID", "B_Title", "B_Type", "Author_Name"}, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        bookTable = new JTable(tableModel);
        loadBooks();
        listPanel.add(new JScrollPane(bookTable), BorderLayout.CENTER);
        
        JPanel listBottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        requestButton = new JButton("Request Book");
        // Faculty can also request a book (if needed)
        requestButton.setEnabled(true);
        requestButton.addActionListener(e -> requestSelectedBook());
        logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            new LoginPage().setVisible(true);
            dispose();
        });
        listBottom.add(requestButton);
        listBottom.add(logoutButton);
        listPanel.add(listBottom, BorderLayout.SOUTH);
        splitPane.setLeftComponent(listPanel);
        
        // Right panel: Book upload form
        JPanel uploadPanel = new JPanel(new GridBagLayout());
        uploadPanel.setBorder(BorderFactory.createTitledBorder("Upload New Book"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        uploadPanel.add(new JLabel("B_ID:"), gbc);
        gbc.gridx = 1;
        bIdField = new JTextField(10);
        uploadPanel.add(bIdField, gbc);
        
        gbc.gridx = 0; gbc.gridy++;
        uploadPanel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        bTitleField = new JTextField(20);
        uploadPanel.add(bTitleField, gbc);
        
        gbc.gridx = 0; gbc.gridy++;
        uploadPanel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        bTypeField = new JTextField(10);
        uploadPanel.add(bTypeField, gbc);
        
        gbc.gridx = 0; gbc.gridy++;
        uploadPanel.add(new JLabel("Author:"), gbc);
        gbc.gridx = 1;
        authorField = new JTextField(20);
        uploadPanel.add(authorField, gbc);
        
        // For PDF books, add a Choose File button and file path label.
        gbc.gridx = 0; gbc.gridy++;
        chooseFileButton = new JButton("Choose PDF File");
        chooseFileButton.addActionListener(e -> choosePdfFile());
        uploadPanel.add(chooseFileButton, gbc);
        gbc.gridx = 1;
        filePathLabel = new JLabel("No file selected");
        uploadPanel.add(filePathLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        uploadButton = new JButton("Upload Book");
        uploadPanel.add(uploadButton, gbc);
        
        uploadButton.addActionListener(e -> uploadBook());
        
        splitPane.setRightComponent(uploadPanel);
        add(splitPane, BorderLayout.CENTER);
    }
    
    // Loads all books from the Book table
    private void loadBooks() {
        try (Connection conn = DBConnection.getConnection()){
            String query = "SELECT B_ID, B_Title, B_Type, Author_Name FROM Book";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            tableModel.setRowCount(0);
            while(rs.next()){
                Object[] row = {
                    rs.getInt("B_ID"),
                    rs.getString("B_Title"),
                    rs.getString("B_Type"),
                    rs.getString("Author_Name")
                };
                tableModel.addRow(row);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading books: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // When a faculty selects a book and clicks "Request Book"
    private void requestSelectedBook() {
        int selectedRow = bookTable.getSelectedRow();
        if(selectedRow == -1){
            JOptionPane.showMessageDialog(this, "Please select a book to request.");
            return;
        }
        String bTitle = tableModel.getValueAt(selectedRow, 1).toString();
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Borrow_Requests (Login_ID, B_Title, Request_Date, Status) VALUES (?, ?, GETDATE(), ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setString(2, bTitle);
            stmt.setString(3, "Pending");
            int rows = stmt.executeUpdate();
            if(rows > 0){
                JOptionPane.showMessageDialog(this, "Book request sent!");
                loadBooks();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to send request.");
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Opens a file chooser to select a PDF file; checks file size and extension.
    private void choosePdfFile() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showOpenDialog(this);
        if(option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            // Check file extension (should be .pdf)
            if(!file.getName().toLowerCase().endsWith(".pdf")) {
                JOptionPane.showMessageDialog(this, "Please select a PDF file.", "Invalid File", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Check file size (max 2MB)
            if(file.length() > 2 * 1024 * 1024) {
                JOptionPane.showMessageDialog(this, "File size exceeds 2MB.", "File Too Large", JOptionPane.ERROR_MESSAGE);
                return;
            }
            selectedFile = file;
            filePathLabel.setText(file.getAbsolutePath());
        }
    }
    
    // Uploads a new book; if type is PDF, uploads the PDF file as a blob.
    private void uploadBook() {
        try (Connection conn = DBConnection.getConnection()){
            int bookId = Integer.parseInt(bIdField.getText().trim());
            String title = bTitleField.getText().trim();
            String type = bTypeField.getText().trim();
            String author = authorField.getText().trim();
            
            if(title.isEmpty() || type.isEmpty() || author.isEmpty()){
                JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if("pdf".equalsIgnoreCase(type)) {
                if(selectedFile == null) {
                    JOptionPane.showMessageDialog(this, "Please choose a PDF file to upload.", "File Required", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // Read the file into a byte array
                byte[] fileData;
                try (FileInputStream fis = new FileInputStream(selectedFile)) {
                    fileData = fis.readAllBytes();
                }
                String query = "INSERT INTO Book (B_ID, B_Title, B_Type, Author_Name, PDF_File) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setInt(1, bookId);
                stmt.setString(2, title);
                stmt.setString(3, type);
                stmt.setString(4, author);
                stmt.setBytes(5, fileData);
                int rows = stmt.executeUpdate();
                if(rows > 0) {
                    JOptionPane.showMessageDialog(this, "PDF book uploaded successfully!");
                    loadBooks();
                    clearUploadFields();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to upload book.");
                }
            } else {
                // For non-PDF books, insert without file data.
                String query = "INSERT INTO Book (B_ID, B_Title, B_Type, Author_Name) VALUES (?, ?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setInt(1, bookId);
                stmt.setString(2, title);
                stmt.setString(3, type);
                stmt.setString(4, author);
                int rows = stmt.executeUpdate();
                if(rows > 0){
                    JOptionPane.showMessageDialog(this, "Book uploaded successfully!");
                    loadBooks();
                    clearUploadFields();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to upload book.");
                }
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch(NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Please enter a valid numeric Book ID.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch(IOException ioe) {
            ioe.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error reading file: " + ioe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Clears the upload form fields after successful upload.
    private void clearUploadFields() {
        bIdField.setText("");
        bTitleField.setText("");
        bTypeField.setText("");
        authorField.setText("");
        selectedFile = null;
        filePathLabel.setText("No file selected");
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FacultyPage(101).setVisible(true));
    }
}
/*import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class FacultyPage extends JFrame {
    private int userId;
    private JTable bookTable;
    private DefaultTableModel tableModel;
    private JButton requestButton, logoutButton;
    
    // Upload panel components
    private JTextField bIdField, bTitleField, bTypeField, authorField;
    private JButton uploadButton;
    
    public FacultyPage(int userId) {
        super("Faculty Page");
        this.userId = userId;
        initUI();
    }
    
    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        // Title at top
        JLabel titleLabel = new JLabel("Welcome, Faculty " + userId, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);
        
        // Split pane: left for book list (and request) and right for upload form.
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(500);
        
        // Left panel: Book list and Request Book & Logout buttons
        JPanel listPanel = new JPanel(new BorderLayout());
        tableModel = new DefaultTableModel(new Object[]{"B_ID", "B_Title", "B_Type", "Author_Name"}, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        bookTable = new JTable(tableModel);
        loadBooks();
        listPanel.add(new JScrollPane(bookTable), BorderLayout.CENTER);
        
        JPanel listBottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        requestButton = new JButton("Request Book");
        // Enable the request button for faculty
        requestButton.setEnabled(true);
        // Add action listener to request a selected book
        requestButton.addActionListener(e -> requestSelectedBook());
        logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            new LoginPage().setVisible(true);
            dispose();
        });
        listBottom.add(requestButton);
        listBottom.add(logoutButton);
        listPanel.add(listBottom, BorderLayout.SOUTH);
        splitPane.setLeftComponent(listPanel);
        
        // Right panel: Book upload form
        JPanel uploadPanel = new JPanel(new GridBagLayout());
        uploadPanel.setBorder(BorderFactory.createTitledBorder("Upload New Book"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        uploadPanel.add(new JLabel("B_ID:"), gbc);
        gbc.gridx = 1;
        bIdField = new JTextField(10);
        uploadPanel.add(bIdField, gbc);
        
        gbc.gridx = 0; gbc.gridy++;
        uploadPanel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        bTitleField = new JTextField(20);
        uploadPanel.add(bTitleField, gbc);
        
        gbc.gridx = 0; gbc.gridy++;
        uploadPanel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        bTypeField = new JTextField(10);
        uploadPanel.add(bTypeField, gbc);
        
        gbc.gridx = 0; gbc.gridy++;
        uploadPanel.add(new JLabel("Author:"), gbc);
        gbc.gridx = 1;
        authorField = new JTextField(20);
        uploadPanel.add(authorField, gbc);
        
        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        uploadButton = new JButton("Upload Book");
        uploadPanel.add(uploadButton, gbc);
        
        uploadButton.addActionListener(e -> uploadBook());
        
        splitPane.setRightComponent(uploadPanel);
        add(splitPane, BorderLayout.CENTER);
    }
    
    // Loads all books from the Book table
    private void loadBooks() {
        try (Connection conn = DBConnection.getConnection()){
            String query = "SELECT B_ID, B_Title, B_Type, Author_Name FROM Book";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            tableModel.setRowCount(0);
            while(rs.next()){
                Object[] row = {
                    rs.getInt("B_ID"),
                    rs.getString("B_Title"),
                    rs.getString("B_Type"),
                    rs.getString("Author_Name")
                };
                tableModel.addRow(row);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading books: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // When a faculty selects a book and clicks "Request Book"
    private void requestSelectedBook() {
        int selectedRow = bookTable.getSelectedRow();
        if(selectedRow == -1){
            JOptionPane.showMessageDialog(this, "Please select a book to request.");
            return;
        }
        String bTitle = tableModel.getValueAt(selectedRow, 1).toString();
        try (Connection conn = DBConnection.getConnection()){
            // Insert into Borrow_Requests using GETDATE() for current date
            String query = "INSERT INTO Borrow_Requests (Login_ID, B_Title, Request_Date, Status) VALUES (?, ?, GETDATE(), ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setString(2, bTitle);
            stmt.setString(3, "Pending");
            int rows = stmt.executeUpdate();
            if(rows > 0){
                JOptionPane.showMessageDialog(this, "Book request sent!");
                // Optionally, refresh the book list if needed
                loadBooks();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to send request.");
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Uploads a new book and clears the upload form fields upon success
    private void uploadBook() {
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Book (B_ID, B_Title, B_Type, Author_Name) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(bIdField.getText().trim()));
            stmt.setString(2, bTitleField.getText().trim());
            stmt.setString(3, bTypeField.getText().trim());
            stmt.setString(4, authorField.getText().trim());
            int rows = stmt.executeUpdate();
            if(rows > 0){
                JOptionPane.showMessageDialog(this, "Book uploaded successfully!");
                loadBooks();  // refresh book list
                // Clear upload form fields
                bIdField.setText("");
                bTitleField.setText("");
                bTypeField.setText("");
                authorField.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "Failed to upload book.");
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch(NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Please enter a valid numeric Book ID.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FacultyPage(101).setVisible(true));
    }
}

/*import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class FacultyPage extends JFrame {
    private int userId;
    private JTable bookTable;
    private DefaultTableModel tableModel;
    private JButton requestButton, logoutButton;
    // Upload panel components
    private JTextField bIdField, bTitleField, bTypeField, authorField;
    private JButton uploadButton;
    
    public FacultyPage(int userId) {
        super("Faculty Page");
        this.userId = userId;
        initUI();
    }
    
    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Welcome, Faculty " + userId, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);
        
        // Split pane: Left for book list, right for upload form
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(500);
        
        // Left panel: Book list
        JPanel listPanel = new JPanel(new BorderLayout());
        tableModel = new DefaultTableModel(new Object[]{"B_ID", "B_Title", "B_Type", "Author_Name"}, 0);
        JTable bookTable = new JTable(tableModel);
        loadBooks();
        listPanel.add(new JScrollPane(bookTable), BorderLayout.CENTER);
        JPanel listBottom = new JPanel();
        requestButton = new JButton("Request Book");
        logoutButton = new JButton("Logout");
        listBottom.add(requestButton);
        listBottom.add(logoutButton);
        listPanel.add(listBottom, BorderLayout.SOUTH);
        splitPane.setLeftComponent(listPanel);
        
        // Right panel: Book upload form
        JPanel uploadPanel = new JPanel(new GridBagLayout());
        uploadPanel.setBorder(BorderFactory.createTitledBorder("Upload New Book"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        uploadPanel.add(new JLabel("B_ID:"), gbc);
        gbc.gridx = 1;
        bIdField = new JTextField(10);
        uploadPanel.add(bIdField, gbc);
        
        gbc.gridx = 0; gbc.gridy++;
        uploadPanel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        bTitleField = new JTextField(20);
        uploadPanel.add(bTitleField, gbc);
        
        gbc.gridx = 0; gbc.gridy++;
        uploadPanel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        bTypeField = new JTextField(10);
        uploadPanel.add(bTypeField, gbc);
        
        gbc.gridx = 0; gbc.gridy++;
        uploadPanel.add(new JLabel("Author:"), gbc);
        gbc.gridx = 1;
        authorField = new JTextField(20);
        uploadPanel.add(authorField, gbc);
        
        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        uploadButton = new JButton("Upload Book");
        uploadPanel.add(uploadButton, gbc);
        
        splitPane.setRightComponent(uploadPanel);
        add(splitPane, BorderLayout.CENTER);
        
        requestButton.addActionListener(e -> requestSelectedBook());
        logoutButton.addActionListener(e -> {
            new LoginPage().setVisible(true);
            dispose();
        });
        uploadButton.addActionListener(e -> uploadBook());
    }
    
    private void loadBooks() {
        try (Connection conn = DBConnection.getConnection()){
            String query = "SELECT B_ID, B_Title, B_Type, Author_Name FROM Book";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            tableModel.setRowCount(0);
            while(rs.next()){
                Object[] row = {
                    rs.getInt("B_ID"),
                    rs.getString("B_Title"),
                    rs.getString("B_Type"),
                    rs.getString("Author_Name")
                };
                tableModel.addRow(row);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading books: " + ex.getMessage());
        }
    }
    
    private void requestSelectedBook() {
        int selectedRow = bookTable.getSelectedRow();
        if(selectedRow == -1){
            JOptionPane.showMessageDialog(this, "Please select a book to request.");
            return;
        }
        String bTitle = tableModel.getValueAt(selectedRow, 1).toString();
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Borrow_Requests (Login_ID, B_Title, Request_Date, Status) VALUES (?, ?, GETDATE(), ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setString(2, bTitle);
            stmt.setString(3, "Pending");
            int rows = stmt.executeUpdate();
            if(rows > 0){
                JOptionPane.showMessageDialog(this, "Book request sent!");
            } else {
                JOptionPane.showMessageDialog(this, "Failed to send request.");
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
    
    private void uploadBook() {
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Book (B_ID, B_Title, B_Type, Author_Name) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(bIdField.getText().trim()));
            stmt.setString(2, bTitleField.getText().trim());
            stmt.setString(3, bTypeField.getText().trim());
            stmt.setString(4, authorField.getText().trim());
            int rows = stmt.executeUpdate();
            if(rows > 0){
                JOptionPane.showMessageDialog(this, "Book uploaded successfully!");
                loadBooks();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to upload book.");
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}

/*import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class FacultyPage extends JFrame {
    private String userId;
    private JTable bookTable;
    private DefaultTableModel tableModel;
    private JButton requestButton, logoutButton;
    // Components for book upload
    private JTextField bIdField, bTitleField, bTypeField, authorField;
    private JButton uploadButton;
    
    public FacultyPage(String userId) {
        super("Faculty Page");
        this.userId = userId;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        // Left panel: list of books and request functionality
        JPanel listPanel = new JPanel(new BorderLayout());
        tableModel = new DefaultTableModel(new Object[]{"B_ID", "B_Title", "B_Type", "Author_Name"}, 0);
        bookTable = new JTable(tableModel);
        loadBooks();
        listPanel.add(new JScrollPane(bookTable), BorderLayout.CENTER);
        JPanel listBottom = new JPanel();
        requestButton = new JButton("Request Book");
        logoutButton = new JButton("Logout");
        listBottom.add(requestButton);
        listBottom.add(logoutButton);
        listPanel.add(listBottom, BorderLayout.SOUTH);
        add(listPanel, BorderLayout.CENTER);
        
        requestButton.addActionListener(e -> requestSelectedBook());
        logoutButton.addActionListener(e -> {
            new LoginPage().setVisible(true);
            dispose();
        });
        
        // Right panel: book upload form
        JPanel uploadPanel = new JPanel(new GridBagLayout());
        uploadPanel.setBorder(BorderFactory.createTitledBorder("Upload New Book"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Font font = new Font("Georgia", Font.PLAIN, 14);
        
        gbc.gridx = 0; gbc.gridy = 0;
        uploadPanel.add(new JLabel("B_ID:"), gbc);
        gbc.gridx = 1;
        bIdField = new JTextField(10);
        uploadPanel.add(bIdField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        uploadPanel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        bTitleField = new JTextField(20);
        uploadPanel.add(bTitleField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        uploadPanel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        bTypeField = new JTextField(10);
        uploadPanel.add(bTypeField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        uploadPanel.add(new JLabel("Author:"), gbc);
        gbc.gridx = 1;
        authorField = new JTextField(20);
        uploadPanel.add(authorField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        uploadButton = new JButton("Upload Book");
        uploadPanel.add(uploadButton, gbc);
        
        add(uploadPanel, BorderLayout.EAST);
        
        uploadButton.addActionListener(e -> uploadBook());
    }
    
    private void loadBooks() {
        try (Connection conn = DBConnection.getConnection()){
            String query = "SELECT B_ID, B_Title, B_Type, Author_Name FROM Book";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            tableModel.setRowCount(0);
            while(rs.next()){
                Object[] row = {
                    rs.getInt("B_ID"),
                    rs.getString("B_Title"),
                    rs.getString("B_Type"),
                    rs.getString("Author_Name")
                };
                tableModel.addRow(row);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading books: " + ex.getMessage());
        }
    }
    
    private void requestSelectedBook() {
        int selectedRow = bookTable.getSelectedRow();
        if(selectedRow == -1){
            JOptionPane.showMessageDialog(this, "Please select a book to request.");
            return;
        }
        String bTitle = tableModel.getValueAt(selectedRow, 1).toString();
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Borrow_Requests (Login_ID, B_Title, Status) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, userId);
            stmt.setString(2, bTitle);
            stmt.setString(3, "Pending");
            int rows = stmt.executeUpdate();
            if(rows > 0){
                JOptionPane.showMessageDialog(this, "Book request sent!");
            } else {
                JOptionPane.showMessageDialog(this, "Failed to send request.");
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
    
    private void uploadBook() {
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Book (B_ID, B_Title, B_Type, Author_Name) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, Integer.parseInt(bIdField.getText().trim()));
            stmt.setString(2, bTitleField.getText().trim());
            stmt.setString(3, bTypeField.getText().trim());
            stmt.setString(4, authorField.getText().trim());
            int rows = stmt.executeUpdate();
            if(rows > 0){
                JOptionPane.showMessageDialog(this, "Book uploaded successfully!");
                loadBooks(); // refresh book table
            } else {
                JOptionPane.showMessageDialog(this, "Failed to upload book.");
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}
/*import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class FacultyPage extends JFrame {
    private JTextField bookNameField, authorField, typeField;
    private JButton addBookButton;

    public FacultyPage() {
        setTitle("Faculty Dashboard");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new FlowLayout());

        // Add book fields
        bookNameField = new JTextField(20);
        authorField = new JTextField(20);
        typeField = new JTextField(20);

        // Add book button
        addBookButton = new JButton("Add Book");

        add(new JLabel("Book Name:"));
        add(bookNameField);
        add(new JLabel("Author Name:"));
        add(authorField);
        add(new JLabel("Type (PDF/Physical):"));
        add(typeField);
        add(addBookButton);

        addBookButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String bookName = bookNameField.getText();
                String authorName = authorField.getText();
                String type = typeField.getText();
                addBook(bookName, authorName, type);
            }
        });
    }

    private void addBook(String bookName, String authorName, String type) {
        // Insert new book into the Books table (DB query here)
        try (Connection conn = DriverManager.getConnection("jdbc:your_database_url", "username", "password")) {
            String query = "INSERT INTO Books (Book_Name, Author_ID, Type, Availability) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, bookName);
            stmt.setString(2, authorName);
            stmt.setString(3, type);
            stmt.setString(4, "available");
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Book added successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to add book.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FacultyPage().setVisible(true));
    }
}
*/