package pt.isec.pd.common;

public class Teacher extends User{

    public Teacher(String name, String email, String password) {
        super(name, email, password);
    }

    @Override
    public String toString() {
        return "Teacher{" +
                "name='" + getName() + '\'' +
                ", email='" + getEmail() + '\'' +
                '}';
    }
}
