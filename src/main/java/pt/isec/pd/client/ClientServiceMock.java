package pt.isec.pd.client;

import pt.isec.pd.common.entities.User;

import java.time.LocalDate;
import java.util.List;

public class ClientServiceMock implements ClientAPI {

    @Override
    public String sendLogin(String email, String password) {
        if (email.equals("teacher@isec.pt")) return "OK;teacher;Professor Mock;1234";
        if (email.equals("student@isec.pt")) return "OK;student;Aluno Mock;9876";
        return "ERROR";
    }

    @Override
    public boolean register(String role, String name, String id, String email, String password) {
        return true;
    }

    @Override
    public String validateQuestionCode(String code) {
        return code.equalsIgnoreCase("ABC123") || code.equalsIgnoreCase("MATH001") ? "VALID" : "INVALID";
    }

    @Override
    public QuestionData getQuestionByCode(String code) {
        if (code.equalsIgnoreCase("ABC123")) {
            return new QuestionData(
                    "\"What is the capital of France What is the capital of France What is the capital of France What is the capital of France?What is the capital of France",
                    List.of("Berlin", "Madrid", "Paris", "Lisbon")
            );
        }
        if (code.equalsIgnoreCase("MATH001")) {
            return new QuestionData(
                    "2 + 2 = ?",
                    List.of("3", "4", "5", "22")
            );
        }
        return null;
    }

    @Override
    public boolean submitAnswer(User user, String code, int index) {
        if (code.equalsIgnoreCase("ABC123")) {
            return index == 2;
        }
        if (code.equalsIgnoreCase("MATH001")) {
            return index == 1;
        }
        return false;
    }

    @Override
    public AnswerResultData getAnswerResult(User user, String code) {
        boolean correct = Math.random() > 0.4;
        String question = "Unknown question";

        if (code.equalsIgnoreCase("ABC123"))
            question = "What is the capital of France What is the capital of France What is the capital of France What is the capital of France?What is the capital of France";
        else if (code.equalsIgnoreCase("MATH001"))
            question = "2 + 2 = ?";

        return new AnswerResultData(
                question,
                correct ? AnswerState.CORRECT : AnswerState.WRONG,
                LocalDate.now()
        );
    }

//    @Override
//    public List<HistoryItem> getStudentHistory(User user, LocalDate start, LocalDate end, String filter) {
//        return List.of(
//                new HistoryItem("What is the capital of France What is the capital of France What is the capital of France What is the capital of France?What is the capital of France", LocalDate.of(2025, 1, 12), true),
//                new HistoryItem("2 + 2 = ?", LocalDate.of(2025, 3, 10), false),
//                new HistoryItem("Who discovered America?", LocalDate.of(2024, 12, 20), true)
//        );
//    }

    @Override
    public boolean deleteQuestion(String questionId) {
        System.out.println("\n[MOCK] DELETE QUESTION");
        System.out.println("Simulating deletion of question ID: " + questionId);
        return true;
    }
}