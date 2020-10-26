package com.deltastream.example.edittextcontroller;

public class Bold {

    private String boldText = "";
    private char textChar;


    public Bold(){

    }

    public char getTextChar() {
        return textChar;
    }

    public void setBoldText(String boldText) {
        this.boldText = boldText;
    }

    public void setTextChar(char textChar) {

        this.textChar = textChar;
        StringBuilder builder = new StringBuilder();
        builder.append(boldText);
        builder.append(textChar);
        this.boldText = builder.toString();

    }

    public String getBoldText() {

        StringBuilder builder = new StringBuilder();
        builder.append("<b>");
        builder.append(boldText);
        builder.append("</b>");
        return builder.toString();

    }
}
