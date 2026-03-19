package com.cinema.dao;

import com.cinema.model.Reservation;
import com.cinema.model.Siege;
import com.cinema.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {

    private static final String SELECT_JOINED =
        "SELECT r.*, CONCAT(cl.prenom,' ',cl.nom) AS nom_client, " +
        "f.titre AS titre_film, se.date_heure AS date_seance, s.numero AS numero_salle " +
        "FROM reservations r " +
        "JOIN clients cl ON cl.id_client = r.id_client " +
        "JOIN seances se ON se.id_seance = r.id_seance " +
        "JOIN films f ON f.id_film = se.id_film " +
        "JOIN salles s ON s.id_salle = se.id_salle ";

    public List<Reservation> getByClient(int idClient) throws SQLException {
        List<Reservation> list = new ArrayList<>();

        // Step 1: load all reservations first, close ResultSet
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                SELECT_JOINED + "WHERE r.id_client = ? ORDER BY r.date_reservation DESC")) {
            ps.setInt(1, idClient);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(map(rs));
            }
        }

        // Step 2: now load sieges for each reservation in separate connections
        for (Reservation res : list) {
            res.setSieges(getSiegesForReservation(res.getId()));
        }

        return list;
    }

    public List<Reservation> getAll() throws SQLException {
        List<Reservation> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement()) {
            ResultSet rs = st.executeQuery(SELECT_JOINED + "ORDER BY r.date_reservation DESC");
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Reservation getById(int id) throws SQLException {
        Reservation res = null;

        // Step 1: load reservation
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_JOINED + "WHERE r.id_reservation = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                res = map(rs);
            }
        }

        // Step 2: load sieges separately
        if (res != null) {
            res.setSieges(getSiegesForReservation(res.getId()));
        }

        return res;
    }

    public Reservation create(int idClient, int idSeance, List<Integer> idSieges, BigDecimal prixTotal) throws SQLException {
        Connection con = DatabaseConnection.getConnection();
        con.setAutoCommit(false);
        int idRes;
        try {
            String ref = generateReference(con);

            String sqlRes = "INSERT INTO reservations (id_client, id_seance, statut, prix_total, reference) VALUES (?,?,'EN_ATTENTE',?,?)";
            try (PreparedStatement ps = con.prepareStatement(sqlRes, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, idClient);
                ps.setInt(2, idSeance);
                ps.setBigDecimal(3, prixTotal);
                ps.setString(4, ref);
                ps.executeUpdate();
                ResultSet rk = ps.getGeneratedKeys();
                rk.next();
                idRes = rk.getInt(1);
            }

            String sqlSiege = "INSERT INTO reservation_sieges (id_reservation, id_siege) VALUES (?,?)";
            try (PreparedStatement ps = con.prepareStatement(sqlSiege)) {
                for (int idSiege : idSieges) {
                    ps.setInt(1, idRes);
                    ps.setInt(2, idSiege);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            String sqlEtat = "INSERT INTO etat_sieges_seance (id_seance, id_siege, etat) VALUES (?,?,'RESERVE') " +
                             "ON DUPLICATE KEY UPDATE etat = 'RESERVE'";
            try (PreparedStatement ps = con.prepareStatement(sqlEtat)) {
                for (int idSiege : idSieges) {
                    ps.setInt(1, idSeance);
                    ps.setInt(2, idSiege);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            String sqlConfirm = "UPDATE reservations SET statut = 'CONFIRMEE' WHERE id_reservation = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlConfirm)) {
                ps.setInt(1, idRes);
                ps.executeUpdate();
            }

            con.commit();
        } catch (SQLException e) {
            con.rollback();
            con.setAutoCommit(true);
            throw e;
        }
        con.setAutoCommit(true);

        return getById(idRes);
    }

    public boolean cancel(int idReservation) throws SQLException {
        Reservation res = getById(idReservation);
        if (res == null) return false;

        Connection con = DatabaseConnection.getConnection();
        con.setAutoCommit(false);
        try {
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE reservations SET statut = 'ANNULEE' WHERE id_reservation = ?")) {
                ps.setInt(1, idReservation);
                ps.executeUpdate();
            }

            if (res.getSieges() != null) {
                String sqlEtat = "UPDATE etat_sieges_seance SET etat = 'DISPONIBLE' WHERE id_seance = ? AND id_siege = ?";
                try (PreparedStatement ps = con.prepareStatement(sqlEtat)) {
                    for (Siege sg : res.getSieges()) {
                        ps.setInt(1, res.getIdSeance());
                        ps.setInt(2, sg.getId());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
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

    public List<Siege> getSiegesForReservation(int idReservation) throws SQLException {
        List<Siege> list = new ArrayList<>();
        String sql = "SELECT sg.* FROM sieges sg " +
                     "JOIN reservation_sieges rs ON rs.id_siege = sg.id_siege " +
                     "WHERE rs.id_reservation = ? ORDER BY sg.rangee, sg.numero_siege";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idReservation);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Siege s = new Siege();
                s.setId(rs.getInt("id_siege"));
                s.setRangee(rs.getString("rangee"));
                s.setNumeroSiege(rs.getInt("numero_siege"));
                s.setTypeSiege(rs.getString("type_siege"));
                list.add(s);
            }
        }
        return list;
    }

    private String generateReference(Connection con) throws SQLException {
        String year = String.valueOf(java.time.Year.now().getValue());
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING_INDEX(reference,'-',-1) AS UNSIGNED)),0)+1 " +
                     "FROM reservations WHERE reference LIKE ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "CIN-" + year + "-%");
            ResultSet rs = ps.executeQuery();
            rs.next();
            int count = rs.getInt(1);
            return String.format("CIN-%s-%04d", year, count);
        }
    }

    private Reservation map(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getInt("id_reservation"));
        r.setIdClient(rs.getInt("id_client"));
        r.setIdSeance(rs.getInt("id_seance"));
        Timestamp ts = rs.getTimestamp("date_reservation");
        r.setDateReservation(ts != null ? ts.toLocalDateTime() : null);
        r.setStatut(rs.getString("statut"));
        r.setPrixTotal(rs.getBigDecimal("prix_total"));
        r.setReference(rs.getString("reference"));
        try { r.setNomClient(rs.getString("nom_client")); } catch (SQLException ignored) {}
        try { r.setTitreFilm(rs.getString("titre_film")); } catch (SQLException ignored) {}
        try {
            Timestamp ds = rs.getTimestamp("date_seance");
            r.setDateSeance(ds != null ? ds.toLocalDateTime() : null);
        } catch (SQLException ignored) {}
        try { r.setNumeroSalle(rs.getString("numero_salle")); } catch (SQLException ignored) {}
        return r;
    }
}
