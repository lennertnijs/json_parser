package org.example;

public final class Index
{
    private int idx;

    public Index(int i)
    {
        if(i < 0){
            throw new IllegalArgumentException("Negative index is illegal.");
        }
        this.idx = i;
    }

    public int getIndex()
    {
        return idx;
    }

    public Index increment()
    {
        this.idx++;
        return this;
    }
}
