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

    public UserManager setRole(String roleStr) {
        if (roleStr == null) {
            this.role = null;
            return this;
        }

        try {
            // 1. Converte para maiúsculas (ex: "student" -> "STUDENT")
            // 2. Tenta encontrar no Enum
            this.role = RoleType.valueOf(roleStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            System.err.println("Erro: Role desconhecido '" + roleStr + "'. A definir como null.");
            this.role = null;
        }
        return this;
    }

    public UserManager setRole(RoleType role) {
        this.role = role;
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