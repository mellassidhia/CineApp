package com.cinema.model;

import java.time.LocalDate;

public class Client {
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private LocalDate dateNaissance;
    private boolean actif;

    public Client() {}

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }
    public String getNom()                      { return nom; }
    public void setNom(String n)                { this.nom = n; }
    public String getPrenom()                   { return prenom; }
    public void setPrenom(String p)             { this.prenom = p; }
    public String getEmail()                    { return email; }
    public void setEmail(String e)              { this.email = e; }
    public String getTelephone()                { return telephone; }
    public void setTelephone(String t)          { this.telephone = t; }
    public LocalDate getDateNaissance()         { return dateNaissance; }
    public void setDateNaissance(LocalDate d)   { this.dateNaissance = d; }
    public boolean isActif()                    { return actif; }
    public void setActif(boolean a)             { this.actif = a; }

    public String getNomComplet()               { return prenom + " " + nom; }

    @Override
    public String toString()                    { return getNomComplet() + " <" + email + ">"; }
}
