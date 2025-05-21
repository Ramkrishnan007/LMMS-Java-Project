import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Pattern;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

// Imports for JDatePicker (download from SourceForge or Maven)
import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

public class SignUpPage extends JFrame {
    // ---------- Main Wizard Panel ----------
    private JPanel mainCardPanel;
    private CardLayout mainCardLayout;
    
    // ---------- Step 1: Account Info ----------
    private JPanel accountInfoPanel;
    private JTextField userIdField;
    private JPasswordField passwordField, confirmPasswordField;
    private JTextField phoneField;
    private JButton sendOtpButton, accountNextButton;
    
    // ---------- Step 2: OTP Verification ----------
    private JPanel otpPanel;
    private JTextField otpField;
    private JButton verifyOtpButton, otpBackButton;
    private String generatedOtp;
    private boolean isOtpVerified = false;
    
    // ---------- Step 3: Role Details ----------
    private JPanel roleDetailsPanel;
    private CardLayout roleCardLayout;
    private JComboBox<String> roleComboBox;  // Allowed values: "Student", "Faculty"
    
    // Student panel gender components
    private JRadioButton stuMaleRadio, stuFemaleRadio;
    // Faculty panel gender components
    private JRadioButton facMaleRadio, facFemaleRadio;
    
    // Student panel fields
    private JTextField stuNameField, stuEmailField, stuPhoneField, stuAddressField, stuAgeField;
    private JDatePickerImpl stuDobPicker; // Calendar widget for DOB
    private JComboBox<String> stuDepComboBox; // Department dropdown
    
    // Faculty panel fields
    private JTextField facNameField, facEmailField, facPhoneField, facAgeField;
    private JDatePickerImpl facDobPicker; // Calendar widget for DOB
    private JComboBox<String> facDepComboBox; // Department dropdown
    
    // Navigation buttons for Role Details
    private JButton registerButton, detailsBackButton;
    
    public SignUpPage() {
        super("Sign Up Page");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 650);
        setLocationRelativeTo(null);
        initUI();
    }
    
    private void initUI() {
        mainCardLayout = new CardLayout();
        mainCardPanel = new JPanel(mainCardLayout);
        
        mainCardPanel.add(createAccountInfoPanel(), "AccountInfo");
        mainCardPanel.add(createOtpPanel(), "OTP");
        mainCardPanel.add(createRoleDetailsPanel(), "RoleDetails");
        
        add(mainCardPanel, BorderLayout.CENTER);
    }
    
    // ---------------- Step 1: Account Information Panel ----------------
    private JPanel createAccountInfoPanel() {
        accountInfoPanel = new JPanel(new GridBagLayout());
        accountInfoPanel.setBorder(BorderFactory.createTitledBorder("Step 1: Account Information"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Font font = new Font("Georgia", Font.PLAIN, 14);
        int row = 0;
        
        // Title
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("Enter Account Information", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 18));
        accountInfoPanel.add(titleLabel, gbc);
        
        // Row 1: User ID
        row++;
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = row;
        accountInfoPanel.add(new JLabel("User ID (number):"), gbc);
        gbc.gridx = 1;
        userIdField = new JTextField(20);
        userIdField.setFont(font);
        accountInfoPanel.add(userIdField, gbc);
        
        // Row 2: Password
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        accountInfoPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        passwordField.setFont(font);
        accountInfoPanel.add(passwordField, gbc);
        
        // Row 3: Confirm Password
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        accountInfoPanel.add(new JLabel("Confirm Password:"), gbc);
        gbc.gridx = 1;
        confirmPasswordField = new JPasswordField(20);
        confirmPasswordField.setFont(font);
        accountInfoPanel.add(confirmPasswordField, gbc);
        
        // Row 4: Phone Number
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        accountInfoPanel.add(new JLabel("Phone (10 digits):"), gbc);
        gbc.gridx = 1;
        phoneField = new JTextField(15);
        phoneField.setFont(font);
        accountInfoPanel.add(phoneField, gbc);
        
        // Row 5: Buttons: Send OTP and Next
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        sendOtpButton = new JButton("Send OTP");
        accountInfoPanel.add(sendOtpButton, gbc);
        gbc.gridx = 1;
        accountNextButton = new JButton("Next");
        accountInfoPanel.add(accountNextButton, gbc);
        
        sendOtpButton.addActionListener(e -> {
            if(validateAccountInfo()) {
                sendOtp();
            }
        });
        accountNextButton.addActionListener(e -> {
            if(validateAccountInfo() && generatedOtp != null) {
                mainCardLayout.show(mainCardPanel, "OTP");
            }
        });
        
        return accountInfoPanel;
    }
    
    private boolean validateAccountInfo() {
        String uid = userIdField.getText().trim();
        String pass = new String(passwordField.getPassword()).trim();
        String confPass = new String(confirmPasswordField.getPassword()).trim();
        String phone = phoneField.getText().trim();
        if(uid.isEmpty() || pass.isEmpty() || confPass.isEmpty() || phone.isEmpty()){
            JOptionPane.showMessageDialog(this, "All account fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            Integer.parseInt(uid);
        } catch(NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "User ID must be a valid number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(pass.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters long.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(!pass.equals(confPass)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(!Pattern.matches("^[0-9]{10}$", phone)) {
            JOptionPane.showMessageDialog(this, "Phone number must be exactly 10 digits.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
    
    private void sendOtp() {
        generatedOtp = String.valueOf(100000 + new Random().nextInt(900000));
        JOptionPane.showMessageDialog(this, "OTP sent to your phone.\n(For demo, OTP is: " + generatedOtp + ")");
    }
    
    // ---------------- Step 2: OTP Verification Panel ----------------
    private JPanel createOtpPanel() {
        otpPanel = new JPanel(new GridBagLayout());
        otpPanel.setBorder(BorderFactory.createTitledBorder("Step 2: OTP Verification"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Font font = new Font("Georgia", Font.PLAIN, 14);
        int row = 0;
        
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        JLabel label = new JLabel("Enter the OTP sent to your phone", SwingConstants.CENTER);
        label.setFont(new Font("Georgia", Font.BOLD, 16));
        otpPanel.add(label, gbc);
        
        // Row 1: OTP field
        row++;
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = row;
        otpPanel.add(new JLabel("OTP:"), gbc);
        gbc.gridx = 1;
        otpField = new JTextField(10);
        otpField.setFont(font);
        otpPanel.add(otpField, gbc);
        
        // Row 2: Verify OTP and Back buttons
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        verifyOtpButton = new JButton("Verify OTP");
        otpBackButton = new JButton("Back");
        btnPanel.add(verifyOtpButton);
        btnPanel.add(otpBackButton);
        otpPanel.add(btnPanel, gbc);
        
        verifyOtpButton.addActionListener(e -> verifyOtp());
        otpBackButton.addActionListener(e -> mainCardLayout.show(mainCardPanel, "AccountInfo"));
        
        return otpPanel;
    }
    
    private void verifyOtp() {
        String enteredOtp = otpField.getText().trim();
        if(generatedOtp != null && generatedOtp.equals(enteredOtp)) {
            JOptionPane.showMessageDialog(this, "OTP Verified Successfully!");
            isOtpVerified = true;
            mainCardLayout.show(mainCardPanel, "RoleDetails");
        } else {
            JOptionPane.showMessageDialog(this, "Invalid OTP. Please try again.", "Verification Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // ---------------- Step 3: Role Details Panel ----------------
    private JPanel createRoleDetailsPanel() {
        roleDetailsPanel = new JPanel(new GridBagLayout());
        roleDetailsPanel.setBorder(BorderFactory.createTitledBorder("Step 3: Enter Role Details"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Font font = new Font("Georgia", Font.PLAIN, 14);
        int row = 0;
        
        // Role dropdown (only "Student" and "Faculty")
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        roleDetailsPanel.add(new JLabel("Select Role:"), gbc);
        gbc.gridx = 2;
        String[] roles = {"Student", "Faculty"};
        roleComboBox = new JComboBox<>(roles);
        roleComboBox.setFont(font);
        roleDetailsPanel.add(roleComboBox, gbc);
        
        // Sub-panel for role-specific details using CardLayout
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 4;
        JPanel subPanel = new JPanel(new CardLayout());
        subPanel.setBorder(BorderFactory.createTitledBorder("Enter Details"));
        roleCardLayout = (CardLayout) subPanel.getLayout();
        
        // ----- Student Details Panel -----
        JPanel stuPanel = new JPanel(new GridBagLayout());
        GridBagConstraints stuGbc = new GridBagConstraints();
        stuGbc.insets = new Insets(5,5,5,5);
        stuGbc.fill = GridBagConstraints.HORIZONTAL;
        int stuRow = 0;
        
        // Student Name
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Name:"), stuGbc);
        stuGbc.gridx = 1;
        stuNameField = new JTextField(20);
        stuNameField.setFont(font);
        stuPanel.add(stuNameField, stuGbc);
        
        // Student Gender
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Gender:"), stuGbc);
        stuGbc.gridx = 1;
        JPanel stuGenderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        stuMaleRadio = new JRadioButton("Male");
        stuFemaleRadio = new JRadioButton("Female");
        ButtonGroup stuGenderGroup = new ButtonGroup();
        stuGenderGroup.add(stuMaleRadio);
        stuGenderGroup.add(stuFemaleRadio);
        stuGenderPanel.add(stuMaleRadio);
        stuGenderPanel.add(stuFemaleRadio);
        stuPanel.add(stuGenderPanel, stuGbc);
        
        // Student Email
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Email:"), stuGbc);
        stuGbc.gridx = 1;
        stuEmailField = new JTextField(20);
        stuEmailField.setFont(font);
        stuPanel.add(stuEmailField, stuGbc);
        
        // Student Phone
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Phone (10 digits):"), stuGbc);
        stuGbc.gridx = 1;
        stuPhoneField = new JTextField(15);
        stuPhoneField.setFont(font);
        stuPhoneField.setText(phoneField.getText().trim());
        stuPanel.add(stuPhoneField, stuGbc);
        
        // Student Address
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Address:"), stuGbc);
        stuGbc.gridx = 1;
        stuAddressField = new JTextField(20);
        stuAddressField.setFont(font);
        stuPanel.add(stuAddressField, stuGbc);
        
        // Student DOB using JDatePicker for a popup calendar:
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("DOB (yyyy-MM-dd):"), stuGbc);
        stuGbc.gridx = 1;
        UtilDateModel stuModel = new UtilDateModel();
        Properties stuProps = new Properties();
        stuProps.put("text.today", "Today");
        stuProps.put("text.month", "Month");
        stuProps.put("text.year", "Year");
        JDatePanelImpl stuDatePanel = new JDatePanelImpl(stuModel, stuProps);
        stuDobPicker = new JDatePickerImpl(stuDatePanel, new DateLabelFormatter());
        stuPanel.add(stuDobPicker, stuGbc);
        
        // Student Age (read-only) - auto calculated
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Age:"), stuGbc);
        stuGbc.gridx = 1;
        stuAgeField = new JTextField(5);
        stuAgeField.setFont(font);
        stuAgeField.setEditable(false);
        stuPanel.add(stuAgeField, stuGbc);
        
        // Add a ChangeListener to auto-calculate age when DOB is selected:
        stuDobPicker.getModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (stuDobPicker.getModel().getValue() != null) {
                    Date selectedDate = (Date) stuDobPicker.getModel().getValue();
                    int age = calculateAge(selectedDate);
                    stuAgeField.setText(String.valueOf(age));
                }
            }
        });
        
        // Student Department Dropdown
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Department:"), stuGbc);
        stuGbc.gridx = 1;
        String[] deptOptions = {"1 - CSE", "2 - EEE", "3 - IT", "4 - MECH"};
        stuDepComboBox = new JComboBox<>(deptOptions);
        stuDepComboBox.setFont(font);
        stuPanel.add(stuDepComboBox, stuGbc);
        
        subPanel.add(stuPanel, "Student");
        
        // ----- Faculty Details Panel -----
        JPanel facPanel = new JPanel(new GridBagLayout());
        GridBagConstraints facGbc = new GridBagConstraints();
        facGbc.insets = new Insets(5,5,5,5);
        facGbc.fill = GridBagConstraints.HORIZONTAL;
        int facRow = 0;
        
        // Faculty Name
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Name:"), facGbc);
        facGbc.gridx = 1;
        facNameField = new JTextField(20);
        facNameField.setFont(font);
        facPanel.add(facNameField, facGbc);
        
        // Faculty Gender
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Gender:"), facGbc);
        facGbc.gridx = 1;
        JPanel facGenderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        facMaleRadio = new JRadioButton("Male");
        facFemaleRadio = new JRadioButton("Female");
        ButtonGroup facGenderGroup = new ButtonGroup();
        facGenderGroup.add(facMaleRadio);
        facGenderGroup.add(facFemaleRadio);
        facGenderPanel.add(facMaleRadio);
        facGenderPanel.add(facFemaleRadio);
        facPanel.add(facGenderPanel, facGbc);
        
        // Faculty Email
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Email:"), facGbc);
        facGbc.gridx = 1;
        facEmailField = new JTextField(20);
        facEmailField.setFont(font);
        facPanel.add(facEmailField, facGbc);
        
        // Faculty Phone
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Phone (10 digits):"), facGbc);
        facGbc.gridx = 1;
        facPhoneField = new JTextField(15);
        facPhoneField.setFont(font);
        facPanel.add(facPhoneField, facGbc);
        
        // Faculty DOB using JDatePicker:
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("DOB:"), facGbc);
        facGbc.gridx = 1;
        UtilDateModel facModel = new UtilDateModel();
        Properties facProps = new Properties();
        facProps.put("text.today", "Today");
        facProps.put("text.month", "Month");
        facProps.put("text.year", "Year");
        JDatePanelImpl facDatePanel = new JDatePanelImpl(facModel, facProps);
        facDobPicker = new JDatePickerImpl(facDatePanel, new DateLabelFormatter());
        facPanel.add(facDobPicker, facGbc);
        
        // Faculty Age (read-only) - auto calculated
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Age:"), facGbc);
        facGbc.gridx = 1;
        facAgeField = new JTextField(5);
        facAgeField.setFont(font);
        facAgeField.setEditable(false);
        facPanel.add(facAgeField, facGbc);
        
        // Auto-calculate age when DOB is chosen:
        facDobPicker.getModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (facDobPicker.getModel().getValue() != null) {
                    Date selectedDate = (Date) facDobPicker.getModel().getValue();
                    int age = calculateAge(selectedDate);
                    facAgeField.setText(String.valueOf(age));
                }
            }
        });
        
        // Faculty Department Dropdown
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Department:"), facGbc);
        facGbc.gridx = 1;
        facDepComboBox = new JComboBox<>(deptOptions);
        facDepComboBox.setFont(font);
        facPanel.add(facDepComboBox, facGbc);
        
        subPanel.add(facPanel, "Faculty");
        
        gbc.gridy++;
        gbc.gridx = 0; gbc.gridwidth = 4;
        roleDetailsPanel.add(subPanel, gbc);
        
        // Row for Register and Back buttons in Role Details Panel
        gbc.gridy++;
        gbc.gridx = 0; gbc.gridwidth = 4;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        registerButton = new JButton("Register");
        detailsBackButton = new JButton("Back");
        btnPanel.add(registerButton);
        btnPanel.add(detailsBackButton);
        roleDetailsPanel.add(btnPanel, gbc);
        
        registerButton.addActionListener(e -> registerUser());
        detailsBackButton.addActionListener(e -> mainCardLayout.show(mainCardPanel, "OTP"));
        
        roleComboBox.addActionListener(e -> {
            String selectedRole = (String) roleComboBox.getSelectedItem();
            roleCardLayout.show(subPanel, selectedRole);
        });
        roleCardLayout.show(subPanel, "Student");
        
        return roleDetailsPanel;
    }
    
    // Calculate age from a java.util.Date
    private int calculateAge(Date dob) {
        Calendar dobCal = Calendar.getInstance();
        dobCal.setTime(dob);
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR);
        if(today.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age;
    }
    
    // ---------------- Registration and DB Insertion ----------------
    private void registerUser() {
        // Validate common fields
        String userIdStr = userIdField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String role = (String) roleComboBox.getSelectedItem();
        if(userIdStr.isEmpty() || password.isEmpty() || role.isEmpty()){
            JOptionPane.showMessageDialog(this, "User ID, Password, and Role are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int userId;
        try {
            userId = Integer.parseInt(userIdStr);
        } catch(NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "User ID must be a valid number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if(password.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters long.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if(role.equalsIgnoreCase("Student")) {
            String stuName = stuNameField.getText().trim();
            String stuEmail = stuEmailField.getText().trim();
            String stuPhone = stuPhoneField.getText().trim();
            String stuAddress = stuAddressField.getText().trim();
            // Retrieve DOB from JDatePicker
            Date stuUtilDob = (Date) stuDobPicker.getModel().getValue();
            if(stuUtilDob == null) {
                JOptionPane.showMessageDialog(this, "Please select a valid DOB.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            java.sql.Date stuDob = new java.sql.Date(stuUtilDob.getTime());
            // Get department from dropdown and extract numeric part
            String stuDepStr = stuDepComboBox.getSelectedItem().toString().split(" ")[0];
            if(stuName.isEmpty() || stuEmail.isEmpty() || stuPhone.isEmpty() ||
               stuAddress.isEmpty() || stuDepStr.isEmpty()){
                JOptionPane.showMessageDialog(this, "All Student fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Validate gender
            String stuGender = null;
            if(stuMaleRadio.isSelected()) {
                stuGender = "Male";
            } else if(stuFemaleRadio.isSelected()) {
                stuGender = "Female";
            }
            if(stuGender == null) {
                JOptionPane.showMessageDialog(this, "Please select gender.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[0-9]{10}$", stuPhone)) {
                JOptionPane.showMessageDialog(this, "Student phone must be exactly 10 digits.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[\\w.-]+@[\\w.-]+\\.com$", stuEmail)) {
                JOptionPane.showMessageDialog(this, "Student email must be valid and end with '.com'.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int age = calculateAge(stuUtilDob);
            if(age < 18) {
                JOptionPane.showMessageDialog(this, "Student must be at least 18 years old.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Integer.parseInt(stuDepStr);
            } catch(NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid Department ID.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(isPhoneRegistered("Student", stuPhone)) {
                JOptionPane.showMessageDialog(this, "This phone number is already registered for a student.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            registerToDB(userId, password, role);
            insertStudent(userId, stuName, stuEmail, stuPhone, stuAddress, stuDob, Integer.parseInt(stuDepStr), stuGender);
            
        } else if(role.equalsIgnoreCase("Faculty")) {
            String facName = facNameField.getText().trim();
            String facEmail = facEmailField.getText().trim();
            String facPhone = facPhoneField.getText().trim();
            // Retrieve DOB from JDatePicker
            Date facUtilDob = (Date) facDobPicker.getModel().getValue();
            if(facUtilDob == null) {
                JOptionPane.showMessageDialog(this, "Please select a valid DOB.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            java.sql.Date facDob = new java.sql.Date(facUtilDob.getTime());
            // Get department from dropdown and extract numeric part
            String facDepStr = facDepComboBox.getSelectedItem().toString().split(" ")[0];
            if(facName.isEmpty() || facEmail.isEmpty() || facPhone.isEmpty() ||
               facDepStr.isEmpty()){
                JOptionPane.showMessageDialog(this, "All Faculty fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[0-9]{10}$", facPhone)) {
                JOptionPane.showMessageDialog(this, "Faculty phone must be exactly 10 digits.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[\\w.-]+@[\\w.-]+\\.com$", facEmail)) {
                JOptionPane.showMessageDialog(this, "Faculty email must be valid and end with '.com'.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String facGender = null;
            if(facMaleRadio.isSelected()) {
                facGender = "Male";
            } else if(facFemaleRadio.isSelected()) {
                facGender = "Female";
            }
            if(facGender == null) {
                JOptionPane.showMessageDialog(this, "Please select gender.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int age = calculateAge(facUtilDob);
            if(age < 26) {
                JOptionPane.showMessageDialog(this, "Faculty must be at least 26 years old.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Integer.parseInt(facDepStr);
            } catch(NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "invalid Department ID.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(isPhoneRegistered("Faculty", facPhone)) {
                JOptionPane.showMessageDialog(this, "This phone number is already registered for a faculty member.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            registerToDB(userId, password, role);
            insertFaculty(userId, facName, facEmail, facPhone, Integer.parseInt(facDepStr), facGender);
        }
        
        JOptionPane.showMessageDialog(this, "Registration successful!");
        new LoginPage().setVisible(true);
        dispose();
    }
    
    // Checks if the phone is already registered in the appropriate table
    private boolean isPhoneRegistered(String role, String phone) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "";
            if(role.equalsIgnoreCase("Student")) {
                query = "SELECT Stu_PhoneNO FROM Student WHERE Stu_PhoneNO = ?";
            } else if(role.equalsIgnoreCase("Faculty")) {
                query = "SELECT F_PhoneNO FROM Faculty WHERE F_PhoneNO = ?";
            }
            if(query.isEmpty()) return false;
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, phone);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch(SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }
    
    // Inserts login details into Login_Table
    private void registerToDB(int userId, String password, String role) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Login_Table (Login_ID, U_Password, U_Role) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setString(2, password);
            stmt.setString(3, role);
            stmt.executeUpdate();
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error registering login details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Inserts student details into Student table (Stu_age is calculated)
    private void insertStudent(int stuId, String name, String email, String phone, String address, java.sql.Date dob, int depId, String gender) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Student (Stu_ID, Stu_Name, Stu_Mail_ID, Stu_PhoneNO, Stu_address, Stu_DOB, Stu_age, dep_id, gender) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, stuId);
            stmt.setString(2, name);
            stmt.setString(3, email);
            stmt.setString(4, phone);
            stmt.setString(5, address);
            stmt.setDate(6, dob);
            int age = calculateAge(new Date(dob.getTime()));
            stmt.setInt(7, age);
            stmt.setInt(8, depId);
            stmt.setString(9, gender);
            stmt.executeUpdate();
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error inserting student details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Inserts faculty details into Faculty table
    private void insertFaculty(int facId, String name, String email, String phone, int depId, String gender) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Faculty (F_ID, F_Name, F_Mail_ID, F_PhoneNO, dep_id, gender) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, facId);
            stmt.setString(2, name);
            stmt.setString(3, email);
            stmt.setString(4, phone);
            stmt.setInt(5, depId);
            stmt.setString(6, gender);
            stmt.executeUpdate();
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error inserting faculty details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SignUpPage().setVisible(true));
    }
}

// A simple formatter for JDatePicker to format dates as "yyyy-MM-dd"
class DateLabelFormatter extends javax.swing.JFormattedTextField.AbstractFormatter {
    private String datePattern = "yyyy-MM-dd";
    private SimpleDateFormat dateFormatter = new SimpleDateFormat(datePattern);
    
    @Override
    public Object stringToValue(String text) throws ParseException {
        return dateFormatter.parseObject(text);
    }
    
    @Override
    public String valueToString(Object value) throws ParseException {
        if (value != null) {
            Calendar cal = (Calendar) value;
            return dateFormatter.format(cal.getTime());
        }
        return "";
    }
}

/*import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.regex.Pattern;


public class SignUpPage extends JFrame {
    // ---------- Main Wizard Panel ----------
    private JPanel mainCardPanel;
    private CardLayout mainCardLayout;
    
    // ---------- Step 1: Account Info ----------
    private JPanel accountInfoPanel;
    private JTextField userIdField;
    private JPasswordField passwordField, confirmPasswordField;
    private JTextField phoneField;
    private JButton sendOtpButton, accountNextButton;
    
    // ---------- Step 2: OTP Verification ----------
    private JPanel otpPanel;
    private JTextField otpField;
    private JButton verifyOtpButton, otpBackButton;
    private String generatedOtp;
    private boolean isOtpVerified = false;
    
    // ---------- Step 3: Role Details ----------
    private JPanel roleDetailsPanel;
    private CardLayout roleCardLayout;
    private JComboBox<String> roleComboBox;  // Allowed values: "Student", "Faculty"
 // Student panel gender components
    private JRadioButton stuMaleRadio, stuFemaleRadio;
    // Faculty panel gender components
    private JRadioButton facMaleRadio, facFemaleRadio;
    
    // Student panel fields
    private JTextField stuNameField, stuEmailField, stuPhoneField, stuAddressField, stuDobField, stuDepField, stuAgeField;
    
    // Faculty panel fields
    private JTextField facNameField, facEmailField, facPhoneField, facDobField, facDepField, facAgeField;
    
    // Navigation buttons for Role Details
    private JButton registerButton, detailsBackButton;
    
    public SignUpPage() {
        super("Sign Up Page");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 650);
        setLocationRelativeTo(null);
        initUI();
    }
    
    private void initUI() {
        mainCardLayout = new CardLayout();
        mainCardPanel = new JPanel(mainCardLayout);
        
        mainCardPanel.add(createAccountInfoPanel(), "AccountInfo");
        mainCardPanel.add(createOtpPanel(), "OTP");
        mainCardPanel.add(createRoleDetailsPanel(), "RoleDetails");
        
        add(mainCardPanel, BorderLayout.CENTER);
    }
    
    // ---------------- Step 1: Account Information Panel ----------------
    private JPanel createAccountInfoPanel() {
        accountInfoPanel = new JPanel(new GridBagLayout());
        accountInfoPanel.setBorder(BorderFactory.createTitledBorder("Step 1: Account Information"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Font font = new Font("Georgia", Font.PLAIN, 14);
        int row = 0;
        
        // Title
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("Enter Account Information", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 18));
        accountInfoPanel.add(titleLabel, gbc);
        
        // Row 1: User ID
        row++;
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = row;
        accountInfoPanel.add(new JLabel("User ID (number):"), gbc);
        gbc.gridx = 1;
        userIdField = new JTextField(20);
        userIdField.setFont(font);
        accountInfoPanel.add(userIdField, gbc);
        
        // Row 2: Password
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        accountInfoPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        passwordField.setFont(font);
        accountInfoPanel.add(passwordField, gbc);
        
        // Row 3: Confirm Password
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        accountInfoPanel.add(new JLabel("Confirm Password:"), gbc);
        gbc.gridx = 1;
        confirmPasswordField = new JPasswordField(20);
        confirmPasswordField.setFont(font);
        accountInfoPanel.add(confirmPasswordField, gbc);
        
        // Row 4: Phone Number
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        accountInfoPanel.add(new JLabel("Phone (10 digits):"), gbc);
        gbc.gridx = 1;
        phoneField = new JTextField(15);
        phoneField.setFont(font);
        accountInfoPanel.add(phoneField, gbc);
        
        // Row 5: Buttons: Send OTP and Next
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        sendOtpButton = new JButton("Send OTP");
        accountInfoPanel.add(sendOtpButton, gbc);
        gbc.gridx = 1;
        accountNextButton = new JButton("Next");
        accountInfoPanel.add(accountNextButton, gbc);
        
        sendOtpButton.addActionListener(e -> {
            if(validateAccountInfo()) {
                sendOtp();
            }
        });
        accountNextButton.addActionListener(e -> {
            if(validateAccountInfo() && generatedOtp != null) {
                mainCardLayout.show(mainCardPanel, "OTP");
            }
        });
        
        return accountInfoPanel;
    }
    
    private boolean validateAccountInfo() {
        String uid = userIdField.getText().trim();
        String pass = new String(passwordField.getPassword()).trim();
        String confPass = new String(confirmPasswordField.getPassword()).trim();
        String phone = phoneField.getText().trim();
        if(uid.isEmpty() || pass.isEmpty() || confPass.isEmpty() || phone.isEmpty()){
            JOptionPane.showMessageDialog(this, "All account fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            Integer.parseInt(uid);
        } catch(NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "User ID must be a valid number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(pass.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters long.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(!pass.equals(confPass)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(!Pattern.matches("^[0-9]{10}$", phone)) {
            JOptionPane.showMessageDialog(this, "Phone number must be exactly 10 digits.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
    
    private void sendOtp() {
        generatedOtp = String.valueOf(100000 + new Random().nextInt(900000));
        JOptionPane.showMessageDialog(this, "OTP sent to your phone.\n(For demo, OTP is: " + generatedOtp + ")");
    }
    
    // ---------------- Step 2: OTP Verification Panel ----------------
    private JPanel createOtpPanel() {
        otpPanel = new JPanel(new GridBagLayout());
        otpPanel.setBorder(BorderFactory.createTitledBorder("Step 2: OTP Verification"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Font font = new Font("Georgia", Font.PLAIN, 14);
        int row = 0;
        
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        JLabel label = new JLabel("Enter the OTP sent to your phone", SwingConstants.CENTER);
        label.setFont(new Font("Georgia", Font.BOLD, 16));
        otpPanel.add(label, gbc);
        
        // Row 1: OTP field
        row++;
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = row;
        otpPanel.add(new JLabel("OTP:"), gbc);
        gbc.gridx = 1;
        otpField = new JTextField(10);
        otpField.setFont(font);
        otpPanel.add(otpField, gbc);
        
        // Row 2: Verify OTP and Back buttons
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        verifyOtpButton = new JButton("Verify OTP");
        otpBackButton = new JButton("Back");
        btnPanel.add(verifyOtpButton);
        btnPanel.add(otpBackButton);
        otpPanel.add(btnPanel, gbc);
        
        verifyOtpButton.addActionListener(e -> verifyOtp());
        otpBackButton.addActionListener(e -> mainCardLayout.show(mainCardPanel, "AccountInfo"));
        
        return otpPanel;
    }
    
    private void verifyOtp() {
        String enteredOtp = otpField.getText().trim();
        if(generatedOtp != null && generatedOtp.equals(enteredOtp)) {
            JOptionPane.showMessageDialog(this, "OTP Verified Successfully!");
            isOtpVerified = true;
            mainCardLayout.show(mainCardPanel, "RoleDetails");
        } else {
            JOptionPane.showMessageDialog(this, "Invalid OTP. Please try again.", "Verification Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // ---------------- Step 3: Role Details Panel ----------------
    private JPanel createRoleDetailsPanel() {
        roleDetailsPanel = new JPanel(new GridBagLayout());
        roleDetailsPanel.setBorder(BorderFactory.createTitledBorder("Step 3: Enter Role Details"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Font font = new Font("Georgia", Font.PLAIN, 14);
        int row = 0;
        
        // Role dropdown (only "Student" and "Faculty")
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        roleDetailsPanel.add(new JLabel("Select Role:"), gbc);
        gbc.gridx = 2;
        String[] roles = {"Student", "Faculty"};
        roleComboBox = new JComboBox<>(roles);
        roleComboBox.setFont(font);
        roleDetailsPanel.add(roleComboBox, gbc);
        
        
        // Sub-panel for role-specific details using CardLayout
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 4;
        JPanel subPanel = new JPanel(new CardLayout());
        subPanel.setBorder(BorderFactory.createTitledBorder("Enter Details"));
        roleCardLayout = (CardLayout) subPanel.getLayout();
        
        // ----- Student Details Panel -----
        JPanel stuPanel = new JPanel(new GridBagLayout());
        GridBagConstraints stuGbc = new GridBagConstraints();
        stuGbc.insets = new Insets(5,5,5,5);
        stuGbc.fill = GridBagConstraints.HORIZONTAL;
        int stuRow = 0;
        
        // Student Name
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Name:"), stuGbc);
        stuGbc.gridx = 1;
        stuNameField = new JTextField(20);
        stuNameField.setFont(font);
        stuPanel.add(stuNameField, stuGbc);
        
     // Student Gender
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Gender:"), stuGbc);
        stuGbc.gridx = 1;
        JPanel stuGenderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        stuMaleRadio = new JRadioButton("Male");
        stuFemaleRadio = new JRadioButton("Female");
        ButtonGroup stuGenderGroup = new ButtonGroup();
        stuGenderGroup.add(stuMaleRadio);
        stuGenderGroup.add(stuFemaleRadio);
        stuGenderPanel.add(stuMaleRadio);
        stuGenderPanel.add(stuFemaleRadio);
        stuPanel.add(stuGenderPanel, stuGbc);
        
        // Student Email
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Email:"), stuGbc);
        stuGbc.gridx = 1;
        stuEmailField = new JTextField(20);
        stuEmailField.setFont(font);
        stuPanel.add(stuEmailField, stuGbc);
        
        // Student Phone
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Phone (10 digits):"), stuGbc);
        stuGbc.gridx = 1;
        stuPhoneField = new JTextField(15);
        stuPhoneField.setFont(font);
        stuPhoneField.setText(phoneField.getText().trim());
        stuPanel.add(stuPhoneField, stuGbc);
        
        // Student Address
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Address:"), stuGbc);
        stuGbc.gridx = 1;
        stuAddressField = new JTextField(20);
        stuAddressField.setFont(font);
        stuPanel.add(stuAddressField, stuGbc);
        
        // Student DOB
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("DOB (yyyy-MM-dd):"), stuGbc);
        stuGbc.gridx = 1;
        stuDobField = new JTextField(10);
        stuDobField.setFont(font);
        stuPanel.add(stuDobField, stuGbc);
        
        // Student Age (read-only)
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Age:"), stuGbc);
        stuGbc.gridx = 1;
        stuAgeField = new JTextField(5);
        stuAgeField.setFont(font);
        stuAgeField.setEditable(false);
        stuPanel.add(stuAgeField, stuGbc);
        
        // Button to calculate Student Age
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        JButton calcStuAgeButton = new JButton("Calculate Age");
        stuPanel.add(calcStuAgeButton, stuGbc);
        calcStuAgeButton.addActionListener(e -> {
            String dobStr = stuDobField.getText().trim();
            if(dobStr.isEmpty()){
                JOptionPane.showMessageDialog(this, "Please enter DOB.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Date parsedDate = new SimpleDateFormat("yyyy-MM-dd").parse(dobStr);
                int age = calculateAge(parsedDate);
                stuAgeField.setText(String.valueOf(age));
            } catch(ParseException ex) {
                JOptionPane.showMessageDialog(this, "DOB must be in yyyy-MM-dd format.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // Student Department ID
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Department ID:"), stuGbc);
        stuGbc.gridx = 1;
        stuDepField = new JTextField(10);
        stuDepField.setFont(font);
        stuPanel.add(stuDepField, stuGbc);
        
        subPanel.add(stuPanel, "Student");
        
        // ----- Faculty Details Panel -----
        JPanel facPanel = new JPanel(new GridBagLayout());
        GridBagConstraints facGbc = new GridBagConstraints();
        facGbc.insets = new Insets(5,5,5,5);
        facGbc.fill = GridBagConstraints.HORIZONTAL;
        int facRow = 0;
        
        // Faculty Name
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Name:"), facGbc);
        facGbc.gridx = 1;
        facNameField = new JTextField(20);
        facNameField.setFont(font);
        facPanel.add(facNameField, facGbc);
        
     // Faculty Gender
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Gender:"), facGbc);
        facGbc.gridx = 1;
        JPanel facGenderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        facMaleRadio = new JRadioButton("Male");
        facFemaleRadio = new JRadioButton("Female");
        ButtonGroup facGenderGroup = new ButtonGroup();
        facGenderGroup.add(facMaleRadio);
        facGenderGroup.add(facFemaleRadio);
        facGenderPanel.add(facMaleRadio);
        facGenderPanel.add(facFemaleRadio);
        facPanel.add(facGenderPanel, facGbc);
        
        // Faculty Email
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Email:"), facGbc);
        facGbc.gridx = 1;
        facEmailField = new JTextField(20);
        facEmailField.setFont(font);
        facPanel.add(facEmailField, facGbc);
        
        // Faculty Phone
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Phone (10 digits):"), facGbc);
        facGbc.gridx = 1;
        facPhoneField = new JTextField(15);
        facPhoneField.setFont(font);
        facPanel.add(facPhoneField, facGbc);
        
        // Faculty DOB
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("DOB (yyyy-MM-dd):"), facGbc);
        facGbc.gridx = 1;
        facDobField = new JTextField(10);
        facDobField.setFont(font);
        facPanel.add(facDobField, facGbc);
        
        // Faculty Age (read-only)
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Age:"), facGbc);
        facGbc.gridx = 1;
        facAgeField = new JTextField(5);
        facAgeField.setFont(font);
        facAgeField.setEditable(false);
        facPanel.add(facAgeField, facGbc);
        
        // Button to calculate Faculty Age
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        JButton calcFacAgeButton = new JButton("Calculate Age");
        facPanel.add(calcFacAgeButton, facGbc);
        calcFacAgeButton.addActionListener(e -> {
            String dobStr = facDobField.getText().trim();
            if(dobStr.isEmpty()){
                JOptionPane.showMessageDialog(this, "Please enter DOB.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Date parsedDate = new SimpleDateFormat("yyyy-MM-dd").parse(dobStr);
                int age = calculateAge(parsedDate);
                facAgeField.setText(String.valueOf(age));
            } catch(ParseException ex) {
                JOptionPane.showMessageDialog(this, "DOB must be in yyyy-MM-dd format.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // Faculty Department ID
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Department ID:"), facGbc);
        facGbc.gridx = 1;
        facDepField = new JTextField(10);
        facDepField.setFont(font);
        facPanel.add(facDepField, facGbc);
        
        subPanel.add(facPanel, "Faculty");
        
        gbc.gridy++;
        gbc.gridx = 0; gbc.gridwidth = 4;
        roleDetailsPanel.add(subPanel, gbc);
        
        // Row for Register and Back buttons in Role Details Panel
        gbc.gridy++;
        gbc.gridx = 0; gbc.gridwidth = 4;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        registerButton = new JButton("Register");
        detailsBackButton = new JButton("Back");
        btnPanel.add(registerButton);
        btnPanel.add(detailsBackButton);
        roleDetailsPanel.add(btnPanel, gbc);
        
        registerButton.addActionListener(e -> registerUser());
        detailsBackButton.addActionListener(e -> mainCardLayout.show(mainCardPanel, "OTP"));
        
        roleComboBox.addActionListener(e -> {
            String selectedRole = (String) roleComboBox.getSelectedItem();
            roleCardLayout.show(subPanel, selectedRole);
        });
        roleCardLayout = (CardLayout) subPanel.getLayout();
        roleCardLayout.show(subPanel, "Student");
        
        return roleDetailsPanel;
    }
    
    // Calculate age from a java.util.Date
    private int calculateAge(Date dob) {
        Calendar dobCal = Calendar.getInstance();
        dobCal.setTime(dob);
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR);
        if(today.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age;
    }
    
    // ---------------- Registration and DB Insertion ----------------
    private void registerUser() {
        // Validate common fields
        String userIdStr = userIdField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String role = (String) roleComboBox.getSelectedItem();
        if(userIdStr.isEmpty() || password.isEmpty() || role.isEmpty()){
            JOptionPane.showMessageDialog(this, "User ID, Password, and Role are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int userId;
        try {
            userId = Integer.parseInt(userIdStr);
        } catch(NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "User ID must be a valid number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if(password.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters long.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if(role.equalsIgnoreCase("Student")) {
            String stuName = stuNameField.getText().trim();
            String stuEmail = stuEmailField.getText().trim();
            String stuPhone = stuPhoneField.getText().trim();
            String stuAddress = stuAddressField.getText().trim();
            String stuDobStr = stuDobField.getText().trim();
            String stuDep = stuDepField.getText().trim();
            if(stuName.isEmpty() || stuEmail.isEmpty() || stuPhone.isEmpty() ||
               stuAddress.isEmpty() || stuDobStr.isEmpty() || stuDep.isEmpty()){
                JOptionPane.showMessageDialog(this, "All Student fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Validate gender
            String stuGender = null;
            if(stuMaleRadio.isSelected()) {
                stuGender = "Male";
            } else if(stuFemaleRadio.isSelected()) {
                stuGender = "Female";
            }
            if(stuGender == null) {
                JOptionPane.showMessageDialog(this, "Please select gender.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if(!Pattern.matches("^[0-9]{10}$", stuPhone)) {
                JOptionPane.showMessageDialog(this, "Student phone must be exactly 10 digits.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[\\w.-]+@[\\w.-]+\\.com$", stuEmail)) {
                JOptionPane.showMessageDialog(this, "Student email must be valid and end with '.com'.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            java.sql.Date stuDob;
            try {
                Date parsedDate = new SimpleDateFormat("yyyy-MM-dd").parse(stuDobStr);
                stuDob = new java.sql.Date(parsedDate.getTime());
            } catch(ParseException ex) {
                JOptionPane.showMessageDialog(this, "DOB must be in format yyyy-MM-dd.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int age = calculateAge(new Date(stuDob.getTime()));
            if(age < 18) {
                JOptionPane.showMessageDialog(this, "Student must be at least 18 years old.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Integer.parseInt(stuDep);
            } catch(NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Department ID must be a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(isPhoneRegistered("Student", stuPhone)) {
                JOptionPane.showMessageDialog(this, "This phone number is already registered for a student.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            registerToDB(userId, password, role);
            insertStudent(userId, stuName, stuEmail, stuPhone, stuAddress, stuDob, Integer.parseInt(stuDep), stuGender);
            
        } else if(role.equalsIgnoreCase("Faculty")) {
            String facName = facNameField.getText().trim();
            String facEmail = facEmailField.getText().trim();
            String facPhone = facPhoneField.getText().trim();
            String facDobStr = facDobField.getText().trim();
            String facDep = facDepField.getText().trim();
            if(facName.isEmpty() || facEmail.isEmpty() || facPhone.isEmpty() ||
               facDobStr.isEmpty() || facDep.isEmpty()){
                JOptionPane.showMessageDialog(this, "All Faculty fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[0-9]{10}$", facPhone)) {
                JOptionPane.showMessageDialog(this, "Faculty phone must be exactly 10 digits.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[\\w.-]+@[\\w.-]+\\.com$", facEmail)) {
                JOptionPane.showMessageDialog(this, "Faculty email must be valid and end with '.com'.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String facGender = null;
            if(facMaleRadio.isSelected()) {
                facGender = "Male";
            } else if(facFemaleRadio.isSelected()) {
                facGender = "Female";
            }
            if(facGender == null) {
                JOptionPane.showMessageDialog(this, "Please select gender.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            java.sql.Date facDob;
            try {
                Date parsedDate = new SimpleDateFormat("yyyy-MM-dd").parse(facDobStr);
                facDob = new java.sql.Date(parsedDate.getTime());
            } catch(ParseException ex) {
                JOptionPane.showMessageDialog(this, "DOB must be in format yyyy-MM-dd.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int age = calculateAge(new Date(facDob.getTime()));
            if(age < 26) {
                JOptionPane.showMessageDialog(this, "Faculty must be at least 26 years old.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Integer.parseInt(facDep);
            } catch(NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Department ID must be a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(isPhoneRegistered("Faculty", facPhone)) {
                JOptionPane.showMessageDialog(this, "This phone number is already registered for a faculty member.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            registerToDB(userId, password, role);
            insertFaculty(userId, facName, facEmail, facPhone, Integer.parseInt(facDep),facGender);
        }
        
        JOptionPane.showMessageDialog(this, "Registration successful!");
        new LoginPage().setVisible(true);
        dispose();
    }
    
    // Checks if the phone is already registered in the appropriate table
    private boolean isPhoneRegistered(String role, String phone) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "";
            if(role.equalsIgnoreCase("Student")) {
                query = "SELECT Stu_PhoneNO FROM Student WHERE Stu_PhoneNO = ?";
            } else if(role.equalsIgnoreCase("Faculty")) {
                query = "SELECT F_PhoneNO FROM Faculty WHERE F_PhoneNO = ?";
            }
            if(query.isEmpty()) return false;
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, phone);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch(SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }
    
    // Inserts login details into Login_Table
    private void registerToDB(int userId, String password, String role) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Login_Table (Login_ID, U_Password, U_Role) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setString(2, password);
            stmt.setString(3, role);
            stmt.executeUpdate();
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error registering login details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Inserts student details into Student table (Stu_age is calculated)
    private void insertStudent(int stuId, String name, String email, String phone, String address, java.sql.Date dob, int depId, String gender) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Student (Stu_ID, Stu_Name, Stu_Mail_ID, Stu_PhoneNO, Stu_address, Stu_DOB, Stu_age, dep_id, gender) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, stuId);
            stmt.setString(2, name);
            stmt.setString(3, email);
            stmt.setString(4, phone);
            stmt.setString(5, address);
            stmt.setDate(6, dob);
            int age = calculateAge(new Date(dob.getTime()));
            stmt.setInt(7, age);
            stmt.setInt(8, depId);
            stmt.setString(9, gender);
            stmt.executeUpdate();
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error inserting student details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Inserts faculty details into Faculty table
    private void insertFaculty(int facId, String name, String email, String phone, int depId, String gender) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Faculty (F_ID, F_Name, F_Mail_ID, F_PhoneNO, dep_id, gender) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, facId);
            stmt.setString(2, name);
            stmt.setString(3, email);
            stmt.setString(4, phone);
            stmt.setInt(5, depId);
            stmt.setString(6, gender);
            stmt.executeUpdate();
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error inserting faculty details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Calculates age from a java.util.Date
    private int calculateAge1(Date dob) {
        Calendar dobCal = Calendar.getInstance();
        dobCal.setTime(dob);
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR);
        if(today.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age;
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SignUpPage().setVisible(true));
    }
}

/*import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.regex.Pattern;

public class SignUpPage extends JFrame {
    // ---------- Main Wizard Panel ----------
    private JPanel mainCardPanel;
    private CardLayout mainCardLayout;
    
    // ---------- Step 1: Account Info ----------
    private JPanel accountInfoPanel;
    private JTextField userIdField;
    private JPasswordField passwordField, confirmPasswordField;
    private JTextField phoneField;
    private JButton sendOtpButton, accountNextButton;
    
    // ---------- Step 2: OTP Verification ----------
    private JPanel otpPanel;
    private JTextField otpField;
    private JButton verifyOtpButton, otpBackButton;
    private String generatedOtp;
    private boolean isOtpVerified = false;
    
    // ---------- Step 3: Role Details ----------
    private JPanel roleDetailsPanel;
    private CardLayout roleCardLayout;
    private JComboBox<String> roleComboBox;  // allowed values: "Student", "Faculty"
    // Student fields
    private JTextField stuNameField, stuEmailField, stuPhoneField, stuAddressField, stuDobField, stuDepField, stuAgeField;
    // Faculty fields
    private JTextField facNameField, facEmailField, facPhoneField, facDobField, facDepField, facAgeField;
    // Navigation buttons for Role Details Panel
    private JButton registerButton, detailsBackButton;
    
    public SignUpPage() {
        super("Sign Up Page");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 650);
        setLocationRelativeTo(null);
        initUI();
    }
    
    private void initUI() {
        mainCardLayout = new CardLayout();
        mainCardPanel = new JPanel(mainCardLayout);
        
        mainCardPanel.add(createAccountInfoPanel(), "AccountInfo");
        mainCardPanel.add(createOtpPanel(), "OTP");
        mainCardPanel.add(createRoleDetailsPanel(), "RoleDetails");
        
        add(mainCardPanel, BorderLayout.CENTER);
    }
    
    // ---------------- Step 1: Account Information Panel ----------------
    private JPanel createAccountInfoPanel() {
        accountInfoPanel = new JPanel(new GridBagLayout());
        accountInfoPanel.setBorder(BorderFactory.createTitledBorder("Step 1: Account Information"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Font font = new Font("Georgia", Font.PLAIN, 14);
        int row = 0;
        
        // Title
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("Enter Account Information", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 18));
        accountInfoPanel.add(titleLabel, gbc);
        
        // Row 1: User ID
        row++;
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = row;
        accountInfoPanel.add(new JLabel("User ID (number):"), gbc);
        gbc.gridx = 1;
        userIdField = new JTextField(20);
        userIdField.setFont(font);
        accountInfoPanel.add(userIdField, gbc);
        
        // Row 2: Password
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        accountInfoPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        passwordField.setFont(font);
        accountInfoPanel.add(passwordField, gbc);
        
        // Row 3: Confirm Password
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        accountInfoPanel.add(new JLabel("Confirm Password:"), gbc);
        gbc.gridx = 1;
        confirmPasswordField = new JPasswordField(20);
        confirmPasswordField.setFont(font);
        accountInfoPanel.add(confirmPasswordField, gbc);
        
        // Row 4: Phone Number
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        accountInfoPanel.add(new JLabel("Phone (10 digits):"), gbc);
        gbc.gridx = 1;
        phoneField = new JTextField(15);
        phoneField.setFont(font);
        accountInfoPanel.add(phoneField, gbc);
        
        // Row 5: Buttons: Send OTP and Next
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        sendOtpButton = new JButton("Send OTP");
        accountInfoPanel.add(sendOtpButton, gbc);
        gbc.gridx = 1;
        accountNextButton = new JButton("Next");
        accountInfoPanel.add(accountNextButton, gbc);
        
        sendOtpButton.addActionListener(e -> {
            if(validateAccountInfo()) {
                sendOtp();
            }
        });
        accountNextButton.addActionListener(e -> {
            if(validateAccountInfo() && generatedOtp != null) {
                mainCardLayout.show(mainCardPanel, "OTP");
            }
        });
        
        return accountInfoPanel;
    }
    
    private boolean validateAccountInfo() {
        String uid = userIdField.getText().trim();
        String pass = new String(passwordField.getPassword()).trim();
        String confPass = new String(confirmPasswordField.getPassword()).trim();
        String phone = phoneField.getText().trim();
        if(uid.isEmpty() || pass.isEmpty() || confPass.isEmpty() || phone.isEmpty()){
            JOptionPane.showMessageDialog(this, "All account fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            Integer.parseInt(uid);
        } catch(NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "User ID must be a valid number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(pass.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters long.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(!pass.equals(confPass)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(!Pattern.matches("^[0-9]{10}$", phone)) {
            JOptionPane.showMessageDialog(this, "Phone number must be exactly 10 digits.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
    
    private void sendOtp() {
        generatedOtp = String.valueOf(100000 + new Random().nextInt(900000));
        JOptionPane.showMessageDialog(this, "OTP sent to your phone.\n(For demo, OTP is: " + generatedOtp + ")");
    }
    
    // ---------------- Step 2: OTP Verification Panel ----------------
    private JPanel createOtpPanel() {
        otpPanel = new JPanel(new GridBagLayout());
        otpPanel.setBorder(BorderFactory.createTitledBorder("Step 2: OTP Verification"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Font font = new Font("Georgia", Font.PLAIN, 14);
        int row = 0;
        
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        JLabel label = new JLabel("Enter the OTP sent to your phone", SwingConstants.CENTER);
        label.setFont(new Font("Georgia", Font.BOLD, 16));
        otpPanel.add(label, gbc);
        
        // Row 1: OTP field
        row++;
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = row;
        otpPanel.add(new JLabel("OTP:"), gbc);
        gbc.gridx = 1;
        otpField = new JTextField(10);
        otpField.setFont(font);
        otpPanel.add(otpField, gbc);
        
        // Row 2: Verify OTP and Back buttons
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        verifyOtpButton = new JButton("Verify OTP");
        otpBackButton = new JButton("Back");
        btnPanel.add(verifyOtpButton);
        btnPanel.add(otpBackButton);
        otpPanel.add(btnPanel, gbc);
        
        verifyOtpButton.addActionListener(e -> verifyOtp());
        otpBackButton.addActionListener(e -> mainCardLayout.show(mainCardPanel, "AccountInfo"));
        
        return otpPanel;
    }
    
    private void verifyOtp() {
        String enteredOtp = otpField.getText().trim();
        if(generatedOtp != null && generatedOtp.equals(enteredOtp)) {
            JOptionPane.showMessageDialog(this, "OTP Verified Successfully!");
            isOtpVerified = true;
            mainCardLayout.show(mainCardPanel, "RoleDetails");
        } else {
            JOptionPane.showMessageDialog(this, "Invalid OTP. Please try again.", "Verification Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // ---------------- Step 3: Role Details Panel ----------------
    private JPanel createRoleDetailsPanel() {
        roleDetailsPanel = new JPanel(new GridBagLayout());
        roleDetailsPanel.setBorder(BorderFactory.createTitledBorder("Step 3: Enter Role Details"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Font font = new Font("Georgia", Font.PLAIN, 14);
        int row = 0;
        
        // Role dropdown (only "Student" and "Faculty")
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        roleDetailsPanel.add(new JLabel("Select Role:"), gbc);
        gbc.gridx = 2;
        String[] roles = {"Student", "Faculty"};
        roleComboBox = new JComboBox<>(roles);
        roleComboBox.setFont(font);
        roleDetailsPanel.add(roleComboBox, gbc);
        
        // Sub-panel for role-specific details using CardLayout
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 4;
        JPanel subPanel = new JPanel(new CardLayout());
        subPanel.setBorder(BorderFactory.createTitledBorder("Enter Details"));
        roleCardLayout = (CardLayout) subPanel.getLayout();
        
        // ----- Student Details Panel -----
        JPanel stuPanel = new JPanel(new GridBagLayout());
        GridBagConstraints stuGbc = new GridBagConstraints();
        stuGbc.insets = new Insets(5,5,5,5);
        stuGbc.fill = GridBagConstraints.HORIZONTAL;
        int stuRow = 0;
        
        // Student Name
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Name:"), stuGbc);
        stuGbc.gridx = 1;
        stuNameField = new JTextField(20);
        stuNameField.setFont(font);
        stuPanel.add(stuNameField, stuGbc);
        
        // Student Email
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Email:"), stuGbc);
        stuGbc.gridx = 1;
        stuEmailField = new JTextField(20);
        stuEmailField.setFont(font);
        stuPanel.add(stuEmailField, stuGbc);
        
        // Student Phone
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Phone (10 digits):"), stuGbc);
        stuGbc.gridx = 1;
        stuPhoneField = new JTextField(15);
        stuPhoneField.setFont(font);
        // Optionally pre-fill from account info phoneField
        stuPhoneField.setText(phoneField.getText().trim());
        stuPanel.add(stuPhoneField, stuGbc);
        
        // Student Address
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Address:"), stuGbc);
        stuGbc.gridx = 1;
        stuAddressField = new JTextField(20);
        stuAddressField.setFont(font);
        stuPanel.add(stuAddressField, stuGbc);
        
        // Student DOB
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("DOB (yyyy-MM-dd):"), stuGbc);
        stuGbc.gridx = 1;
        stuDobField = new JTextField(10);
        stuDobField.setFont(font);
        stuPanel.add(stuDobField, stuGbc);
        
        // Student Age (read-only)
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Age:"), stuGbc);
        stuGbc.gridx = 1;
        stuAgeField = new JTextField(5);
        stuAgeField.setFont(font);
        stuAgeField.setEditable(false);
        stuPanel.add(stuAgeField, stuGbc);
        
        // Button to calculate Student Age
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        JButton calcStuAgeButton = new JButton("Calculate Age");
        stuPanel.add(calcStuAgeButton, stuGbc);
        calcStuAgeButton.addActionListener(e -> {
            String dobStr = stuDobField.getText().trim();
            if(dobStr.isEmpty()){
                JOptionPane.showMessageDialog(this, "Please enter DOB.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Date parsedDate = new SimpleDateFormat("yyyy-MM-dd").parse(dobStr);
                int age = calculateAge(parsedDate);
                stuAgeField.setText(String.valueOf(age));
            } catch(ParseException ex) {
                JOptionPane.showMessageDialog(this, "DOB must be in yyyy-MM-dd format.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // Student Department ID
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Department ID:"), stuGbc);
        stuGbc.gridx = 1;
        stuDepField = new JTextField(10);
        stuDepField.setFont(font);
        stuPanel.add(stuDepField, stuGbc);
        
        subPanel.add(stuPanel, "Student");
        
        // ----- Faculty Details Panel -----
        JPanel facPanel = new JPanel(new GridBagLayout());
        GridBagConstraints facGbc = new GridBagConstraints();
        facGbc.insets = new Insets(5,5,5,5);
        facGbc.fill = GridBagConstraints.HORIZONTAL;
        int facRow = 0;
        
        // Faculty Name
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Name:"), facGbc);
        facGbc.gridx = 1;
        facNameField = new JTextField(20);
        facNameField.setFont(font);
        facPanel.add(facNameField, facGbc);
        
        // Faculty Email
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Email:"), facGbc);
        facGbc.gridx = 1;
        facEmailField = new JTextField(20);
        facEmailField.setFont(font);
        facPanel.add(facEmailField, facGbc);
        
        // Faculty Phone
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Phone (10 digits):"), facGbc);
        facGbc.gridx = 1;
        facPhoneField = new JTextField(15);
        facPhoneField.setFont(font);
        facPanel.add(facPhoneField, facGbc);
        
        // Faculty DOB
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("DOB (yyyy-MM-dd):"), facGbc);
        facGbc.gridx = 1;
        facDobField = new JTextField(10);
        facDobField.setFont(font);
        facPanel.add(facDobField, facGbc);
        
        // Faculty Age (read-only)
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Age:"), facGbc);
        facGbc.gridx = 1;
        facAgeField = new JTextField(5);
        facAgeField.setFont(font);
        facAgeField.setEditable(false);
        facPanel.add(facAgeField, facGbc);
        
        // Button to calculate Faculty Age
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        JButton calcFacAgeButton = new JButton("Calculate Age");
        facPanel.add(calcFacAgeButton, facGbc);
        calcFacAgeButton.addActionListener(e -> {
            String dobStr = facDobField.getText().trim();
            if(dobStr.isEmpty()){
                JOptionPane.showMessageDialog(this, "Please enter DOB.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Date parsedDate = new SimpleDateFormat("yyyy-MM-dd").parse(dobStr);
                int age = calculateAge(parsedDate);
                facAgeField.setText(String.valueOf(age));
            } catch(ParseException ex) {
                JOptionPane.showMessageDialog(this, "DOB must be in yyyy-MM-dd format.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // Faculty Department ID
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Department ID:"), facGbc);
        facGbc.gridx = 1;
        facDepField = new JTextField(10);
        facDepField.setFont(font);
        facPanel.add(facDepField, facGbc);
        
        subPanel.add(facPanel, "Faculty");
        
        // Add the sub-panel to Role Details Panel
        gbc.gridy++;
        gbc.gridx = 0; gbc.gridwidth = 4;
        roleDetailsPanel.add(subPanel, gbc);
        
        // Row for Register and Back buttons in Role Details Panel
        gbc.gridy++;
        gbc.gridx = 0; gbc.gridwidth = 4;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        registerButton = new JButton("Register");
        detailsBackButton = new JButton("Back");
        btnPanel.add(registerButton);
        btnPanel.add(detailsBackButton);
        roleDetailsPanel.add(btnPanel, gbc);
        
        // Action listeners for Role Details Panel
        registerButton.addActionListener(e -> registerUser());
        detailsBackButton.addActionListener(e -> mainCardLayout.show(mainCardPanel, "OTP"));
        
        roleComboBox.addActionListener(e -> {
            String selectedRole = (String) roleComboBox.getSelectedItem();
            roleCardLayout.show(subPanel, selectedRole);
        });
        // Default to showing Student details
        roleCardLayout = (CardLayout) subPanel.getLayout();
        roleCardLayout.show(subPanel, "Student");
        
        return roleDetailsPanel;
    }
    
    // Calculate age from a java.util.Date
    private int calculateAge(Date dob) {
        Calendar dobCal = Calendar.getInstance();
        dobCal.setTime(dob);
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR);
        if(today.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age;
    }
    
    // ---------------- Registration and DB Insertion ----------------
    private void registerUser() {
        // Validate common fields
        String userIdStr = userIdField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String role = (String) roleComboBox.getSelectedItem();
        if(userIdStr.isEmpty() || password.isEmpty() || role.isEmpty()){
            JOptionPane.showMessageDialog(this, "User ID, Password, and Role are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int userId;
        try {
            userId = Integer.parseInt(userIdStr);
        } catch(NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "User ID must be a valid number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if(password.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters long.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Based on role, validate fields and insert data into respective tables
        if(role.equalsIgnoreCase("Student")) {
            String stuName = stuNameField.getText().trim();
            String stuEmail = stuEmailField.getText().trim();
            String stuPhone = stuPhoneField.getText().trim();
            String stuAddress = stuAddressField.getText().trim();
            String stuDobStr = stuDobField.getText().trim();
            String stuDep = stuDepField.getText().trim();
            
            if(stuName.isEmpty() || stuEmail.isEmpty() || stuPhone.isEmpty() ||
               stuAddress.isEmpty() || stuDobStr.isEmpty() || stuDep.isEmpty()){
                JOptionPane.showMessageDialog(this, "All Student fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[0-9]{10}$", stuPhone)) {
                JOptionPane.showMessageDialog(this, "Student phone must be exactly 10 digits.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[\\w.-]+@[\\w.-]+\\.com$", stuEmail)) {
                JOptionPane.showMessageDialog(this, "Student email must be valid and end with '.com'.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            java.sql.Date stuDob;
            try {
                Date parsedDate = new SimpleDateFormat("yyyy-MM-dd").parse(stuDobStr);
                stuDob = new java.sql.Date(parsedDate.getTime());
            } catch(ParseException ex) {
                JOptionPane.showMessageDialog(this, "DOB must be in format yyyy-MM-dd.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int age = calculateAge(new Date(stuDob.getTime()));
            if(age < 18) {
                JOptionPane.showMessageDialog(this, "Student must be at least 18 years old.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Integer.parseInt(stuDep);
            } catch(NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Department ID must be a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(isPhoneRegistered("Student", stuPhone)) {
                JOptionPane.showMessageDialog(this, "This phone number is already registered for a student.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            registerToDB(userId, password, role);
            insertStudent(userId, stuName, stuEmail, stuPhone, stuAddress, stuDob, Integer.parseInt(stuDep));
            
        } else if(role.equalsIgnoreCase("Faculty")) {
            String facName = facNameField.getText().trim();
            String facEmail = facEmailField.getText().trim();
            String facPhone = facPhoneField.getText().trim();
            String facDobStr = facDobField.getText().trim();
            String facDep = facDepField.getText().trim();
            if(facName.isEmpty() || facEmail.isEmpty() || facPhone.isEmpty() ||
               facDobStr.isEmpty() || facDep.isEmpty()){
                JOptionPane.showMessageDialog(this, "All Faculty fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[0-9]{10}$", facPhone)) {
                JOptionPane.showMessageDialog(this, "Faculty phone must be exactly 10 digits.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[\\w.-]+@[\\w.-]+\\.com$", facEmail)) {
                JOptionPane.showMessageDialog(this, "Faculty email must be valid and end with '.com'.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            java.sql.Date facDob;
            try {
                Date parsedDate = new SimpleDateFormat("yyyy-MM-dd").parse(facDobStr);
                facDob = new java.sql.Date(parsedDate.getTime());
            } catch(ParseException ex) {
                JOptionPane.showMessageDialog(this, "DOB must be in format yyyy-MM-dd.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int age = calculateAge(new Date(facDob.getTime()));
            if(age < 26) {
                JOptionPane.showMessageDialog(this, "Faculty must be at least 26 years old.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Integer.parseInt(facDep);
            } catch(NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Department ID must be a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(isPhoneRegistered("Faculty", facPhone)) {
                JOptionPane.showMessageDialog(this, "This phone number is already registered for a faculty member.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            registerToDB(userId, password, role);
            insertFaculty(userId, facName, facEmail, facPhone, Integer.parseInt(facDep));
        }
        
        JOptionPane.showMessageDialog(this, "Registration successful!");
        new LoginPage().setVisible(true);
        dispose();
    }
    
    // Checks if the phone number is already registered in the corresponding table
    private boolean isPhoneRegistered(String role, String phone) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "";
            if(role.equalsIgnoreCase("Student")) {
                query = "SELECT Stu_PhoneNO FROM Student WHERE Stu_PhoneNO = ?";
            } else if(role.equalsIgnoreCase("Faculty")) {
                query = "SELECT F_PhoneNO FROM Faculty WHERE F_PhoneNO = ?";
            }
            if(query.isEmpty()) return false;
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, phone);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch(SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }
    
    // Inserts common login details into Login_Table
    private void registerToDB(int userId, String password, String role) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Login_Table (Login_ID, U_Password, Role) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setString(2, password);
            stmt.setString(3, role);
            stmt.executeUpdate();
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error registering login details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Inserts student details into Student table (including age)
    private void insertStudent(int stuId, String name, String email, String phone, String address, java.sql.Date dob, int depId) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Student (Stu_ID, Stu_Name, Stu_Mail_ID, Stu_PhoneNO, Stu_address, Stu_DOB, Stu_age, dep_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, stuId);
            stmt.setString(2, name);
            stmt.setString(3, email);
            stmt.setString(4, phone);
            stmt.setString(5, address);
            stmt.setDate(6, dob);
            int age = calculateAge(new Date(dob.getTime()));
            stmt.setInt(7, age);
            stmt.setInt(8, depId);
            stmt.executeUpdate();
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error inserting student details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Inserts faculty details into Faculty table
    private void insertFaculty(int facId, String name, String email, String phone, int depId) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Faculty (F_ID, F_Name, F_Mail_ID, F_PhoneNO, dep_id) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, facId);
            stmt.setString(2, name);
            stmt.setString(3, email);
            stmt.setString(4, phone);
            stmt.setInt(5, depId);
            stmt.executeUpdate();
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error inserting faculty details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Calculate age from a java.util.Date
    private int calculateAge1(Date dob) {
        Calendar dobCal = Calendar.getInstance();
        dobCal.setTime(dob);
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR);
        if(today.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age;
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SignUpPage().setVisible(true));
    }
}


/*import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.regex.Pattern;

public class SignUpPage extends JFrame {
    // ---------- Step 1: Account Info Fields ----------
    private JTextField userIdField;
    private JPasswordField passwordField, confirmPasswordField;
    private JTextField phoneField; // Common phone number for OTP
    private JButton sendOtpButton, nextToOtpButton;
    
    // ---------- Step 2: OTP Verification ----------
    private JTextField otpField;
    private JButton verifyOtpButton, otpBackButton;
    private String generatedOtp;
    private boolean isOtpVerified = false;
    
    // ---------- Step 3: Role Details Panel ----------
    // Role dropdown: only "Student" and "Faculty"
    private JComboBox<String> roleComboBox;
    // Container panel with CardLayout for role-specific details
    private JPanel roleDetailsPanel;
    private CardLayout roleCardLayout;
    
    // Student fields
    private JTextField stuNameField, stuEmailField, stuPhoneField, stuAddressField, stuDobField, stuDepField, stuAgeField;
    
    // Faculty fields
    private JTextField facNameField, facEmailField, facPhoneField, facDobField, facDepField, facAgeField;
    
    // Navigation buttons for the wizard steps
    private JButton accountNextButton, otpNextButton, detailsBackButton, registerButton, finalBackButton;
    
    // Main card panel for the three steps
    private JPanel mainCardPanel;
    private CardLayout mainCardLayout;
    
    public SignUpPage() {
        super("Sign Up Page");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 650);
        setLocationRelativeTo(null);
        initUI();
    }
    
    private void initUI() {
        // Use mainCardPanel to hold the three steps
        mainCardLayout = new CardLayout();
        mainCardPanel = new JPanel(mainCardLayout);
        
        mainCardPanel.add(createAccountInfoPanel(), "AccountInfo");
        mainCardPanel.add(createOtpPanel(), "OTP");
        mainCardPanel.add(createRoleDetailsPanel(), "RoleDetails");
        
        add(mainCardPanel, BorderLayout.CENTER);
    }
    
    // ----------- Step 1: Account Info Panel -----------
    private JPanel createAccountInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Step 1: Account Information"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Font font = new Font("Georgia", Font.PLAIN, 14);
        int row = 0;
        
        // Title
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("Enter Account Information", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 18));
        panel.add(titleLabel, gbc);
        
        // Row 1: User ID
        row++;
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("User ID (number):"), gbc);
        gbc.gridx = 1;
        userIdField = new JTextField(20);
        userIdField.setFont(font);
        panel.add(userIdField, gbc);
        
        // Row 2: Password
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        passwordField.setFont(font);
        panel.add(passwordField, gbc);
        
        // Row 3: Confirm Password
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Confirm Password:"), gbc);
        gbc.gridx = 1;
        confirmPasswordField = new JPasswordField(20);
        confirmPasswordField.setFont(font);
        panel.add(confirmPasswordField, gbc);
        
        // Row 4: Phone Number for OTP
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Phone (10 digits):"), gbc);
        gbc.gridx = 1;
        phoneField = new JTextField(15);
        phoneField.setFont(font);
        panel.add(phoneField, gbc);
        
        // Row 5: Send OTP button and Next button to OTP step
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        sendOtpButton = new JButton("Send OTP");
        panel.add(sendOtpButton, gbc);
        gbc.gridx = 1;
        nextToOtpButton = new JButton("Next");
        panel.add(nextToOtpButton, gbc);
        
        // Action listeners for account info step
        sendOtpButton.addActionListener(e -> {
            if(validateAccountInfo()) {
                sendOtp();
            }
        });
        
        nextToOtpButton.addActionListener(e -> {
            if(validateAccountInfo() && generatedOtp != null) {
                mainCardLayout.show(mainCardPanel, "OTP");
            }
        });
        
        return panel;
    }
    
    // Validate Account Info fields
    private boolean validateAccountInfo() {
        String uid = userIdField.getText().trim();
        String pass = new String(passwordField.getPassword()).trim();
        String confPass = new String(confirmPasswordField.getPassword()).trim();
        String phone = phoneField.getText().trim();
        if(uid.isEmpty() || pass.isEmpty() || confPass.isEmpty() || phone.isEmpty()){
            JOptionPane.showMessageDialog(this, "All account fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            Integer.parseInt(uid);
        } catch(NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "User ID must be a valid number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(pass.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters long.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(!pass.equals(confPass)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(!Pattern.matches("^[0-9]{10}$", phone)) {
            JOptionPane.showMessageDialog(this, "Phone number must be exactly 10 digits.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
    
    // ----------- Step 2: OTP Panel -----------
    private JPanel createOtpPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Step 2: OTP Verification"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Font font = new Font("Georgia", Font.PLAIN, 14);
        int row = 0;
        
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        JLabel label = new JLabel("Enter the OTP sent to your phone", SwingConstants.CENTER);
        label.setFont(new Font("Georgia", Font.BOLD, 16));
        panel.add(label, gbc);
        
        // Row 1: OTP field
        row++;
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("OTP:"), gbc);
        gbc.gridx = 1;
        otpField = new JTextField(10);
        otpField.setFont(font);
        panel.add(otpField, gbc);
        
        // Row 2: Verify OTP and Back buttons
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        verifyOtpButton = new JButton("Verify OTP");
        otpBackButton = new JButton("Back");
        btnPanel.add(verifyOtpButton);
        btnPanel.add(otpBackButton);
        panel.add(btnPanel, gbc);
        
        verifyOtpButton.addActionListener(e -> verifyOtp());
        otpBackButton.addActionListener(e -> mainCardLayout.show(mainCardPanel, "AccountInfo"));
        
        return panel;
    }
    
    private void verifyOtp() {
        String enteredOtp = otpField.getText().trim();
        if(generatedOtp != null && generatedOtp.equals(enteredOtp)) {
            JOptionPane.showMessageDialog(this, "OTP Verified Successfully!");
            isOtpVerified = true;
            mainCardLayout.show(mainCardPanel, "RoleDetails");
        } else {
            JOptionPane.showMessageDialog(this, "Invalid OTP. Please try again.", "Verification Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Simulate OTP generation (for demo purposes)
    private void sendOtp() {
        generatedOtp = String.valueOf(100000 + new Random().nextInt(900000));
        JOptionPane.showMessageDialog(this, "OTP sent to your phone.\n(For demo, OTP is: " + generatedOtp + ")");
    }
    
    // ----------- Step 3: Role Details Panel -----------
    private JPanel createRoleDetailsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Step 3: Enter Role Details"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Font font = new Font("Georgia", Font.PLAIN, 14);
        int row = 0;
        
        // Role dropdown (only Student and Faculty allowed)
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        panel.add(new JLabel("Select Role:"), gbc);
        gbc.gridx = 2;
        String[] roles = {"Student", "Faculty"};
        roleComboBox = new JComboBox<>(roles);
        roleComboBox.setFont(font);
        panel.add(roleComboBox, gbc);
        
        // Sub-panel for role-specific details using CardLayout
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 4;
        JPanel subPanel = new JPanel();
        subPanel.setBorder(BorderFactory.createTitledBorder("Enter Details"));
        roleCardLayout = new CardLayout();
        subPanel.setLayout(roleCardLayout);
        
        // ----- Student Details Panel -----
        JPanel stuPanel = new JPanel(new GridBagLayout());
        GridBagConstraints stuGbc = new GridBagConstraints();
        stuGbc.insets = new Insets(5,5,5,5);
        stuGbc.fill = GridBagConstraints.HORIZONTAL;
        int stuRow = 0;
        
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Name:"), stuGbc);
        stuGbc.gridx = 1;
        stuNameField = new JTextField(20);
        stuNameField.setFont(font);
        stuPanel.add(stuNameField, stuGbc);
        
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Email:"), stuGbc);
        stuGbc.gridx = 1;
        stuEmailField = new JTextField(20);
        stuEmailField.setFont(font);
        stuPanel.add(stuEmailField, stuGbc);
        
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Phone (10 digits):"), stuGbc);
        stuGbc.gridx = 1;
        stuPhoneField = new JTextField(15);
        stuPhoneField.setFont(font);
        stuPanel.add(stuPhoneField, stuGbc);
        
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Address:"), stuGbc);
        stuGbc.gridx = 1;
        stuAddressField = new JTextField(20);
        stuAddressField.setFont(font);
        stuPanel.add(stuAddressField, stuGbc);
        
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("DOB (yyyy-MM-dd):"), stuGbc);
        stuGbc.gridx = 1;
        stuDobField = new JTextField(10);
        stuDobField.setFont(font);
        stuPanel.add(stuDobField, stuGbc);
        
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Age:"), stuGbc);
        stuGbc.gridx = 1;
        stuAgeField = new JTextField(5);
        stuAgeField.setFont(font);
        stuAgeField.setEditable(false);
        stuPanel.add(stuAgeField, stuGbc);
        
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        JButton calcStuAgeButton = new JButton("Calculate Age");
        stuPanel.add(calcStuAgeButton, stuGbc);
        calcStuAgeButton.addActionListener(e -> {
            String dobStr = stuDobField.getText().trim();
            if(dobStr.isEmpty()){
                JOptionPane.showMessageDialog(this, "Please enter DOB.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Date parsedDate = new SimpleDateFormat("yyyy-MM-dd").parse(dobStr);
                int age = calculateAge(parsedDate);
                stuAgeField.setText(String.valueOf(age));
            } catch(ParseException ex) {
                JOptionPane.showMessageDialog(this, "DOB must be in yyyy-MM-dd format.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Department ID:"), stuGbc);
        stuGbc.gridx = 1;
        stuDepField = new JTextField(10);
        stuDepField.setFont(font);
        stuPanel.add(stuDepField, stuGbc);
        
        subPanel.add(stuPanel, "Student");
        
        // ----- Faculty Details Panel -----
        JPanel facPanel = new JPanel(new GridBagLayout());
        GridBagConstraints facGbc = new GridBagConstraints();
        facGbc.insets = new Insets(5,5,5,5);
        facGbc.fill = GridBagConstraints.HORIZONTAL;
        int facRow = 0;
        
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Name:"), facGbc);
        facGbc.gridx = 1;
        facNameField = new JTextField(20);
        facNameField.setFont(font);
        facPanel.add(facNameField, facGbc);
        
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Email:"), facGbc);
        facGbc.gridx = 1;
        facEmailField = new JTextField(20);
        facEmailField.setFont(font);
        facPanel.add(facEmailField, facGbc);
        
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Phone (10 digits):"), facGbc);
        facGbc.gridx = 1;
        facPhoneField = new JTextField(15);
        facPhoneField.setFont(font);
        facPanel.add(facPhoneField, facGbc);
        
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("DOB (yyyy-MM-dd):"), facGbc);
        facGbc.gridx = 1;
        facDobField = new JTextField(10);
        facDobField.setFont(font);
        facPanel.add(facDobField, facGbc);
        
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Age:"), facGbc);
        facGbc.gridx = 1;
        facAgeField = new JTextField(5);
        facAgeField.setFont(font);
        facAgeField.setEditable(false);
        facPanel.add(facAgeField, facGbc);
        
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        JButton calcFacAgeButton = new JButton("Calculate Age");
        facPanel.add(calcFacAgeButton, facGbc);
        calcFacAgeButton.addActionListener(e -> {
            String dobStr = facDobField.getText().trim();
            if(dobStr.isEmpty()){
                JOptionPane.showMessageDialog(this, "Please enter DOB.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Date parsedDate = new SimpleDateFormat("yyyy-MM-dd").parse(dobStr);
                int age = calculateAge(parsedDate);
                facAgeField.setText(String.valueOf(age));
            } catch(ParseException ex) {
                JOptionPane.showMessageDialog(this, "DOB must be in yyyy-MM-dd format.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Department ID:"), facGbc);
        facGbc.gridx = 1;
        facDepField = new JTextField(10);
        facDepField.setFont(font);
        facPanel.add(facDepField, facGbc);
        
        subPanel.add(facPanel, "Faculty");
        
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 4;
        panel.add(subPanel, gbc);
        
        // Row for Register and Back buttons in Role Details Panel
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 4;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        registerButton = new JButton("Register");
        detailsBackButton = new JButton("Back");
        btnPanel.add(registerButton);
        btnPanel.add(detailsBackButton);
        panel.add(btnPanel, gbc);
        
        // Action listeners for role details
        registerButton.addActionListener(e -> registerUser());
        detailsBackButton.addActionListener(e -> mainCardLayout.show(mainCardPanel, "OTP"));
        
        return panel;
    }
    
    // Calculate age from java.util.Date
    private int calculateAge(Date dob) {
        Calendar dobCal = Calendar.getInstance();
        dobCal.setTime(dob);
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR);
        if(today.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age;
    }
    
    // ---------------- Registration: Validate and Insert Data ----------------
    private void registerUser() {
        // Validate common fields
        String userIdStr = userIdField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String role = (String) roleComboBox.getSelectedItem();
        if(userIdStr.isEmpty() || password.isEmpty() || role.isEmpty()){
            JOptionPane.showMessageDialog(this, "User ID, Password, and Role are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int userId;
        try {
            userId = Integer.parseInt(userIdStr);
        } catch(NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "User ID must be a valid number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Validate password length
        if(password.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters long.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Based on role, validate fields and insert data into respective tables
        if(role.equalsIgnoreCase("Student")) {
            String stuName = stuNameField.getText().trim();
            String stuEmail = stuEmailField.getText().trim();
            String stuPhone = stuPhoneField.getText().trim();
            String stuAddress = stuAddressField.getText().trim();
            String stuDobStr = stuDobField.getText().trim();
            String stuDep = stuDepField.getText().trim();
            if(stuName.isEmpty() || stuEmail.isEmpty() || stuPhone.isEmpty() ||
               stuAddress.isEmpty() || stuDobStr.isEmpty() || stuDep.isEmpty()){
                JOptionPane.showMessageDialog(this, "All Student fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[0-9]{10}$", stuPhone)) {
                JOptionPane.showMessageDialog(this, "Student phone must be exactly 10 digits.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[\\w.-]+@[\\w.-]+\\.com$", stuEmail)) {
                JOptionPane.showMessageDialog(this, "Student email must be valid and end with '.com'.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            java.sql.Date stuDob;
            try {
                Date parsedDate = new SimpleDateFormat("yyyy-MM-dd").parse(stuDobStr);
                stuDob = new java.sql.Date(parsedDate.getTime());
            } catch(ParseException ex) {
                JOptionPane.showMessageDialog(this, "DOB must be in format yyyy-MM-dd.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int age = calculateAge(new Date(stuDob.getTime()));
            if(age < 18) {
                JOptionPane.showMessageDialog(this, "Student must be at least 18 years old.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Integer.parseInt(stuDep);
            } catch(NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Department ID must be a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Insert into Login_Table and Student table
            registerToDB(userId, password, role);
            insertStudent(userId, stuName, stuEmail, stuPhone, stuAddress, stuDob, Integer.parseInt(stuDep));
            
        } else if(role.equalsIgnoreCase("Faculty")) {
            String facName = facNameField.getText().trim();
            String facEmail = facEmailField.getText().trim();
            String facPhone = facPhoneField.getText().trim();
            String facDobStr = facDobField.getText().trim();
            String facDep = facDepField.getText().trim();
            if(facName.isEmpty() || facEmail.isEmpty() || facPhone.isEmpty() ||
               facDobStr.isEmpty() || facDep.isEmpty()){
                JOptionPane.showMessageDialog(this, "All Faculty fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[0-9]{10}$", facPhone)) {
                JOptionPane.showMessageDialog(this, "Faculty phone must be exactly 10 digits.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[\\w.-]+@[\\w.-]+\\.com$", facEmail)) {
                JOptionPane.showMessageDialog(this, "Faculty email must be valid and end with '.com'.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            java.sql.Date facDob;
            try {
                Date parsedDate = new SimpleDateFormat("yyyy-MM-dd").parse(facDobStr);
                facDob = new java.sql.Date(parsedDate.getTime());
            } catch(ParseException ex) {
                JOptionPane.showMessageDialog(this, "DOB must be in format yyyy-MM-dd.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int age = calculateAge(new Date(facDob.getTime()));
            if(age < 26) {
                JOptionPane.showMessageDialog(this, "Faculty must be at least 26 years old.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Integer.parseInt(facDep);
            } catch(NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Department ID must be a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            registerToDB(userId, password, role);
            insertFaculty(userId, facName, facEmail, facPhone, Integer.parseInt(facDep));
        }
        
        JOptionPane.showMessageDialog(this, "Registration successful!");
        new LoginPage().setVisible(true);
        dispose();
    }
    
    // Inserts common login details into Login_Table
    private void registerToDB(int userId, String password, String role) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Login_Table (Login_ID, U_Password, Role) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setString(2, password);
            stmt.setString(3, role);
            stmt.executeUpdate();
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error registering login details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Inserts student details into Student table
    private void insertStudent(int stuId, String name, String email, String phone, String address, java.sql.Date dob, int depId) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Student (Stu_ID, Stu_Name, Stu_Mail_ID, Stu_PhoneNO, Stu_address, Stu_DOB, dep_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, stuId);
            stmt.setString(2, name);
            stmt.setString(3, email);
            stmt.setString(4, phone);
            stmt.setString(5, address);
            stmt.setDate(6, dob);
            stmt.setInt(7, depId);
            stmt.executeUpdate();
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error inserting student details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Inserts faculty details into Faculty table
    private void insertFaculty(int facId, String name, String email, String phone, int depId) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Faculty (F_ID, F_Name, F_Mail_ID, F_PhoneNO, dep_id) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, facId);
            stmt.setString(2, name);
            stmt.setString(3, email);
            stmt.setString(4, phone);
            stmt.setInt(5, depId);
            stmt.executeUpdate();
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error inserting faculty details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SignUpPage().setVisible(true));
    }
}
/*import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.regex.Pattern;

public class SignUpPage extends JFrame {
    // Card names for the wizard steps
    private static final String CARD_ACCOUNT_INFO = "AccountInfo";
    private static final String CARD_OTP = "OTPVerification";
    private static final String CARD_ROLE_DETAILS = "RoleDetails";
    
    // Main card panel and layout
    private JPanel cardPanel;
    private CardLayout mainCardLayout;
    
    // ---------- Account Info Panel Components ----------
    private JTextField accUserIdField;
    private JPasswordField accPasswordField, accConfirmPasswordField;
    private JTextField accPhoneField;
    private JButton sendOtpButton, goToOtpButton;
    
    // ---------- OTP Panel Components ----------
    private JTextField otpField;
    private JButton verifyOtpButton, otpBackButton;
    private String generatedOtp;
    private boolean isOtpVerified = false;
    
    // ---------- Role Details Panel Components ----------
    private JComboBox<String> roleComboBox; // Only "Student" and "Faculty"
    private JPanel roleDetailsPanel;
    private CardLayout roleCardLayout;
    
    // Student fields
    private JTextField stuNameField, stuEmailField, stuAddressField, stuDepField, stuDobField;
    private JTextField stuAgeField; // Read-only, auto-calculated
    
    // Faculty fields
    private JTextField facNameField, facEmailField, facDepField, facDobField;
    private JTextField facAgeField; // Read-only, auto-calculated

    // Common buttons for role details
    private JButton registerButton, detailsBackButton;
    
    public SignUpPage() {
        super("Sign Up Page");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 650);
        setLocationRelativeTo(null);
        initUI();
    }
    
    private void initUI() {
        // Use a main card layout for the wizard steps
        mainCardLayout = new CardLayout();
        cardPanel = new JPanel(mainCardLayout);
        
        // Create each step's panel and add to the main card panel.
        cardPanel.add(createAccountInfoPanel(), CARD_ACCOUNT_INFO);
        cardPanel.add(createOtpPanel(), CARD_OTP);
        cardPanel.add(createRoleDetailsPanel(), CARD_ROLE_DETAILS);
        
        add(cardPanel, BorderLayout.CENTER);
    }
    
    // ---------------- Account Info Panel ----------------
    private JPanel createAccountInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Account Information"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Font font = new Font("Georgia", Font.PLAIN, 14);
        int row = 0;
        
        // Title label
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        JLabel label = new JLabel("Enter Your Account Information", SwingConstants.CENTER);
        label.setFont(new Font("Georgia", Font.BOLD, 18));
        panel.add(label, gbc);
        
        // Row 1: User ID
        row++;
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("User ID (number):"), gbc);
        gbc.gridx = 1;
        accUserIdField = new JTextField(20);
        accUserIdField.setFont(font);
        panel.add(accUserIdField, gbc);
        
        // Row 2: Password
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        accPasswordField = new JPasswordField(20);
        accPasswordField.setFont(font);
        panel.add(accPasswordField, gbc);
        
        // Row 3: Confirm Password
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Confirm Password:"), gbc);
        gbc.gridx = 1;
        accConfirmPasswordField = new JPasswordField(20);
        accConfirmPasswordField.setFont(font);
        panel.add(accConfirmPasswordField, gbc);
        
        // Row 4: Phone Number
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Phone (10 digits):"), gbc);
        gbc.gridx = 1;
        accPhoneField = new JTextField(15);
        accPhoneField.setFont(font);
        panel.add(accPhoneField, gbc);
        
        // Row 5: Send OTP Button
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        sendOtpButton = new JButton("Send OTP");
        panel.add(sendOtpButton, gbc);
        
        // Row 6: Next button to go to OTP panel
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        goToOtpButton = new JButton("Next");
        panel.add(goToOtpButton, gbc);
        
        // Action listeners
        sendOtpButton.addActionListener(e -> sendOtp());
        goToOtpButton.addActionListener(e -> {
            if (validateAccountInfo()) {
                mainCardLayout.show(cardPanel, CARD_OTP);
            }
        });
        
        return panel;
    }
    
    // Validate common account info fields
    private boolean validateAccountInfo() {
        String userIdStr = accUserIdField.getText().trim();
        String password = new String(accPasswordField.getPassword()).trim();
        String confirmPassword = new String(accConfirmPasswordField.getPassword()).trim();
        String phone = accPhoneField.getText().trim();
        
        if(userIdStr.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || phone.isEmpty()){
            JOptionPane.showMessageDialog(this, "All fields are required in Account Info.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            Integer.parseInt(userIdStr);
        } catch(NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "User ID must be a valid number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(password.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters long.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(!Pattern.matches("^[0-9]{10}$", phone)) {
            JOptionPane.showMessageDialog(this, "Phone number must be exactly 10 digits.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
    
    // Simulate OTP generation
    private void sendOtp() {
        // For demo: generate a random 6-digit OTP and display it
        generatedOtp = String.valueOf(100000 + new Random().nextInt(900000));
        JOptionPane.showMessageDialog(this, "OTP sent to your phone.\n(For demo, OTP is: " + generatedOtp + ")");
        // In a real system, send the OTP via SMS.
    }
    
    // ---------------- OTP Verification Panel ----------------
    private JPanel createOtpPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("OTP Verification"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Font font = new Font("Georgia", Font.PLAIN, 14);
        int row = 0;
        
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        JLabel label = new JLabel("Enter the OTP sent to your phone", SwingConstants.CENTER);
        label.setFont(new Font("Georgia", Font.BOLD, 16));
        panel.add(label, gbc);
        
        // Row 1: OTP Field
        row++;
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("OTP:"), gbc);
        gbc.gridx = 1;
        otpField = new JTextField(10);
        otpField.setFont(font);
        panel.add(otpField, gbc);
        
        // Row 2: Verify OTP and Back buttons
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        verifyOtpButton = new JButton("Verify OTP");
        JButton otpBackButton = new JButton("Back");
        btnPanel.add(verifyOtpButton);
        btnPanel.add(otpBackButton);
        panel.add(btnPanel, gbc);
        
        verifyOtpButton.addActionListener(e -> verifyOtp());
        otpBackButton.addActionListener(e -> mainCardLayout.show(cardPanel, CARD_ACCOUNT_INFO));
        
        return panel;
    }
    
    private void verifyOtp() {
        String enteredOtp = otpField.getText().trim();
        if (generatedOtp != null && generatedOtp.equals(enteredOtp)) {
            JOptionPane.showMessageDialog(this, "OTP Verified Successfully!");
            isOtpVerified = true;
            mainCardLayout.show(cardPanel, CARD_ROLE_DETAILS);
        } else {
            JOptionPane.showMessageDialog(this, "Invalid OTP. Please try again.", "Verification Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // ---------------- Role and Details Panel ----------------
    private JPanel createRoleDetailsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Role Details"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Font font = new Font("Georgia", Font.PLAIN, 14);
        int row = 0;
        
        // Role dropdown (only Student and Faculty)
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Select Role:"), gbc);
        gbc.gridx = 1;
        String[] roles = {"Student", "Faculty"};
        roleComboBox = new JComboBox<>(roles);
        roleComboBox.setFont(font);
        panel.add(roleComboBox, gbc);
        
        // Create a sub-panel with CardLayout for role-specific fields
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 2;
        JPanel subPanel = new JPanel();
        subPanel.setBorder(BorderFactory.createTitledBorder("Enter Details"));
        roleCardLayout = new CardLayout();
        subPanel.setLayout(roleCardLayout);
        
        // ----- Student Details Panel -----
        JPanel stuPanel = new JPanel(new GridBagLayout());
        GridBagConstraints stuGbc = new GridBagConstraints();
        stuGbc.insets = new Insets(5,5,5,5);
        stuGbc.fill = GridBagConstraints.HORIZONTAL;
        int stuRow = 0;
        
        // Student Name
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Name:"), stuGbc);
        stuGbc.gridx = 1;
        stuNameField = new JTextField(20);
        stuNameField.setFont(font);
        stuPanel.add(stuNameField, stuGbc);
        
        // Student Email
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Email:"), stuGbc);
        stuGbc.gridx = 1;
        stuEmailField = new JTextField(20);
        stuEmailField.setFont(font);
        stuPanel.add(stuEmailField, stuGbc);
        
        // Student Address
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Address:"), stuGbc);
        stuGbc.gridx = 1;
        stuAddressField = new JTextField(20);
        stuAddressField.setFont(font);
        stuPanel.add(stuAddressField, stuGbc);
        
        // Student DOB
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("DOB (yyyy-MM-dd):"), stuGbc);
        stuGbc.gridx = 1;
        stuDobField = new JTextField(10);
        stuDobField.setFont(font);
        stuPanel.add(stuDobField, stuGbc);
        
        // Student Age (read-only; auto-calculated)
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Age:"), stuGbc);
        stuGbc.gridx = 1;
        stuAgeField = new JTextField(5);
        stuAgeField.setFont(font);
        stuAgeField.setEditable(false);
        stuPanel.add(stuAgeField, stuGbc);
        
        // Add a button to calculate age based on DOB
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        JButton calcAgeStuButton = new JButton("Calculate Age");
        stuPanel.add(calcAgeStuButton, stuGbc);
        calcAgeStuButton.addActionListener(e -> {
            String dobStr = stuDobField.getText().trim();
            if(dobStr.isEmpty()){
                JOptionPane.showMessageDialog(this, "Please enter DOB.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Date parsedDate = new SimpleDateFormat("yyyy-MM-dd").parse(dobStr);
                int age = calculateAge(parsedDate);
                stuAgeField.setText(String.valueOf(age));
            } catch(ParseException ex) {
                JOptionPane.showMessageDialog(this, "DOB must be in yyyy-MM-dd format.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // Student Department ID
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Department ID:"), stuGbc);
        stuGbc.gridx = 1;
        stuDepField = new JTextField(10);
        stuDepField.setFont(font);
        stuPanel.add(stuDepField, stuGbc);
        
        subPanel.add(stuPanel, "Student");
        
        // ----- Faculty Details Panel -----
        JPanel facPanel = new JPanel(new GridBagLayout());
        GridBagConstraints facGbc = new GridBagConstraints();
        facGbc.insets = new Insets(5,5,5,5);
        facGbc.fill = GridBagConstraints.HORIZONTAL;
        int facRow = 0;
        
        // Faculty Name
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Name:"), facGbc);
        facGbc.gridx = 1;
        facNameField = new JTextField(20);
        facNameField.setFont(font);
        facPanel.add(facNameField, facGbc);
        
        // Faculty Email
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Email:"), facGbc);
        facGbc.gridx = 1;
        facEmailField = new JTextField(20);
        facEmailField.setFont(font);
        facPanel.add(facEmailField, facGbc);
        
        // Faculty DOB
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("DOB (yyyy-MM-dd):"), facGbc);
        facGbc.gridx = 1;
        facDobField = new JTextField(10);
        facDobField.setFont(font);
        facPanel.add(facDobField, facGbc);
        
        // Faculty Age (read-only; auto-calculated)
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Age:"), facGbc);
        facGbc.gridx = 1;
        facAgeField = new JTextField(5);
        facAgeField.setFont(font);
        facAgeField.setEditable(false);
        facPanel.add(facAgeField, facGbc);
        
        // Calculate Faculty Age button
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        JButton calcAgeFacButton = new JButton("Calculate Age");
        facPanel.add(calcAgeFacButton, facGbc);
        calcAgeFacButton.addActionListener(e -> {
            String dobStr = facDobField.getText().trim();
            if(dobStr.isEmpty()){
                JOptionPane.showMessageDialog(this, "Please enter DOB.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Date parsedDate = new SimpleDateFormat("yyyy-MM-dd").parse(dobStr);
                int age = calculateAge(parsedDate);
                facAgeField.setText(String.valueOf(age));
            } catch(ParseException ex) {
                JOptionPane.showMessageDialog(this, "DOB must be in yyyy-MM-dd format.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // Faculty Department ID
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Department ID:"), facGbc);
        facGbc.gridx = 1;
        facDepField = new JTextField(10);
        facDepField.setFont(font);
        facPanel.add(facDepField, facGbc);
        
        subPanel.add(facPanel, "Faculty");
        
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        panel.add(subPanel, gbc);
        
        // Row for Register and Back buttons
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        registerButton = new JButton("Register");
        detailsBackButton = new JButton("Back");
        btnPanel.add(registerButton);
        btnPanel.add(detailsBackButton);
        panel.add(btnPanel, gbc);
        
        // Action listeners for Register and Back buttons
        registerButton.addActionListener(e -> registerUser());
        detailsBackButton.addActionListener(e -> mainCardLayout.show(cardPanel, CARD_OTP));
        
        return panel;
    }
    
    // Calculate age from a java.util.Date
    private int calculateAge(Date dob) {
        Calendar dobCal = Calendar.getInstance();
        dobCal.setTime(dob);
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age;
    }
    
    private void registerUser() {
        // Validate common fields
        String userIdStr = userIdField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String role = (String) roleComboBox.getSelectedItem();
        if(userIdStr.isEmpty() || password.isEmpty() || role.isEmpty()){
            JOptionPane.showMessageDialog(this, "User ID, Password, and Role are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int userId;
        try {
            userId = Integer.parseInt(userIdStr);
        } catch(NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "User ID must be a valid number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Validate password length
        if(password.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters long.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Based on role, validate fields and insert data into respective tables
        if(role.equalsIgnoreCase("Student")) {
            String stuName = stuNameField.getText().trim();
            String stuEmail = stuEmailField.getText().trim();
            String stuPhone = stuPhoneField.getText().trim();
            String stuAddress = stuAddressField.getText().trim();
            String stuDobStr = stuDobField.getText().trim();
            String stuDep = stuDepField.getText().trim();
            if(stuName.isEmpty() || stuEmail.isEmpty() || stuPhone.isEmpty() ||
               stuAddress.isEmpty() || stuDobStr.isEmpty() || stuDep.isEmpty()){
                JOptionPane.showMessageDialog(this, "All Student fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[0-9]{10}$", stuPhone)) {
                JOptionPane.showMessageDialog(this, "Student phone must be exactly 10 digits.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[\\w.-]+@[\\w.-]+\\.com$", stuEmail)) {
                JOptionPane.showMessageDialog(this, "Student email must be valid and end with '.com'.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            java.sql.Date stuDob;
            try {
                Date parsedDate = new SimpleDateFormat("yyyy-MM-dd").parse(stuDobStr);
                stuDob = new java.sql.Date(parsedDate.getTime());
            } catch(ParseException ex) {
                JOptionPane.showMessageDialog(this, "DOB must be in format yyyy-MM-dd.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int age = calculateAge(new Date(stuDob.getTime()));
            if(age < 18) {
                JOptionPane.showMessageDialog(this, "Student must be at least 18 years old.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Integer.parseInt(stuDep);
            } catch(NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Department ID must be a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Insert into Login_Table and Student table
            registerToDB(userId, password, role);
            insertStudent(userId, stuName, stuEmail, stuPhone, stuAddress, stuDob, Integer.parseInt(stuDep));
            
        } else if(role.equalsIgnoreCase("Faculty")) {
            String facName = facNameField.getText().trim();
            String facEmail = facEmailField.getText().trim();
            String facPhone = facPhoneField.getText().trim();
            String facDobStr = facDobField.getText().trim();
            String facDep = facDepField.getText().trim();
            if(facName.isEmpty() || facEmail.isEmpty() || facPhone.isEmpty() ||
               facDobStr.isEmpty() || facDep.isEmpty()){
                JOptionPane.showMessageDialog(this, "All Faculty fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[0-9]{10}$", facPhone)) {
                JOptionPane.showMessageDialog(this, "Faculty phone must be exactly 10 digits.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[\\w.-]+@[\\w.-]+\\.com$", facEmail)) {
                JOptionPane.showMessageDialog(this, "Faculty email must be valid and end with '.com'.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            java.sql.Date facDob;
            try {
                Date parsedDate = new SimpleDateFormat("yyyy-MM-dd").parse(facDobStr);
                facDob = new java.sql.Date(parsedDate.getTime());
            } catch(ParseException ex) {
                JOptionPane.showMessageDialog(this, "DOB must be in format yyyy-MM-dd.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int age = calculateAge(new Date(facDob.getTime()));
            if(age < 26) {
                JOptionPane.showMessageDialog(this, "Faculty must be at least 26 years old.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Integer.parseInt(facDep);
            } catch(NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Department ID must be a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            registerToDB(userId, password, role);
            insertFaculty(userId, facName, facEmail, facPhone, Integer.parseInt(facDep));
        }
        
        JOptionPane.showMessageDialog(this, "Registration successful!");
        new LoginPage().setVisible(true);
        dispose();
    }
    
    // Inserts into Login_Table
    private void registerToDB(int userId, String password, String role) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Login_Table (Login_ID, U_Password, Role) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setString(2, password);
            stmt.setString(3, role);
            stmt.executeUpdate();
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error in registering login details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Inserts student details into Student table
    private void insertStudent(int stuId, String name, String email, String phone, String address, java.sql.Date dob, int depId) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Student (Stu_ID, Stu_Name, Stu_Mail_ID, Stu_PhoneNO, Stu_address, Stu_DOB, dep_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, stuId);
            stmt.setString(2, name);
            stmt.setString(3, email);
            stmt.setString(4, phone);
            stmt.setString(5, address);
            stmt.setDate(6, dob);
            stmt.setInt(7, depId);
            stmt.executeUpdate();
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error inserting student details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Inserts faculty details into Faculty table
    private void insertFaculty(int facId, String name, String email, String phone, int depId) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Faculty (F_ID, F_Name, F_Mail_ID, F_PhoneNO, dep_id) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, facId);
            stmt.setString(2, name);
            stmt.setString(3, email);
            stmt.setString(4, phone);
            stmt.setInt(5, depId);
            stmt.executeUpdate();
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error inserting faculty details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SignUpPage().setVisible(true));
    }
}
/*import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

public class SignUpPage extends JFrame {
    // Common fields
    private JTextField userIdField;
    private JPasswordField passwordField;
    private JComboBox<String> roleComboBox;
    // OTP fields (if needed; here you can add OTP functionality as separate)
    // private JTextField phoneField, otpField;
    // Role-specific details panel using CardLayout
    private JPanel detailsPanel;
    private CardLayout cardLayout;
    
    // Student panel fields
    private JTextField stuNameField, stuEmailField, stuPhoneField, stuAddressField, stuDepField, stuDobField;
    // Faculty panel fields
    private JTextField facNameField, facEmailField, facPhoneField, facDepField;
    // Admin panel fields
    private JTextField adminEmailField, adminPhoneField;
    
    // Buttons
    private JButton registerButton, backButton;
    
    public SignUpPage() {
        super("Sign Up Page");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(550, 600);
        setLocationRelativeTo(null);
        initUI();
    }
    
    private void initUI() {
        // Main panel using GridBagLayout
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8,8,8,8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Font font = new Font("Georgia", Font.PLAIN, 14);
        int row = 0;
        
        // Title
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("Register New Account", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 20));
        mainPanel.add(titleLabel, gbc);
        
        // Row 1: User ID
        row++;
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("User ID (number):"), gbc);
        gbc.gridx = 1;
        userIdField = new JTextField(20);
        userIdField.setFont(font);
        mainPanel.add(userIdField, gbc);
        
        // Row 2: Password
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        passwordField.setFont(font);
        mainPanel.add(passwordField, gbc);
        
        // Row 3: Role selection using dropdown
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Select Role:"), gbc);
        gbc.gridx = 1;
        String[] roles = {"Student", "Faculty", "Admin"};
        roleComboBox = new JComboBox<>(roles);
        roleComboBox.setFont(font);
        mainPanel.add(roleComboBox, gbc);
        
        // Row 4: Role-specific details panel using CardLayout
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 2;
        detailsPanel = new JPanel();
        cardLayout = new CardLayout();
        detailsPanel.setLayout(cardLayout);
        detailsPanel.setBorder(BorderFactory.createTitledBorder("Role Details"));
        
        // ----- Student Panel -----
        JPanel stuPanel = new JPanel(new GridBagLayout());
        GridBagConstraints stuGbc = new GridBagConstraints();
        stuGbc.insets = new Insets(5,5,5,5);
        stuGbc.fill = GridBagConstraints.HORIZONTAL;
        int stuRow = 0;
        
        // Student Name
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Name:"), stuGbc);
        stuGbc.gridx = 1;
        stuNameField = new JTextField(20);
        stuNameField.setFont(font);
        stuPanel.add(stuNameField, stuGbc);
        
        // Student Email
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Email:"), stuGbc);
        stuGbc.gridx = 1;
        stuEmailField = new JTextField(20);
        stuEmailField.setFont(font);
        stuPanel.add(stuEmailField, stuGbc);
        
        // Student Phone (10 digits)
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Phone (10 digits):"), stuGbc);
        stuGbc.gridx = 1;
        stuPhoneField = new JTextField(15);
        stuPhoneField.setFont(font);
        stuPanel.add(stuPhoneField, stuGbc);
        
        // Student Address
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Address:"), stuGbc);
        stuGbc.gridx = 1;
        stuAddressField = new JTextField(20);
        stuAddressField.setFont(font);
        stuPanel.add(stuAddressField, stuGbc);
        
        // Student DOB as text (format yyyy-MM-dd)
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("DOB (yyyy-MM-dd):"), stuGbc);
        stuGbc.gridx = 1;
        stuDobField = new JTextField(10);
        stuDobField.setFont(font);
        stuPanel.add(stuDobField, stuGbc);
        
        // Student Department ID
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Department ID:"), stuGbc);
        stuGbc.gridx = 1;
        stuDepField = new JTextField(10);
        stuDepField.setFont(font);
        stuPanel.add(stuDepField, stuGbc);
        
        detailsPanel.add(stuPanel, "Student");
        
        // ----- Faculty Panel -----
        JPanel facPanel = new JPanel(new GridBagLayout());
        GridBagConstraints facGbc = new GridBagConstraints();
        facGbc.insets = new Insets(5,5,5,5);
        facGbc.fill = GridBagConstraints.HORIZONTAL;
        int facRow = 0;
        
        // Faculty Name
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Name:"), facGbc);
        facGbc.gridx = 1;
        facNameField = new JTextField(20);
        facNameField.setFont(font);
        facPanel.add(facNameField, facGbc);
        
        // Faculty Email
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Email:"), facGbc);
        facGbc.gridx = 1;
        facEmailField = new JTextField(20);
        facEmailField.setFont(font);
        facPanel.add(facEmailField, facGbc);
        
        // Faculty Phone (10 digits)
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Phone (10 digits):"), facGbc);
        facGbc.gridx = 1;
        facPhoneField = new JTextField(15);
        facPhoneField.setFont(font);
        facPanel.add(facPhoneField, facGbc);
        
        // Faculty Department ID
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Department ID:"), facGbc);
        facGbc.gridx = 1;
        facDepField = new JTextField(10);
        facDepField.setFont(font);
        facPanel.add(facDepField, facGbc);
        
        detailsPanel.add(facPanel, "Faculty");
        
        // ----- Admin Panel -----
        JPanel adminPanel = new JPanel(new GridBagLayout());
        GridBagConstraints adminGbc = new GridBagConstraints();
        adminGbc.insets = new Insets(5,5,5,5);
        adminGbc.fill = GridBagConstraints.HORIZONTAL;
        int adminRow = 0;
        
        // Admin Email
        adminGbc.gridx = 0; adminGbc.gridy = adminRow;
        adminPanel.add(new JLabel("Email:"), adminGbc);
        adminGbc.gridx = 1;
        adminEmailField = new JTextField(20);
        adminEmailField.setFont(font);
        adminPanel.add(adminEmailField, adminGbc);
        
        // Admin Phone (10 digits)
        adminRow++;
        adminGbc.gridx = 0; adminGbc.gridy = adminRow;
        adminPanel.add(new JLabel("Phone (10 digits):"), adminGbc);
        adminGbc.gridx = 1;
        adminPhoneField = new JTextField(15);
        adminPhoneField.setFont(font);
        adminPanel.add(adminPhoneField, adminGbc);
        
        detailsPanel.add(adminPanel, "Admin");
        
        // When role changes, show corresponding card
        roleComboBox.addActionListener(e -> {
            String selectedRole = (String) roleComboBox.getSelectedItem();
            cardLayout.show(detailsPanel, selectedRole);
        });
        
        // Initialize CardLayout to show Student details by default
        cardLayout = (CardLayout) detailsPanel.getLayout();
        cardLayout.show(detailsPanel, "Student");
        
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        mainPanel.add(detailsPanel, gbc);
        
        // Row 8: Register and Back buttons
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        registerButton = new JButton("Register");
        backButton = new JButton("Back to Login");
        btnPanel.add(registerButton);
        btnPanel.add(backButton);
        mainPanel.add(btnPanel, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Action listeners
        registerButton.addActionListener(e -> registerUser());
        backButton.addActionListener(e -> {
            new LoginPage().setVisible(true);
            dispose();
        });
    }
    
    private void registerUser() {
        // Validate common fields
        String userIdStr = userIdField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String role = (String) roleComboBox.getSelectedItem();
        if(userIdStr.isEmpty() || password.isEmpty() || role.isEmpty()){
            JOptionPane.showMessageDialog(this, "User ID, Password, and Role are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int userId;
        try {
            userId = Integer.parseInt(userIdStr);
        } catch(NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "User ID must be a valid number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Validate password length
        if(password.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters long.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Based on role, validate fields and insert data into respective tables
        if(role.equalsIgnoreCase("Student")) {
            String stuName = stuNameField.getText().trim();
            String stuEmail = stuEmailField.getText().trim();
            String stuPhone = stuPhoneField.getText().trim();
            String stuAddress = stuAddressField.getText().trim();
            String stuDobStr = stuDobField.getText().trim();
            String stuDep = stuDepField.getText().trim();
            if(stuName.isEmpty() || stuEmail.isEmpty() || stuPhone.isEmpty() ||
               stuAddress.isEmpty() || stuDobStr.isEmpty() || stuDep.isEmpty()){
                JOptionPane.showMessageDialog(this, "All Student fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[0-9]{10}$", stuPhone)) {
                JOptionPane.showMessageDialog(this, "Student phone must be exactly 10 digits.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[\\w.-]+@[\\w.-]+\\.com$", stuEmail)) {
                JOptionPane.showMessageDialog(this, "Student email must be valid and end with '.com'.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Validate DOB format
            java.sql.Date stuDob;
            try {
                Date parsedDate = (Date) new SimpleDateFormat("yyyy-MM-dd").parse(stuDobStr);
                stuDob = new java.sql.Date(parsedDate.getTime());
            } catch(ParseException ex) {
                JOptionPane.showMessageDialog(this, "Date of Birth must be in format yyyy-MM-dd.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Integer.parseInt(stuDep);
            } catch(NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Department ID must be a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Insert into Login_Table and Student
            registerToDB(userId, password, role);
            insertStudent(userId, stuName, stuEmail, stuPhone, stuAddress, stuDob, Integer.parseInt(stuDep));
            
        } else if(role.equalsIgnoreCase("Faculty")) {
            String facName = facNameField.getText().trim();
            String facEmail = facEmailField.getText().trim();
            String facPhone = facPhoneField.getText().trim();
            String facDep = facDepField.getText().trim();
            if(facName.isEmpty() || facEmail.isEmpty() || facPhone.isEmpty() || facDep.isEmpty()){
                JOptionPane.showMessageDialog(this, "All Faculty fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[0-9]{10}$", facPhone)) {
                JOptionPane.showMessageDialog(this, "Faculty phone must be exactly 10 digits.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[\\w.-]+@[\\w.-]+\\.com$", facEmail)) {
                JOptionPane.showMessageDialog(this, "Faculty email must be valid and end with '.com'.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Integer.parseInt(facDep);
            } catch(NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Department ID must be a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            registerToDB(userId, password, role);
            insertFaculty(userId, facName, facEmail, facPhone, Integer.parseInt(facDep));
            
        } else if(role.equalsIgnoreCase("Admin")) {
            String adminEmail = adminEmailField.getText().trim();
            String adminPhone = adminPhoneField.getText().trim();
            if(adminEmail.isEmpty() || adminPhone.isEmpty()){
                JOptionPane.showMessageDialog(this, "Admin email and phone are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[0-9]{10}$", adminPhone)) {
                JOptionPane.showMessageDialog(this, "Admin phone must be exactly 10 digits.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[\\w.-]+@[\\w.-]+\\.com$", adminEmail)) {
                JOptionPane.showMessageDialog(this, "Admin email must be valid and end with '.com'.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            registerToDB(userId, password, role);
            insertAdmin(userId, adminEmail, adminPhone);
        }
        
        JOptionPane.showMessageDialog(this, "Registration successful!");
        new LoginPage().setVisible(true);
        dispose();
    }
    
    // Inserts into Login_Table
    private void registerToDB(int userId, String password, String role) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Login_Table (Login_ID, U_Password, Role) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setString(2, password);
            stmt.setString(3, role);
            stmt.executeUpdate();
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error in registering login details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Inserts student details into Student table
    private void insertStudent(int stuId, String name, String email, String phone, String address, java.sql.Date dob, int depId) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Student (Stu_ID, Stu_Name, Stu_Mail_ID, Stu_PhoneNO, Stu_address, Stu_DOB, dep_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, stuId);
            stmt.setString(2, name);
            stmt.setString(3, email);
            stmt.setString(4, phone);
            stmt.setString(5, address);
            stmt.setDate(6, dob);
            stmt.setInt(7, depId);
            stmt.executeUpdate();
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error inserting student details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Inserts faculty details into Faculty table
    private void insertFaculty(int facId, String name, String email, String phone, int depId) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Faculty (F_ID, F_Name, F_Mail_ID, F_PhoneNO, dep_id) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, facId);
            stmt.setString(2, name);
            stmt.setString(3, email);
            stmt.setString(4, phone);
            stmt.setInt(5, depId);
            stmt.executeUpdate();
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error inserting faculty details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Inserts admin details into Admin_Table
    private void insertAdmin(int adminId, String email, String phone) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Admin_Table (Ad_ID, Ad_Mail_ID, Ad_PhoneNO) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, adminId);
            stmt.setString(2, email);
            stmt.setString(3, phone);
            stmt.executeUpdate();
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error inserting admin details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SignUpPage().setVisible(true));
    }
}

/*
 * Reference: Java Swing Form Validation techniques – see GeeksforGeeks article at 
 * https://www.geeksforgeeks.org/java-swing-form-validation/
 */

/*import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

public class SignUpPage extends JFrame {
    // Common fields
    private JTextField userIdField;
    private JPasswordField passwordField;
    private JComboBox<String> roleComboBox;
    
    // Panel for role-specific details (using CardLayout)
    private JPanel detailsPanel;
    private CardLayout cardLayout;
    
    // Student fields
    private JTextField stuNameField, stuEmailField, stuAddressField, stuDepField;
    private JTextField stuPhoneField;
    private JTextField stuDobField; // Entered as yyyy-MM-dd
    
    // Faculty fields
    private JTextField facNameField, facEmailField, facDepField;
    private JTextField facPhoneField;
    
    // Admin fields
    private JTextField adminEmailField, adminPhoneField;
    
    // Buttons
    private JButton registerButton, backButton;
    
    public SignUpPage() {
        super("Sign Up Page");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(550, 550);
        setLocationRelativeTo(null);
        initUI();
    }
    
    private void initUI() {
        // Main panel with GridBagLayout
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Font font = new Font("Georgia", Font.PLAIN, 14);
        int row = 0;
        
        // Title
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("Register New Account", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 20));
        mainPanel.add(titleLabel, gbc);
        
        // Row 1: User ID
        row++;
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("User ID (number):"), gbc);
        gbc.gridx = 1;
        userIdField = new JTextField(20);
        userIdField.setFont(font);
        mainPanel.add(userIdField, gbc);
        
        // Row 2: Password
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        passwordField.setFont(font);
        mainPanel.add(passwordField, gbc);
        
        // Row 3: Role Selection
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Select Role:"), gbc);
        gbc.gridx = 1;
        String[] roles = {"Student", "Faculty", "Admin"};
        roleComboBox = new JComboBox<>(roles);
        roleComboBox.setFont(font);
        mainPanel.add(roleComboBox, gbc);
        
        // Row 4: Role-specific details using CardLayout
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 2;
        detailsPanel = new JPanel();
        cardLayout = new CardLayout();
        detailsPanel.setLayout(cardLayout);
        detailsPanel.setBorder(BorderFactory.createTitledBorder("Role Details"));
        
        // Student Panel
        JPanel stuPanel = new JPanel(new GridBagLayout());
        GridBagConstraints stuGbc = new GridBagConstraints();
        stuGbc.insets = new Insets(5,5,5,5);
        stuGbc.fill = GridBagConstraints.HORIZONTAL;
        int stuRow = 0;
        // Student Name
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Name:"), stuGbc);
        stuGbc.gridx = 1;
        stuNameField = new JTextField(20);
        stuNameField.setFont(font);
        stuPanel.add(stuNameField, stuGbc);
        // Student Email
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Email:"), stuGbc);
        stuGbc.gridx = 1;
        stuEmailField = new JTextField(20);
        stuEmailField.setFont(font);
        stuPanel.add(stuEmailField, stuGbc);
        // Student Phone (10 digits)
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Phone (10 digits):"), stuGbc);
        stuGbc.gridx = 1;
        stuPhoneField = new JTextField(15);
        stuPhoneField.setFont(font);
        stuPanel.add(stuPhoneField, stuGbc);
        // Student Address
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Address:"), stuGbc);
        stuGbc.gridx = 1;
        stuAddressField = new JTextField(20);
        stuAddressField.setFont(font);
        stuPanel.add(stuAddressField, stuGbc);
        // Student DOB (as string, format yyyy-MM-dd)
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("DOB (yyyy-MM-dd):"), stuGbc);
        stuGbc.gridx = 1;
        stuDobField = new JTextField(10);
        stuDobField.setFont(font);
        stuPanel.add(stuDobField, stuGbc);
        // Student Department ID
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Department ID:"), stuGbc);
        stuGbc.gridx = 1;
        stuDepField = new JTextField(10);
        stuDepField.setFont(font);
        stuPanel.add(stuDepField, stuGbc);
        
        detailsPanel.add(stuPanel, "Student");
        
        // Faculty Panel
        JPanel facPanel = new JPanel(new GridBagLayout());
        GridBagConstraints facGbc = new GridBagConstraints();
        facGbc.insets = new Insets(5,5,5,5);
        facGbc.fill = GridBagConstraints.HORIZONTAL;
        int facRow = 0;
        // Faculty Name
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Name:"), facGbc);
        facGbc.gridx = 1;
        facNameField = new JTextField(20);
        facNameField.setFont(font);
        facPanel.add(facNameField, facGbc);
        // Faculty Email
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Email:"), facGbc);
        facGbc.gridx = 1;
        facEmailField = new JTextField(20);
        facEmailField.setFont(font);
        facPanel.add(facEmailField, facGbc);
        // Faculty Phone
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Phone (10 digits):"), facGbc);
        facGbc.gridx = 1;
        facPhoneField = new JTextField(15);
        facPhoneField.setFont(font);
        facPanel.add(facPhoneField, facGbc);
        // Faculty Department ID
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Department ID:"), facGbc);
        facGbc.gridx = 1;
        facDepField = new JTextField(10);
        facDepField.setFont(font);
        facPanel.add(facDepField, facGbc);
        
        detailsPanel.add(facPanel, "Faculty");
        
        // Admin Panel (for signup, we require email and phone)
        JPanel adminPanel = new JPanel(new GridBagLayout());
        GridBagConstraints adminGbc = new GridBagConstraints();
        adminGbc.insets = new Insets(5,5,5,5);
        adminGbc.fill = GridBagConstraints.HORIZONTAL;
        int adminRow = 0;
        adminGbc.gridx = 0; adminGbc.gridy = adminRow;
        adminPanel.add(new JLabel("Email:"), adminGbc);
        adminGbc.gridx = 1;
        adminEmailField = new JTextField(20);
        adminEmailField.setFont(font);
        adminPanel.add(adminEmailField, adminGbc);
        adminRow++;
        adminGbc.gridx = 0; adminGbc.gridy = adminRow;
        adminPanel.add(new JLabel("Phone (10 digits):"), adminGbc);
        adminGbc.gridx = 1;
        adminPhoneField = new JTextField(15);
        adminPhoneField.setFont(font);
        adminPanel.add(adminPhoneField, adminGbc);
        
        detailsPanel.add(adminPanel, "Admin");
        
        // Change the card based on role selection
        roleComboBox.addActionListener(e -> {
            String selectedRole = (String) roleComboBox.getSelectedItem();
            cardLayout.show(detailsPanel, selectedRole);
        });
        
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        // Initialize card layout to show Student by default
        cardLayout = (CardLayout) detailsPanel.getLayout();
        cardLayout.show(detailsPanel, "Student");
        mainPanel.add(detailsPanel, gbc);
        
        // Row 8: Register and Back buttons
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        registerButton = new JButton("Register");
        backButton = new JButton("Back to Login");
        btnPanel.add(registerButton);
        btnPanel.add(backButton);
        mainPanel.add(btnPanel, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Action Listeners
        registerButton.addActionListener(e -> registerUser());
        backButton.addActionListener(e -> {
            new LoginPage().setVisible(true);
            dispose();
        });
    }
    
    private void registerUser() {
        // Validate common fields
        String userIdStr = userIdField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String role = (String) roleComboBox.getSelectedItem();
        String phoneCommon = phoneField.getText().trim(); // we assume phoneField is common? 
        // In this design, we use role-specific phone fields.
        
        if(userIdStr.isEmpty() || password.isEmpty() || role.isEmpty()){
            JOptionPane.showMessageDialog(this, "User ID, Password, and Role are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int userId;
        try {
            userId = Integer.parseInt(userIdStr);
        } catch(NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "User ID must be a valid number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Validate password length (example: at least 6 characters)
        if(password.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters long.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Validate additional fields based on role
        if(role.equalsIgnoreCase("Student")) {
            String stuName = stuNameField.getText().trim();
            String stuEmail = stuEmailField.getText().trim();
            String stuPhone = stuPhoneField.getText().trim();
            String stuAddress = stuAddressField.getText().trim();
            String stuDobStr = stuDobField.getText().trim();
            String stuDep = stuDepField.getText().trim();
            
            if(stuName.isEmpty() || stuEmail.isEmpty() || stuPhone.isEmpty() ||
               stuAddress.isEmpty() || stuDobStr.isEmpty() || stuDep.isEmpty()){
                JOptionPane.showMessageDialog(this, "All Student fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[0-9]{10}$", stuPhone)) {
                JOptionPane.showMessageDialog(this, "Student phone must be exactly 10 digits.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[\\w.-]+@[\\w.-]+\\.com$", stuEmail)) {
                JOptionPane.showMessageDialog(this, "Student email must be valid and end with '.com'.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Validate date format for DOB
            Date stuDob;
            try {
                stuDob = new java.sql.Date(new SimpleDateFormat("yyyy-MM-dd").parse(stuDobStr).getTime());
            } catch(ParseException ex) {
                JOptionPane.showMessageDialog(this, "Date of Birth must be in format yyyy-MM-dd.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Validate department id
            try {
                Integer.parseInt(stuDep);
            } catch(NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Department ID must be a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // If valid, insert into Login_Table and Student table below.
            registerToDB(userId, password, role);
            insertStudent(userId, stuName, stuEmail, stuPhone, stuAddress, stuDob, Integer.parseInt(stuDep));
            
        } else if(role.equalsIgnoreCase("Faculty")) {
            String facName = facNameField.getText().trim();
            String facEmail = facEmailField.getText().trim();
            String facPhone = facPhoneField.getText().trim();
            String facDep = facDepField.getText().trim();
            
            if(facName.isEmpty() || facEmail.isEmpty() || facPhone.isEmpty() || facDep.isEmpty()){
                JOptionPane.showMessageDialog(this, "All Faculty fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[0-9]{10}$", facPhone)) {
                JOptionPane.showMessageDialog(this, "Faculty phone must be exactly 10 digits.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[\\w.-]+@[\\w.-]+\\.com$", facEmail)) {
                JOptionPane.showMessageDialog(this, "Faculty email must be valid and end with '.com'.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Integer.parseInt(facDep);
            } catch(NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Department ID must be a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            registerToDB(userId, password, role);
            insertFaculty(userId, facName, facEmail, facPhone, Integer.parseInt(facDep));
            
        } else if(role.equalsIgnoreCase("Admin")) {
            String adminEmail = adminEmailField.getText().trim();
            String adminPhone = adminPhoneField.getText().trim();
            
            if(adminEmail.isEmpty() || adminPhone.isEmpty()){
                JOptionPane.showMessageDialog(this, "Admin email and phone are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[0-9]{10}$", adminPhone)) {
                JOptionPane.showMessageDialog(this, "Admin phone must be exactly 10 digits.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if(!Pattern.matches("^[\\w.-]+@[\\w.-]+\\.com$", adminEmail)) {
                JOptionPane.showMessageDialog(this, "Admin email must be valid and end with '.com'.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            registerToDB(userId, password, role);
            insertAdmin(userId, adminEmail, adminPhone);
        }
        
        JOptionPane.showMessageDialog(this, "Registration successful!");
        new LoginPage().setVisible(true);
        dispose();
    }
    
    // Registers in Login_Table
    private void registerToDB(int userId, String password, String role) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Login_Table (Login_ID, U_Password, Role) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setString(2, password);
            stmt.setString(3, role);
            stmt.executeUpdate();
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error in registering login details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Inserts student details into Student table
    private void insertStudent(int stuId, String name, String email, String phone, String address, Date dob, int depId) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Student (Stu_ID, Stu_Name, Stu_Mail_ID, Stu_PhoneNO, Stu_address, Stu_DOB, dep_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, stuId);
            stmt.setString(2, name);
            stmt.setString(3, email);
            stmt.setString(4, phone);
            stmt.setString(5, address);
            stmt.setDate(6, dob);
            stmt.setInt(7, depId);
            stmt.executeUpdate();
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error inserting student details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Inserts faculty details into Faculty table
    private void insertFaculty(int facId, String name, String email, String phone, int depId) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Faculty (F_ID, F_Name, F_Mail_ID, F_PhoneNO, dep_id) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, facId);
            stmt.setString(2, name);
            stmt.setString(3, email);
            stmt.setString(4, phone);
            stmt.setInt(5, depId);
            stmt.executeUpdate();
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error inserting faculty details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Inserts admin details into Admin_Table
    private void insertAdmin(int adminId, String email, String phone) {
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Admin_Table (Ad_ID, Ad_Mail_ID, Ad_PhoneNO) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, adminId);
            stmt.setString(2, email);
            stmt.setString(3, phone);
            stmt.executeUpdate();
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error inserting admin details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SignUpPage().setVisible(true));
    }
}

/*import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

public class SignUpPage extends JFrame {
    // Common fields
    private JTextField userIdField;
    private JPasswordField passwordField;
    private JComboBox<String> roleComboBox; // Dropdown for role
    private JButton sendOtpButton, verifyOtpButton;
    private JTextField phoneField, otpField;
    
    // Card layout for role-specific details
    private JPanel detailsPanel; 
    private CardLayout cardLayout;
    
    // Student panel fields
    private JTextField stuNameField, stuEmailField, stuAddressField, stuDepField;
    private JSpinner stuDobSpinner;
    
    // Faculty panel fields
    private JTextField facNameField, facEmailField, facPhoneField, facDepField;
    
    // Admin panel fields
    private JTextField adminEmailField, adminPhoneField;
    
    // Buttons
    private JButton registerButton, backButton;
    
    // OTP variables
    private String generatedOtp;
    private boolean isOtpVerified = false;
    
    public SignUpPage() {
        super("Sign Up Page");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(550, 500);
        setLocationRelativeTo(null);
        initUI();
    }
    
    private void initUI() {
        // Main panel using GridBagLayout
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Font font = new Font("Georgia", Font.PLAIN, 14);
        
        int row = 0;
        // Title
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 3;
        JLabel titleLabel = new JLabel("Register New Account", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 20));
        mainPanel.add(titleLabel, gbc);
        
        // Row 1: User ID
        row++;
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("User ID (number):"), gbc);
        gbc.gridx = 1;
        userIdField = new JTextField(20);
        userIdField.setFont(font);
        mainPanel.add(userIdField, gbc);
        
        // Row 2: Password
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        passwordField.setFont(font);
        mainPanel.add(passwordField, gbc);
        
        // Row 3: Phone Number (for OTP)
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Phone Number:"), gbc);
        gbc.gridx = 1;
        phoneField = new JTextField(15);
        phoneField.setFont(font);
        mainPanel.add(phoneField, gbc);
        
        // Row 4: Send OTP button
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        sendOtpButton = new JButton("Send OTP");
        mainPanel.add(sendOtpButton, gbc);
        
        // Row 5: OTP field and Verify OTP button
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Enter OTP:"), gbc);
        gbc.gridx = 1;
        otpField = new JTextField(6);
        otpField.setFont(font);
        otpField.setEnabled(false);
        mainPanel.add(otpField, gbc);
        gbc.gridx = 2;
        verifyOtpButton = new JButton("Verify OTP");
        verifyOtpButton.setEnabled(false);
        mainPanel.add(verifyOtpButton, gbc);
        
        // Row 6: Role selection using dropdown
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Select Role:"), gbc);
        gbc.gridx = 1;
        String[] roles = {"Student", "Faculty", "Admin"};
        roleComboBox = new JComboBox<>(roles);
        roleComboBox.setFont(font);
        mainPanel.add(roleComboBox, gbc);
        
        // Row 7: Role-specific details using CardLayout
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 3;
        detailsPanel = new JPanel();
        cardLayout = new CardLayout();
        detailsPanel.setLayout(cardLayout);
        detailsPanel.setBorder(BorderFactory.createTitledBorder("Role Details"));
        
        // Student panel
        JPanel stuPanel = new JPanel(new GridBagLayout());
        GridBagConstraints stuGbc = new GridBagConstraints();
        stuGbc.insets = new Insets(5,5,5,5);
        stuGbc.fill = GridBagConstraints.HORIZONTAL;
        int stuRow = 0;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Name:"), stuGbc);
        stuGbc.gridx = 1;
        stuNameField = new JTextField(20);
        stuPanel.add(stuNameField, stuGbc);
        
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Email:"), stuGbc);
        stuGbc.gridx = 1;
        stuEmailField = new JTextField(20);
        stuPanel.add(stuEmailField, stuGbc);
        
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Address:"), stuGbc);
        stuGbc.gridx = 1;
        stuAddressField = new JTextField(20);
        stuPanel.add(stuAddressField, stuGbc);
        
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("DOB (yyyy-MM-dd):"), stuGbc);
        stuGbc.gridx = 1;
        SpinnerDateModel dateModel = new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH);
        stuDobSpinner = new JSpinner(dateModel);
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(stuDobSpinner, "yyyy-MM-dd");
        stuDobSpinner.setEditor(dateEditor);
        stuPanel.add(stuDobSpinner, stuGbc);
        
        stuRow++;
        stuGbc.gridx = 0; stuGbc.gridy = stuRow;
        stuPanel.add(new JLabel("Department ID:"), stuGbc);
        stuGbc.gridx = 1;
        stuDepField = new JTextField(10);
        stuPanel.add(stuDepField, stuGbc);
        
        detailsPanel.add(stuPanel, "Student");
        
        // Faculty panel
        JPanel facPanel = new JPanel(new GridBagLayout());
        GridBagConstraints facGbc = new GridBagConstraints();
        facGbc.insets = new Insets(5,5,5,5);
        facGbc.fill = GridBagConstraints.HORIZONTAL;
        int facRow = 0;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Name:"), facGbc);
        facGbc.gridx = 1;
        facNameField = new JTextField(20);
        facPanel.add(facNameField, facGbc);
        
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Email:"), facGbc);
        facGbc.gridx = 1;
        facEmailField = new JTextField(20);
        facPanel.add(facEmailField, facGbc);
        
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Phone:"), facGbc);
        facGbc.gridx = 1;
        facPhoneField = new JTextField(15);
        facPanel.add(facPhoneField, facGbc);
        
        facRow++;
        facGbc.gridx = 0; facGbc.gridy = facRow;
        facPanel.add(new JLabel("Department ID:"), facGbc);
        facGbc.gridx = 1;
        facDepField = new JTextField(10);
        facPanel.add(facDepField, facGbc);
        
        detailsPanel.add(facPanel, "Faculty");
        
        // Admin panel
        JPanel adminPanel = new JPanel(new GridBagLayout());
        GridBagConstraints adminGbc = new GridBagConstraints();
        adminGbc.insets = new Insets(5,5,5,5);
        adminGbc.fill = GridBagConstraints.HORIZONTAL;
        int adminRow = 0;
        adminGbc.gridx = 0; adminGbc.gridy = adminRow;
        adminPanel.add(new JLabel("Email:"), adminGbc);
        adminGbc.gridx = 1;
        adminEmailField = new JTextField(20);
        adminPanel.add(adminEmailField, adminGbc);
        
        adminRow++;
        adminGbc.gridx = 0; adminGbc.gridy = adminRow;
        adminPanel.add(new JLabel("Phone:"), adminGbc);
        adminGbc.gridx = 1;
        adminPhoneField = new JTextField(15);
        adminPanel.add(adminPhoneField, adminGbc);
        
        detailsPanel.add(adminPanel, "Admin");
        
        // Show default card based on dropdown selection
        roleComboBox.addActionListener(e -> {
            String selectedRole = (String) roleComboBox.getSelectedItem();
            cardLayout.show(detailsPanel, selectedRole);
        });
        
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        mainPanel.add(detailsPanel, gbc);
        
        // Row 8: Register and Back buttons
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        registerButton = new JButton("Register");
        backButton = new JButton("Back to Login");
        btnPanel.add(registerButton);
        btnPanel.add(backButton);
        mainPanel.add(btnPanel, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // OTP functionality: For simplicity, assume OTP is not required here; 
        // If needed, add phone and OTP fields above as demonstrated in previous examples.
        // For now, we assume registration proceeds after role details are filled.
        
        registerButton.addActionListener(e -> registerUser());
        backButton.addActionListener(e -> {
            new LoginPage().setVisible(true);
            dispose();
        });
    }
    
    private void registerUser() {
        // Validate common fields
        String userIdStr = userIdField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String role = (String) roleComboBox.getSelectedItem();
        if(userIdStr.isEmpty() || password.isEmpty() || role.isEmpty()){
            JOptionPane.showMessageDialog(this, "User ID, Password and Role are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int userId;
        try {
            userId = Integer.parseInt(userIdStr);
        } catch(NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "User ID must be a valid number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Insert into Login_Table
        try (Connection conn = DBConnection.getConnection()){
            String loginQuery = "INSERT INTO Login_Table (Login_ID, U_Password, Role) VALUES (?, ?, ?)";
            PreparedStatement loginStmt = conn.prepareStatement(loginQuery);
            loginStmt.setInt(1, userId);
            loginStmt.setString(2, password);
            loginStmt.setString(3, role);
            int loginRows = loginStmt.executeUpdate();
            if(loginRows <= 0){
                JOptionPane.showMessageDialog(this, "Registration failed on login details.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Based on role, insert into respective table
            if(role.equalsIgnoreCase("Student")) {
                // Get student details
                String stuName = stuNameField.getText().trim();
                String stuEmail = stuEmailField.getText().trim();
                String stuAddress = stuAddressField.getText().trim();
                String stuDep = stuDepField.getText().trim();
                Date stuDob = new Date(((SpinnerDateModel) stuDobSpinner.getModel()).getDate().getTime());
                
                if(stuName.isEmpty() || stuEmail.isEmpty() || stuAddress.isEmpty() || stuDep.isEmpty()){
                    JOptionPane.showMessageDialog(this, "All Student fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                String stuQuery = "INSERT INTO Student (Stu_ID, Stu_Name, Stu_Mail_ID, Stu_PhoneNO, Stu_address, Stu_DOB, dep_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement stuStmt = conn.prepareStatement(stuQuery);
                stuStmt.setInt(1, userId);
                stuStmt.setString(2, stuName);
                stuStmt.setString(3, stuEmail);
                stuStmt.setString(4, phoneField.getText().trim()); // Using common phone field
                stuStmt.setString(5, stuAddress);
                stuStmt.setDate(6, stuDob);
                stuStmt.setInt(7, Integer.parseInt(stuDep));
                stuStmt.executeUpdate();
            } else if(role.equalsIgnoreCase("Faculty")) {
                String facName = facNameField.getText().trim();
                String facEmail = facEmailField.getText().trim();
                String facPhone = facPhoneField.getText().trim();
                String facDep = facDepField.getText().trim();
                if(facName.isEmpty() || facEmail.isEmpty() || facPhone.isEmpty() || facDep.isEmpty()){
                    JOptionPane.showMessageDialog(this, "All Faculty fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String facQuery = "INSERT INTO Faculty (F_ID, F_Name, F_Mail_ID, F_PhoneNO, dep_id) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement facStmt = conn.prepareStatement(facQuery);
                facStmt.setInt(1, userId);
                facStmt.setString(2, facName);
                facStmt.setString(3, facEmail);
                facStmt.setString(4, facPhone);
                facStmt.setInt(5, Integer.parseInt(facDep));
                facStmt.executeUpdate();
            } else if(role.equalsIgnoreCase("Admin")) {
                String adminEmail = adminEmailField.getText().trim();
                String adminPhone = adminPhoneField.getText().trim();
                if(adminEmail.isEmpty() || adminPhone.isEmpty()){
                    JOptionPane.showMessageDialog(this, "All Admin fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String adminQuery = "INSERT INTO Admin_Table (Ad_ID, Ad_Mail_ID, Ad_PhoneNO) VALUES (?, ?, ?)";
                PreparedStatement adminStmt = conn.prepareStatement(adminQuery);
                adminStmt.setInt(1, userId);
                adminStmt.setString(2, adminEmail);
                adminStmt.setString(3, adminPhone);
                adminStmt.executeUpdate();
            }
            
            JOptionPane.showMessageDialog(this, "Registration successful!");
            new LoginPage().setVisible(true);
            dispose();
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SignUpPage().setVisible(true));
    }
}
/*import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Random;

public class SignUpPage extends JFrame {
    // Form fields
    private JTextField userIdField, phoneField, roleField, otpField;
    private JPasswordField passwordField;
    private JButton sendOtpButton, verifyOtpButton, registerButton, backButton;
    
    // Stores the generated OTP and verification status
    private String generatedOtp;
    private boolean isOtpVerified = false;
    
    public SignUpPage() {
        super("Sign Up Page");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);
        initUI();
    }
    
    private void initUI() {
        // Main panel with GridBagLayout for a neat layout
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        Font font = new Font("Georgia", Font.PLAIN, 14);
        int row = 0;
        
        // Title
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 3;
        JLabel titleLabel = new JLabel("Register New Account", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 18));
        panel.add(titleLabel, gbc);
        
        // Row 1: User ID
        row++;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("User ID (number):"), gbc);
        gbc.gridx = 1;
        userIdField = new JTextField(20);
        userIdField.setFont(font);
        panel.add(userIdField, gbc);
        
        // Row 2: Phone Number
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Phone Number:"), gbc);
        gbc.gridx = 1;
        phoneField = new JTextField(15);
        phoneField.setFont(font);
        panel.add(phoneField, gbc);
        
        // Row 3: Send OTP button
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        sendOtpButton = new JButton("Send OTP");
        panel.add(sendOtpButton, gbc);
        
        // Row 4: OTP field and Verify OTP button
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Enter OTP:"), gbc);
        gbc.gridx = 1;
        otpField = new JTextField(6);
        otpField.setFont(font);
        otpField.setEnabled(false);
        panel.add(otpField, gbc);
        gbc.gridx = 2;
        verifyOtpButton = new JButton("Verify OTP");
        verifyOtpButton.setEnabled(false);
        panel.add(verifyOtpButton, gbc);
        
        // Row 5: Password field
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        passwordField.setFont(font);
        panel.add(passwordField, gbc);
        
        // Row 6: Role field
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Role (Student/Faculty/Admin):"), gbc);
        gbc.gridx = 1;
        roleField = new JTextField(20);
        roleField.setFont(font);
        panel.add(roleField, gbc);
        
        // Row 7: Buttons: Register and Back
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 3;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        registerButton = new JButton("Register");
        // Initially disable register button until OTP verified
        registerButton.setEnabled(false);
        backButton = new JButton("Back to Login");
        btnPanel.add(registerButton);
        btnPanel.add(backButton);
        panel.add(btnPanel, gbc);
        
        add(panel, BorderLayout.CENTER);
        
        // Action listeners for OTP functionality and registration
        sendOtpButton.addActionListener(e -> sendOtp());
        verifyOtpButton.addActionListener(e -> verifyOtp());
        registerButton.addActionListener(e -> registerUser());
        backButton.addActionListener(e -> {
            new LoginPage().setVisible(true);
            dispose();
        });
    }
    
    private void sendOtp() {
        String phone = phoneField.getText().trim();
        if (phone.isEmpty() || !phone.matches("\\d{10,15}")) { // simple check for digits (10-15)
            JOptionPane.showMessageDialog(this, "Enter a valid phone number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // For demo: generate a 6-digit OTP and display it in a dialog
        generatedOtp = String.valueOf(100000 + new Random().nextInt(900000));
        JOptionPane.showMessageDialog(this, "OTP sent to " + phone + "\n(For demo, OTP is: " + generatedOtp + ")");
        otpField.setEnabled(true);
        verifyOtpButton.setEnabled(true);
    }
    
    private void verifyOtp() {
        String enteredOtp = otpField.getText().trim();
        if (generatedOtp != null && generatedOtp.equals(enteredOtp)) {
            JOptionPane.showMessageDialog(this, "OTP Verified Successfully!");
            isOtpVerified = true;
            // Enable registration only after OTP verification
            registerButton.setEnabled(true);
            // Optionally disable OTP fields
            otpField.setEnabled(false);
            verifyOtpButton.setEnabled(false);
        } else {
            JOptionPane.showMessageDialog(this, "Invalid OTP. Please try again.", "Verification Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void registerUser() {
        if (!isOtpVerified) {
            JOptionPane.showMessageDialog(this, "Please verify OTP first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String userIdStr = userIdField.getText().trim();
        String password = passwordField.getText().trim();
        String role = roleField.getText().trim();
        if(userIdStr.isEmpty() || password.isEmpty() || role.isEmpty()){
            JOptionPane.showMessageDialog(this, "All fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int userId;
        try {
            userId = Integer.parseInt(userIdStr);
        } catch(NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "User ID must be a valid number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Insert into Login_Table
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Login_Table (Login_ID, U_Password, Role) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setString(2, password);
            stmt.setString(3, role);
            int rows = stmt.executeUpdate();
            if(rows > 0){
                JOptionPane.showMessageDialog(this, "Registration successful!");
                new LoginPage().setVisible(true);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Registration failed.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SignUpPage().setVisible(true));
    }
}

/*import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class SignUpPage extends JFrame {
    private JTextField userIdField, passwordField, roleField;
    private JButton registerButton, backButton;

    public SignUpPage() {
        super("Sign Up Page");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        initUI();
    }

    private void initUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("Register New Account", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 18));
        panel.add(titleLabel, gbc);

        // User ID
        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("User ID (number):"), gbc);
        gbc.gridx = 1;
        userIdField = new JTextField(20);
        panel.add(userIdField, gbc);

        // Password
        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JTextField(20); // For demo; use JPasswordField for security
        panel.add(passwordField, gbc);

        // Role
        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Role (Student/Faculty/Admin):"), gbc);
        gbc.gridx = 1;
        roleField = new JTextField(20);
        panel.add(roleField, gbc);

        // Buttons
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel();
        registerButton = new JButton("Register");
        backButton = new JButton("Back to Login");
        btnPanel.add(registerButton);
        btnPanel.add(backButton);
        panel.add(btnPanel, gbc);

        add(panel, BorderLayout.CENTER);

        registerButton.addActionListener(e -> registerUser());
        backButton.addActionListener(e -> {
            new LoginPage().setVisible(true);
            dispose();
        });
    }

    private void registerUser() {
        String userIdStr = userIdField.getText().trim();
        String password = passwordField.getText().trim();
        String role = roleField.getText().trim();
        if(userIdStr.isEmpty() || password.isEmpty() || role.isEmpty()){
            JOptionPane.showMessageDialog(this, "All fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int userId;
        try {
            userId = Integer.parseInt(userIdStr);
        } catch(NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "User ID must be a valid number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try (Connection conn = DBConnection.getConnection()){
            String query = "INSERT INTO Login_Table (Login_ID, U_Password, Role) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setString(2, password);
            stmt.setString(3, role);
            int rows = stmt.executeUpdate();
            if(rows > 0){
                JOptionPane.showMessageDialog(this, "Registration successful!");
                new LoginPage().setVisible(true);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Registration failed.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch(SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SignUpPage().setVisible(true));
    }
}
/*import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.regex.Pattern;

public class SignUpPage extends JFrame {

    // UI Components
    private JTextField nameField, phoneField, emailField, idField;
    private JPasswordField passwordField, confirmPasswordField;
    private JComboBox<String> roleComboBox;
    private JSpinner dobSpinner;
    private JLabel ageLabel;
    private JButton sendOtpButton, verifyOtpButton, submitButton;
    private JTextField otpField;
    private JRadioButton maleRadioButton, femaleRadioButton;
    private ButtonGroup genderGroup;

    // To store generated OTP
    private String generatedOtp;

    public SignUpPage() {
        super("Library Management System - Sign Up");
        // Build the UI
        initComponents();
    }

    private void initComponents() {
        // Set default close operation and layout
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create a panel using GridBagLayout for our form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // spacing between components
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Font font = new Font("Georgia", Font.PLAIN, 14);

        int row = 0;

        // Row 0: Role Selection
        gbc.gridx = 0;
        gbc.gridy = row;
        formPanel.add(new JLabel("Select Role:"), gbc);
        gbc.gridx = 1;
        roleComboBox = new JComboBox<>(new String[]{"Student", "Faculty"});
        roleComboBox.setFont(font);
        formPanel.add(roleComboBox, gbc);

        // Row 1: Full Name
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        formPanel.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 1;
        nameField = new JTextField(20);
        nameField.setFont(font);
        formPanel.add(nameField, gbc);

        // Row 2: Phone Number
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        formPanel.add(new JLabel("Phone Number:"), gbc);
        gbc.gridx = 1;
        phoneField = new JTextField(10);
        phoneField.setFont(font);
        formPanel.add(phoneField, gbc);

        // Row 3: Send OTP Button
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        sendOtpButton = new JButton("Send OTP");
        formPanel.add(sendOtpButton, gbc);

        // Row 4: OTP Field and Verify OTP Button
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        formPanel.add(new JLabel("Enter OTP:"), gbc);
        gbc.gridx = 1;
        otpField = new JTextField(6);
        otpField.setFont(font);
        otpField.setEnabled(false);
        formPanel.add(otpField, gbc);
        gbc.gridx = 2;
        verifyOtpButton = new JButton("Verify OTP");
        verifyOtpButton.setEnabled(false);
        formPanel.add(verifyOtpButton, gbc);

        // Row 5: Gender
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        formPanel.add(new JLabel("Gender:"), gbc);
        gbc.gridx = 1;
        JPanel genderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        maleRadioButton = new JRadioButton("Male");
        femaleRadioButton = new JRadioButton("Female");
        maleRadioButton.setFont(font);
        femaleRadioButton.setFont(font);
        genderGroup = new ButtonGroup();
        genderGroup.add(maleRadioButton);
        genderGroup.add(femaleRadioButton);
        genderPanel.add(maleRadioButton);
        genderPanel.add(femaleRadioButton);
        formPanel.add(genderPanel, gbc);

        // Row 6: Date of Birth (with spinner)
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        formPanel.add(new JLabel("Date of Birth:"), gbc);
        gbc.gridx = 1;
        SpinnerDateModel dateModel = new SpinnerDateModel();
        dobSpinner = new JSpinner(dateModel);
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dobSpinner, "yyyy-MM-dd");
        dobSpinner.setEditor(dateEditor);
        formPanel.add(dobSpinner, gbc);

        // Row 7: Age Label (calculated)
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        formPanel.add(new JLabel("Age:"), gbc);
        gbc.gridx = 1;
        ageLabel = new JLabel("Age will be calculated");
        ageLabel.setFont(font);
        formPanel.add(ageLabel, gbc);

        // Update age whenever DOB changes
        dobSpinner.addChangeListener((ChangeEvent e) -> {
            Date dob = (Date) dobSpinner.getValue();
            int age = calculateAge(dob);
            ageLabel.setText(age + " years");
        });

        // Row 8: ID Field
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        formPanel.add(new JLabel("ID:"), gbc);
        gbc.gridx = 1;
        idField = new JTextField(20);
        idField.setFont(font);
        formPanel.add(idField, gbc);

        // Row 9: Email Field
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        emailField = new JTextField(20);
        emailField.setFont(font);
        formPanel.add(emailField, gbc);

        // Row 10: Password Field
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        formPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        passwordField.setFont(font);
        formPanel.add(passwordField, gbc);

        // Row 11: Confirm Password Field
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        formPanel.add(new JLabel("Confirm Password:"), gbc);
        gbc.gridx = 1;
        confirmPasswordField = new JPasswordField(20);
        confirmPasswordField.setFont(font);
        formPanel.add(confirmPasswordField, gbc);

        // Row 12: Submit Button (spanning two columns)
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        submitButton = new JButton("Register");
        formPanel.add(submitButton, gbc);

        // Add action listeners
        sendOtpButton.addActionListener(e -> sendOtp());
        verifyOtpButton.addActionListener(e -> verifyOtp());
        submitButton.addActionListener(e -> {
            if (validateForm()) {
                // In a full implementation, you would now save the details to a database.
                JOptionPane.showMessageDialog(SignUpPage.this, "Registration Successful!");
            }
        });

        // Add the form panel to the frame
        add(formPanel, BorderLayout.CENTER);
        pack(); // Size frame according to components
        setLocationRelativeTo(null); // Center on screen
    }

    
    private void sendOtp() {
        String phone = phoneField.getText().trim();
        if (!isValidPhoneNumber(phone)) {
            JOptionPane.showMessageDialog(this, "Please enter a valid 10-digit phone number.");
            return;
        }
        // For demo, we generate a random OTP and show it in a dialog.
        generatedOtp = String.valueOf(100000 + new Random().nextInt(900000));
        JOptionPane.showMessageDialog(this, "OTP sent to " + phone + "\nOTP: " + generatedOtp);
        otpField.setEnabled(true);
        verifyOtpButton.setEnabled(true);
    }

    
    private void verifyOtp() {
        String enteredOtp = otpField.getText().trim();
        if (generatedOtp != null && generatedOtp.equals(enteredOtp)) {
            JOptionPane.showMessageDialog(this, "OTP Verified Successfully!");
            // Optionally disable the OTP field and verify button after verification
            otpField.setEnabled(false);
            verifyOtpButton.setEnabled(false);
        } else {
            JOptionPane.showMessageDialog(this, "Invalid OTP. Please try again.");
        }
    }

    
    private boolean validateForm() {
        if (nameField.getText().trim().isEmpty() ||
            phoneField.getText().trim().isEmpty() ||
            emailField.getText().trim().isEmpty() ||
            idField.getText().trim().isEmpty() ||
            new String(passwordField.getPassword()).isEmpty() ||
            new String(confirmPasswordField.getPassword()).isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.");
            return false;
        }
        if (!isValidEmail(emailField.getText().trim())) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address.");
            return false;
        }
        String pass = new String(passwordField.getPassword());
        String confirmPass = new String(confirmPasswordField.getPassword());
        if (!pass.equals(confirmPass)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.");
            return false;
        }
        return true;
    }

   
    private boolean isValidPhoneNumber(String phone) {
        return Pattern.matches("\\d{10}", phone);
    }

    
    private boolean isValidEmail(String email) {
        String emailRegex = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";
        return Pattern.matches(emailRegex, email);
    }

   
    private int calculateAge(Date dob) {
        Calendar dobCal = Calendar.getInstance();
        dobCal.setTime(dob);
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age;
    }

   
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashed = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Password hashing failed", ex);
        }
    }

    public static void main(String[] args) {
        // Launch the UI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            new SignUpPage().setVisible(true);
        });
    }
}

/*import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SignUpPage extends JFrame {
    // UI Components
    private JTextField nameField, phoneNumberField, emailField, idField, departmentField, addressField, otpField;
    private JPasswordField passwordField, confirmPasswordField;
    private JComboBox<String> roleComboBox;
    private JSpinner dobSpinner;
    private JLabel ageLabel;
    private JRadioButton maleRadioButton, femaleRadioButton;
    private ButtonGroup genderGroup;
    private JButton submitButton, sendOtpButton, verifyOtpButton;

    private String generatedOtp; // To store generated OTP for verification

    public SignUpPage() {
        // Initialize the frame
        initializeFrame();

        // Create and arrange components
        createComponents();

        // Set up event listeners
        setupEventListeners();
    }
    

    
    private void initializeFrame() {
        setTitle("Library Management System - Sign Up");
        setSize(500, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(0, 2, 10, 10)); // Flexible grid layout
    }

   
    private void createComponents() {
        // Role Selection
        add(new JLabel("Select Role:"));
        roleComboBox = new JComboBox<>(new String[]{"Select Role", "Student", "Faculty"});
        add(roleComboBox);

        // Name Field
        add(new JLabel("Full Name:"));
        nameField = new JTextField(20);
        add(nameField);

        // Phone Number
        add(new JLabel("Phone Number:"));
        phoneNumberField = new JTextField(10);
        add(phoneNumberField);

        // Send OTP Button
        sendOtpButton = new JButton("Send OTP");
        add(sendOtpButton);

        // OTP Field
        add(new JLabel("Enter OTP:"));
        otpField = new JTextField(6);
        otpField.setEnabled(false); // Initially disabled until OTP is sent
        add(otpField);

        // Verify OTP Button
        verifyOtpButton = new JButton("Verify OTP");
        verifyOtpButton.setEnabled(false); // Initially disabled until OTP is sent
        add(verifyOtpButton);

        // Gender Selection
        add(new JLabel("Gender:"));
        JPanel genderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        genderGroup = new ButtonGroup();
        maleRadioButton = new JRadioButton("Male");
        femaleRadioButton = new JRadioButton("Female");
        genderGroup.add(maleRadioButton);
        genderGroup.add(femaleRadioButton);
        genderPanel.add(maleRadioButton);
        genderPanel.add(femaleRadioButton);
        add(genderPanel);

        // Date of Birth with Age Calculation
        add(new JLabel("Date of Birth:"));
        SpinnerDateModel dateModel = new SpinnerDateModel();
        dobSpinner = new JSpinner(dateModel);
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dobSpinner, "yyyy-MM-dd");
        dobSpinner.setEditor(dateEditor);
        add(dobSpinner);

        // Age Label
        add(new JLabel("Age:"));
        ageLabel = new JLabel("Age will be calculated");
        add(ageLabel);

        // Update age when date changes
        dobSpinner.addChangeListener(e -> {
            Date selectedDate = (Date) dobSpinner.getValue();
            int age = calculateAge(selectedDate);
            ageLabel.setText(age + " years");
        });

        // ID Field (dynamic based on role)
        add(new JLabel("ID:"));
        idField = new JTextField(20);
        add(idField);

        // Email Field
        add(new JLabel("Email:"));
        emailField = new JTextField(20);
        add(emailField);

        // Department Field
        add(new JLabel("Department:"));
        departmentField = new JTextField(20);
        add(departmentField);

        // Conditional Address Field (for students)
        add(new JLabel("Address (Students):"));
        addressField = new JTextField(20);
        add(addressField);

        // Password Fields
        add(new JLabel("Password:"));
        passwordField = new JPasswordField(20);
        add(passwordField);

        add(new JLabel("Confirm Password:"));
        confirmPasswordField = new JPasswordField(20);
        add(confirmPasswordField);

        // Submit Button
        submitButton = new JButton("Register");
        add(submitButton);
    }

   
    private void setupEventListeners() {
        // Role Selection Listener
    	roleComboBox.addActionListener(e -> {
    	    String selectedRole = (String) roleComboBox.getSelectedItem();
    	    resetForm();  // Reset form data when the role is selected
    	    updateComponentsBasedOnRole(selectedRole);
    	});

        // Send OTP Button Listener
        sendOtpButton.addActionListener(e -> sendOtp());

        // Verify OTP Button Listener
        verifyOtpButton.addActionListener(e -> verifyOtp());

        // Submit Button Listener
        submitButton.addActionListener(e -> {
            if (validateAndSubmitForm()) {
                saveUserDetails();
            }
        });
    }
    private void resetForm() {
        // Clear all fields
        nameField.setText("");
        phoneNumberField.setText("");
        emailField.setText("");
        idField.setText("");
        departmentField.setText("");
        addressField.setText("");
        passwordField.setText("");
        confirmPasswordField.setText("");
        otpField.setText("");
        ageLabel.setText("Age will be calculated");

        // Reset gender selection
        genderGroup.clearSelection();

        // Disable the OTP fields and buttons initially
        otpField.setEnabled(false);
        verifyOtpButton.setEnabled(false);
    }

    
    private void updateComponentsBasedOnRole(String role) {
        // Enable the address field only for students
        addressField.setEnabled(role.equals("Student"));

        // Update other fields based on role if needed (e.g., Faculty-specific fields)
        if (role.equals("Faculty")) {
            departmentField.setEnabled(true);  // Example: Enable the department field for Faculty
        } else {
            departmentField.setEnabled(false);
        }
    }

    
    private void sendOtp() {
        String phoneNumber = phoneNumberField.getText();
        if (isValidPhoneNumber(phoneNumber)) {
            generatedOtp = String.valueOf(new Random().nextInt(999999 - 100000 + 1) + 100000);
            JOptionPane.showMessageDialog(this, "OTP sent to: " + phoneNumber + "\nOTP: " + generatedOtp);
            otpField.setEnabled(true);
            verifyOtpButton.setEnabled(true);
        } else {
            showError("Enter a valid phone number.");
        }
    }

   
    private void verifyOtp() {
        String enteredOtp = otpField.getText();
        if (generatedOtp != null && generatedOtp.equals(enteredOtp)) {
            JOptionPane.showMessageDialog(this, "OTP Verified Successfully!");
            otpField.setEnabled(false);
            verifyOtpButton.setEnabled(false);
        } else {
            showError("Invalid OTP. Please try again.");
        }
    }

   
    private boolean validateAndSubmitForm() {
        String name = nameField.getText();
        String phoneNumber = phoneNumberField.getText();
        String email = emailField.getText();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        if (name.isEmpty() || phoneNumber.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("All fields are required!");
            return false;
        }
        if (!isValidPhoneNumber(phoneNumber)) {
            showError("Invalid phone number.");
            return false;
        }
        if (!isValidEmail(email)) {
            showError("Invalid email address.");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match.");
            return false;
        }
        return true;
    }

   /
    private void saveUserDetails() {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/your_database", "username", "password")) {
            String query = "INSERT INTO users (name, phone, email, password) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, nameField.getText());
            stmt.setString(2, phoneNumberField.getText());
            stmt.setString(3, emailField.getText());
            stmt.setString(4, new String(passwordField.getPassword())); // Store raw password here
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Registration successful!");
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Database error. Please try again.");
        }
    }


    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private int calculateAge(Date dob) {
        Calendar dobCalendar = Calendar.getInstance();
        dobCalendar.setTime(dob);
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dobCalendar.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < dobCalendar.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age;
    }

    
    private boolean isValidPhoneNumber(String phoneNumber) {
        return Pattern.matches("\\d{10}", phoneNumber);
    }

    private boolean isValidEmail(String email) {
        return Pattern.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$", email);
    }

    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SignUpPage().setVisible(true));
    }
}

/*import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.Random;
import java.util.regex.Pattern;
import java.sql.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class SignUpPage extends JFrame {
    // UI Components
    private JTextField phoneNumberField;
    private JTextField nameField;
    private JTextField emailField, addressField, departmentField, idField;
    private JTextField otpField;
    private JButton sendOtpButton;
    private JButton verifyOtpButton;
    private JButton submitButton;
    private JComboBox<String> roleComboBox;
    private JPasswordField passwordField, confirmPasswordField;
    private JSpinner dobSpinner;
    private JPanel detailsPanel;

    // OTP and Verification Variables
    private String generatedOtp;
    private boolean isOtpVerified = false;

    public SignUpPage() {
        initializeFrame();
        createComponents();
        setupEventListeners();
    }

    private void initializeFrame() {
        setTitle("Library Management System - Sign Up");
        setSize(500, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new FlowLayout());
    }

    private void createComponents() {
        // Name Field
        add(new JLabel("Full Name:"));
        nameField = new JTextField(20);
        nameField.setToolTipText("Enter your full name");
        add(nameField);

        // Phone Number Field
        add(new JLabel("Phone Number:"));
        phoneNumberField = new JTextField(10);
        phoneNumberField.setToolTipText("Enter 10-digit mobile number");
        add(phoneNumberField);

        // OTP Components
        sendOtpButton = new JButton("Send OTP");
        add(sendOtpButton);

        otpField = new JTextField(6);
        otpField.setEditable(false);
        add(new JLabel("Enter OTP:"));
        add(otpField);

        verifyOtpButton = new JButton("Verify OTP");
        verifyOtpButton.setEnabled(false);
        add(verifyOtpButton);

        // Role Selection
        add(new JLabel("Select Role:"));
        String[] roles = {"Student", "Faculty"};
        roleComboBox = new JComboBox<>(roles);
        roleComboBox.setEnabled(false);
        add(roleComboBox);

        // ID Field
        add(new JLabel("ID (Student/Faculty):"));
        idField = new JTextField(20);
        idField.setEditable(false);
        add(idField);

        // Password Fields
        add(new JLabel("Password:"));
        passwordField = new JPasswordField(20);
        passwordField.setToolTipText("Minimum 8 characters, include uppercase, lowercase, and numbers");
        add(passwordField);

        add(new JLabel("Confirm Password:"));
        confirmPasswordField = new JPasswordField(20);
        add(confirmPasswordField);

        // Date of Birth
        add(new JLabel("Date of Birth:"));
        SpinnerDateModel dateModel = new SpinnerDateModel();
        dobSpinner = new JSpinner(dateModel);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(dobSpinner, "yyyy-MM-dd");
        dobSpinner.setEditor(editor);
        add(dobSpinner);

        // Details Panel
        detailsPanel = new JPanel(new GridLayout(0, 2));
        add(detailsPanel);

        // Submit Button
        submitButton = new JButton("Register");
        submitButton.setEnabled(false);
        add(submitButton);
    }
    

    private void setupEventListeners() {
        // Send OTP Button Listener
    	// Send OTP Button Listener
    	sendOtpButton.addActionListener(e -> {
    	    String phoneNumber = phoneNumberField.getText().trim();
    	    
    	    if (!validatePhoneNumber(phoneNumber)) {
    	        return;
    	    }

    	    if (isPhoneNumberRegistered(phoneNumber)) {
    	        JOptionPane.showMessageDialog(this, 
    	            "Phone number is already registered!", 
    	            "Error", 
    	            JOptionPane.ERROR_MESSAGE);
    	        return;
    	    }

    	    generatedOtp = generateOtp();
    	    JOptionPane.showMessageDialog(this, 
    	        "OTP sent to " + phoneNumber, 
    	        "OTP Sent", 
    	        JOptionPane.INFORMATION_MESSAGE);

    	    otpField.setEditable(true);  // Allow the user to enter OTP
    	    otpField.setText(generatedOtp); // Show the generated OTP in the text field
    	    verifyOtpButton.setEnabled(true);  // Enable the Verify OTP button
    	});

    	// Verify OTP Button Listener
    	verifyOtpButton.addActionListener(e -> {
    	    String enteredOtp = otpField.getText().trim();
    	    
    	    if (enteredOtp.equals(generatedOtp)) {
    	        isOtpVerified = true;
    	        JOptionPane.showMessageDialog(this, 
    	            "OTP Verified Successfully!", 
    	            "Verification", 
    	            JOptionPane.INFORMATION_MESSAGE);
    	        
    	        // Hide OTP Field and Clear it for Security
    	        otpField.setText("");  // Clear OTP field
    	        otpField.setEditable(false);  // Disable the OTP field to prevent further edits
    	        generatedOtp = null;  // Nullify the generated OTP for security
    	        
    	        roleComboBox.setEnabled(true);
    	        verifyOtpButton.setEnabled(false);  // Disable the Verify OTP button after successful verification
    	    } else {
    	        isOtpVerified = false;
    	        JOptionPane.showMessageDialog(this, 
    	            "Invalid OTP. Please try again.", 
    	            "Error", 
    	            JOptionPane.ERROR_MESSAGE);
    	    }
    	});


        // Verify OTP Button Listener
        verifyOtpButton.addActionListener(e -> {
            String enteredOtp = otpField.getText().trim();
            
            if (enteredOtp.equals(generatedOtp)) {
                isOtpVerified = true;
                JOptionPane.showMessageDialog(this, 
                    "OTP Verified Successfully!", 
                    "Verification", 
                    JOptionPane.INFORMATION_MESSAGE);
                
                roleComboBox.setEnabled(true);
                verifyOtpButton.setEnabled(false);
            } else {
                isOtpVerified = false;
                JOptionPane.showMessageDialog(this, 
                    "Invalid OTP. Please try again.", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        // Role Selection Listener
        roleComboBox.addActionListener(e -> {
            String selectedRole = (String) roleComboBox.getSelectedItem();
            idField.setEditable(true);
            updateDetailsPanel(selectedRole);
        });

        // Submit Button Listener
        submitButton.addActionListener(e -> {
            if (validateAndRegisterUser()) {
                JOptionPane.showMessageDialog(this, 
                    "Registration Successful!", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
                dispose(); // Close signup window
                new LoginPage().setVisible(true);
            }
        });
    }
    

    private boolean validatePhoneNumber(String phoneNumber) {
        if (phoneNumber.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Phone number cannot be empty!", 
                "Validation Error", 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (!phoneNumber.matches("\\d{10}")) {
            JOptionPane.showMessageDialog(this, 
                "Please enter a valid 10-digit phone number!", 
                "Validation Error", 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }
    

    private void updateDetailsPanel(String role) {
        detailsPanel.removeAll();

        // Common for both Student and Faculty
        detailsPanel.add(new JLabel("Email:"));
        emailField = new JTextField(20);
        detailsPanel.add(emailField);

        if (role.equals("Student")) {
            detailsPanel.add(new JLabel("Address:"));
            addressField = new JTextField(20);
            detailsPanel.add(addressField);
        }

        detailsPanel.add(new JLabel("Department:"));
        departmentField = new JTextField(20);
        detailsPanel.add(departmentField);

        detailsPanel.revalidate();
        detailsPanel.repaint();
        submitButton.setEnabled(true);
    }

    private boolean validateAndRegisterUser() {
        // Validate Name
        String name = nameField.getText().trim();
        if (name.isEmpty() || name.length() < 2) {
            JOptionPane.showMessageDialog(this, 
                "Please enter a valid name!", 
                "Validation Error", 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Validate ID
        String id = idField.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "ID cannot be empty!", 
                "Validation Error", 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Validate Passwords
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, 
                "Passwords do not match!", 
                "Validation Error", 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (!isStrongPassword(password)) {
            JOptionPane.showMessageDialog(this, 
                "Password must be at least 8 characters with uppercase, lowercase, and numbers!", 
                "Validation Error", 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Validate Date of Birth
        Date dob = (Date) dobSpinner.getValue();
        int age = calculateAge(dob);

        if (age < 16) {
            JOptionPane.showMessageDialog(this, 
                "You must be at least 16 years old to register!", 
                "Age Restriction", 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Validate Email
        String email = emailField.getText().trim();
        if (!isValidEmail(email)) {
            JOptionPane.showMessageDialog(this, 
                "Please enter a valid email address!", 
                "Validation Error", 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
        

        // Registration Logic
        try {
            String role = (String) roleComboBox.getSelectedItem();
            String phoneNumber = phoneNumberField.getText().trim();
            String department = departmentField.getText().trim();
            String hashedPassword = hashPassword(password);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String dobString = sdf.format(dob);

            // Save User Details
            saveUserDetails(
                role, id, name, phoneNumber, email, 
                addressField != null ? addressField.getText().trim() : "",
                department, dobString, age, hashedPassword
            );

            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Registration failed: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    yprivate void saveUserDetails(String role, String id, String name, String phoneNumber, 
            String email, String address, String department, 
            String dob, int age, String rawPassword) {
        Connection conn = null;
        
        try {
            // Establish database connection
            conn = DBConnection.getConnection();

            // Disable auto-commit for transaction management
            conn.setAutoCommit(false);

            // Rest of the code remains the same as in the original implementation
            // Only change is in the Login table insertion, where we use rawPassword instead of hashedPassword
            String loginSql = "INSERT INTO Login " +
            "(User_ID, Password, Role, Phone_Number) " +
            "VALUES (?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(loginSql)) {
                stmt.setString(1, id);
                stmt.setString(2, rawPassword);  // Store raw password
                stmt.setString(3, role);
                stmt.setString(4, phoneNumber);

                stmt.executeUpdate();
            }

            // Commit the transaction
            conn.commit();

        } catch (SQLException e) {
            // Error handling remains the same
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }

            System.err.println("Database Error: " + e.getMessage());
            throw new RuntimeException("Failed to save user details: " + e.getMessage(), e);

        } finally {
            // Connection closing remains the same
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }    private boolean isPhoneNumberRegistered(String phoneNumber) {
        try (Connection conn = DBConnection.getConnection()) {
            // Check in the Login table
            String loginQuery = "SELECT COUNT(*) FROM Login WHERE Phone_Number = ?";
            PreparedStatement loginStmt = conn.prepareStatement(loginQuery);
            loginStmt.setString(1, phoneNumber);
            ResultSet loginRs = loginStmt.executeQuery();

            if (loginRs.next() && loginRs.getInt(1) > 0) {
                return true; // Phone number already exists in Login table
            }

            // Check in the Student table
            String studentQuery = "SELECT COUNT(*) FROM Student WHERE Stu_PhoneNO = ?";
            PreparedStatement studentStmt = conn.prepareStatement(studentQuery);
            studentStmt.setString(1, phoneNumber);
            ResultSet studentRs = studentStmt.executeQuery();

            if (studentRs.next() && studentRs.getInt(1) > 0) {
                return true; // Phone number already exists in Student table
            }

            // Check in the Faculty table (if needed)
            String facultyQuery = "SELECT COUNT(*) FROM Faculty WHERE F_PhoneNO = ?";
            PreparedStatement facultyStmt = conn.prepareStatement(facultyQuery);
            facultyStmt.setString(1, phoneNumber);
            ResultSet facultyRs = facultyStmt.executeQuery();

            if (facultyRs.next() && facultyRs.getInt(1) > 0) {
                return true; // Phone number already exists in Faculty table
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false; // Phone number is not registered
    }


 private int calculateAge(Date dob) {
        Calendar dobCalendar = Calendar.getInstance();
        dobCalendar.setTime(dob);

        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dobCalendar.get(Calendar.YEAR);

        if (today.get(Calendar.MONTH) < dobCalendar.get(Calendar.MONTH) ||
            (today.get(Calendar.MONTH) == dobCalendar.get(Calendar.MONTH) &&
             today.get(Calendar.DAY_OF_MONTH) < dobCalendar.get(Calendar.DAY_OF_MONTH))) {
            age--;
        }

        return age;
    } private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // Generates a 6-digit OTP
        return String.valueOf(otp);
    }
    // Existing methods like generateOtp(), calculateAge(), 
    // isPhoneNumberRegistered(), etc. remain the same as in your original implementation

    private boolean isStrongPassword(String password) {
        return password.length() >= 8 && 
               password.matches(".*[A-Z].*") && 
               password.matches(".*[a-z].*") && 
               password.matches(".*\\d.*");
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email.matches(emailRegex);
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }
    
    private boolean isValidId(String role, String id) {
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT COUNT(*) FROM " + (role.equals("Student") ? "Student" : "Faculty") + " WHERE " + (role.equals("Student") ? "Stu_ID" : "F_ID") + " = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) == 0; // If the count is 0, the ID is available
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SignUpPage().setVisible(true));
    }
    
}*/