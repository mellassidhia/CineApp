package com.cinema.dao;

import com.cinema.model.Film;
import com.cinema.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FilmDAO {

    public List<Film> getAll(String genreFilter, Boolean actifFilter) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM films WHERE 1=1");
        if (genreFilter != null && !genreFilter.isEmpty()) sql.append(" AND genre = ?");
        if (actifFilter != null) sql.append(" AND actif = ?");
        sql.append(" ORDER BY titre");

        List<Film> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int idx = 1;
            if (genreFilter != null && !genreFilter.isEmpty()) ps.setString(idx++, genreFilter);
            if (actifFilter != null) ps.setBoolean(idx, actifFilter);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Film getById(int id) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM films WHERE id_film = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? map(rs) : null;
        }
    }

    public void insert(Film f) throws SQLException {
        String sql = "INSERT INTO films (titre, duree, genre, description, date_sortie, classification, affiche_path, actif) VALUES (?,?,?,?,?,?,?,1)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParams(ps, f);
            ps.executeUpdate();
            ResultSet rk = ps.getGeneratedKeys();
            if (rk.next()) f.setId(rk.getInt(1));
        }
    }

    public void update(Film f) throws SQLException {
        String sql = "UPDATE films SET titre=?, duree=?, genre=?, description=?, date_sortie=?, classification=?, affiche_path=?, actif=? WHERE id_film=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setParams(ps, f);
            ps.setBoolean(8, f.isActif());
            ps.setInt(9, f.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Delete only if no future seances exist for this film.
     * Returns false if there are future seances.
     */
    public boolean delete(int idFilm) throws SQLException {
        String check = "SELECT COUNT(*) FROM seances WHERE id_film = ? AND date_heure >= NOW() AND statut != 'ANNULEE'";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(check)) {
            ps.setInt(1, idFilm);
            ResultSet rs = ps.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) return false;
        }
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM films WHERE id_film = ?")) {
            ps.setInt(1, idFilm);
            ps.executeUpdate();
        }
        return true;
    }

    public List<String> getGenres() throws SQLException {
        List<String> g = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT DISTINCT genre FROM films ORDER BY genre");
            while (rs.next()) g.add(rs.getString(1));
        }
        return g;
    }

    private void setParams(PreparedStatement ps, Film f) throws SQLException {
        ps.setString(1, f.getTitre());
        ps.setInt(2, f.getDuree());
        ps.setString(3, f.getGenre());
        ps.setString(4, f.getDescription());
        ps.setDate(5, Date.valueOf(f.getDateSortie()));
        ps.setString(6, f.getClassification());
        ps.setString(7, f.getAffichePath());
    }

    private Film map(ResultSet rs) throws SQLException {
        Film f = new Film();
        f.setId(rs.getInt("id_film"));
        f.setTitre(rs.getString("titre"));
        f.setDuree(rs.getInt("duree"));
        f.setGenre(rs.getString("genre"));
        f.setDescription(rs.getString("description"));
        Date d = rs.getDate("date_sortie");
        f.setDateSortie(d != null ? d.toLocalDate() : null);
        f.setClassification(rs.getString("classification"));
        f.setAffichePath(rs.getString("affiche_path"));
        f.setActif(rs.getBoolean("actif"));
        return f;
    }
}
