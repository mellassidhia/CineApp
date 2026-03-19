package com.cinema.model;

public class Siege {
    private int id;
    private int idSalle;
    private String rangee;
    private int numeroSiege;
    private String typeSiege;
    private String etat; // DISPONIBLE, RESERVE, OCCUPE (runtime per seance)

    public Siege() {}

    public int getId()                  { return id; }
    public void setId(int id)           { this.id = id; }
    public int getIdSalle()             { return idSalle; }
    public void setIdSalle(int s)       { this.idSalle = s; }
    public String getRangee()           { return rangee; }
    public void setRangee(String r)     { this.rangee = r; }
    public int getNumeroSiege()         { return numeroSiege; }
    public void setNumeroSiege(int n)   { this.numeroSiege = n; }
    public String getTypeSiege()        { return typeSiege; }
    public void setTypeSiege(String t)  { this.typeSiege = t; }
    public String getEtat()             { return etat; }
    public void setEtat(String e)       { this.etat = e; }

    public String getLabel()            { return rangee + numeroSiege; }

    @Override
    public String toString()            { return rangee + numeroSiege + " [" + typeSiege + "]"; }
}
