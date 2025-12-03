package pt.isec.pd.common;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class TeacherResultsData implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String questionText;
    private final List<String> options;
    private final String correctOptionLetter;
    private final String creationDate; // Campo adicionado
    private final int totalAnswers;
    private final List<StudentAnswerInfo> answers;

    public TeacherResultsData(String questionText, List<String> options, String correctOptionLetter,
                              String creationDate, int totalAnswers, List<StudentAnswerInfo> answers) {
        this.questionText = questionText;
        this.options = options;
        this.correctOptionLetter = correctOptionLetter;
        this.creationDate = creationDate;
        this.totalAnswers = totalAnswers;
        this.answers = answers;
    }

    public String getQuestionText() {
        return questionText;
    }

    public List<String> getOptions() {
        return options;
    }

    public String getCorrectOptionLetter() {
        return correctOptionLetter;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public int getTotalAnswers() {
        return totalAnswers;
    }

    public List<StudentAnswerInfo> getAnswers() {
        return answers;
    }

    @Override
    public String toString() {
        return "TeacherResultsData{" +
                "question='" + questionText + '\'' +
                ", date='" + creationDate + '\'' +
                ", total=" + totalAnswers +
                '}';
    }
}