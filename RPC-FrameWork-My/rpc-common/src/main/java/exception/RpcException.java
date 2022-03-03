package exception;

import enumeration.RpcError;

/**
 * RPC服务调用异常
 */
public class RpcException extends RuntimeException {

    //public RuntimeException(String message) {super(message);}

    public RpcException(RpcError error, String detail) {
        super(error.getMessage() + ": " + detail);
    }

    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }

    public RpcException(RpcError error) {
        super(error.getMessage());
    }

}
