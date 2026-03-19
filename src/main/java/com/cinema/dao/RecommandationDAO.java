package com.cinema.dao;

import com.cinema.model.Film;
import com.cinema.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RecommandationDAO {

    /** Films already seen by the client */
    public List<Integer> getFilmsVusIds(int idClient) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = """
            SELECT DISTINCT se.id_film
            FROM reservations r
            JOIN seances se ON se.id_seance = r.id_seance
            WHERE r.id_client = ? AND r.statut = 'CONFIRMEE'
            """;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idClient);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt(1));
        }
        return ids;
    }

    /** Favourite genres of a client (by reservation count) */
    public List<String> getGenresFavoris(int idClient) throws SQLException {
        List<String> genres = new ArrayList<>();
        String sql = """
            SELECT f.genre, COUNT(*) AS cnt
            FROM reservations r
            JOIN seances se ON se.id_seance = r.id_seance
            JOIN films f ON f.id_film = se.id_film
            WHERE r.id_client = ? AND r.statut = 'CONFIRMEE'
            GROUP BY f.genre
            ORDER BY cnt DESC
            LIMIT 3
            """;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idClient);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) genres.add(rs.getString("genre"));
        }
        return genres;
    }

    /** Recommend films matching favourite genres, not yet seen by client */
    public List<Film> getRecommandationsParGenre(int idClient, List<String> genres, List<Integer> dejaVus) throws SQLException {
        if (genres.isEmpty()) return new ArrayList<>();

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < genres.size(); i++) {
            if (i > 0) placeholders.append(",");
            placeholders.append("?");
        }

        String sql = "SELECT * FROM films WHERE actif = 1 AND genre IN (" + placeholders + ")";
        if (!dejaVus.isEmpty()) {
            sql += " AND id_film NOT IN (" + "?,".repeat(dejaVus.size()).replaceAll(",$", "") + ")";
        }
        sql += " ORDER BY date_sortie DESC LIMIT 6";

        List<Film> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int idx = 1;
            for (String g : genres) ps.setString(idx++, g);
            for (int id : dejaVus) ps.setInt(idx++, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapFilm(rs));
        }
        return list;
    }

    /** Most popular films (most reservations) not yet seen */
    public List<Film> getFilmsPopulaires(List<Integer> dejaVus) throws SQLException {
        String sql = """
            SELECT f.*, COUNT(r.id_reservation) AS nb_res
            FROM films f
            LEFT JOIN seances se ON se.id_film = f.id_film
            LEFT JOIN reservations r ON r.id_seance = se.id_seance AND r.statut = 'CONFIRMEE'
            WHERE f.actif = 1
            """;
        if (!dejaVus.isEmpty()) {
            sql += " AND f.id_film NOT IN (" + "?,".repeat(dejaVus.size()).replaceAll(",$", "") + ")";
        }
        sql += " GROUP BY f.id_film ORDER BY nb_res DESC LIMIT 6";

        List<Film> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int idx = 1;
            for (int id : dejaVus) ps.setInt(idx++, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapFilm(rs));
        }
        return list;
    }

    /** Latest releases not yet seen */
    public List<Film> getNouveautes(List<Integer> dejaVus) throws SQLException {
        String sql = "SELECT * FROM films WHERE actif = 1";
        if (!dejaVus.isEmpty()) {
            sql += " AND id_film NOT IN (" + "?,".repeat(dejaVus.size()).replaceAll(",$", "") + ")";
        }
        sql += " ORDER BY date_sortie DESC LIMIT 4";

        List<Film> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int idx = 1;
            for (int id : dejaVus) ps.setInt(idx++, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapFilm(rs));
        }
        return list;
    }

    private Film mapFilm(ResultSet rs) throws SQLException {
        Film f = new Film();
        f.setId(rs.getInt("id_film"));
        f.setTitre(rs.getString("titre"));
        f.setDuree(rs.getInt("duree"));
        f.setGenre(rs.getString("genre"));
        f.setDescription(rs.getString("description"));
        Date d = rs.getDate("date_sortie");
        f.setDateSortie(d != null ? d.toLocalDate() : null);
        f.setClassification(rs.getString("classification"));
        f.setActif(rs.getBoolean("actif"));
        return f;
    }
}
