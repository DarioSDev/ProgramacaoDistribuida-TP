package pt.isec.pd.common;

public enum Command {
    CLIENT_AUTH_REQUEST,
    CLIENT_REGISTER_REQUEST;

    public static Command fromString(String s) {
        try {
            return Command.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
