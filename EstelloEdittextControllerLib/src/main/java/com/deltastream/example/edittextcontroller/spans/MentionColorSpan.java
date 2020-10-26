package com.deltastream.example.edittextcontroller.spans;

public class MentionColorSpan extends android.text.style.ForegroundColorSpan implements RTSpan<Integer> {

    public MentionColorSpan(int color) {
        super(color);
    }

    @Override
    public Integer getValue() {
        return getForegroundColor();
    }

}