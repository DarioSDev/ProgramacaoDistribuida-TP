package pt.isec.pd.client;

import jdk.jshell.spi.ExecutionControl;
import pt.isec.pd.common.Question;
import pt.isec.pd.common.TeacherResultsData;
import pt.isec.pd.common.User;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ClientAPI {

    default String sendLogin(String email, String password) throws Exception {
        throw new UnsupportedOperationException("sendLogin não implementado.");
    }

    default boolean register(String role, String name, String id, String email, String password) throws IOException {
        throw new UnsupportedOperationException("register não implementado.");
    }

    default boolean createQuestion(String question, List<String> choices, String correctChoice) {
        throw new UnsupportedOperationException("create Question not implemented");
    }

    default QuestionData getQuestionByCode(String code) {
        throw new UnsupportedOperationException("getQuestionByCode não implementado.");
    }

    default boolean submitAnswer(User user, String code, int index) throws IOException {
        throw new UnsupportedOperationException("submitAnswer não implementado.");
    }

    default String validateQuestionCode(String code) throws IOException{
        throw new UnsupportedOperationException("validateQuestionCode não implementado.");
    }

    default AnswerResultData getAnswerResult(User user, String code) {
        throw new UnsupportedOperationException("getAnswerResult não implementado.");
    }

    default List<HistoryItem> getStudentHistory(User user, LocalDate start, LocalDate end, String filter) throws IOException {
        throw new UnsupportedOperationException("getStudentHistory não implementado.");
    }

//    default List<TeacherQuestionItem> getTeacherQuestions(User user, String filter){
//        throw new UnsupportedOperationException("getTeacherQuestions não implementado.");
//    }

    default List<Question> getTeacherQuestions(User user, String filter) throws IOException {
        throw new UnsupportedOperationException("getTeacherQuestions não implementado.");
    }

    record QuestionResult(boolean success, String id) {}

    default QuestionResult createQuestion(
            User user,
            String text,
            List<String> options,
            String correctOption,
            LocalDate sd,
            LocalTime st,
            LocalDate ed,
            LocalTime et
    ) throws IOException {
        throw new UnsupportedOperationException("createQuestion não implementado.");
    }

    default TeacherResultsData getQuestionResults(User user, String questionCode) {
        throw new UnsupportedOperationException("getQuestionResults não implementado.");
    }

    // --- Records e Enums auxiliares ---

    record QuestionData(String text, List<String> options) {}

    record HistoryItem(String questionText, LocalDate date, boolean correct) {}

    enum AnswerState { CORRECT, WRONG, SUBMITTED }

    record AnswerResultData(String question, AnswerState state, LocalDate date) {}

    record TeacherQuestionItem(String title, LocalDate date, String status) {}

    default boolean deleteQuestion(String questionId) {
		throw new UnsupportedOperationException("deleteQuestion não implementado.");
	}

}

