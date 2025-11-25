package pt.isec.pd.common;

public enum Command {
    CONNECTION,
    LOGIN,
    REGISTER_STUDENT,
    REGISTER_TEACHER,
    GET_USER_INFO,
    CREATE_QUESTION,
    LOGOUT;

    public static Command fromString(String s) {
        try {
            return Command.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
