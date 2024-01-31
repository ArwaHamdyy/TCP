
import java.net.*;
import java.io.*;
import java.sql.Timestamp;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.DriverManager;

public class ServerTCP {

    private Socket socket = null;
    private ServerSocket server = null;
    private DataInputStream in = null;
    private DataOutputStream out = null;
    private String clientName = null;

    private Connection connection = null;
    private String jdbcUrl = "jdbc:postgresql://127.0.0.1:5432/chatdb";
    private String username = "postgres";
    private String password = "root";

    public ServerTCP(int port) throws SQLException {
        try {

            server = new ServerSocket(port);
            System.out.println("Server started");
            System.out.println("Waiting for a client ...");

            socket = server.accept();
            System.out.println("Client accepted");

            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            clientName = in.readUTF();
            System.out.println("Welcome " + clientName);

            String sentence = "";

            while (!sentence.equalsIgnoreCase("exit")) {
                try {
                    sentence = in.readUTF();

                    String uppercaseSentence = sentence.toUpperCase();
                    out.writeUTF(uppercaseSentence);

                    try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {

                        try (PreparedStatement preparedStatement = connection.prepareStatement(
                                "INSERT INTO chat_messages (name, message) VALUES (?, ?) RETURNING timestamp")) {
                            preparedStatement.setString(1, clientName);
                            preparedStatement.setString(2, sentence);

                            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                                if (resultSet.next()) {
                                    Timestamp dbTimestamp = resultSet.getTimestamp("timestamp");
                                    System.out.println(dbTimestamp + " " + clientName + " : " + sentence);
                                }
                            }
                        }
                    }

                } catch (IOException i) {
                    System.out.println(i);
                }
            }
            System.out.println("Closing connection");

            socket.close();
            in.close();
            out.close();
        } catch (IOException i) {
            System.out.println(i);
        }
    }

    public static void main(String args[]) throws SQLException {
        ServerTCP server = new ServerTCP(5001);
    }
}
