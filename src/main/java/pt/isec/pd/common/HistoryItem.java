package pt.isec.pd.common;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

public class HistoryItem implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String questionText;
    private final LocalDate date;
    private final boolean correct;

    public HistoryItem(String questionText, LocalDate date, boolean correct) {
        this.questionText = questionText;
        this.date = date;
        this.correct = correct;
    }

    public String getQuestionText() {
        return questionText;
    }

    public LocalDate getDate() {
        return date;
    }

    public boolean isCorrect() {
        return correct;
    }

    @Override
    public String toString() {
        return "HistoryItem{" +
                "question='" + questionText + '\'' +
                ", date=" + date +
                ", correct=" + correct +
                '}';
    }
}