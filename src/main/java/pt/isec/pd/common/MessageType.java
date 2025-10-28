package pt.isec.pd.common;

public enum MessageType {
    REGISTER,
    HEARTBEAT,
    REQUEST_SERVER;

    public static MessageType fromString(String s) {
        try {
            return MessageType.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

