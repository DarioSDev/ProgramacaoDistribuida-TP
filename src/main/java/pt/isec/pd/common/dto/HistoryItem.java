package pt.isec.pd.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

public class HistoryItem implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String questionText;
    private final LocalDate date;
    private final boolean correct;

    private final List<String> options;
    private final String correctOption;
    private final String studentOption;

    public HistoryItem(String id, String questionText, LocalDate date, boolean correct,
                       List<String> options, String correctOption, String studentOption) {
        this.id = id;
        this.questionText = questionText;
        this.date = date;
        this.correct = correct;
        this.options = options;
        this.correctOption = correctOption;
        this.studentOption = studentOption;
    }

    public String getId() { return id; }
    public String getQuestionText() { return questionText; }
    public LocalDate getDate() { return date; }
    public boolean isCorrect() { return correct; }

    public List<String> getOptions() { return options; }
    public String getCorrectOption() { return correctOption; }
    public String getStudentOption() { return studentOption; }

    @Override
    public String toString() {
        return "HistoryItem{" +
                "id='" + id + '\'' +
                ", question='" + questionText + '\'' +
                ", date=" + date +
                ", correct=" + correct +
                '}';
    }
}