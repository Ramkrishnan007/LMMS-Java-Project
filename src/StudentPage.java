import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;

public class StudentPage extends JFrame {
    private int userId;
    private JTabbedPane tabbedPane;
    
    // Tab 1: Available Books
    // Added an extra "Status" column for coloring rows.
    private JTable availableBooksTable;
    private DefaultTableModel availableBooksModel;
    private JButton requestBookButton, downloadPdfButton;
    private JTextField searchField;
    private JButton searchButton;
    
    // Tab 2: My Requests (approved and not expired)
    private JTable myRequestsTable;
    private DefaultTableModel myRequestsModel;
    private JButton refreshRequestsButton;
    
    // Logout button for entire page
    private JButton logoutButton;
    
    public StudentPage(int userId) {
        super("Student Page");
        this.userId = userId;
        initUI();
    }
    
    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        // Title Label at top
        JLabel titleLabel = new JLabel("Welcome, Student " + userId, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 20));
        add(titleLabel, BorderLayout.NORTH);
        
        // Create a tabbed pane for available books and my requests
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Available Books", createAvailableBooksPanel());
        tabbedPane.addTab("My Requests", createMyRequestsPanel());
        add(tabbedPane, BorderLayout.CENTER);
        
        // Logout button at bottom
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            new LoginPage().setVisible(true);
            dispose();
        });
        bottomPanel.add(logoutButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    // Panel showing available books with search bar and download PDF support.
    private JPanel createAvailableBooksPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Top panel for search bar
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(20);
        searchButton = new JButton("Search");
        searchButton.addActionListener(e -> loadAvailableBooks());
        searchPanel.add(new JLabel("Search (Title or Author):"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        panel.add(searchPanel, BorderLayout.NORTH);
        
        // Table with 5 columns: B_ID, B_Title, B_Type, Author_Name, Status
        availableBooksModel = new DefaultTableModel(new Object[]{"B_ID", "B_Title", "B_Type", "Author_Name", "Status"}, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        availableBooksTable = new JTable(availableBooksModel);
        // Set custom cell renderer for row coloring based on Status.
        availableBooksTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = (String) table.getModel().getValueAt(row, 4);
                if (!isSelected) {
                    if ("Available".equalsIgnoreCase(status)) {
                        c.setBackground(Color.green);
                    } else if ("Requested".equalsIgnoreCase(status)) {
                        c.setBackground(Color.yellow);
                    } else if ("Held".equalsIgnoreCase(status)) {
                        c.setBackground(Color.red);
                    } else {
                        c.setBackground(Color.white);
                    }
                } else {
                    c.setBackground(table.getSelectionBackground());
                }
                return c;
            }
        });
        loadAvailableBooks();
        panel.add(new JScrollPane(availableBooksTable), BorderLayout.CENTER);
        
        // Bottom panel for Request and Download buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        requestBookButton = new JButton("Request Book");
        requestBookButton.addActionListener(e -> requestSelectedBook());
        downloadPdfButton = new JButton("Download PDF");
        downloadPdfButton.addActionListener(e -> downloadSelectedPdf());
        btnPanel.add(requestBookButton);
        btnPanel.add(downloadPdfButton);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    // Panel showing student's current approved requests (within 2 days)
    private JPanel createMyRequestsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        myRequestsModel = new DefaultTableModel(new Object[]{"Request_ID", "B_Title", "Request_Date", "Status"}, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        myRequestsTable = new JTable(myRequestsModel);
        loadMyRequests();
        panel.add(new JScrollPane(myRequestsTable), BorderLayout.CENTER);
        
        refreshRequestsButton = new JButton("Refresh");
        refreshRequestsButton.addActionListener(e -> loadMyRequests());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.add(refreshRequestsButton);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    // Load available books with search and status.
    private void loadAvailableBooks() {
        try (Connection conn = DBConnection.getConnection()){
            String searchTerm = "%" + searchField.getText().trim() + "%";
            // The query uses subqueries to check status:
            // If any approved request (within 2 days) exists by another student, status = 'Held'
            // Else if a pending request exists by THIS student, status = 'Requested'
            // Otherwise, status = 'Available'
            String query = "SELECT B.B_ID, B.B_Title, B.B_Type, B.Author_Name, " +
                    "CASE " +
                    "  WHEN EXISTS (SELECT 1 FROM Borrow_Requests BR WHERE BR.B_ID = B.B_ID " +
                    "         AND BR.Status = 'Approved' AND DATEDIFF(day, BR.Request_Date, GETDATE()) < 2 " +
                    "         AND BR.Login_ID <> ?) THEN 'Held' " +
                    "  WHEN EXISTS (SELECT 1 FROM Borrow_Requests BR WHERE BR.B_ID = B.B_ID " +
                    "         AND BR.Status = 'Pending' AND BR.Login_ID = ?) THEN 'Requested' " +
                    "  ELSE 'Available' " +
                    "END as BookStatus " +
                    "FROM Book B " +
                    "WHERE (B.B_Title LIKE ? OR B.Author_Name LIKE ?)";


            
            /*String query = "SELECT B.B_ID, B.B_Title, B.B_Type, B.Author_Name, " +
                    "CASE " +
                    "  WHEN EXISTS (SELECT 1 FROM Borrow_Requests BR WHERE BR.B_Title = B.B_Title " +
                    "         AND BR.Status = 'Approved' AND DATEDIFF(day, BR.Request_Date, GETDATE()) < 2 " +
                    "         AND BR.Login_ID <> ?) THEN 'Held' " +
                    "  WHEN EXISTS (SELECT 1 FROM Borrow_Requests BR WHERE BR.B_Title = B.B_Title " +
                    "         AND BR.Status = 'Pending' AND BR.Login_ID = ?) THEN 'Requested' " +
                    "  ELSE 'Available' " +
                    "END as BookStatus " +
                    "FROM Book B " +
                    "WHERE (B.B_Title LIKE ? OR B.Author_Name LIKE ?)";*/
            
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setString(3, searchTerm);
            stmt.setString(4, searchTerm);
            ResultSet rs = stmt.executeQuery();
            availableBooksModel.setRowCount(0);
            while(rs.next()){
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("B_ID"));
                row.add(rs.getString("B_Title"));
                row.add(rs.getString("B_Type"));
                row.add(rs.getString("Author_Name"));
                row.add(rs.getString("BookStatus"));
                availableBooksModel.addRow(row);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading available books: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Load student's approved requests (within 2 days)
    private void loadMyRequests() {
        try (Connection conn = DBConnection.getConnection()){
            String query = "SELECT Request_ID, B_Title, Request_Date, Status " +
                           "FROM Borrow_Requests " +
                           "WHERE Login_ID = ? AND Status = 'Approved' AND DATEDIFF(day, Request_Date, GETDATE()) < 2";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            myRequestsModel.setRowCount(0);
            while(rs.next()){
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("Request_ID"));
                row.add(rs.getString("B_Title"));
                row.add(rs.getDate("Request_Date"));
                row.add(rs.getString("Status"));
                myRequestsModel.addRow(row);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading your requests: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // When student selects a book and clicks "Request Book" (only for non-PDF books)
    private void requestSelectedBook() {
        int selectedRow = availableBooksTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book to request.");
            return;
        }
        // Get book type and status from the table
        String bType = availableBooksModel.getValueAt(selectedRow, 2).toString();
        String status = availableBooksModel.getValueAt(selectedRow, 4).toString();
        // For PDF books, do not send a request; they are handled via download.
        if ("PDF".equalsIgnoreCase(bType)) {
            JOptionPane.showMessageDialog(this, "PDF books cannot be requested. Please use the Download button.");
            return;
        }
        // Enforce request limit only for non-PDF books
        try (Connection conn = DBConnection.getConnection()) {
            String countQuery = "SELECT COUNT(*) AS total FROM Borrow_Requests " +
                                "WHERE Login_ID = ? AND " +
                                "((Status = 'Approved' AND DATEDIFF(day, Request_Date, GETDATE()) < 2) " +
                                "OR Status = 'Pending')";
            PreparedStatement countStmt = conn.prepareStatement(countQuery);
            countStmt.setInt(1, userId);
            ResultSet rs = countStmt.executeQuery();
            if (rs.next()) {
                int currentCount = rs.getInt("total");
                if (currentCount >= 3) {
                    JOptionPane.showMessageDialog(
                        this, 
                        "You have reached the maximum limit of 3 books (approved or pending).",
                        "Limit Exceeded",
                        JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
            }
            
            // Proceed with the request
            String bTitle = availableBooksModel.getValueAt(selectedRow, 1).toString();
            String insertQuery = "INSERT INTO Borrow_Requests (Login_ID, B_Title, Request_Date, Status) " +
                                 "VALUES (?, ?, GETDATE(), 'Pending')";
            PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setInt(1, userId);
            insertStmt.setString(2, bTitle);
            
            int affectedRows = insertStmt.executeUpdate();
            if (affectedRows > 0) {
                JOptionPane.showMessageDialog(this, "Book request submitted successfully!");
                loadAvailableBooks();
                loadMyRequests();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                this, 
                "Error processing request: " + ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
    
    // When student selects a PDF book and clicks "Download PDF"
    private void downloadSelectedPdf() {
        int selectedRow = availableBooksTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a PDF book to download.");
            return;
        }
        String bType = availableBooksModel.getValueAt(selectedRow, 2).toString();
        if (!"PDF".equalsIgnoreCase(bType)) {
            JOptionPane.showMessageDialog(this, "Selected book is not a PDF.");
            return;
        }
        int bId = (int) availableBooksModel.getValueAt(selectedRow, 0);
        try (Connection conn = DBConnection.getConnection()){
            String query = "SELECT PDF_File FROM Book WHERE B_ID = ? AND B_Type = 'PDF'";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, bId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                byte[] pdfData = rs.getBytes("PDF_File");
                if (pdfData == null) {
                    JOptionPane.showMessageDialog(this, "PDF file not available for download.");
                    return;
                }
                // Use a file chooser to let student select save location
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setSelectedFile(new java.io.File(availableBooksModel.getValueAt(selectedRow, 1) + ".pdf"));
                int option = fileChooser.showSaveDialog(this);
                if (option == JFileChooser.APPROVE_OPTION) {
                    java.io.File file = fileChooser.getSelectedFile();
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                        fos.write(pdfData);
                    }
                    JOptionPane.showMessageDialog(this, "PDF downloaded successfully!");
                }
            } else {
                JOptionPane.showMessageDialog(this, "PDF file not found.");
            }
        } catch(SQLException | java.io.IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error downloading PDF: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StudentPage(1).setVisible(true));
    }
}
/*import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;

public class StudentPage extends JFrame {
    private int userId;
    private JTabbedPane tabbedPane;
    
    // Tab 1: Available Books
    private JTable availableBooksTable;
    private DefaultTableModel availableBooksModel;
    private JButton requestBookButton;
    
    // Tab 2: My Requests (approved and not expired)
    private JTable myRequestsTable;
    private DefaultTableModel myRequestsModel;
    private JButton refreshRequestsButton;
    
    // Logout button for entire page
    private JButton logoutButton;
    
    public StudentPage(int userId) {
        super("Student Page");
        this.userId = userId;
        initUI();
    }
    
    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        // Title Label at top
        JLabel titleLabel = new JLabel("Welcome, Student " + userId, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 20));
        add(titleLabel, BorderLayout.NORTH);
        
        // Create a tabbed pane for available books and my requests
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Available Books", createAvailableBooksPanel());
        tabbedPane.addTab("My Requests", createMyRequestsPanel());
        add(tabbedPane, BorderLayout.CENTER);
        
        // Logout button at bottom
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            new LoginPage().setVisible(true);
            dispose();
        });
        bottomPanel.add(logoutButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    // Panel showing available books (only books without an approved request in last 2 days)
    private JPanel createAvailableBooksPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        availableBooksModel = new DefaultTableModel(new Object[]{"B_ID", "B_Title", "B_Type", "Author_Name"}, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        availableBooksTable = new JTable(availableBooksModel);
        loadAvailableBooks();
        panel.add(new JScrollPane(availableBooksTable), BorderLayout.CENTER);
        
        // Request Book Button
        requestBookButton = new JButton("Request Book");
        requestBookButton.addActionListener(e -> requestSelectedBook());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.add(requestBookButton);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    // Panel showing student's current approved requests (within 2 days)
    private JPanel createMyRequestsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        myRequestsModel = new DefaultTableModel(new Object[]{"Request_ID", "B_Title", "Request_Date", "Status"}, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        myRequestsTable = new JTable(myRequestsModel);
        loadMyRequests();
        panel.add(new JScrollPane(myRequestsTable), BorderLayout.CENTER);
        
        refreshRequestsButton = new JButton("Refresh");
        refreshRequestsButton.addActionListener(e -> loadMyRequests());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.add(refreshRequestsButton);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    // Load available books
    private void loadAvailableBooks() {
        try (Connection conn = DBConnection.getConnection()){
            // Query: select books that do not have an approved borrow request within the last 2 days.
            // Note: This assumes that Borrow_Requests uses B_Title to link to Book.
            String query = "SELECT B.B_ID, B.B_Title, B.B_Type, B.Author_Name " +
                           "FROM Book B " +
                           "LEFT JOIN Borrow_Requests BR ON B.B_Title = BR.B_Title AND BR.Status = 'Approved' " +
                           "   AND DATEDIFF(day, BR.Request_Date, GETDATE()) < 2 " +
                           "WHERE BR.Request_ID IS NULL";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            availableBooksModel.setRowCount(0);
            while(rs.next()){
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("B_ID"));
                row.add(rs.getString("B_Title"));
                row.add(rs.getString("B_Type"));
                row.add(rs.getString("Author_Name"));
                availableBooksModel.addRow(row);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading available books: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Load student's approved requests (within 2 days)
    private void loadMyRequests() {
        try (Connection conn = DBConnection.getConnection()){
            // Query: select approved requests for this student that are within 2 days
            String query = "SELECT Request_ID, B_Title, Request_Date, Status " +
                           "FROM Borrow_Requests " +
                           "WHERE Login_ID = ? AND Status = 'Approved' AND DATEDIFF(day, Request_Date, GETDATE()) < 2";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            myRequestsModel.setRowCount(0);
            while(rs.next()){
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("Request_ID"));
                row.add(rs.getString("B_Title"));
                row.add(rs.getDate("Request_Date"));
                row.add(rs.getString("Status"));
                myRequestsModel.addRow(row);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading your requests: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
 // When student selects a book and clicks "Request Book"
    private void requestSelectedBook() {
        int selectedRow = availableBooksTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book to request.");
            return;
        }

        // Check if the student has reached the request limit
        try (Connection conn = DBConnection.getConnection()) {
            // Check current active approved + pending requests
            String countQuery = "SELECT COUNT(*) AS total FROM Borrow_Requests " +
                                "WHERE Login_ID = ? AND " +
                                "((Status = 'Approved' AND DATEDIFF(day, Request_Date, GETDATE()) < 2) " +
                                "OR Status = 'Pending')";
            PreparedStatement countStmt = conn.prepareStatement(countQuery);
            countStmt.setInt(1, userId);
            ResultSet rs = countStmt.executeQuery();

            if (rs.next()) {
                int currentCount = rs.getInt("total");
                if (currentCount >= 3) {
                    JOptionPane.showMessageDialog(
                        this, 
                        "You have reached the maximum limit of 3 books (approved or pending).",
                        "Limit Exceeded",
                        JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
            }

            // Proceed with the request
            String bTitle = availableBooksModel.getValueAt(selectedRow, 1).toString();
            String insertQuery = "INSERT INTO Borrow_Requests (Login_ID, B_Title, Request_Date, Status) " +
                                 "VALUES (?, ?, GETDATE(), 'Pending')";
            PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setInt(1, userId);
            insertStmt.setString(2, bTitle);
            
            int affectedRows = insertStmt.executeUpdate();
            if (affectedRows > 0) {
                JOptionPane.showMessageDialog(this, "Book request submitted successfully!");
                loadAvailableBooks(); // Refresh available books list
                loadMyRequests();     // Refresh my requests list
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                this, 
                "Error processing request: " + ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
    
    
    // Main method
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StudentPage(1).setVisible(true));
    }
}
/*import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class StudentPage extends JFrame {
    private int userId;
    private JTable bookTable;
    private DefaultTableModel tableModel;
    private JButton requestButton, logoutButton;
    
    public StudentPage(int userId) {
        super("Student Page");
        this.userId = userId;
        initUI();
    }
    
    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Welcome, Student " + userId, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);
        
        tableModel = new DefaultTableModel(new Object[]{"B_ID", "B_Title", "B_Type", "Author_Name"}, 0);
        bookTable = new JTable(tableModel);
        loadBooks();
        add(new JScrollPane(bookTable), BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel();
        requestButton = new JButton("Request Book");
        logoutButton = new JButton("Logout");
        bottomPanel.add(requestButton);
        bottomPanel.add(logoutButton);
        add(bottomPanel, BorderLayout.SOUTH);
        
        requestButton.addActionListener(e -> requestSelectedBook());
        logoutButton.addActionListener(e -> {
            new LoginPage().setVisible(true);
            dispose();
        });
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
            // Using GETDATE() for SQL Server current date
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
}
/*import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class StudentPage extends JFrame {
    private int userId;
    private JTable bookTable;
    private DefaultTableModel tableModel;
    private JButton requestButton, logoutButton;

    public StudentPage(int userId) {
        super("Student Page");
        this.userId = userId;
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Title panel
        JLabel titleLabel = new JLabel("Welcome, Student " + userId, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);

        // Table to display books
        tableModel = new DefaultTableModel(new Object[]{"B_ID", "B_Title", "B_Type", "Author_Name"}, 0);
        bookTable = new JTable(tableModel);
        loadBooks();
        add(new JScrollPane(bookTable), BorderLayout.CENTER);

        // Bottom panel for buttons
        JPanel bottomPanel = new JPanel();
        requestButton = new JButton("Request Book");
        logoutButton = new JButton("Logout");
        bottomPanel.add(requestButton);
        bottomPanel.add(logoutButton);
        add(bottomPanel, BorderLayout.SOUTH);

        requestButton.addActionListener(e -> requestSelectedBook());
        logoutButton.addActionListener(e -> {
            new LoginPage().setVisible(true);
            dispose();
        });
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
            String query = "INSERT INTO Borrow_Requests (Login_ID, B_Title, Request_Date, Status) VALUES (?, ?, CURDATE(), ?)";
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
}
/*import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class StudentPage extends JFrame {
    private String userId;
    private JTable bookTable;
    private DefaultTableModel tableModel;
    private JButton requestButton, logoutButton;
    
    public StudentPage(String userId) {
        super("Student Page");
        this.userId = userId;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        tableModel = new DefaultTableModel(new Object[]{"B_ID", "B_Title", "B_Type", "Author_Name"}, 0);
        bookTable = new JTable(tableModel);
        loadBooks();
        JScrollPane scrollPane = new JScrollPane(bookTable);
        add(scrollPane, BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel();
        requestButton = new JButton("Request Book");
        logoutButton = new JButton("Logout");
        bottomPanel.add(requestButton);
        bottomPanel.add(logoutButton);
        add(bottomPanel, BorderLayout.SOUTH);
        
        requestButton.addActionListener(e -> requestSelectedBook());
        logoutButton.addActionListener(e -> {
            new LoginPage().setVisible(true);
            dispose();
        });
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
}

/*import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.table.DefaultTableModel;

public class StudentPage extends JFrame {
    private JTable bookTable;
    private JButton requestButton;
    private String userId;

    // Constructor now accepts the userId
    public StudentPage(String userId) {
        this.userId = userId; // Store the userId passed from LoginPage

        setTitle("Student Dashboard");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Set up table to show books
        String[] columns = {"Book ID", "Book Name", "Author", "Availability"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        bookTable = new JTable(model);

        // Populate table with books
        loadBooks(model);

        JScrollPane scrollPane = new JScrollPane(bookTable);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        requestButton = new JButton("Request Book");
        buttonPanel.add(requestButton);
        add(buttonPanel, BorderLayout.SOUTH);

        requestButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = bookTable.getSelectedRow();
                if (selectedRow != -1) {
                    String bookId = (String) bookTable.getValueAt(selectedRow, 0);
                    requestBook(bookId);
                }
            }
        });
    }

    private void loadBooks(DefaultTableModel model) {
        // Load books from DB (select query here)
        try (Connection conn = DBConnection.getConnection()) {  // Use DBConnection class for the connection
            if (conn == null) {
                JOptionPane.showMessageDialog(this, "Failed to connect to the database.");
                return;
            }

            String query = "SELECT * FROM Books WHERE Availability = 1";  // Assuming 1 means available
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String bookId = rs.getString("Book_ID");
                String bookName = rs.getString("Book_Name");
                String author = rs.getString("Author_ID");
                String availability = rs.getString("Availability");
                model.addRow(new Object[]{bookId, bookName, author, availability});
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading books.");
        }
    }

    private void requestBook(String bookId) {
        // Request book (DB insert query here)
        try (Connection conn = DBConnection.getConnection()) {  // Use DBConnection for database connection
            if (conn == null) {
                JOptionPane.showMessageDialog(this, "Failed to connect to the database.");
                return;
            }

            String query = "INSERT INTO BookRequests (User_ID, Book_ID, Role, Status) VALUES (?, ?, 'Student', 'Pending')";
            PreparedStatement stmt = conn.prepareStatement(query);

            stmt.setString(1, userId);  // Use the userId passed from the LoginPage
            stmt.setString(2, bookId);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Request sent successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to request book.");
        }
    }

    public static void main(String[] args) {
        // The main method will not be necessary to handle the userId anymore
        // as it is passed dynamically from the LoginPage constructor.
    }
}
*/