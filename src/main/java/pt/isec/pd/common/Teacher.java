package pt.isec.pd.common;

import java.io.Serial;
import java.util.UUID;

public class Teacher extends User {
    @Serial
    private static final long serialVersionUID = 1L;
    private String id;

    public Teacher(String name, String email, String password, String teacherCode) {
        super(name, email, password, teacherCode);
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
