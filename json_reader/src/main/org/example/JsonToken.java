package org.example;

public record JsonToken(JsonTokenType type, String value)
{

    @Override
    public String toString()
    {
        return String.format("type: %s, value: %s", type, value);
    }
}
