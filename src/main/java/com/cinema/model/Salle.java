package com.cinema.model;

public class Salle {
    private int id;
    private String numero;
    private int capacite;
    private String typeSalle;
    private boolean actif;

    public Salle() {}

    public int getId()                  { return id; }
    public void setId(int id)           { this.id = id; }
    public String getNumero()           { return numero; }
    public void setNumero(String n)     { this.numero = n; }
    public int getCapacite()            { return capacite; }
    public void setCapacite(int c)      { this.capacite = c; }
    public String getTypeSalle()        { return typeSalle; }
    public void setTypeSalle(String t)  { this.typeSalle = t; }
    public boolean isActif()            { return actif; }
    public void setActif(boolean a)     { this.actif = a; }

    @Override
    public String toString()            { return numero + " (" + typeSalle + ")"; }
}
