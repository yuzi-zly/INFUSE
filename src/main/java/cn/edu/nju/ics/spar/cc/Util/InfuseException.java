package cn.edu.nju.ics.spar.cc.Util;

/**
 * INFUSE 框架的自定义异常类
 * 用于替代 System.exit() 和改善错误处理
 */
public class InfuseException extends RuntimeException {
    
    public InfuseException(String message) {
        super(message);
    }
    
    public InfuseException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public InfuseException(Throwable cause) {
        super(cause);
    }
}

