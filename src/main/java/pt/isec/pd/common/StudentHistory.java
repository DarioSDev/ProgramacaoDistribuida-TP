package pt.isec.pd.common;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class StudentHistory implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String studentEmail;
    private final List<HistoryItem> items;

    public StudentHistory(String studentEmail, List<HistoryItem> items) {
        this.studentEmail = studentEmail;
        this.items = items;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public List<HistoryItem> getItems() {
        return items;
    }

    @Override
    public String toString() {
        return "StudentHistory{" +
                "email='" + studentEmail + '\'' +
                ", count=" + (items != null ? items.size() : 0) +
                '}';
    }
}