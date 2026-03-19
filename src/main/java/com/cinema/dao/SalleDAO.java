package com.cinema.dao;

import com.cinema.model.Salle;
import com.cinema.model.Siege;
import com.cinema.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SalleDAO {

    public List<Salle> getAll() throws SQLException {
        List<Salle> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT * FROM salles ORDER BY numero");
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Salle getById(int id) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM salles WHERE id_salle = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? map(rs) : null;
        }
    }

    public void insert(Salle s) throws SQLException {
        String sql = "INSERT INTO salles (numero, capacite, type_salle, actif) VALUES (?,?,?,1)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getNumero());
            ps.setInt(2, s.getCapacite());
            ps.setString(3, s.getTypeSalle());
            ps.executeUpdate();
            ResultSet rk = ps.getGeneratedKeys();
            if (rk.next()) s.setId(rk.getInt(1));
        }
    }

    public void update(Salle s) throws SQLException {
        String sql = "UPDATE salles SET numero=?, capacite=?, type_salle=?, actif=? WHERE id_salle=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, s.getNumero());
            ps.setInt(2, s.getCapacite());
            ps.setString(3, s.getTypeSalle());
            ps.setBoolean(4, s.isActif());
            ps.setInt(5, s.getId());
            ps.executeUpdate();
        }
    }

    public boolean delete(int idSalle) throws SQLException {
        String check = "SELECT COUNT(*) FROM seances WHERE id_salle = ? AND date_heure >= NOW() AND statut != 'ANNULEE'";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(check)) {
            ps.setInt(1, idSalle);
            ResultSet rs = ps.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) return false;
        }
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM salles WHERE id_salle = ?")) {
            ps.setInt(1, idSalle);
            ps.executeUpdate();
        }
        return true;
    }

    // ---------- Sieges ----------

    public List<Siege> getSiegesBySalle(int idSalle) throws SQLException {
        List<Siege> list = new ArrayList<>();
        String sql = "SELECT * FROM sieges WHERE id_salle = ? ORDER BY rangee, numero_siege";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idSalle);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapSiege(rs));
        }
        return list;
    }

    /** Returns sieges for a seance with their current etat */
    public List<Siege> getSiegesWithEtat(int idSeance) throws SQLException {
        List<Siege> list = new ArrayList<>();
        String sql = """
                SELECT s.*, COALESCE(e.etat, 'DISPONIBLE') AS etat
                FROM sieges s
                JOIN seances se ON se.id_salle = s.id_salle AND se.id_seance = ?
                LEFT JOIN etat_sieges_seance e ON e.id_siege = s.id_siege AND e.id_seance = ?
                ORDER BY s.rangee, s.numero_siege
                """;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idSeance);
            ps.setInt(2, idSeance);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Siege sg = mapSiege(rs);
                sg.setEtat(rs.getString("etat"));
                list.add(sg);
            }
        }
        return list;
    }

    public void addSiege(Siege s) throws SQLException {
        String sql = "INSERT INTO sieges (id_salle, rangee, numero_siege, type_siege) VALUES (?,?,?,?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getIdSalle());
            ps.setString(2, s.getRangee());
            ps.setInt(3, s.getNumeroSiege());
            ps.setString(4, s.getTypeSiege());
            ps.executeUpdate();
            ResultSet rk = ps.getGeneratedKeys();
            if (rk.next()) s.setId(rk.getInt(1));
        }
    }

    public void deleteSiege(int idSiege) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM sieges WHERE id_siege = ?")) {
            ps.setInt(1, idSiege);
            ps.executeUpdate();
        }
    }

    private Salle map(ResultSet rs) throws SQLException {
        Salle s = new Salle();
        s.setId(rs.getInt("id_salle"));
        s.setNumero(rs.getString("numero"));
        s.setCapacite(rs.getInt("capacite"));
        s.setTypeSalle(rs.getString("type_salle"));
        s.setActif(rs.getBoolean("actif"));
        return s;
    }

    private Siege mapSiege(ResultSet rs) throws SQLException {
        Siege s = new Siege();
        s.setId(rs.getInt("id_siege"));
        s.setIdSalle(rs.getInt("id_salle"));
        s.setRangee(rs.getString("rangee"));
        s.setNumeroSiege(rs.getInt("numero_siege"));
        s.setTypeSiege(rs.getString("type_siege"));
        return s;
    }
}
