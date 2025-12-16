package pt.isec.pd.common.entities;

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

    public String getTeacherId() {
        return teacherId;
    }
    private int totalAnswers;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String teacherId;

    public Question(String question, String correctOption, String[] options,
                    LocalDateTime startTime, LocalDateTime endTime, String teacherId) {
        this.id = generateId(); // Gera novo ID
        this.question = question;
        this.correctOption = correctOption;
        this.options = options;
        this.hasAnswers = false;
        this.startTime = startTime;
        this.endTime = endTime;
        this.teacherId = teacherId;
    }

    public Question(String id, String question, String correctOption, String[] options,
                    LocalDateTime startTime, LocalDateTime endTime, String teacherId, boolean hasAnswers) {
        this.id = id;
        this.question = question;
        this.correctOption = correctOption;
        this.options = options;
        this.hasAnswers = hasAnswers;
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

    public void setId(String id) {
        this.id = id;
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

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public int getTotalAnswers() { return totalAnswers; }

    public void setTotalAnswers(int totalAnswers) { this.totalAnswers = totalAnswers; }

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
                ", totalAnswers=" + totalAnswers +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", teacherId='" + teacherId + '\'' +
                ", hasAnswers=" + hasAnswers +
                '}';
    }
}