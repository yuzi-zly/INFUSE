package cn.edu.nju.ics.spar.cc.Util;

public class NotSupportedException extends Exception{
    public NotSupportedException() {
        super("It is not supported!");
    }

    public NotSupportedException(String message) {
        super(message);
    }
}
