package com.cinema.dao;

import com.cinema.util.DatabaseConnection;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class StatistiquesDAO {

    /** Total reservations grouped by day for last N days */
    public Map<String, Integer> getReservationsParJour(int days) throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = """
            SELECT DATE(date_reservation) AS jour, COUNT(*) AS total
            FROM reservations
            WHERE statut = 'CONFIRMEE'
              AND date_reservation >= DATE_SUB(NOW(), INTERVAL ? DAY)
            GROUP BY DATE(date_reservation)
            ORDER BY jour
            """;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, days);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) map.put(rs.getString("jour"), rs.getInt("total"));
        }
        return map;
    }

    /** Total reservations grouped by month for last 12 months */
    public Map<String, Integer> getReservationsParMois() throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = """
            SELECT DATE_FORMAT(date_reservation, '%Y-%m') AS mois, COUNT(*) AS total
            FROM reservations
            WHERE statut = 'CONFIRMEE'
              AND date_reservation >= DATE_SUB(NOW(), INTERVAL 12 MONTH)
            GROUP BY mois ORDER BY mois
            """;
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) map.put(rs.getString("mois"), rs.getInt("total"));
        }
        return map;
    }

    /** Top N most watched films */
    public Map<String, Integer> getTopFilms(int limit) throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = """
            SELECT f.titre, COUNT(r.id_reservation) AS total
            FROM reservations r
            JOIN seances se ON se.id_seance = r.id_seance
            JOIN films f ON f.id_film = se.id_film
            WHERE r.statut = 'CONFIRMEE'
            GROUP BY f.id_film, f.titre
            ORDER BY total DESC
            LIMIT ?
            """;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) map.put(rs.getString("titre"), rs.getInt("total"));
        }
        return map;
    }

    /** Revenue per film */
    public Map<String, Double> getRecetteParFilm() throws SQLException {
        Map<String, Double> map = new LinkedHashMap<>();
        String sql = """
            SELECT f.titre, SUM(r.prix_total) AS recette
            FROM reservations r
            JOIN seances se ON se.id_seance = r.id_seance
            JOIN films f ON f.id_film = se.id_film
            WHERE r.statut = 'CONFIRMEE'
            GROUP BY f.id_film, f.titre
            ORDER BY recette DESC
            """;
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) map.put(rs.getString("titre"), rs.getDouble("recette"));
        }
        return map;
    }

    /** Fill rate per salle: reserved seats / total capacity */
    public Map<String, Double> getTauxRemplissage() throws SQLException {
        Map<String, Double> map = new LinkedHashMap<>();
        String sql = """
            SELECT s.numero,
                   s.capacite,
                   COUNT(DISTINCT rs.id) AS sieges_reserves
            FROM salles s
            LEFT JOIN seances se ON se.id_salle = s.id_salle
            LEFT JOIN reservations r ON r.id_seance = se.id_seance AND r.statut = 'CONFIRMEE'
            LEFT JOIN reservation_sieges rs ON rs.id_reservation = r.id_reservation
            GROUP BY s.id_salle, s.numero, s.capacite
            ORDER BY s.numero
            """;
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                int cap = rs.getInt("capacite");
                int reserved = rs.getInt("sieges_reserves");
                double taux = cap > 0 ? (reserved * 100.0 / cap) : 0;
                map.put(rs.getString("numero"), Math.min(taux, 100.0));
            }
        }
        return map;
    }

    /** Global revenue */
    public double getChiffreAffairesGlobal() throws SQLException {
        String sql = "SELECT COALESCE(SUM(prix_total), 0) FROM reservations WHERE statut = 'CONFIRMEE'";
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            rs.next();
            return rs.getDouble(1);
        }
    }

    /** Total confirmed reservations */
    public int getTotalReservations() throws SQLException {
        String sql = "SELECT COUNT(*) FROM reservations WHERE statut = 'CONFIRMEE'";
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            rs.next();
            return rs.getInt(1);
        }
    }

    /** Total active clients */
    public int getTotalClients() throws SQLException {
        String sql = "SELECT COUNT(*) FROM clients WHERE actif = 1";
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            rs.next();
            return rs.getInt(1);
        }
    }

    /** Total films */
    public int getTotalFilms() throws SQLException {
        String sql = "SELECT COUNT(*) FROM films WHERE actif = 1";
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            rs.next();
            return rs.getInt(1);
        }
    }

    /** Revenue by genre */
    public Map<String, Double> getRecetteParGenre() throws SQLException {
        Map<String, Double> map = new LinkedHashMap<>();
        String sql = """
            SELECT f.genre, SUM(r.prix_total) AS recette
            FROM reservations r
            JOIN seances se ON se.id_seance = r.id_seance
            JOIN films f ON f.id_film = se.id_film
            WHERE r.statut = 'CONFIRMEE'
            GROUP BY f.genre
            ORDER BY recette DESC
            """;
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) map.put(rs.getString("genre"), rs.getDouble("recette"));
        }
        return map;
    }
}
