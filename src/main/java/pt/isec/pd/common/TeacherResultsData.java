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
    private final String date;
    private final String startTime;
    private final String endTime;
    private final int totalAnswers;
    private final List<StudentAnswerInfo> answers;

    public TeacherResultsData(String questionText, List<String> options, String correctOptionLetter,
                              String date, String startTime, String endTime,
                              int totalAnswers, List<StudentAnswerInfo> answers) {
        this.questionText = questionText;
        this.options = options;
        this.correctOptionLetter = correctOptionLetter;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalAnswers = totalAnswers;
        this.answers = answers;
    }

    // Getters
    public String getQuestionText() { return questionText; }
    public List<String> getOptions() { return options; }
    public String getCorrectOptionLetter() { return correctOptionLetter; }
    public String getDate() { return date; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public int getTotalAnswers() { return totalAnswers; }
    public List<StudentAnswerInfo> getAnswers() { return answers; }
}