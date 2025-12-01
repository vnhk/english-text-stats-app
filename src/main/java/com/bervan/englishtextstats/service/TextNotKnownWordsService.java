package com.bervan.englishtextstats.service;

import com.bervan.common.search.SearchService;
import com.bervan.common.service.AuthService;
import com.bervan.common.service.BaseService;
import com.bervan.englishtextstats.ExtractedEbookText;
import com.bervan.englishtextstats.KnownWord;
import com.bervan.englishtextstats.Word;
import com.bervan.logging.JsonLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TextNotKnownWordsService extends BaseService<UUID, KnownWord> {
    private final JsonLogger log = JsonLogger.getLogger(getClass());
    private final ExtractedEbookTextRepository extractedEbookTextRepository;
    private final Map<UUID, List<KnownWord>> inMemoryWordsForUser = new ConcurrentHashMap<>();
    @Value("${file.service.storage.folder}")
    private String pathToFileStorage;

    public TextNotKnownWordsService(ExtractedEbookTextRepository extractedEbookTextRepository, KnownWordRepository knownWordRepository,
                                    SearchService searchService, @Value("${file.service.storage.folder}") String pathToFileStorage) {
        super(knownWordRepository, searchService);
        this.pathToFileStorage = pathToFileStorage;
        this.extractedEbookTextRepository = extractedEbookTextRepository;
    }

    public void markAsLearned(String word) {
        if (word == null || word.isBlank()) {
            return;
        }

        word = word.trim();
        KnownWord knownWord = new KnownWord();
        if (!existsByValue(word)) {
            knownWord.setValue(word);
            knownWord = repository.save(knownWord);
            updateInMemoryWords(Collections.singletonList(knownWord));
        }
    }

    private boolean existsByValue(String word) {
        if (inMemoryWordsForUser.get(AuthService.getLoggedUserId()) == null) {
            loadIntoMemory();
        }
        return inMemoryWordsForUser.get(AuthService.getLoggedUserId()).stream()
                .anyMatch(e -> e.getValue().equalsIgnoreCase(word));
    }

    protected void updateInMemoryWords(Collection<KnownWord> toBeAdded) {
        toBeAdded.forEach(e -> e.setValue(e.getValue().toLowerCase()));
        inMemoryWordsForUser.computeIfAbsent(AuthService.getLoggedUserId(), k -> new ArrayList<>());
        inMemoryWordsForUser.get(AuthService.getLoggedUserId()).addAll(toBeAdded);
    }

    public List<Word> getNotLearnedWords(int howMany, String englishSubtitlesPath) {
        if (inMemoryWordsForUser.get(AuthService.getLoggedUserId()) == null) {
            loadIntoMemory();
        }

        String extractedText = getEbookText(englishSubtitlesPath);

        return processTextAndGetNotKnownWords(howMany, extractedText);
    }

    public List<Word> getNotLearnedWords(int howMany, UUID ebookId) {
        if (inMemoryWordsForUser.get(AuthService.getLoggedUserId()) == null) {
            loadIntoMemory();
        }

        String extractedText = getEbookText(ebookId);

        return processTextAndGetNotKnownWords(howMany, extractedText);
    }

    private List<Word> processTextAndGetNotKnownWords(int howMany, String extractedText) {
        try {
            log.info("Extracted Ebook text length: " + extractedText.length());
            UUID loggedUserId = AuthService.getLoggedUserId();
            ConcurrentMap<String, Long> wordCounterComplete = Arrays.stream(extractedText.toLowerCase().split("\\W+"))
                    .parallel()
                    .filter(word -> word.length() > 0)
                    .map(String::trim)
                    .filter(word -> !isLearned(word, inMemoryWordsForUser.get(loggedUserId).stream().map(KnownWord::getValue)
                            .collect(Collectors.toList())))
                    .collect(Collectors.groupingByConcurrent(Function.identity(), Collectors.counting()));

            List<Map.Entry<String, Long>> sortedWordsComplete = wordCounterComplete.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .toList();

            List<Word> resultReduced = sortedWordsComplete.stream()
                    .limit(howMany)
                    .map(entry -> new Word(entry.getKey(), entry.getValue(), null))
                    .collect(Collectors.toList());

            log.info("All Words Not Learned: " + sortedWordsComplete.size());

            return resultReduced;
        } catch (Exception e) {
            log.error("Could not extract English words.", e);
            throw new RuntimeException("Could not extract English words.", e);
        }
    }

    public void loadIntoMemory() {
        updateInMemoryWords(load(Pageable.ofSize(100000000)));
    }

    private boolean isLearned(String word, List<String> learnedWords) {
        try {
            Double.parseDouble(word);
            return true;
        } catch (NumberFormatException ignored) {
        } //filter numbers out

        word = word.trim().toLowerCase();
        if (word.equals("true")) {
            return true;
        }
        if (word.endsWith("s") && learnedWords.contains(word.substring(0, word.length() - 1))) {
            return true;
        }
        if (word.endsWith("ed")) {
            String baseWord = word.substring(0, word.length() - 2);
            if (learnedWords.contains(baseWord) || learnedWords.contains(baseWord + "e")) {
                return true;
            }
        }
        if (word.endsWith("ing")) {
            String baseWord = word.substring(0, word.length() - 3);
            if (learnedWords.contains(baseWord) || learnedWords.contains(baseWord + "e")) {
                return true;
            }
        }
        if (word.endsWith("ly") && learnedWords.contains(word.substring(0, word.length() - 2))) {
            return true;
        }
        if (word.endsWith("ies") && learnedWords.contains(word.substring(0, word.length() - 3) + "y")) {
            return true;
        }
        return learnedWords.contains(word);
    }

    private String getEbookText(UUID id) {
        Optional<ExtractedEbookText> byEbook = extractedEbookTextRepository.findById(id);
        if (byEbook.isPresent()) {
            return byEbook.get().getContent();
        } else {
            throw new RuntimeException("Could not find ebook by id!");
        }
    }

    private String getEbookText(String englishSubtitlesPath) {
        return ExtractedEbookTextService.extractText(pathToFileStorage + File.separator + englishSubtitlesPath);
    }
}
