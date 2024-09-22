package com.bervan.englishtextstats;

import com.bervan.common.service.BaseService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class WordService implements BaseService<Word> {
    private final EpubNotKnownWordsService epubNotKnownWordsService;

    public WordService(EpubNotKnownWordsService epubNotKnownWords) {
        this.epubNotKnownWordsService = epubNotKnownWords;
    }

    @Override
    public void save(List<Word> data) {
        for (Word datum : data) {
            epubNotKnownWordsService.markAsLearned(datum.getTableFilterableColumnValue());
        }
    }

    @Override
    public Word save(Word data) {
        epubNotKnownWordsService.markAsLearned(data.getTableFilterableColumnValue());
        return data;
    }

    @Override
    public Set<Word> load() {
        return epubNotKnownWordsService.getNotLearnedWords(100);
    }

    @Override
    public void delete(Word item) {
        throw new RuntimeException("Not valid method!");
    }

    public String getActualEpub() {
        return epubNotKnownWordsService.getActualEpub();
    }

    public void setActualEpub(String actualEbook) {
        epubNotKnownWordsService.setActualEpub(actualEbook);
    }
}
