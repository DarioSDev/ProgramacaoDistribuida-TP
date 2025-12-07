package pt.isec.pd.common.core;

public enum Command {
    CONNECTION,
    LOGIN,
    REGISTER_STUDENT,
    REGISTER_TEACHER,
    GET_USER_INFO,
    CREATE_QUESTION,
    LOGOUT,
    UPDATE_NOTIFICATION,

    VALIDATE_QUESTION_CODE, // Verificar se o código existe e está ativo
    GET_QUESTION,           // Obter os dados da pergunta
    SUBMIT_ANSWER,         // Enviar a resposta do aluno
    EDIT_QUESTION,
    DELETE_QUESTION,

    GET_TEACHER_QUESTIONS,
    GET_STUDENT_HISTORY,
    GET_QUESTION_RESULTS;

    public static Command fromString(String s) {
        try {
            return Command.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
