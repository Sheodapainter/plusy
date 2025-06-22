package org.example;

import java.time.LocalDateTime;

public class Word {
    private final String word;
    private final LocalDateTime time;

    public Word(String word) {
        this.word = word;
        this.time = LocalDateTime.now();
    }
    public String getWord() {
        return word;
    }
    public LocalDateTime getTime() {
        return time;
    }

    @Override
    public String toString() {
        return time.getHour() + ":" + time.getMinute() + ":" + time.getSecond() + " " + word.toLowerCase();
    }
}
