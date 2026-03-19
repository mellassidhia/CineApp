package com.cinema.dao;

import com.cinema.model.User;
import com.cinema.util.DatabaseConnection;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;

public class UserDAO {

    public User authenticate(String username, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ? AND password_hash = ? AND actif = 1";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hashPassword(password));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
        }
        return null;
    }

    public boolean register(String username, String password, String nom, String prenom,
                            String email, String telephone) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        con.setAutoCommit(false);
        try {
            int idClient;
            String sqlClient = "INSERT INTO clients (nom, prenom, email, telephone, actif) VALUES (?,?,?,?,1)";
            try (PreparedStatement ps = con.prepareStatement(sqlClient, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, nom);
                ps.setString(2, prenom);
                ps.setString(3, email);
                ps.setString(4, telephone);
                ps.executeUpdate();
                ResultSet rk = ps.getGeneratedKeys();
                rk.next();
                idClient = rk.getInt(1);
            }
            String sqlUser = "INSERT INTO users (username, password_hash, role, id_client, actif) VALUES (?,?,'USER',?,1)";
            try (PreparedStatement ps = con.prepareStatement(sqlUser)) {
                ps.setString(1, username);
                ps.setString(2, hashPassword(password));
                ps.setInt(3, idClient);
                ps.executeUpdate();
            }
            con.commit();
            return true;
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
    }

    public boolean usernameExists(String username) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT id FROM users WHERE username = ?")) {
            ps.setString(1, username);
            return ps.executeQuery().next();
        }
    }

    public boolean emailExists(String email) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT id_client FROM clients WHERE email = ?")) {
            ps.setString(1, email);
            return ps.executeQuery().next();
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(rs.getString("role"));
        int idClient = rs.getInt("id_client");
        u.setIdClient(rs.wasNull() ? 0 : idClient);
        return u;
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return password;
        }
    }
}
