
import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class ClientTCP {

    private Socket socket = null;
    private DataInputStream input = null;
    private DataOutputStream out = null;
    private Connection dbConnection = null;

    public ClientTCP(String address, int port) {
        try {

            socket = new Socket(address, port);
            System.out.println("Connected to Database");

            input = new DataInputStream(System.in);
            out = new DataOutputStream(socket.getOutputStream());

            System.out.print("Please Enter your name: ");
            String userName = input.readLine();
            out.writeUTF(userName);

            String jdbcUrl = "jdbc:postgresql://127.0.0.1:5432/chatdb";
            String dbUsername = "postgres";
            String dbPassword = "root";
            dbConnection = DriverManager.getConnection(jdbcUrl, dbUsername, dbPassword);

            while (true) {

                String message = input.readLine();

                out.writeUTF(message);

                String response = new DataInputStream(socket.getInputStream()).readUTF();
                System.out.println("Received From Server: " + response);

                try (PreparedStatement preparedStatement = dbConnection.prepareStatement(
                        "SELECT timestamp FROM chat_messages WHERE name = ? AND message = ?")) {
                    preparedStatement.setString(1, userName);
                    preparedStatement.setString(2, message);

                    try (var resultSet = preparedStatement.executeQuery()) {
                        if (resultSet.next()) {
                            Timestamp dbTimestamp = resultSet.getTimestamp("timestamp");
                            System.out.println(dbTimestamp + " " + userName + " : " + message + ".");
                        }
                    }
                }
            }
        } catch (IOException | SQLException e) {
            System.out.println(e);
        } finally {

            try {
                if (input != null) {
                    input.close();
                }
                if (out != null) {
                    out.close();
                }
                if (socket != null) {
                    socket.close();
                }
                if (dbConnection != null) {
                    dbConnection.close();
                }
            } catch (IOException | SQLException e) {
                System.out.println(e);
            }
        }
    }

    public static void main(String[] args) {

        ClientTCP client = new ClientTCP("127.0.0.1", 5001);
    }
}
