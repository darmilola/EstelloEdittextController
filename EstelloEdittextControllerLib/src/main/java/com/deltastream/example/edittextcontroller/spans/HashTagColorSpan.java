package com.deltastream.example.edittextcontroller.spans;

public class HashTagColorSpan  extends android.text.style.ForegroundColorSpan implements RTSpan<Integer> {

    public HashTagColorSpan(int color) {
        super(color);
    }

    @Override
    public Integer getValue() {
        return getForegroundColor();
    }

}