package com.bervan.englishtextstats;

import com.bervan.common.service.BaseService;
import com.bervan.ieentities.ExcelIEEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class WordService implements BaseService<UUID, Word> {
    private final EbookNotKnownWordsService ebookNotKnownWordsService;

    public WordService(EbookNotKnownWordsService epubNotKnownWords) {
        this.ebookNotKnownWordsService = epubNotKnownWords;
    }

    @Override
    public void save(List<Word> data) {
        for (Word datum : data) {
            ebookNotKnownWordsService.markAsLearned(datum.getTableFilterableColumnValue());
        }
    }

    @Override
    public Word save(Word data) {
        ebookNotKnownWordsService.markAsLearned(data.getTableFilterableColumnValue());
        return data;
    }

    @Override
//    @PostFilter("(T(com.bervan.common.service.AuthService).hasAccess(filterObject.owners))")
    public Set<Word> load() {
        return ebookNotKnownWordsService.getNotLearnedWords(100);
    }

    @Override
    public void delete(Word item) {
        throw new RuntimeException("Not valid method!");
    }

    @Override
    public void saveIfValid(List<? extends ExcelIEEntity> objects) {
        throw new RuntimeException("Not supported yet!");
    }

    public String getActualEpub() {
        return ebookNotKnownWordsService.getActualEbook();
    }

    public void setActualEpub(String actualEbook) {
        ebookNotKnownWordsService.setActualEbook(actualEbook);
    }
}
