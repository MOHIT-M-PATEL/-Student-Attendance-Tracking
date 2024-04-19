package attendance;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

public class Attendance {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/your_database";
    private static final String DB_USER = "db_username";
    private static final String DB_PASSWORD = "db_password";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("Connected to database.");

            Scanner scanner = new Scanner(System.in);
            int choice;

            while (true) {
                System.out.println("\nMenu:");
                System.out.println("1. Fetch Students");
                System.out.println("2. Mark Attendance");
                System.out.println("3. Exit");
                System.out.println("Enter your choice:");

                try {
                    if (scanner.hasNextLine()) {
                        String input = scanner.nextLine().trim();

                        if (!input.isEmpty() && input.matches("\\d+")) {
                            choice = Integer.parseInt(input);

                            switch (choice) {
                                case 1:
                                    fetchStudents(conn);
                                    break;
                                case 2:
                                    markAttendance(conn);
                                    break;
                                case 3:
                                    System.out.println("Exiting program.");
                                    return; // Exit the program
                                default:
                                    System.out.println("Invalid choice. Please enter a valid option.");
                                    break;
                            }
                        } else {
                            System.out.println("Invalid input. Please enter a number.");
                        }
                    } else {
                        System.out.println("Input stream terminated. Exiting program.");
                        return; // Exit the program
                    }
                } catch (NoSuchElementException e) {
                    System.out.println("Input stream terminated. Exiting program.");
                    return; // Exit the program
                }
            }

        } catch (SQLException e) {
            System.err.println("Error connecting to database: " + e.getMessage());
        }
    }

    private static void fetchStudents(Connection conn) throws SQLException {
        String sql = "SELECT s.roll_number, s.name, s.attendance_count " +
                     "FROM students s " +
                     "ORDER BY s.roll_number";

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            System.out.println("Student List with Attendance Count:");
            while (rs.next()) {
                String rollNumber = rs.getString("roll_number");
                String name = rs.getString("name");
                int attendanceCount = rs.getInt("attendance_count");

                System.out.println("\nRoll Number: " + rollNumber);
                System.out.println("Name: " + name);
                System.out.println("Attendance Count: " + attendanceCount);
                System.out.println("-----------------------------");
            }
        }
    }

    private static void markAttendance(Connection conn) throws SQLException {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Enter student Roll Numbers to mark attendance (comma-separated):");
            String rollNumbersInput = scanner.nextLine();
            String[] rollNumbersArray = rollNumbersInput.split(",");

            // Get the current date
            LocalDate currentDate = LocalDate.now();

            for (String rollNumber : rollNumbersArray) {
                // Check if the student Roll Number is valid
                if (isRollNumberValid(conn, rollNumber)) {
                    // Check if the student has already attended today
                    if (!hasStudentAttendedToday(conn, rollNumber, currentDate)) {
                        // Mark attendance for the student
                        String sql = "INSERT INTO attendance (roll_number, attendance_date) " +
                                     "SELECT id, ? FROM students WHERE roll_number = ?";
                        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                            stmt.setDate(1, Date.valueOf(currentDate));
                            stmt.setString(2, rollNumber);
                            int rowsAffected = stmt.executeUpdate();
                            if (rowsAffected > 0) {
                                System.out.println("Attendance marked successfully for Roll Number " + rollNumber);

                                // Update attendance count for the student in the students table
                                updateAttendanceCount(conn, rollNumber);
                            } else {
                                System.out.println("Failed to mark attendance for Roll Number " + rollNumber);
                            }
                        }
                    } else {
                        System.out.println("Roll Number " + rollNumber + " has already attended today.");
                    }
                } else {
                    System.out.println("Invalid Roll Number: " + rollNumber);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error marking attendance: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
        }
    }

    private static boolean hasStudentAttendedToday(Connection conn, String rollNumber, LocalDate currentDate) throws SQLException {
        String sql = "SELECT COUNT(*) AS count FROM attendance WHERE roll_number = (SELECT id FROM students WHERE roll_number = ?) AND attendance_date = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, rollNumber);
            stmt.setDate(2, Date.valueOf(currentDate));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    return count > 0;
                }
            }
        }
        return false;
    }


    private static void updateAttendanceCount(Connection conn, String rollNumber) throws SQLException {
        String updateSql = "UPDATE students SET attendance_count = attendance_count + 1 WHERE roll_number = ?";

        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
            updateStmt.setString(1, rollNumber);

            int rowsUpdated = updateStmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Attendance count updated successfully for Roll Number " + rollNumber);
            } else {
                System.out.println("Failed to update attendance count for Roll Number " + rollNumber);
            }
        }
    }

    private static boolean isRollNumberValid(Connection conn, String rollNumber) throws SQLException {
        String sql = "SELECT roll_number FROM students WHERE roll_number = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, rollNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();  // If a record is found, the Roll Number is valid
            }
        }
    }
}
