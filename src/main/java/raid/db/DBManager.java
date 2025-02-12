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
        finally {
            closeResource(con);
        }

        return user;
    }

    public String getPassword(User user) {
        String password = null;
        Connection con = null;

        try {
            con = DriverManager.getConnection(USER, URL, PASSWORD);
            String SQL = "SELECT user_password FROM file_owner WHERE user_name = ?";

            PreparedStatement preparedStatement = con.prepareStatement(SQL);
            preparedStatement.setString(1, user.getName());
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                password = resultSet.getString(1);
            }

        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            closeResource(con);
        }

        return password;
    }
}
