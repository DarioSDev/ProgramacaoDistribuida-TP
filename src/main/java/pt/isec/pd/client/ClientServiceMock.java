package pt.isec.pd.client;

import pt.isec.pd.common.User;

import java.time.LocalDate;
import java.time.LocalTime;
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
                    "What is the capital of France?",
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
            question = "What is the capital of France?";
        else if (code.equalsIgnoreCase("MATH001"))
            question = "2 + 2 = ?";

        return new AnswerResultData(
                question,
                correct ? AnswerState.CORRECT : AnswerState.WRONG,
                LocalDate.now()
        );
    }

    @Override
    public List<HistoryItem> getStudentHistory(User user, LocalDate start, LocalDate end, String filter) {
        return List.of(
                new HistoryItem("What is the capital of France?", LocalDate.of(2025, 1, 12), true),
                new HistoryItem("2 + 2 = ?", LocalDate.of(2025, 3, 10), false),
                new HistoryItem("Who discovered America?", LocalDate.of(2024, 12, 20), true)
        );
    }

    @Override
    public List<TeacherQuestionItem> getTeacherQuestions(User user, String filter) {
        return List.of(
                new TeacherQuestionItem("Capital Cities", LocalDate.of(2025, 1, 12), "Active"),
                new TeacherQuestionItem("Basic Math", LocalDate.of(2025, 3, 10), "Future"),
                new TeacherQuestionItem("Biology Quiz", LocalDate.of(2024, 11, 2), "Expired")
        );
    }

    @Override
    public boolean createQuestion(
            User user,
            String text,
            List<String> options,
            int correctIndex,
            LocalDate sd,
            LocalTime st,
            LocalDate ed,
            LocalTime et
    ) {
        System.out.println("\n[MOCK] CREATE QUESTION");
        System.out.println("Teacher: " + user.getEmail());
        System.out.println("Question: " + text);
        for (int i = 0; i < options.size(); i++) {
            System.out.println(" " + (char) ('a' + i) + ") " + options.get(i));
        }
        System.out.println("Correct index: " + correctIndex);
        System.out.println("Start: " + sd + " " + st);
        System.out.println("End: " + ed + " " + et);
        return true;
    }

    @Override
    public TeacherResultsData getQuestionResults(User user, String code) {
        TeacherResultsData dataABC = new TeacherResultsData(
                "What is the capital of France?",
                List.of("Berlin", "Madrid", "Paris", "Lisbon"),
                "c",
                3,
                List.of(
                        new StudentAnswerInfo("Ana Silva", "ana@example.com", "a", false),
                        new StudentAnswerInfo("João Costa", "joao@example.com", "c", true),
                        new StudentAnswerInfo("Maria Santos", "maria@example.com", "b", false)
                )
        );

        TeacherResultsData dataMath = new TeacherResultsData(
                "2 + 2 = ?",
                List.of("3", "4", "5", "22"),
                "b",
                2,
                List.of(
                        new StudentAnswerInfo("Pedrito", "pedro@example.com", "b", true),
                        new StudentAnswerInfo("Sara Gomes", "sara@example.com", "a", false)
                )
        );

        if (code.equalsIgnoreCase("ABC123")) return dataABC;
        if (code.equalsIgnoreCase("MATH001")) return dataMath;
        return null;
    }
}