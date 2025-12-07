package pt.isec.pd.common.entities;

import java.io.Serial;
import java.io.Serializable;

public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private int id;
    private String name;
    private String email;
    private String password;

    private String role;   // "student" ou "teacher"
    private String extra;  // idNumber (student) ou teacherCode (teacher)


    public User(String name, String email, String password, String role, String extra) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.extra = extra;
    }
    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public User(String name, String email, String password, String extra) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.extra = extra;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) { this.role = role; }

    public String getExtra() {
        return extra;
    }

    public int getId() {
        return id;
    }
    public void setExtra(String extra) { this.extra = extra; }

    public String getIdNumber() {
        if ("student".equalsIgnoreCase(role)) {
            return extra;
        }
        return null;
    }

    public String getTeacherCode() {
        if ("teacher".equalsIgnoreCase(role)) {
            return extra;
        }
        return null;
    }

    public void setIdNumber(String idNumber) {
        if ("student".equalsIgnoreCase(role)) {
            this.extra = idNumber;
        }
    }

    public void setTeacherCode(String teacherCode) {
        if ("teacher".equalsIgnoreCase(role)) {
            this.extra = teacherCode;
        }
    }


    @Override
    public String toString() {
        return "User{" +
                "name='" + getName() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", role='" + getRole() + '\'' +
                '}';
    }
}
