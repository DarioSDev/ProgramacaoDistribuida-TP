package pt.isec.pd.common.core;
import java.io.Serial;
import java.io.Serializable;

public class Message implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Command command;
    private Object data;

    public Message(Command command, Object data) {
        this.command = command;
        this.data = data;
    }

    public Command getCommand() {
        return command;
    }

    public Object getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Message{" +
                "command=" + command +
                ", data=" + (data != null ? data.toString() : "null") +
                '}';
    }
}