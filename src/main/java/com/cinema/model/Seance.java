package com.cinema.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Seance {
    private int id;
    private int idFilm;
    private int idSalle;
    private LocalDateTime dateHeure;
    private BigDecimal prixBillet;
    private String langue;
    private String statut;

    // Join fields (for display)
    private String titreFilm;
    private String numeroSalle;

    public Seance() {}

    public int getId()                        { return id; }
    public void setId(int id)                 { this.id = id; }
    public int getIdFilm()                    { return idFilm; }
    public void setIdFilm(int f)              { this.idFilm = f; }
    public int getIdSalle()                   { return idSalle; }
    public void setIdSalle(int s)             { this.idSalle = s; }
    public LocalDateTime getDateHeure()       { return dateHeure; }
    public void setDateHeure(LocalDateTime d) { this.dateHeure = d; }
    public BigDecimal getPrixBillet()         { return prixBillet; }
    public void setPrixBillet(BigDecimal p)   { this.prixBillet = p; }
    public String getLangue()                 { return langue; }
    public void setLangue(String l)           { this.langue = l; }
    public String getStatut()                 { return statut; }
    public void setStatut(String s)           { this.statut = s; }
    public String getTitreFilm()              { return titreFilm; }
    public void setTitreFilm(String t)        { this.titreFilm = t; }
    public String getNumeroSalle()            { return numeroSalle; }
    public void setNumeroSalle(String n)      { this.numeroSalle = n; }

    @Override
    public String toString() {
        return (titreFilm != null ? titreFilm : "Film#" + idFilm)
             + " - " + (dateHeure != null ? dateHeure.toString() : "");
    }
}
