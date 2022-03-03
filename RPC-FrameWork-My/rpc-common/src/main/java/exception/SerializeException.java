package exception;

/**
 * 序列化异常
 *
 * @author ziyang
 */
public class SerializeException extends RuntimeException {
    public SerializeException(String msg) {
        //public RuntimeException(String message) {super(message);}
        super(msg);
    }
}
