package org.example;

public enum JsonTokenType
{
    START_OBJECT,
    END_OBJECT,
    START_ARRAY,
    END_ARRAY,
    KEY,
    STRING,
    NUMBER,
    TRUE,
    FALSE,
    NULL,
    COLON,
    COMMA,
    SOF,
    EOF,
    BATCH_SPLIT
}
