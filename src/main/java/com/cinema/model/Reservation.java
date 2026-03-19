package com.cinema.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Reservation {
    private int id;
    private int idClient;
    private int idSeance;
    private LocalDateTime dateReservation;
    private String statut;
    private BigDecimal prixTotal;
    private String reference;

    // Join / runtime fields
    private String nomClient;
    private String titreFilm;
    private LocalDateTime dateSeance;
    private String numeroSalle;
    private List<Siege> sieges;

    public Reservation() {}

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }
    public int getIdClient()                        { return idClient; }
    public void setIdClient(int c)                  { this.idClient = c; }
    public int getIdSeance()                        { return idSeance; }
    public void setIdSeance(int s)                  { this.idSeance = s; }
    public LocalDateTime getDateReservation()       { return dateReservation; }
    public void setDateReservation(LocalDateTime d) { this.dateReservation = d; }
    public String getStatut()                       { return statut; }
    public void setStatut(String s)                 { this.statut = s; }
    public BigDecimal getPrixTotal()                { return prixTotal; }
    public void setPrixTotal(BigDecimal p)          { this.prixTotal = p; }
    public String getReference()                    { return reference; }
    public void setReference(String r)              { this.reference = r; }
    public String getNomClient()                    { return nomClient; }
    public void setNomClient(String n)              { this.nomClient = n; }
    public String getTitreFilm()                    { return titreFilm; }
    public void setTitreFilm(String t)              { this.titreFilm = t; }
    public LocalDateTime getDateSeance()            { return dateSeance; }
    public void setDateSeance(LocalDateTime d)      { this.dateSeance = d; }
    public String getNumeroSalle()                  { return numeroSalle; }
    public void setNumeroSalle(String n)            { this.numeroSalle = n; }
    public List<Siege> getSieges()                  { return sieges; }
    public void setSieges(List<Siege> s)            { this.sieges = s; }
}
