package com.cinema.model;

public class User {
    private int id;
    private String username;
    private String passwordHash;
    private String role;       // "ADMIN" | "USER"
    private int idClient;      // 0 if admin

    public User() {}

    public User(int id, String username, String passwordHash, String role, int idClient) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.idClient = idClient;
    }

    public int getId()                    { return id; }
    public void setId(int id)             { this.id = id; }
    public String getUsername()           { return username; }
    public void setUsername(String u)     { this.username = u; }
    public String getPasswordHash()       { return passwordHash; }
    public void setPasswordHash(String p) { this.passwordHash = p; }
    public String getRole()               { return role; }
    public void setRole(String r)         { this.role = r; }
    public int getIdClient()              { return idClient; }
    public void setIdClient(int c)        { this.idClient = c; }
}
