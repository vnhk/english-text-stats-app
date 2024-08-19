package com.bervan.englishtextstats;

public class Word {
    private String name;
    private Long count;
    private String translation;

    public Word(String name, Long count, String translation) {
        this.name = name;
        this.count = count;
        this.translation = translation;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }

    @Override
    public String toString() {
        return
                name + " - " + count;
    }
}
