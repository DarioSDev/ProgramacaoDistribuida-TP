package pt.isec.pd.common;

import java.io.Serial;
import java.io.Serializable;

public class StudentAnswerInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String studentName;
    private final String studentEmail;
    private final String answerLetter;
    private final boolean correct;

    public StudentAnswerInfo(String studentName, String studentEmail, String answerLetter, boolean correct) {
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.answerLetter = answerLetter;
        this.correct = correct;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public String getAnswerLetter() {
        return answerLetter;
    }

    public boolean isCorrect() {
        return correct;
    }

    @Override
    public String toString() {
        return "StudentAnswerInfo{" +
                "name='" + studentName + '\'' +
                ", email='" + studentEmail + '\'' +
                ", answer='" + answerLetter + '\'' +
                ", correct=" + correct +
                '}';
    }
}