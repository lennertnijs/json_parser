package org.example;

public final class IllegalJsonFormatException extends RuntimeException{

    public IllegalJsonFormatException()
    {

    }

    public IllegalJsonFormatException(String msg)
    {
        super(msg);
    }
}
