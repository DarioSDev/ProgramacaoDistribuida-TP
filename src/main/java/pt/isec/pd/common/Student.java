package pt.isec.pd.common;

public class Student extends User {
    private String idNumber;

    public Student(String name, String email, String password, String idNumber) {
        super(name, email, password);
        this.idNumber = idNumber;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    @Override
    public String toString() {
        return "Student{" +
                "name='" + getName() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", idNumber='" + idNumber + '\'' +
                '}';
    }
}
