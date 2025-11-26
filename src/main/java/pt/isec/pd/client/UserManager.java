package pt.isec.pd.client;

import pt.isec.pd.common.RoleType;
import pt.isec.pd.common.User;

public class UserManager {
    private static UserManager instance;

    private RoleType role;
    private User user;
    private boolean loggedIn = false;

    private UserManager() {
        reset();
    }

    public static UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    public RoleType getRole() {
        return role;
    }

    public UserManager setRole(RoleType role) {
        this.role = role;
        return this;
    }

    public UserManager setRole(String role) {
        this.role = RoleType.valueOf(role);
        return this;
    }

    public User getUser() {
        return user;
    }

    public UserManager setUser(User user) {
        this.user = user;
        return this;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public UserManager setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
        return this;
    }

    public void logOut() {
        reset();
    }

    private void reset() {
        this.role = null;
        this.user = null;
        this.loggedIn = false;
    }
}