package pt.isec.pd.common;

import java.util.UUID;

public class Teacher extends User{
    private String id;

    public Teacher(String name, String email, String password) {
        super(name, email, password);
        id = UUID.randomUUID().toString();
    }

    public String getTeacherId() {
        return this.id;
    }

    @Override
    public String toString() {
        return "Teacher{" +
                "name='" + getName() + '\'' +
                ", email='" + getEmail() + '\'' +
                '}';
    }
}
