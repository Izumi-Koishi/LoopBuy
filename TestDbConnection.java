import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestDbConnection {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/shiwu?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&allowPublicKeyRetrieval=true";
        String username = "root";
        String password = "bryant0825TLF";

        System.out.println("Testing database connection...");
        System.out.println("URL: " + url);
        System.out.println("Username: " + username);

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, username, password);
            System.out.println("SUCCESS: Database connection established!");
            conn.close();
        } catch (ClassNotFoundException e) {
            System.out.println("ERROR: MySQL driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("ERROR: Database connection failed: " + e.getMessage());
            System.out.println("Error code: " + e.getErrorCode());
            System.out.println("SQL state: " + e.getSQLState());
        }
    }
}
