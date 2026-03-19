package com.cinema.dao;

import com.cinema.model.Client;
import com.cinema.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClientDAO {

    public List<Client> getAll() throws SQLException {
        List<Client> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT * FROM clients ORDER BY nom, prenom");
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Client getById(int id) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM clients WHERE id_client = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? map(rs) : null;
        }
    }

    public void insert(Client c) throws SQLException {
        String sql = "INSERT INTO clients (nom, prenom, email, telephone, date_naissance, actif) VALUES (?,?,?,?,?,1)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getNom());
            ps.setString(2, c.getPrenom());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getTelephone());
            ps.setDate(5, c.getDateNaissance() != null ? Date.valueOf(c.getDateNaissance()) : null);
            ps.executeUpdate();
            ResultSet rk = ps.getGeneratedKeys();
            if (rk.next()) c.setId(rk.getInt(1));
        }
    }

    public void update(Client c) throws SQLException {
        String sql = "UPDATE clients SET nom=?, prenom=?, email=?, telephone=?, date_naissance=? WHERE id_client=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getNom());
            ps.setString(2, c.getPrenom());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getTelephone());
            ps.setDate(5, c.getDateNaissance() != null ? Date.valueOf(c.getDateNaissance()) : null);
            ps.setInt(6, c.getId());
            ps.executeUpdate();
        }
    }

    private Client map(ResultSet rs) throws SQLException {
        Client c = new Client();
        c.setId(rs.getInt("id_client"));
        c.setNom(rs.getString("nom"));
        c.setPrenom(rs.getString("prenom"));
        c.setEmail(rs.getString("email"));
        c.setTelephone(rs.getString("telephone"));
        Date d = rs.getDate("date_naissance");
        c.setDateNaissance(d != null ? d.toLocalDate() : null);
        c.setActif(rs.getBoolean("actif"));
        return c;
    }
}
