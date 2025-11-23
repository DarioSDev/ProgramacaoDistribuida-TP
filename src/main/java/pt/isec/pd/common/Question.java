package pt.isec.pd.common;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Arrays;

public class Question implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String question;
    private String correctOption;
    private String[] options;
    private boolean hasAnswers;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String teacherId;

    public Question(String question, String correctOption, String[] options,
                    LocalDateTime startTime, LocalDateTime endTime, String teacherId) {
        this.id = generateId();
        this.question = question;
        this.correctOption = correctOption;
        this.options = options;
        this.hasAnswers = false;
        this.startTime = startTime;
        this.endTime = endTime;
        this.teacherId = teacherId;
    }

    private String generateId() {
        int num = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(num);
    }

    public String getId() {
        return id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getCorrectOption() {
        return correctOption;
    }

    public void setCorrectOption(String correctOption) {
        this.correctOption = correctOption;
    }

    public String[] getOptions() {
        return options;
    }

    public void setOptions(String[] options) {
        this.options = options;
    }

    public boolean hasAnswers() {
        return hasAnswers;
    }

    public void setHasAnswers(boolean hasAnswers) {
        this.hasAnswers = hasAnswers;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(startTime) && now.isBefore(endTime);
    }

    @Override
    public String toString() {
        return "Question{" +
                "id='" + id + '\'' +
                ", question='" + question + '\'' +
                ", options=" + Arrays.toString(options) +
                ", correctOption='" + correctOption + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", teacherId='" + teacherId + '\'' +
                '}';
    }
}
