package com.bervan.englishtextstats.service;

import com.bervan.common.search.SearchRequest;
import com.bervan.common.search.model.SortDirection;
import com.bervan.common.service.BaseService;
import com.bervan.core.model.BervanLogger;
import com.bervan.englishtextstats.Word;
import com.bervan.ieentities.ExcelIEEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class WordService extends BaseService<UUID, Word> {
    private TextNotKnownWordsService textNotKnownWordsService;
    private final BervanLogger log;

    public WordService(TextNotKnownWordsService textNotKnownWordsService, BervanLogger log) {
        super(null, null);
        this.textNotKnownWordsService = textNotKnownWordsService;
        this.log = log;
    }

    @Override
    public void save(List<Word> data) {
        for (Word datum : data) {
            textNotKnownWordsService.markAsLearned(datum.getTableFilterableColumnValue());
        }
    }

    @Override
    public Word save(Word data) {
        textNotKnownWordsService.markAsLearned(data.getTableFilterableColumnValue());
        return data;
    }

    public List<Word> loadNotKnownWords(UUID ebookId) {
        return textNotKnownWordsService.getNotLearnedWords(50, ebookId);
    }

    @Override
    public List<Word> load(SearchRequest request, Pageable pageable, String sort, SortDirection direction, List<String> columnsToFetch) {
        throw new RuntimeException("Unsupported!");
    }

    @Override
    public Set<Word> load(Pageable pageable) {
        throw new RuntimeException("Unsupported!");
    }

    @Override
    public long loadCount(SearchRequest request) {
        throw new RuntimeException("Unsupported!");
    }

    @Override
    public long loadCount() {
        throw new RuntimeException("Unsupported!");
    }

    @Override
    public void delete(Word item) {
        throw new RuntimeException("Not valid method!");
    }

    @Override
    public void saveIfValid(List<? extends ExcelIEEntity<UUID>> objects) {
        throw new RuntimeException("Not supported!");
    }

    public List<Word> loadNotKnownWords(String englishSubtitlesPath) {
        return textNotKnownWordsService.getNotLearnedWords(50, englishSubtitlesPath);
    }
}
