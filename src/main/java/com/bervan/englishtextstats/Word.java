package com.bervan.englishtextstats;

import com.bervan.common.model.PersistableTableData;

import java.util.Objects;
import java.util.UUID;

public class Word implements PersistableTableData {
    private final UUID uuid = UUID.randomUUID();
    private String name;
    private Long count;
    private String translation;

    public Word(String name, Long count, String translation) {
        this.name = name;
        this.count = count;
        this.translation = translation;
    }

    public String getTableFilterableColumnValue() {
        return name;
    }

    @Override
    public UUID getId() {
        return uuid;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Word word)) return false;
        return Objects.equals(name, word.name) && Objects.equals(count, word.count) && Objects.equals(translation, word.translation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, count, translation);
    }
}
