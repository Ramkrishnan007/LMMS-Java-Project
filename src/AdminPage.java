import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.sql.*;
import java.util.Vector;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

public class AdminPage extends JFrame {
    private int userId;
    private JTabbedPane tabbedPane;
    
    // Tab 1: Book Requests
    private JTable requestsTable;
    private DefaultTableModel requestsModel;
    private JButton approveButton, denyButton, refreshRequestsButton;
    private JComboBox<String> requestFilterCombo;
    
    // Tab 2: Book Holds
    private JTable holdsTable;
    private DefaultTableModel holdsModel;
    private JButton returnButton, refreshHoldsButton;
    
    // Tab 3: User Management
    private JTable usersTable;
    private DefaultTableModel usersModel;
    private JButton updateRoleButton, refreshUsersButton;
    private JComboBox<String> roleUpdateCombo;
    
    // Tab 4: Dashboard
    private JPanel dashboardPanel;
    private ChartPanel barChartPanel;
    private ChartPanel pieChartPanel;
    private JButton refreshDashboardButton;
    
    // Logout button for all tabs
    private JButton logoutButton;
    
    public AdminPage(int userId) {
        super("Admin Page");
        this.userId = userId;
        initUI();
    }
    
    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        // Title label
        JLabel titleLabel = new JLabel("Admin Page - Manage Requests, Holds, Users & Dashboard", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 22));
        add(titleLabel, BorderLayout.NORTH);
        
        // Create tabbed pane and add tabs
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Book Requests", createRequestsPanel());
        tabbedPane.addTab("Book Holds", createHoldsPanel());
        tabbedPane.addTab("User Management", createUsersPanel());
        tabbedPane.addTab("Dashboard", createDashboardPanel());
        add(tabbedPane, BorderLayout.CENTER);
        
        // Logout button at bottom
        logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            new LoginPage().setVisible(true);
            dispose();
        });
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(logoutButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    // ------------------ Tab 1: Book Requests Panel ------------------
    private JPanel createRequestsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] filterOptions = {"All", "Pending", "Approved", "Denied", "Returned"};
        requestFilterCombo = new JComboBox<>(filterOptions);
        requestFilterCombo.addActionListener(e -> loadRequests());
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Filter by Status:"));
        filterPanel.add(requestFilterCombo);
        panel.add(filterPanel, BorderLayout.NORTH);
        
        requestsModel = new DefaultTableModel(new Object[]{"Request_ID", "Login_ID", "B_Title", "Request_Date", "Status"}, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        requestsTable = new JTable(requestsModel);
        loadRequests();
        panel.add(new JScrollPane(requestsTable), BorderLayout.CENTER);
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        approveButton = new JButton("Approve");
        denyButton = new JButton("Deny");
        refreshRequestsButton = new JButton("Refresh");
        btnPanel.add(approveButton);
        btnPanel.add(denyButton);
        btnPanel.add(refreshRequestsButton);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        approveButton.addActionListener(e -> updateRequestStatus("Approved"));
        denyButton.addActionListener(e -> updateRequestStatus("Denied"));
        refreshRequestsButton.addActionListener(e -> loadRequests());
        
        return panel;
    }
    
    private void loadRequests() {
        try (Connection conn = DBConnection.getConnection()){
            String filter = (String) requestFilterCombo.getSelectedItem();
            String query = "SELECT Request_ID, Login_ID, B_Title, Request_Date, Status FROM Borrow_Requests";
            if (!"All".equalsIgnoreCase(filter)) {
                query += " WHERE Status = ?";
            }
            PreparedStatement stmt = conn.prepareStatement(query);
            if (!"All".equalsIgnoreCase(filter)) {
                stmt.setString(1, filter);
            }
            ResultSet rs = stmt.executeQuery();
            requestsModel.setRowCount(0);
            while(rs.next()){
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("Request_ID"));
                row.add(rs.getInt("Login_ID"));
                row.add(rs.getString("B_Title"));
                row.add(rs.getDate("Request_Date"));
                row.add(rs.getString("Status"));
                requestsModel.addRow(row);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading requests: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateRequestStatus(String newStatus) {
        int selectedRow = requestsTable.getSelectedRow();
        if(selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a request to update.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String currentStatus = (String) requestsModel.getValueAt(selectedRow, 4);
        if("Approved".equalsIgnoreCase(currentStatus) ||
           "Denied".equalsIgnoreCase(currentStatus) ||
           "Returned".equalsIgnoreCase(currentStatus)) {
            JOptionPane.showMessageDialog(this, "This request is already finalized and cannot be changed.", "Update Not Allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int requestId = (int) requestsModel.getValueAt(selectedRow, 0);
        try (Connection conn = DBConnection.getConnection()){
            String query = "UPDATE Borrow_Requests SET Status = ? WHERE Request_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, newStatus);
            stmt.setInt(2, requestId);
            int rows = stmt.executeUpdate();
            if(rows > 0) {
                JOptionPane.showMessageDialog(this, "Request updated to: " + newStatus);
                loadRequests();
                loadHolds();
                updateDashboard();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update request.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating request: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // ------------------ Tab 2: Book Holds Panel ------------------
    private JPanel createHoldsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        holdsModel = new DefaultTableModel(new Object[]{"Request_ID", "Login_ID", "B_Title", "Request_Date", "Status"}, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        holdsTable = new JTable(holdsModel);
        loadHolds();
        panel.add(new JScrollPane(holdsTable), BorderLayout.CENTER);
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        returnButton = new JButton("Mark as Returned");
        refreshHoldsButton = new JButton("Refresh");
        btnPanel.add(returnButton);
        btnPanel.add(refreshHoldsButton);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        returnButton.addActionListener(e -> markHoldAsReturned());
        refreshHoldsButton.addActionListener(e -> loadHolds());
        
        return panel;
    }
    
    private void loadHolds() {
        try (Connection conn = DBConnection.getConnection()){
            String query = "SELECT Request_ID, Login_ID, B_Title, Request_Date, Status " +
                           "FROM Borrow_Requests WHERE Status = 'Approved'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            holdsModel.setRowCount(0);
            while(rs.next()){
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("Request_ID"));
                row.add(rs.getInt("Login_ID"));
                row.add(rs.getString("B_Title"));
                row.add(rs.getDate("Request_Date"));
                row.add(rs.getString("Status"));
                holdsModel.addRow(row);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading holds: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void markHoldAsReturned() {
        int selectedRow = holdsTable.getSelectedRow();
        if(selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a hold to mark as returned.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int requestId = (int) holdsModel.getValueAt(selectedRow, 0);
        try (Connection conn = DBConnection.getConnection()){
            String query = "UPDATE Borrow_Requests SET Status = 'Returned' WHERE Request_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, requestId);
            int rows = stmt.executeUpdate();
            if(rows > 0) {
                JOptionPane.showMessageDialog(this, "Hold marked as returned.");
                loadHolds();
                updateDashboard();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update hold.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error marking hold as returned: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // ------------------ Tab 3: User Management Panel ------------------
    private JPanel createUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        usersModel = new DefaultTableModel(new Object[]{"Login_ID", "U_Password", "U_Role"}, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        usersTable = new JTable(usersModel);
        loadUsers();
        panel.add(new JScrollPane(usersTable), BorderLayout.CENTER);
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        roleUpdateCombo = new JComboBox<>(new String[]{"Student", "Faculty", "Admin"});
        updateRoleButton = new JButton("Update Role");
        refreshUsersButton = new JButton("Refresh");
        btnPanel.add(new JLabel("New Role:"));
        btnPanel.add(roleUpdateCombo);
        btnPanel.add(updateRoleButton);
        btnPanel.add(refreshUsersButton);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        updateRoleButton.addActionListener(e -> updateUserRole());
        refreshUsersButton.addActionListener(e -> loadUsers());
        
        return panel;
    }
    
    private void loadUsers() {
        try (Connection conn = DBConnection.getConnection()){
            String query = "SELECT Login_ID, U_Password, U_Role FROM Login_Table";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            usersModel.setRowCount(0);
            while(rs.next()){
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("Login_ID"));
                row.add(rs.getString("U_Password"));
                row.add(rs.getString("U_Role"));
                usersModel.addRow(row);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading users: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateUserRole() {
        int selectedRow = usersTable.getSelectedRow();
        if(selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to update.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int loginId = (int) usersModel.getValueAt(selectedRow, 0);
        String newRole = (String) roleUpdateCombo.getSelectedItem();
        try (Connection conn = DBConnection.getConnection()){
            String query = "UPDATE Login_Table SET U_Role = ? WHERE Login_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, newRole);
            stmt.setInt(2, loginId);
            int rows = stmt.executeUpdate();
            if(rows > 0) {
                JOptionPane.showMessageDialog(this, "User role updated successfully.");
                loadUsers();
                updateDashboard();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update user role.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating user role: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // ------------------ Tab 4: Dashboard Panel ------------------
    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel summaryPanel = new JPanel(new GridLayout(2, 3, 10, 10));
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Summary"));
        
        // Labels to display summary data
        JLabel studentCountLabel = new JLabel("Students: ");
        JLabel facultyCountLabel = new JLabel("Faculty: ");
        JLabel adminCountLabel = new JLabel("Admins: ");
        JLabel bookCountLabel = new JLabel("Books: ");
        JLabel pendingCountLabel = new JLabel("Pending Requests: ");
        JLabel approvedCountLabel = new JLabel("Approved Requests: ");
        
        // Get counts from DB
        int students = getCount("Student");
        int faculty = getCount("Faculty");
        int admins = getCount("Admin_Table");
        int books = getCount("Book");
        int pending = getRequestCount("Pending");
        int approved = getRequestCount("Approved");
        
        // Set text with counts
        studentCountLabel.setText("Students: " + students);
        facultyCountLabel.setText("Faculty: " + faculty);
        adminCountLabel.setText("Admins: " + admins);
        bookCountLabel.setText("Books: " + books);
        pendingCountLabel.setText("Pending Requests: " + pending);
        approvedCountLabel.setText("Approved Requests: " + approved);
        
        summaryPanel.add(studentCountLabel);
        summaryPanel.add(facultyCountLabel);
        summaryPanel.add(adminCountLabel);
        summaryPanel.add(bookCountLabel);
        summaryPanel.add(pendingCountLabel);
        summaryPanel.add(approvedCountLabel);
        
        // Create datasets for charts
        DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
        updateBarDataset(barDataset);
        JFreeChart barChart = ChartFactory.createBarChart("Dashboard - Counts", "Category", "Count", barDataset);
        barChartPanel = new ChartPanel(barChart);
        
        DefaultPieDataset pieDataset = new DefaultPieDataset();
        updatePieDataset(pieDataset);
        JFreeChart pieChart = ChartFactory.createPieChart("Borrow Requests Distribution", pieDataset, true, true, false);
        pieChartPanel = new ChartPanel(pieChart);
        
        // Combine charts in a split pane
        JSplitPane chartsSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, barChartPanel, pieChartPanel);
        chartsSplit.setDividerLocation(500);
        
        // Build the dashboard panel
        panel.add(summaryPanel, BorderLayout.NORTH);
        panel.add(chartsSplit, BorderLayout.CENTER);
        
        refreshDashboardButton = new JButton("Refresh Dashboard");
        refreshDashboardButton.addActionListener(e -> updateDashboard());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.add(refreshDashboardButton);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    // Helper to get count from a table
    private int getCount(String tableName) {
        int count = 0;
        try (Connection conn = DBConnection.getConnection()){
            String query = "SELECT COUNT(*) AS cnt FROM " + tableName;
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            if(rs.next()){
                count = rs.getInt("cnt");
            }
        } catch(SQLException ex){
            ex.printStackTrace();
        }
        return count;
    }
    
    // Helper to get request count by status
    private int getRequestCount(String status) {
        int count = 0;
        try (Connection conn = DBConnection.getConnection()){
            String query = "SELECT COUNT(*) AS cnt FROM Borrow_Requests WHERE Status = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, status);
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                count = rs.getInt("cnt");
            }
        } catch(SQLException ex){
            ex.printStackTrace();
        }
        return count;
    }
    
    // Update the bar chart dataset with counts for Students, Faculty, Admins, Books, and Borrow Requests by status
    private void updateBarDataset(DefaultCategoryDataset dataset) {
        try (Connection conn = DBConnection.getConnection()){
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM Student");
            if(rs.next()){
                dataset.addValue(rs.getInt("cnt"), "Count", "Students");
            }
            rs.close();
            
            rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM Faculty");
            if(rs.next()){
                dataset.addValue(rs.getInt("cnt"), "Count", "Faculty");
            }
            rs.close();
            
            rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM Admin_Table");
            if(rs.next()){
                dataset.addValue(rs.getInt("cnt"), "Count", "Admins");
            }
            rs.close();
            
            rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM Book");
            if(rs.next()){
                dataset.addValue(rs.getInt("cnt"), "Count", "Books");
            }
            rs.close();
            
            rs = stmt.executeQuery("SELECT Status, COUNT(*) AS cnt FROM Borrow_Requests GROUP BY Status");
            while(rs.next()){
                String status = rs.getString("Status");
                dataset.addValue(rs.getInt("cnt"), "Count", "Requests (" + status + ")");
            }
            rs.close();
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating bar dataset: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Update the pie chart dataset with borrow requests distribution by status
    private void updatePieDataset(DefaultPieDataset dataset) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "SELECT Status, COUNT(*) AS cnt FROM Borrow_Requests GROUP BY Status";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while(rs.next()){
                dataset.setValue(rs.getString("Status"), rs.getInt("cnt"));
            }
            rs.close();
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating pie dataset: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateDashboard() {
        // Update summary panel counts by rebuilding the Dashboard tab
        tabbedPane.setComponentAt(3, createDashboardPanel());
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminPage(201).setVisible(true));
    }
}
/*import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Vector;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

public class AdminPage extends JFrame {
    private int userId;
    private JTabbedPane tabbedPane;
    
    // Tab 1: Book Requests
    private JTable requestsTable;
    private DefaultTableModel requestsModel;
    private JButton approveButton, denyButton, refreshRequestsButton;
    private JComboBox<String> requestFilterCombo;
    
    // Tab 2: Book Holds
    private JTable holdsTable;
    private DefaultTableModel holdsModel;
    private JButton returnButton, refreshHoldsButton;
    
    // Tab 3: User Management
    private JTable usersTable;
    private DefaultTableModel usersModel;
    private JButton updateRoleButton, refreshUsersButton;
    private JComboBox<String> roleUpdateCombo;
    
    // Tab 4: Dashboard
    private ChartPanel barChartPanel;
    private ChartPanel pieChartPanel;
    private JButton refreshDashboardButton;
    
    // Logout button for entire page
    private JButton logoutButton;
    
    public AdminPage(int userId) {
        super("Admin Page");
        this.userId = userId;
        initUI();
    }
    
    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        // Title label
        JLabel titleLabel = new JLabel("Admin Page - Manage Requests, Holds, Users & Dashboard", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 22));
        add(titleLabel, BorderLayout.NORTH);
        
        // Create tabbed pane and add tabs
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Book Requests", createRequestsPanel());
        tabbedPane.addTab("Book Holds", createHoldsPanel());
        tabbedPane.addTab("User Management", createUsersPanel());
        tabbedPane.addTab("Dashboard", createDashboardPanel());
        add(tabbedPane, BorderLayout.CENTER);
        
        // Logout button at bottom
        logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            new LoginPage().setVisible(true);
            dispose();
        });
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(logoutButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    // ------------------ Tab 1: Book Requests Panel ------------------
    private JPanel createRequestsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        // Create filter combo box for request status
        String[] filterOptions = {"All", "Pending", "Approved", "Denied"};
        requestFilterCombo = new JComboBox<>(filterOptions);
        requestFilterCombo.addActionListener(e -> loadRequests());
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Filter by Status:"));
        filterPanel.add(requestFilterCombo);
        panel.add(filterPanel, BorderLayout.NORTH);
        
        requestsModel = new DefaultTableModel(new Object[]{"Request_ID", "Login_ID", "B_Title", "Request_Date", "Status"}, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        requestsTable = new JTable(requestsModel);
        loadRequests();
        panel.add(new JScrollPane(requestsTable), BorderLayout.CENTER);
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        approveButton = new JButton("Approve");
        denyButton = new JButton("Deny");
        refreshRequestsButton = new JButton("Refresh");
        btnPanel.add(approveButton);
        btnPanel.add(denyButton);
        btnPanel.add(refreshRequestsButton);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        approveButton.addActionListener(e -> updateRequestStatus("Approved"));
        denyButton.addActionListener(e -> updateRequestStatus("Denied"));
        refreshRequestsButton.addActionListener(e -> loadRequests());
        
        return panel;
    }
    
    private void loadRequests() {
        try (Connection conn = DBConnection.getConnection()){
            String filter = (String) requestFilterCombo.getSelectedItem();
            String query = "SELECT Request_ID, Login_ID, B_Title, Request_Date, Status FROM Borrow_Requests";
            if (!"All".equalsIgnoreCase(filter)) {
                query += " WHERE Status = ?";
            }
            PreparedStatement stmt = conn.prepareStatement(query);
            if (!"All".equalsIgnoreCase(filter)) {
                stmt.setString(1, filter);
            }
            ResultSet rs = stmt.executeQuery();
            requestsModel.setRowCount(0);
            while(rs.next()){
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("Request_ID"));
                row.add(rs.getInt("Login_ID"));
                row.add(rs.getString("B_Title"));
                row.add(rs.getDate("Request_Date"));
                row.add(rs.getString("Status"));
                requestsModel.addRow(row);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading requests: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateRequestStatus(String newStatus) {
        int selectedRow = requestsTable.getSelectedRow();
        if(selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a request to update.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Prevent changing status if already Approved or Denied
        String currentStatus = (String) requestsModel.getValueAt(selectedRow, 4);
        if("Approved".equalsIgnoreCase(currentStatus) || "Denied".equalsIgnoreCase(currentStatus)) {
            JOptionPane.showMessageDialog(this, "This request has already been finalized.", "Update Not Allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int requestId = (int) requestsModel.getValueAt(selectedRow, 0);
        try (Connection conn = DBConnection.getConnection()){
            String query = "UPDATE Borrow_Requests SET Status = ? WHERE Request_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, newStatus);
            stmt.setInt(2, requestId);
            int rows = stmt.executeUpdate();
            if(rows > 0) {
                JOptionPane.showMessageDialog(this, "Request updated to: " + newStatus);
                loadRequests();
                loadHolds();
                updateDashboard();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update request.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating request: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // ------------------ Tab 2: Book Holds Panel ------------------
    private JPanel createHoldsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        holdsModel = new DefaultTableModel(new Object[]{"Request_ID", "Login_ID", "B_Title", "Request_Date", "Status"}, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        holdsTable = new JTable(holdsModel);
        loadHolds();
        panel.add(new JScrollPane(holdsTable), BorderLayout.CENTER);
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        returnButton = new JButton("Mark as Returned");
        refreshHoldsButton = new JButton("Refresh");
        btnPanel.add(returnButton);
        btnPanel.add(refreshHoldsButton);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        returnButton.addActionListener(e -> markHoldAsReturned());
        refreshHoldsButton.addActionListener(e -> loadHolds());
        
        return panel;
    }
    
    private void loadHolds() {
        try (Connection conn = DBConnection.getConnection()){
            String query = "SELECT Request_ID, Login_ID, B_Title, Request_Date, Status " +
                           "FROM Borrow_Requests WHERE Status = 'Approved'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            holdsModel.setRowCount(0);
            while(rs.next()){
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("Request_ID"));
                row.add(rs.getInt("Login_ID"));
                row.add(rs.getString("B_Title"));
                row.add(rs.getDate("Request_Date"));
                row.add(rs.getString("Status"));
                holdsModel.addRow(row);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading holds: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void markHoldAsReturned() {
        int selectedRow = holdsTable.getSelectedRow();
        if(selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a hold to mark as returned.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int requestId = (int) holdsModel.getValueAt(selectedRow, 0);
        try (Connection conn = DBConnection.getConnection()){
            String query = "UPDATE Borrow_Requests SET Status = 'Returned' WHERE Request_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, requestId);
            int rows = stmt.executeUpdate();
            if(rows > 0) {
                JOptionPane.showMessageDialog(this, "Hold marked as returned.");
                loadHolds();
                updateDashboard();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update hold.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error marking hold as returned: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // ------------------ Tab 3: User Management Panel ------------------
    private JPanel createUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        usersModel = new DefaultTableModel(new Object[]{"Login_ID", "U_Password", "U_Role"}, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        usersTable = new JTable(usersModel);
        loadUsers();
        panel.add(new JScrollPane(usersTable), BorderLayout.CENTER);
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        roleUpdateCombo = new JComboBox<>(new String[]{"Student", "Faculty", "Admin"});
        updateRoleButton = new JButton("Update Role");
        refreshUsersButton = new JButton("Refresh");
        btnPanel.add(new JLabel("New Role:"));
        btnPanel.add(roleUpdateCombo);
        btnPanel.add(updateRoleButton);
        btnPanel.add(refreshUsersButton);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        updateRoleButton.addActionListener(e -> updateUserRole());
        refreshUsersButton.addActionListener(e -> loadUsers());
        
        return panel;
    }
    
    private void loadUsers() {
        try (Connection conn = DBConnection.getConnection()){
            String query = "SELECT Login_ID, U_Password, U_Role FROM Login_Table";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            usersModel.setRowCount(0);
            while(rs.next()){
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("Login_ID"));
                row.add(rs.getString("U_Password"));
                row.add(rs.getString("U_Role"));
                usersModel.addRow(row);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading users: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateUserRole() {
        int selectedRow = usersTable.getSelectedRow();
        if(selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to update.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int loginId = (int) usersModel.getValueAt(selectedRow, 0);
        String newRole = (String) roleUpdateCombo.getSelectedItem();
        try (Connection conn = DBConnection.getConnection()){
            String query = "UPDATE Login_Table SET U_Role = ? WHERE Login_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, newRole);
            stmt.setInt(2, loginId);
            int rows = stmt.executeUpdate();
            if(rows > 0) {
                JOptionPane.showMessageDialog(this, "User role updated successfully.");
                loadUsers();
                updateDashboard();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update user role.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating user role: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // ------------------ Tab 4: Dashboard Panel ------------------
    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Create dataset for bar chart
        DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
        updateBarDataset(barDataset);
        JFreeChart barChart = ChartFactory.createBarChart(
                "LMS Dashboard - Counts", "Category", "Count", barDataset);
        ChartPanel barChartPanel = new ChartPanel(barChart);
        
        // Create dataset for pie chart (Borrow Requests by Status)
        DefaultPieDataset pieDataset = new DefaultPieDataset();
        updatePieDataset(pieDataset);
        JFreeChart pieChart = ChartFactory.createPieChart("Borrow Requests Distribution", pieDataset, true, true, false);
        ChartPanel pieChartPanel = new ChartPanel(pieChart);
        
        // Combine charts in a split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, barChartPanel, pieChartPanel);
        splitPane.setDividerLocation(500);
        panel.add(splitPane, BorderLayout.CENTER);
        
        refreshDashboardButton = new JButton("Refresh Dashboard");
        refreshDashboardButton.addActionListener(e -> updateDashboard());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.add(refreshDashboardButton);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    // Update the bar chart dataset with counts for Students, Faculty, Admins, Books, and Borrow Requests by status
    private void updateBarDataset(DefaultCategoryDataset dataset) {
        try (Connection conn = DBConnection.getConnection()){
            Statement stmt = conn.createStatement();
            // Students count
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM Student");
            if(rs.next()){
                dataset.addValue(rs.getInt("cnt"), "Count", "Students");
            }
            rs.close();
            // Faculty count
            rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM Faculty");
            if(rs.next()){
                dataset.addValue(rs.getInt("cnt"), "Count", "Faculty");
            }
            rs.close();
            // Admins count (from Admin_Table)
            rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM Admin_Table");
            if(rs.next()){
                dataset.addValue(rs.getInt("cnt"), "Count", "Admins");
            }
            rs.close();
            // Books count
            rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM Book");
            if(rs.next()){
                dataset.addValue(rs.getInt("cnt"), "Count", "Books");
            }
            rs.close();
            // Borrow Requests count by status
            rs = stmt.executeQuery("SELECT Status, COUNT(*) AS cnt FROM Borrow_Requests GROUP BY Status");
            while(rs.next()){
                String status = rs.getString("Status");
                dataset.addValue(rs.getInt("cnt"), "Count", "Requests (" + status + ")");
            }
            rs.close();
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating bar dataset: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Update the pie chart dataset with borrow requests distribution by status
    private void updatePieDataset(DefaultPieDataset dataset) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "SELECT Status, COUNT(*) AS cnt FROM Borrow_Requests GROUP BY Status";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while(rs.next()){
                dataset.setValue(rs.getString("Status"), rs.getInt("cnt"));
            }
            rs.close();
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating pie dataset: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateDashboard() {
        // Recreate datasets and update charts
        DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
        updateBarDataset(barDataset);
        JFreeChart barChart = ChartFactory.createBarChart(
                "LMS Dashboard - Counts", "Category", "Count", barDataset);
        
        DefaultPieDataset pieDataset = new DefaultPieDataset();
        updatePieDataset(pieDataset);
        JFreeChart pieChart = ChartFactory.createPieChart("Borrow Requests Distribution", pieDataset, true, true, false);
        
        // Update dashboard chart panels in the Dashboard tab.
        // Since the Dashboard tab was built with a JSplitPane containing two ChartPanels,
        // we need to replace the charts inside them. One approach is to rebuild the Dashboard tab.
        // For simplicity, we call loadDashboardTab() here.
        tabbedPane.setComponentAt(3, createDashboardPanel());
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminPage(201).setVisible(true));
    }
}

/*import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

public class AdminPage extends JFrame {
    private int userId;
    private JTabbedPane tabbedPane;
    
    // Tab 1: Book Requests
    private JTable requestsTable;
    private DefaultTableModel requestsModel;
    private JButton approveButton, denyButton, refreshRequestsButton;
    
    // Tab 2: Book Holds
    private JTable holdsTable;
    private DefaultTableModel holdsModel;
    private JButton returnButton, refreshHoldsButton;
    
    // Tab 3: User Management
    private JTable usersTable;
    private DefaultTableModel usersModel;
    private JButton updateRoleButton, refreshUsersButton;
    private JComboBox<String> roleUpdateCombo;
    
    // Tab 4: Dashboard (statistics)
    private ChartPanel dashboardChartPanel;
    
    // Logout button for all tabs
    private JButton logoutButton;
    
    public AdminPage(int userId) {
        super("Admin Page");
        this.userId = userId;
        initUI();
    }
    
    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        // Title label
        JLabel titleLabel = new JLabel("Admin Page - Manage Requests, Holds, Users & Dashboard", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 22));
        add(titleLabel, BorderLayout.NORTH);
        
        // Create tabbed pane and add tabs
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Book Requests", createRequestsPanel());
        tabbedPane.addTab("Book Holds", createHoldsPanel());
        tabbedPane.addTab("User Management", createUsersPanel());
        tabbedPane.addTab("Dashboard", createDashboardPanel());
        add(tabbedPane, BorderLayout.CENTER);
        
        // Logout button at bottom
        logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            new LoginPage().setVisible(true);
            dispose();
        });
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(logoutButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    // ------------------ Tab 1: Book Requests Panel ------------------
    private JPanel createRequestsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        requestsModel = new DefaultTableModel(new Object[]{"Request_ID", "Login_ID", "B_Title", "Request_Date", "Status"}, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        requestsTable = new JTable(requestsModel);
        loadRequests();
        panel.add(new JScrollPane(requestsTable), BorderLayout.CENTER);
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        approveButton = new JButton("Approve");
        denyButton = new JButton("Deny");
        refreshRequestsButton = new JButton("Refresh");
        btnPanel.add(approveButton);
        btnPanel.add(denyButton);
        btnPanel.add(refreshRequestsButton);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        approveButton.addActionListener(e -> updateRequestStatus("Approved"));
        denyButton.addActionListener(e -> updateRequestStatus("Denied"));
        refreshRequestsButton.addActionListener(e -> loadRequests());
        
        return panel;
    }
    
    private void loadRequests() {
        try (Connection conn = DBConnection.getConnection()){
            String query = "SELECT Request_ID, Login_ID, B_Title, Request_Date, Status FROM Borrow_Requests";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            requestsModel.setRowCount(0);
            while(rs.next()){
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("Request_ID"));
                row.add(rs.getInt("Login_ID"));
                row.add(rs.getString("B_Title"));
                row.add(rs.getDate("Request_Date"));
                row.add(rs.getString("Status"));
                requestsModel.addRow(row);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading requests: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateRequestStatus(String newStatus) {
        int selectedRow = requestsTable.getSelectedRow();
        if(selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a request to update.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int requestId = (int) requestsModel.getValueAt(selectedRow, 0);
        try (Connection conn = DBConnection.getConnection()){
            String query = "UPDATE Borrow_Requests SET Status = ? WHERE Request_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, newStatus);
            stmt.setInt(2, requestId);
            int rows = stmt.executeUpdate();
            if(rows > 0) {
                JOptionPane.showMessageDialog(this, "Request updated to: " + newStatus);
                loadRequests();
                loadHolds(); // refresh holds if needed
                updateDashboard();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update request.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating request: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // ------------------ Tab 2: Book Holds Panel ------------------
    private JPanel createHoldsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        holdsModel = new DefaultTableModel(new Object[]{"Request_ID", "Login_ID", "B_Title", "Request_Date", "Status"}, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        holdsTable = new JTable(holdsModel);
        loadHolds();
        panel.add(new JScrollPane(holdsTable), BorderLayout.CENTER);
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        returnButton = new JButton("Mark as Returned");
        refreshHoldsButton = new JButton("Refresh");
        btnPanel.add(returnButton);
        btnPanel.add(refreshHoldsButton);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        returnButton.addActionListener(e -> markHoldAsReturned());
        refreshHoldsButton.addActionListener(e -> loadHolds());
        
        return panel;
    }
    
    private void loadHolds() {
        try (Connection conn = DBConnection.getConnection()){
            // Show holds with Status = 'Approved'
            String query = "SELECT Request_ID, Login_ID, B_Title, Request_Date, Status FROM Borrow_Requests WHERE Status = 'Approved'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            holdsModel.setRowCount(0);
            while(rs.next()){
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("Request_ID"));
                row.add(rs.getInt("Login_ID"));
                row.add(rs.getString("B_Title"));
                row.add(rs.getDate("Request_Date"));
                row.add(rs.getString("Status"));
                holdsModel.addRow(row);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading holds: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void markHoldAsReturned() {
        int selectedRow = holdsTable.getSelectedRow();
        if(selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a hold to mark as returned.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int requestId = (int) holdsModel.getValueAt(selectedRow, 0);
        try (Connection conn = DBConnection.getConnection()){
            String query = "UPDATE Borrow_Requests SET Status = 'Returned' WHERE Request_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, requestId);
            int rows = stmt.executeUpdate();
            if(rows > 0) {
                JOptionPane.showMessageDialog(this, "Hold marked as returned.");
                loadHolds();
                updateDashboard();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update hold.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error marking hold as returned: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // ------------------ Tab 3: User Management Panel ------------------
    private JPanel createUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        usersModel = new DefaultTableModel(new Object[]{"Login_ID", "U_Password", "U_Role"}, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        usersTable = new JTable(usersModel);
        loadUsers();
        panel.add(new JScrollPane(usersTable), BorderLayout.CENTER);
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        roleUpdateCombo = new JComboBox<>(new String[]{"Student", "Faculty", "Admin"});
        updateRoleButton = new JButton("Update Role");
        refreshUsersButton = new JButton("Refresh");
        btnPanel.add(new JLabel("New Role:"));
        btnPanel.add(roleUpdateCombo);
        btnPanel.add(updateRoleButton);
        btnPanel.add(refreshUsersButton);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        updateRoleButton.addActionListener(e -> updateUserRole());
        refreshUsersButton.addActionListener(e -> loadUsers());
        
        return panel;
    }
    
    private void loadUsers() {
        try (Connection conn = DBConnection.getConnection()){
            String query = "SELECT Login_ID, U_Password, U_Role FROM Login_Table";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            usersModel.setRowCount(0);
            while(rs.next()){
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("Login_ID"));
                row.add(rs.getString("U_Password"));
                row.add(rs.getString("U_Role"));
                usersModel.addRow(row);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading users: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateUserRole() {
        int selectedRow = usersTable.getSelectedRow();
        if(selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to update.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int loginId = (int) usersModel.getValueAt(selectedRow, 0);
        String newRole = (String) roleUpdateCombo.getSelectedItem();
        try (Connection conn = DBConnection.getConnection()){
            String query = "UPDATE Login_Table SET U_Role = ? WHERE Login_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, newRole);
            stmt.setInt(2, loginId);
            int rows = stmt.executeUpdate();
            if(rows > 0) {
                JOptionPane.showMessageDialog(this, "User role updated successfully.");
                loadUsers();
                updateDashboard();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update user role.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating user role: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // ------------------ Tab 4: Dashboard Panel ------------------
    // This tab displays statistics using a bar chart (via JFreeChart)
    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        // Create dataset for bar chart
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        updateDashboardDataset(dataset);
        // Create the chart
        JFreeChart chart = ChartFactory.createBarChart(
                "LMS Dashboard",      // Chart title
                "Category",           // Domain axis label
                "Count",              // Range axis label
                dataset);
        // Create and add the chart panel
        dashboardChartPanel = new ChartPanel(chart);
        panel.add(dashboardChartPanel, BorderLayout.CENTER);
        // Optionally add a Refresh button
        JButton refreshDashboardButton = new JButton("Refresh Dashboard");
        refreshDashboardButton.addActionListener(e -> {
            updateDashboard();
        });
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.add(refreshDashboardButton);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }
    
    // Updates the dashboard dataset by querying counts from the database
    private void updateDashboardDataset(DefaultCategoryDataset dataset) {
        try (Connection conn = DBConnection.getConnection()){
            // Query count of Students
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM Student");
            if(rs.next()){
                dataset.addValue(rs.getInt("cnt"), "Students", "Students");
            }
            rs.close();
            
            // Query count of Faculty
            rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM Faculty");
            if(rs.next()){
                dataset.addValue(rs.getInt("cnt"), "Faculty", "Faculty");
            }
            rs.close();
            
            // Query count of Admins from Admin_Table
            rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM Admin_Table");
            if(rs.next()){
                dataset.addValue(rs.getInt("cnt"), "Admins", "Admins");
            }
            rs.close();
            
            // Query count of Books
            rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM Book");
            if(rs.next()){
                dataset.addValue(rs.getInt("cnt"), "Books", "Books");
            }
            rs.close();
            
            // Query counts of Borrow Requests by status
            rs = stmt.executeQuery("SELECT Status, COUNT(*) AS cnt FROM Borrow_Requests GROUP BY Status");
            while(rs.next()){
                String status = rs.getString("Status");
                dataset.addValue(rs.getInt("cnt"), "Borrow Requests (" + status + ")", status);
            }
            rs.close();
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating dashboard: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Refreshes the dashboard by updating the chart dataset.
    private void updateDashboard() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        updateDashboardDataset(dataset);
        JFreeChart chart = ChartFactory.createBarChart(
                "LMS Dashboard",
                "Category",
                "Count",
                dataset);
        dashboardChartPanel.setChart(chart);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminPage(201).setVisible(true));
    }
}

/*import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;

public class AdminPage extends JFrame {
    private int userId;
    private JTabbedPane tabbedPane;
    
    // ------------------ Tab 1: Book Requests ------------------
    private JTable requestsTable;
    private DefaultTableModel requestsModel;
    private JButton approveButton, denyButton, refreshRequestsButton;
    
    // ------------------ Tab 2: Book Holds ------------------
    private JTable holdsTable;
    private DefaultTableModel holdsModel;
    private JButton returnButton, refreshHoldsButton;
    
    // ------------------ Tab 3: User Management ------------------
    private JTable usersTable;
    private DefaultTableModel usersModel;
    private JButton updateRoleButton, refreshUsersButton;
    private JComboBox<String> roleUpdateCombo;
    
    // Logout button for all tabs
    private JButton logoutButton;
    
    public AdminPage(int userId) {
        super("Admin Page");
        this.userId = userId;
        initUI();
    }
    
    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        // Title label
        JLabel titleLabel = new JLabel("Admin Page - Manage Requests, Holds & Users", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 22));
        add(titleLabel, BorderLayout.NORTH);
        
        // Create tabbed pane
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Book Requests", createRequestsPanel());
        tabbedPane.addTab("Book Holds", createHoldsPanel());
        tabbedPane.addTab("User Management", createUsersPanel());
        add(tabbedPane, BorderLayout.CENTER);
        
        // Logout button at bottom
        logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            new LoginPage().setVisible(true);
            dispose();
        });
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(logoutButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    // ------------------ Tab 1: Book Requests Panel ------------------
    private JPanel createRequestsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        requestsModel = new DefaultTableModel(new Object[]{"Request_ID", "Login_ID", "B_Title", "Request_Date", "Status"}, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        requestsTable = new JTable(requestsModel);
        loadRequests();
        panel.add(new JScrollPane(requestsTable), BorderLayout.CENTER);
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        approveButton = new JButton("Approve");
        denyButton = new JButton("Deny");
        refreshRequestsButton = new JButton("Refresh");
        btnPanel.add(approveButton);
        btnPanel.add(denyButton);
        btnPanel.add(refreshRequestsButton);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        approveButton.addActionListener(e -> updateRequestStatus("Approved"));
        denyButton.addActionListener(e -> updateRequestStatus("Denied"));
        refreshRequestsButton.addActionListener(e -> loadRequests());
        
        return panel;
    }
    
    private void loadRequests() {
        try (Connection conn = DBConnection.getConnection()){
            String query = "SELECT Request_ID, Login_ID, B_Title, Request_Date, Status FROM Borrow_Requests";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            requestsModel.setRowCount(0);
            while(rs.next()){
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("Request_ID"));
                row.add(rs.getInt("Login_ID"));
                row.add(rs.getString("B_Title"));
                row.add(rs.getDate("Request_Date"));
                row.add(rs.getString("Status"));
                requestsModel.addRow(row);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading requests: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateRequestStatus(String newStatus) {
        int selectedRow = requestsTable.getSelectedRow();
        if(selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a request to update.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int requestId = (int) requestsModel.getValueAt(selectedRow, 0);
        try (Connection conn = DBConnection.getConnection()){
            String query = "UPDATE Borrow_Requests SET Status = ? WHERE Request_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, newStatus);
            stmt.setInt(2, requestId);
            int rows = stmt.executeUpdate();
            if(rows > 0) {
                JOptionPane.showMessageDialog(this, "Request updated to: " + newStatus);
                loadRequests();
                loadHolds(); // refresh holds if needed
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update request.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // ------------------ Tab 2: Book Holds Panel ------------------
    private JPanel createHoldsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        holdsModel = new DefaultTableModel(new Object[]{"Request_ID", "Login_ID", "B_Title", "Request_Date", "Status"}, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        holdsTable = new JTable(holdsModel);
        loadHolds();
        panel.add(new JScrollPane(holdsTable), BorderLayout.CENTER);
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        returnButton = new JButton("Mark as Returned");
        refreshHoldsButton = new JButton("Refresh");
        btnPanel.add(returnButton);
        btnPanel.add(refreshHoldsButton);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        returnButton.addActionListener(e -> markHoldAsReturned());
        refreshHoldsButton.addActionListener(e -> loadHolds());
        
        return panel;
    }
    
    private void loadHolds() {
        try (Connection conn = DBConnection.getConnection()){
            // Show holds with Status = 'Approved'
            String query = "SELECT Request_ID, Login_ID, B_Title, Request_Date, Status FROM Borrow_Requests WHERE Status = 'Approved'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            holdsModel.setRowCount(0);
            while(rs.next()){
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("Request_ID"));
                row.add(rs.getInt("Login_ID"));
                row.add(rs.getString("B_Title"));
                row.add(rs.getDate("Request_Date"));
                row.add(rs.getString("Status"));
                holdsModel.addRow(row);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading holds: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void markHoldAsReturned() {
        int selectedRow = holdsTable.getSelectedRow();
        if(selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a hold to mark as returned.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int requestId = (int) holdsModel.getValueAt(selectedRow, 0);
        try (Connection conn = DBConnection.getConnection()){
            String query = "UPDATE Borrow_Requests SET Status = 'Returned' WHERE Request_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, requestId);
            int rows = stmt.executeUpdate();
            if(rows > 0) {
                JOptionPane.showMessageDialog(this, "Hold marked as returned.");
                loadHolds();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update hold.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // ------------------ Tab 3: User Management Panel ------------------
    private JPanel createUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        usersModel = new DefaultTableModel(new Object[]{"Login_ID", "U_Password", "U_Role"}, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        usersTable = new JTable(usersModel);
        loadUsers();
        panel.add(new JScrollPane(usersTable), BorderLayout.CENTER);
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        roleUpdateCombo = new JComboBox<>(new String[]{"Student", "Faculty", "Admin"});
        updateRoleButton = new JButton("Update Role");
        refreshUsersButton = new JButton("Refresh");
        btnPanel.add(new JLabel("New Role:"));
        btnPanel.add(roleUpdateCombo);
        btnPanel.add(updateRoleButton);
        btnPanel.add(refreshUsersButton);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        updateRoleButton.addActionListener(e -> updateUserRole());
        refreshUsersButton.addActionListener(e -> loadUsers());
        
        return panel;
    }
    
    private void loadUsers() {
        try (Connection conn = DBConnection.getConnection()){
            String query = "SELECT Login_ID, U_Password, U_Role FROM Login_Table";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            usersModel.setRowCount(0);
            while(rs.next()){
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("Login_ID"));
                row.add(rs.getString("U_Password"));
                row.add(rs.getString("U_Role"));
                usersModel.addRow(row);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading users: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateUserRole() {
        int selectedRow = usersTable.getSelectedRow();
        if(selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to update.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int loginId = (int) usersModel.getValueAt(selectedRow, 0);
        String newRole = (String) roleUpdateCombo.getSelectedItem();
        try (Connection conn = DBConnection.getConnection()){
            String query = "UPDATE Login_Table SET U_Role = ? WHERE Login_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, newRole);
            stmt.setInt(2, loginId);
            int rows = stmt.executeUpdate();
            if(rows > 0) {
                JOptionPane.showMessageDialog(this, "User role updated successfully.");
                loadUsers();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update user role.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminPage(201).setVisible(true));
    }
}
/*import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AdminPage extends JFrame {
    private int userId;
    private JTable requestTable;
    private DefaultTableModel tableModel;
    
    // Panel for managing selected request
    private JTextField dueDateField; // admin enters the new due date here (format: yyyy-MM-dd)
    private JButton setDueDateButton, approveButton, denyButton, refreshButton, logoutButton;
    
    public AdminPage(int userId) {
        super("Admin Page");
        this.userId = userId;
        initUI();
    }
    
    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        // Title at the top
        JLabel titleLabel = new JLabel("Admin Page - Manage Borrow Requests", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 20));
        add(titleLabel, BorderLayout.NORTH);
        
        // Center: Table showing all borrow requests
        tableModel = new DefaultTableModel(new Object[]{"Request_ID", "Login_ID", "B_Title", "Request_Date", "Due_Date", "Status"}, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        requestTable = new JTable(tableModel);
        loadRequests();
        add(new JScrollPane(requestTable), BorderLayout.CENTER);
        
        // South: Admin controls panel
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Font font = new Font("Georgia", Font.PLAIN, 14);
        
        // Due date field
        gbc.gridx = 0; gbc.gridy = 0;
        controlPanel.add(new JLabel("New Due Date (yyyy-MM-dd):"), gbc);
        gbc.gridx = 1;
        dueDateField = new JTextField(10);
        dueDateField.setFont(font);
        controlPanel.add(dueDateField, gbc);
        
        // Set Due Date button
        gbc.gridx = 2;
        setDueDateButton = new JButton("Set Due Date");
        controlPanel.add(setDueDateButton, gbc);
        
        // Approve button
        gbc.gridx = 0; gbc.gridy = 1;
        approveButton = new JButton("Approve Request");
        controlPanel.add(approveButton, gbc);
        
        // Deny button
        gbc.gridx = 1;
        denyButton = new JButton("Deny Request");
        controlPanel.add(denyButton, gbc);
        
        // Refresh button
        gbc.gridx = 2;
        refreshButton = new JButton("Refresh");
        controlPanel.add(refreshButton, gbc);
        
        // Logout button
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3;
        logoutButton = new JButton("Logout");
        controlPanel.add(logoutButton, gbc);
        
        add(controlPanel, BorderLayout.SOUTH);
        
        // Action Listeners
        setDueDateButton.addActionListener(e -> setDueDate());
        approveButton.addActionListener(e -> updateRequestStatus("Approved"));
        denyButton.addActionListener(e -> updateRequestStatus("Denied"));
        refreshButton.addActionListener(e -> loadRequests());
        logoutButton.addActionListener(e -> {
            new LoginPage().setVisible(true);
            dispose();
        });
    }
    
    // Load all borrow requests from the table.
    // This query assumes your Borrow_Requests table now has a Due_Date column.
    private void loadRequests() {
        try (Connection conn = DBConnection.getConnection()){
            String query = "SELECT Request_ID, Login_ID, B_Title, Request_Date, Due_Date, Status FROM Borrow_Requests";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            tableModel.setRowCount(0);
            while(rs.next()){
                Object[] row = {
                    rs.getInt("Request_ID"),
                    rs.getInt("Login_ID"),
                    rs.getString("B_Title"),
                    rs.getDate("Request_Date"),
                    rs.getDate("Due_Date"), // may be null
                    rs.getString("Status")
                };
                tableModel.addRow(row);
            }
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading requests: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Updates the status of the selected request (for Approve or Deny actions)
    private void updateRequestStatus(String newStatus) {
        int selectedRow = requestTable.getSelectedRow();
        if(selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a request to update.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int requestId = (int) tableModel.getValueAt(selectedRow, 0);
        try (Connection conn = DBConnection.getConnection()){
            String query = "UPDATE Borrow_Requests SET Status = ? WHERE Request_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, newStatus);
            stmt.setInt(2, requestId);
            int rows = stmt.executeUpdate();
            if(rows > 0) {
                JOptionPane.showMessageDialog(this, "Request updated to: " + newStatus);
                loadRequests();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update request.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Allows admin to set/update the due date of the selected borrow request.
    private void setDueDate() {
        int selectedRow = requestTable.getSelectedRow();
        if(selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a request to update due date.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String dueDateStr = dueDateField.getText().trim();
        if(dueDateStr.isEmpty()){
            JOptionPane.showMessageDialog(this, "Please enter a due date in format yyyy-MM-dd.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        java.sql.Date dueDate;
        try {
            Date parsedDate = new SimpleDateFormat("yyyy-MM-dd").parse(dueDateStr);
            dueDate = new java.sql.Date(parsedDate.getTime());
        } catch(ParseException ex) {
            JOptionPane.showMessageDialog(this, "Due date must be in format yyyy-MM-dd.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int requestId = (int) tableModel.getValueAt(selectedRow, 0);
        try (Connection conn = DBConnection.getConnection()){
            String query = "UPDATE Borrow_Requests SET Due_Date = ? WHERE Request_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setDate(1, dueDate);
            stmt.setInt(2, requestId);
            int rows = stmt.executeUpdate();
            if(rows > 0) {
                JOptionPane.showMessageDialog(this, "Due date updated successfully.");
                loadRequests();
                dueDateField.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update due date.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminPage(201).setVisible(true));
    }
}
/*import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class AdminPage extends JFrame {
    private int userId;
    private JTable requestTable;
    private DefaultTableModel tableModel;
    private JButton approveButton, denyButton, logoutButton;

    public AdminPage(int userId) {
        super("Admin Page");
        this.userId = userId;
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Welcome, Admin " + userId, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{"Request_ID", "Login_ID", "B_Title", "Request_Date", "Status"}, 0);
        requestTable = new JTable(tableModel);
        loadRequests();
        add(new JScrollPane(requestTable), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        approveButton = new JButton("Approve");
        denyButton = new JButton("Deny");
        logoutButton = new JButton("Logout");
        bottomPanel.add(approveButton);
        bottomPanel.add(denyButton);
        bottomPanel.add(logoutButton);
        add(bottomPanel, BorderLayout.SOUTH);

        approveButton.addActionListener(e -> updateRequestStatus("Approved"));
        denyButton.addActionListener(e -> updateRequestStatus("Denied"));
        logoutButton.addActionListener(e -> {
            new LoginPage().setVisible(true);
            dispose();
        });
    }

    private void loadRequests() {
        try (Connection conn = DBConnection.getConnection()){
            String query = "SELECT Request_ID, Login_ID, B_Title, Request_Date, Status FROM Borrow_Requests WHERE Status = 'Pending'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            tableModel.setRowCount(0);
            while(rs.next()){
                Object[] row = {
                    rs.getInt("Request_ID"),
                    rs.getInt("Login_ID"),
                    rs.getString("B_Title"),
                    rs.getDate("Request_Date"),
                    rs.getString("Status")
                };
                tableModel.addRow(row);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading requests: " + ex.getMessage());
        }
    }

    private void updateRequestStatus(String newStatus) {
        int selectedRow = requestTable.getSelectedRow();
        if(selectedRow == -1){
            JOptionPane.showMessageDialog(this, "Please select a request.");
            return;
        }
        int requestId = (int) tableModel.getValueAt(selectedRow, 0);
        try (Connection conn = DBConnection.getConnection()){
            String query = "UPDATE Borrow_Requests SET Status = ? WHERE Request_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, newStatus);
            stmt.setInt(2, requestId);
            int rows = stmt.executeUpdate();
            if(rows > 0){
                JOptionPane.showMessageDialog(this, "Request " + newStatus + " successfully!");
                loadRequests();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update request.");
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

public class AdminPage extends JFrame {
    private int userId;
    private JTable requestTable;
    private DefaultTableModel tableModel;
    private JButton approveButton, denyButton, logoutButton;

    public AdminPage(int userId) {
        super("Admin Page");
        this.userId = userId;
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Top: Title label
        JLabel titleLabel = new JLabel("Welcome, Admin " + userId, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);

        // Center: Table for pending requests
        tableModel = new DefaultTableModel(new Object[]{"Request_ID", "Login_ID", "B_Title", "Request_Date", "Status"}, 0);
        requestTable = new JTable(tableModel);
        loadRequests();
        add(new JScrollPane(requestTable), BorderLayout.CENTER);

        // Bottom: Buttons for approve, deny, and logout
        JPanel bottomPanel = new JPanel();
        approveButton = new JButton("Approve");
        denyButton = new JButton("Deny");
        logoutButton = new JButton("Logout");
        bottomPanel.add(approveButton);
        bottomPanel.add(denyButton);
        bottomPanel.add(logoutButton);
        add(bottomPanel, BorderLayout.SOUTH);

        approveButton.addActionListener(e -> updateRequestStatus("Approved"));
        denyButton.addActionListener(e -> updateRequestStatus("Denied"));
        logoutButton.addActionListener(e -> {
            new LoginPage().setVisible(true);
            dispose();
        });
    }

    private void loadRequests() {
        try (Connection conn = DBConnection.getConnection()){
            String query = "SELECT Request_ID, Login_ID, B_Title, Request_Date, Status FROM Borrow_Requests WHERE Status = 'Pending'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            tableModel.setRowCount(0);
            while(rs.next()){
                Object[] row = {
                    rs.getInt("Request_ID"),
                    rs.getInt("Login_ID"),
                    rs.getString("B_Title"),
                    rs.getDate("Request_Date"),
                    rs.getString("Status")
                };
                tableModel.addRow(row);
            }
        } catch(SQLException ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading requests: " + ex.getMessage());
        }
    }

    private void updateRequestStatus(String newStatus) {
        int selectedRow = requestTable.getSelectedRow();
        if(selectedRow == -1){
            JOptionPane.showMessageDialog(this, "Please select a request.");
            return;
        }
        int requestId = (int) tableModel.getValueAt(selectedRow, 0);
        try (Connection conn = DBConnection.getConnection()){
            String query = "UPDATE Borrow_Requests SET Status = ? WHERE Request_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, newStatus);
            stmt.setInt(2, requestId);
            int rows = stmt.executeUpdate();
            if(rows > 0){
                JOptionPane.showMessageDialog(this, "Request " + newStatus + " successfully!");
                loadRequests();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update request.");
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

public class AdminPage extends JFrame {
    private JTable requestTable;
    private JButton approveButton, rejectButton;

    public AdminPage() {
        setTitle("Admin Dashboard");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Set up table to show book requests
        String[] columns = {"Request ID", "Book ID", "User ID", "Status"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        requestTable = new JTable(model);

        // Populate table with pending requests (DB query here)
        loadRequests(model);

        JScrollPane scrollPane = new JScrollPane(requestTable);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        approveButton = new JButton("Approve");
        rejectButton = new JButton("Reject");
        buttonPanel.add(approveButton);
        buttonPanel.add(rejectButton);
        add(buttonPanel, BorderLayout.SOUTH);

        approveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = requestTable.getSelectedRow();
                if (selectedRow != -1) {
                    String requestId = (String) requestTable.getValueAt(selectedRow, 0);
                    updateRequestStatus(requestId, "approved");
                }
            }
        });

        rejectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = requestTable.getSelectedRow();
                if (selectedRow != -1) {
                    String requestId = (String) requestTable.getValueAt(selectedRow, 0);
                    updateRequestStatus(requestId, "rejected");
                }
            }
        });
    }

    private void loadRequests(DefaultTableModel model) {
        // Load pending requests from DB (select query to fetch requests with 'pending' status)
        try (Connection conn = DriverManager.getConnection("jdbc:your_database_url", "username", "password")) {
            String query = "SELECT * FROM BookRequests WHERE Status = 'pending'";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String requestId = rs.getString("Req_ID");
                String bookId = rs.getString("Book_ID");
                String userId = rs.getString("User_ID");
                String status = rs.getString("Status");
                model.addRow(new Object[]{requestId, bookId, userId, status});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateRequestStatus(String requestId, String status) {
        try (Connection conn = DriverManager.getConnection("jdbc:your_database_url", "username", "password")) {
            String query = "UPDATE BookRequests SET Status = ? WHERE Req_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, status);
            stmt.setString(2, requestId);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Request " + status + " successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to update request.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminPage().setVisible(true));
    }
}
*/