package com.cinema.dao;

import com.cinema.model.Seance;
import com.cinema.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SeanceDAO {

    private static final String SELECT_JOINED =
        "SELECT se.*, f.titre AS titre_film, s.numero AS numero_salle " +
        "FROM seances se " +
        "JOIN films f ON f.id_film = se.id_film " +
        "JOIN salles s ON s.id_salle = se.id_salle ";

    public List<Seance> getAll(Integer idFilm, String dateStr) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_JOINED + "WHERE 1=1");
        if (idFilm != null) sql.append(" AND se.id_film = ?");
        if (dateStr != null && !dateStr.isEmpty()) sql.append(" AND DATE(se.date_heure) = ?");
        sql.append(" ORDER BY se.date_heure");

        List<Seance> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int idx = 1;
            if (idFilm != null) ps.setInt(idx++, idFilm);
            if (dateStr != null && !dateStr.isEmpty()) ps.setString(idx, dateStr);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Seance> getFutureSeances() throws SQLException {
        List<Seance> list = new ArrayList<>();
        String sql = SELECT_JOINED + "WHERE se.date_heure >= NOW() AND se.statut != 'ANNULEE' ORDER BY se.date_heure";
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Seance getById(int id) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_JOINED + "WHERE se.id_seance = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? map(rs) : null;
        }
    }

    /** Returns true if there's a scheduling conflict (same salle, overlapping time +-3h) */
    public boolean hasConflict(int idSalle, LocalDateTime dateHeure, int dureeMin, int excludeId) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM seances se
            JOIN films f ON f.id_film = se.id_film
            WHERE se.id_salle = ?
              AND se.statut != 'ANNULEE'
              AND se.id_seance != ?
              AND (
                ? BETWEEN DATE_SUB(se.date_heure, INTERVAL 30 MINUTE)
                      AND DATE_ADD(se.date_heure, INTERVAL (f.duree + 30) MINUTE)
                OR
                DATE_ADD(?, INTERVAL ? MINUTE) BETWEEN se.date_heure
                      AND DATE_ADD(se.date_heure, INTERVAL f.duree MINUTE)
              )
            """;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idSalle);
            ps.setInt(2, excludeId);
            ps.setTimestamp(3, Timestamp.valueOf(dateHeure));
            ps.setTimestamp(4, Timestamp.valueOf(dateHeure));
            ps.setInt(5, dureeMin);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    public void insert(Seance s) throws SQLException {
        String sql = "INSERT INTO seances (id_film, id_salle, date_heure, prix_billet, langue, statut) VALUES (?,?,?,?,?,?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getIdFilm());
            ps.setInt(2, s.getIdSalle());
            ps.setTimestamp(3, Timestamp.valueOf(s.getDateHeure()));
            ps.setBigDecimal(4, s.getPrixBillet());
            ps.setString(5, s.getLangue());
            ps.setString(6, s.getStatut() != null ? s.getStatut() : "PLANIFIEE");
            ps.executeUpdate();
            ResultSet rk = ps.getGeneratedKeys();
            if (rk.next()) {
                s.setId(rk.getInt(1));
                // Initialize seat states
                initSieges(s.getId(), con);
            }
        }
    }

    public boolean update(Seance s) throws SQLException {
        // Check no confirmed reservations
        if (hasConfirmedReservations(s.getId())) return false;
        String sql = "UPDATE seances SET id_film=?, id_salle=?, date_heure=?, prix_billet=?, langue=?, statut=? WHERE id_seance=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, s.getIdFilm());
            ps.setInt(2, s.getIdSalle());
            ps.setTimestamp(3, Timestamp.valueOf(s.getDateHeure()));
            ps.setBigDecimal(4, s.getPrixBillet());
            ps.setString(5, s.getLangue());
            ps.setString(6, s.getStatut());
            ps.setInt(7, s.getId());
            ps.executeUpdate();
        }
        return true;
    }

    public boolean delete(int id) throws SQLException {
        if (hasConfirmedReservations(id)) return false;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM seances WHERE id_seance = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        return true;
    }

    private boolean hasConfirmedReservations(int idSeance) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reservations WHERE id_seance = ? AND statut = 'CONFIRMEE'";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idSeance);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private void initSieges(int idSeance, Connection con) throws SQLException {
        String sql = """
            INSERT IGNORE INTO etat_sieges_seance (id_seance, id_siege, etat)
            SELECT ?, id_siege, 'DISPONIBLE' FROM sieges
            WHERE id_salle = (SELECT id_salle FROM seances WHERE id_seance = ?)
            """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idSeance);
            ps.setInt(2, idSeance);
            ps.executeUpdate();
        }
    }

    private Seance map(ResultSet rs) throws SQLException {
        Seance s = new Seance();
        s.setId(rs.getInt("id_seance"));
        s.setIdFilm(rs.getInt("id_film"));
        s.setIdSalle(rs.getInt("id_salle"));
        Timestamp ts = rs.getTimestamp("date_heure");
        s.setDateHeure(ts != null ? ts.toLocalDateTime() : null);
        s.setPrixBillet(rs.getBigDecimal("prix_billet"));
        s.setLangue(rs.getString("langue"));
        s.setStatut(rs.getString("statut"));
        try { s.setTitreFilm(rs.getString("titre_film")); } catch (SQLException ignored) {}
        try { s.setNumeroSalle(rs.getString("numero_salle")); } catch (SQLException ignored) {}
        return s;
    }
}
