# Library Management System (LMMS)

A Java-based Library Management System project built with Eclipse IDE and connected to a MySQL database.

## 📚 Project Description

This project helps manage books in a library by providing features like:

- Book management (Add, View, Search)
- User and Admin login system
- Book request and approval system (Admin only)
- Faculty can add books
- MySQL database for persistent storage

## 🛠️ Technologies Used

- Java (Core Java, JDBC)
- MySQL Server
- Eclipse IDE
- XAMPP (for MySQL)
- Git & GitHub for version control

## 🗃️ Database Details

- Database Name: `library_db`
- Tables:
  - `users` – Stores login credentials and roles
  - `books` – Stores book details
  - `requests` – Stores book issue/return requests
- Make sure to import the provided `.sql` file (if available)

## 🚀 How to Run the Project

1. Clone this repository:
   ```bash
   git clone https://github.com/Ramkrishnan007/LMMS-Java-Project.git

2.Open the project in Eclipse IDE.

3.Set up your MySQL database:

  Open XAMPP and start MySQL

  Import the library_db.sql file (if provided)

4.Update your JDBC connection string in the Java files:

  java
  Copy code
  String url = "jdbc:mysql://localhost:3306/library_db";
  String username = "root";
  String password = ""; // Change if your DB password is set
5.Run the Main.java or relevant entry file.

🧑‍💻 Contributors
Ramkrishnan

📄 License
This project is for educational use only.
