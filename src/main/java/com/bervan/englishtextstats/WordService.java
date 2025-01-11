package com.bervan.englishtextstats;

import com.bervan.common.service.BaseService;
import com.bervan.core.model.BervanLogger;
import com.bervan.ieentities.ExcelIEEntity;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class WordService extends BaseService<UUID, Word> {
    private TextNotKnownWordsService textNotKnownWordsService;
    @Value("${file.service.storage.folder}")
    private String pathToFileStorage;

    @Value("${ebook-not-known-words.file-storage-relative-path}")
    private String appConfigFolder;

    private final BervanLogger log;

    public WordService(BervanLogger log) {
        super(null, null);
        this.log = log;
    }

    @PostConstruct
    private void init() {
        this.textNotKnownWordsService = new TextNotKnownWordsService(pathToFileStorage, appConfigFolder, log);
    }

    public void setPath(String path) {
        textNotKnownWordsService.buildPath(path);
    }

    public void setActualEbookAndUpdatePath(String actualEbook) {
        textNotKnownWordsService.setActualEbookAndUpdatePath(actualEbook);
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

    @Override
    public Set<Word> load() {
        return textNotKnownWordsService.getNotLearnedWords(100);
    }

    @Override
    public void delete(Word item) {
        throw new RuntimeException("Not valid method!");
    }

    @Override
    public void saveIfValid(List<? extends ExcelIEEntity<UUID>> objects) {
        throw new RuntimeException("Not supported!");
    }

    public String getActualEbook() {
        return textNotKnownWordsService.getActualEbook();
    }
}
