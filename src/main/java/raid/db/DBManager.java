package raid.db;

import raid.model.User;

import java.sql.*;

import static raid.misc.Util.*;

public class DBManager {
    private final static String USER = getProperty("user", "connection.properties");
    private final static String URL = getProperty("url", "connection.properties");
    private final static String PASSWORD = getProperty("password", "connection.properties");

    public User getUser(String name) {
        Connection con = null;
        User user = null;
        try {
            con = DriverManager.getConnection(URL, USER, PASSWORD);
            String SQL = "SELECT user_dni" +
                    "FROM system_user" +
                    "WHERE user_name = ?";

            PreparedStatement preparedStatement = con.prepareStatement(SQL);
            preparedStatement.setString(1, name);
            ResultSet resultSet = preparedStatement.getResultSet();

            if (resultSet.next()) {
                String userDNI = resultSet.getString(1);
                user = new User(userDNI, name);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return user;
    }

    public String getPassword(User user) {

    }
}
