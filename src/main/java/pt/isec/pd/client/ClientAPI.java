package pt.isec.pd.client;

import pt.isec.pd.common.User;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ClientAPI {

    String sendLogin(String email, String password) throws Exception;

    default boolean register(User user, String password) {
        return false;
    }

    QuestionData getQuestionByCode(String code);

    boolean submitAnswer(User user, String code, int index);

    String validateQuestionCode(String code);

    AnswerResultData getAnswerResult(User user, String code);

    List<HistoryItem> getStudentHistory(User user, LocalDate start, LocalDate end, String filter);

    List<TeacherQuestionItem> getTeacherQuestions(User user, String filter);

    boolean createQuestion(
            User user,
            String text,
            List<String> options,
            int correctIndex,
            LocalDate sd,
            LocalTime st,
            LocalDate ed,
            LocalTime et
    );

    TeacherResultsData getQuestionResults(User user, String questionCode);

    record QuestionData(String text, List<String> options) {}

    record HistoryItem(String questionText, LocalDate date, boolean correct) {}

    enum AnswerState { CORRECT, WRONG, SUBMITTED }

    record AnswerResultData(String question, AnswerState state, LocalDate date) {}

    record StudentAnswerInfo(String studentName, String studentEmail, String answerLetter, boolean correct) {}

    record TeacherResultsData(
            String questionText,
            List<String> options,
            String correctOptionLetter,
            int totalAnswers,
            List<StudentAnswerInfo> answers
    ) {}

    record TeacherQuestionItem(String title, LocalDate date, String status) {}

}

