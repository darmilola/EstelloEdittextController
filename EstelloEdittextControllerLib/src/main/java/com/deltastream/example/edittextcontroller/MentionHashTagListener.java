package com.deltastream.example.edittextcontroller;

public interface MentionHashTagListener {

    void onMentionSelected(CharSequence sequence);
    void onHashTagSelected(CharSequence sequence);
    void onStopMentioning();
    void onStopHashTags();
}
