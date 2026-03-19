package com.cinema.model;

import java.time.LocalDate;

public class Film {
    private int id;
    private String titre;
    private int duree;
    private String genre;
    private String description;
    private LocalDate dateSortie;
    private String classification;
    private String affichePath;
    private boolean actif;

    public Film() {}

    // Getters & Setters
    public int getId()                        { return id; }
    public void setId(int id)                 { this.id = id; }
    public String getTitre()                  { return titre; }
    public void setTitre(String t)            { this.titre = t; }
    public int getDuree()                     { return duree; }
    public void setDuree(int d)               { this.duree = d; }
    public String getGenre()                  { return genre; }
    public void setGenre(String g)            { this.genre = g; }
    public String getDescription()            { return description; }
    public void setDescription(String d)      { this.description = d; }
    public LocalDate getDateSortie()          { return dateSortie; }
    public void setDateSortie(LocalDate d)    { this.dateSortie = d; }
    public String getClassification()         { return classification; }
    public void setClassification(String c)   { this.classification = c; }
    public String getAffichePath()            { return affichePath; }
    public void setAffichePath(String p)      { this.affichePath = p; }
    public boolean isActif()                  { return actif; }
    public void setActif(boolean a)           { this.actif = a; }

    @Override
    public String toString() { return titre; }
}
