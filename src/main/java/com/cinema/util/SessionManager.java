package com.cinema.util;

import com.cinema.model.User;

public class SessionManager {

    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public User getCurrentUser()              { return currentUser; }
    public void setCurrentUser(User u)        { this.currentUser = u; }
    public boolean isAdmin()                  { return currentUser != null && "ADMIN".equals(currentUser.getRole()); }
    public void logout()                      { currentUser = null; }
}
