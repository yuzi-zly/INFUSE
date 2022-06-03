package com.CC.Middleware;

public class NotSupportedException extends Exception{
    public NotSupportedException() {
        super("It is not supported!");
    }

    public NotSupportedException(String message) {
        super(message);
    }
}
