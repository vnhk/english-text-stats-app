package com.bervan.englishtextstats;

import com.bervan.common.service.BaseService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WordService implements BaseService<Word> {
    private final EpubNotKnownWordsService epubNotKnownWordsService;

    public WordService(EpubNotKnownWordsService epubNotKnownWords) {
        this.epubNotKnownWordsService = epubNotKnownWords;
    }

    @Override
    public void save(List<Word> data) {
        for (Word datum : data) {
            epubNotKnownWordsService.markAsLearned(datum.getName());
        }
    }

    @Override
    public void save(Word data) {
        epubNotKnownWordsService.markAsLearned(data.getName());
    }

    @Override
    public List<Word> load() {
        return epubNotKnownWordsService.getNotLearnedWords(100);
    }

    public String getActualEpub() {
        return epubNotKnownWordsService.getActualEpub();
    }

    public void setActualEpub(String actualEbook) {
        epubNotKnownWordsService.setActualEpub(actualEbook);
    }
}
