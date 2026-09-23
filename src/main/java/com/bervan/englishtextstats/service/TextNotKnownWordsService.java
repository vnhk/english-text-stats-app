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
    private final JsonLogger log = JsonLogger.getLogger(getClass(), "english-ebook");
    private final ExtractedEbookTextRepository extractedEbookTextRepository;
    private final ExtractedEbookTextService extractedEbookTextService;
    private final Map<UUID, List<KnownWord>> inMemoryWordsForUser = new ConcurrentHashMap<>();
    @Value("${file.service.storage.folder.main}")
    private String pathToFileStorage;

    public TextNotKnownWordsService(ExtractedEbookTextRepository extractedEbookTextRepository,
                                    ExtractedEbookTextService extractedEbookTextService,
                                    KnownWordRepository knownWordRepository,
                                    SearchService searchService,
                                    @Value("${file.service.storage.folder.main}") String pathToFileStorage) {
        super(knownWordRepository, searchService);
        this.pathToFileStorage = pathToFileStorage;
        this.extractedEbookTextRepository = extractedEbookTextRepository;
        this.extractedEbookTextService = extractedEbookTextService;
    }

    public void markAsLearned(String word) {
        if (word == null || word.isBlank()) {
            return;
        }

        word = word.trim().toLowerCase();
        if (!existsByValue(word)) {
            KnownWord knownWord = new KnownWord();
            knownWord.setValue(word);
            knownWord = repository.save(knownWord);
            updateInMemoryWords(Collections.singletonList(knownWord));
        }
    }

    private boolean existsByValue(String word) {
        UUID userId = AuthService.getLoggedUserId();
        if (inMemoryWordsForUser.get(userId) == null) {
            loadIntoMemory();
        }
        List<KnownWord> userWords = inMemoryWordsForUser.get(userId);
        if (userWords == null) {
            return false;
        }
        return userWords.stream()
                .filter(Objects::nonNull)
                .map(KnownWord::getValue)
                .filter(Objects::nonNull)
                .anyMatch(e -> e.equalsIgnoreCase(word));
    }

    protected void updateInMemoryWords(Collection<KnownWord> toBeAdded) {
        if (toBeAdded == null) return;
        List<KnownWord> sanitized = toBeAdded.stream()
                .filter(e -> e != null && e.getValue() != null && !e.getValue().isBlank())
                .map(e -> {
                    KnownWord copy = new KnownWord();
                    copy.setId(e.getId());
                    copy.setValue(e.getValue().trim().toLowerCase());
                    return copy;
                })
                .collect(Collectors.toList());

        UUID userId = AuthService.getLoggedUserId();
        inMemoryWordsForUser.computeIfAbsent(userId, k -> new ArrayList<>());
        inMemoryWordsForUser.get(userId).addAll(sanitized);
    }

    public List<Word> getNotLearnedWords(int howMany, String englishSubtitlesPath) {
        UUID userId = AuthService.getLoggedUserId();
        if (inMemoryWordsForUser.get(userId) == null) {
            loadIntoMemory();
        }

        String extractedText = getEbookText(englishSubtitlesPath);
        return processTextAndGetNotKnownWords(howMany, extractedText);
    }

    public List<Word> getNotLearnedWords(int howMany, UUID ebookId) {
        UUID userId = AuthService.getLoggedUserId();
        if (inMemoryWordsForUser.get(userId) == null) {
            loadIntoMemory();
        }

        String extractedText = getEbookText(ebookId);
        return processTextAndGetNotKnownWords(howMany, extractedText);
    }

    private List<Word> processTextAndGetNotKnownWords(int howMany, String extractedText) {
        try {
            if (extractedText == null || extractedText.isBlank()) {
                log.warn("Extracted Ebook text is empty!");
                return Collections.emptyList();
            }

            log.info("Extracted Ebook text length: " + extractedText.length());
            UUID loggedUserId = AuthService.getLoggedUserId();
            List<KnownWord> userWords = inMemoryWordsForUser.getOrDefault(loggedUserId, Collections.emptyList());
            Set<String> learnedWordsSet = userWords.stream()
                    .filter(Objects::nonNull)
                    .map(KnownWord::getValue)
                    .filter(Objects::nonNull)
                    .map(w -> w.trim().toLowerCase())
                    .collect(Collectors.toSet());

            ConcurrentMap<String, Long> wordCounterComplete = Arrays.stream(extractedText.toLowerCase().split("[^a-zA-Z]+"))
                    .parallel()
                    .filter(word -> word != null && word.length() > 1)
                    .map(String::trim)
                    .filter(word -> !isLearned(word, learnedWordsSet))
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

    private boolean isLearned(String word, Set<String> learnedWords) {
        if (word == null || word.isBlank()) {
            return true;
        }
        word = word.trim().toLowerCase();
        if (word.length() <= 1 && !word.equals("a") && !word.equals("i")) {
            return true;
        }
        if (learnedWords.contains(word)) {
            return true;
        }
        if (word.endsWith("s") && word.length() > 2 && learnedWords.contains(word.substring(0, word.length() - 1))) {
            return true;
        }
        if (word.endsWith("ed") && word.length() > 3) {
            String baseWord = word.substring(0, word.length() - 2);
            if (learnedWords.contains(baseWord) || learnedWords.contains(baseWord + "e")) {
                return true;
            }
        }
        if (word.endsWith("ing") && word.length() > 4) {
            String baseWord = word.substring(0, word.length() - 3);
            if (learnedWords.contains(baseWord) || learnedWords.contains(baseWord + "e")) {
                return true;
            }
        }
        if (word.endsWith("ly") && word.length() > 3 && learnedWords.contains(word.substring(0, word.length() - 2))) {
            return true;
        }
        if (word.endsWith("ies") && word.length() > 4 && learnedWords.contains(word.substring(0, word.length() - 3) + "y")) {
            return true;
        }
        return false;
    }

    private String getEbookText(UUID id) {
        Optional<ExtractedEbookText> byEbook = extractedEbookTextRepository.findById(id);
        if (byEbook.isPresent()) {
            ExtractedEbookText ebook = byEbook.get();
            if (ebook.getContent() == null || ebook.getContent().isBlank()) {
                log.info("Ebook content in DB is empty for id {}. Attempting re-extraction...", id);
                String content = extractedEbookTextService.getEbookText(ebook.getEbookName());
                if (content != null && !content.isBlank()) {
                    ebook.setContent(content);
                    extractedEbookTextRepository.save(ebook);
                }
                return content != null ? content : "";
            }
            return ebook.getContent();
        } else {
            throw new RuntimeException("Could not find ebook by id: " + id);
        }
    }

    private String getEbookText(String englishSubtitlesPath) {
        return ExtractedEbookTextService.extractText(pathToFileStorage + File.separator + englishSubtitlesPath);
    }
}
