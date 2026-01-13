package scene_master.writer;

public class ObjWriterException extends RuntimeException {
    public ObjWriterException(String message) {
        super(message);
    }

    public ObjWriterException(String message, Throwable cause) {
        super(message, cause);
    }

    public ObjWriterException(String format, Object... args) {
        super(String.format(format, args));
    }
}