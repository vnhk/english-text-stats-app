package com.bervan.englishtextstats.api;

import com.bervan.common.service.AuthService;
import com.bervan.englishtextstats.ExtractedEbookText;
import com.bervan.englishtextstats.Word;
import com.bervan.englishtextstats.service.ExtractedEbookTextRepository;
import com.bervan.englishtextstats.service.ExtractedEbookTextService;
import com.bervan.englishtextstats.service.TextNotKnownWordsService;
import com.bervan.englishtextstats.service.WordService;
import com.bervan.languageapp.service.AddAsFlashcardService;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ebook")
@RolesAllowed("USER")
public class EbookRestController {

    private final ExtractedEbookTextService extractedEbookTextService;
    private final ExtractedEbookTextRepository extractedEbookTextRepository;
    private final WordService wordService;
    private final TextNotKnownWordsService textNotKnownWordsService;
    private final AddAsFlashcardService addAsFlashcardService;

    public EbookRestController(ExtractedEbookTextService extractedEbookTextService,
                               ExtractedEbookTextRepository extractedEbookTextRepository,
                               WordService wordService,
                               TextNotKnownWordsService textNotKnownWordsService,
                               AddAsFlashcardService addAsFlashcardService) {
        this.extractedEbookTextService = extractedEbookTextService;
        this.extractedEbookTextRepository = extractedEbookTextRepository;
        this.wordService = wordService;
        this.textNotKnownWordsService = textNotKnownWordsService;
        this.addAsFlashcardService = addAsFlashcardService;
    }

    @GetMapping("/ebooks")
    public ResponseEntity<List<EbookDto>> listEbooks() {
        UUID userId = AuthService.getLoggedUserId();
        List<EbookDto> dtos = extractedEbookTextRepository.findAllAvailable(userId).stream()
                .map(s -> new EbookDto(s.getId(), s.getName(), null, null))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/ebooks")
    public ResponseEntity<EbookDto> createEbook(@RequestBody CreateEbookRequest req) {
        ExtractedEbookText ebook = new ExtractedEbookText();
        ebook.setId(UUID.randomUUID());
        ebook.setEbookName(req.getEbookName());
        ExtractedEbookText saved = extractedEbookTextService.save(ebook);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new EbookDto(saved.getId(), saved.getEbookName(), saved.getCreationDate(), saved.getModificationDate()));
    }

    @DeleteMapping("/ebooks/{id}")
    public ResponseEntity<Void> deleteEbook(@PathVariable UUID id) {
        UUID userId = AuthService.getLoggedUserId();
        boolean owned = extractedEbookTextRepository.findAllAvailable(userId).stream()
                .anyMatch(e -> e.getId().equals(id));
        if (!owned) return ResponseEntity.notFound().build();
        extractedEbookTextRepository.softDeleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/not-learned")
    public ResponseEntity<List<WordDto>> getNotLearnedWords(@RequestParam UUID ebookId) {
        UUID userId = AuthService.getLoggedUserId();
        boolean owned = extractedEbookTextRepository.findAllAvailable(userId).stream()
                .anyMatch(e -> e.getId().equals(ebookId));
        if (!owned) return ResponseEntity.notFound().build();

        List<WordDto> words = wordService.loadNotKnownWords(ebookId).stream()
                .map(w -> new WordDto(w.getValue(), w.getCount()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(words);
    }

    @PostMapping("/mark-learned")
    public ResponseEntity<Void> markAsLearned(@RequestBody WordRequest req) {
        Word word = new Word(req.getWord(), 0L, null);
        wordService.save(word);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/add-flashcard")
    public ResponseEntity<Void> addAsFlashcard(@RequestBody WordRequest req) {
        Word word = new Word(req.getWord(), 0L, null);
        wordService.save(word);
        addAsFlashcardService.addAsFlashcardAsync(word, "EN");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/import-known-words")
    public ResponseEntity<Void> importKnownWords(@RequestParam("file") MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        String content = new String(bytes);
        Set<String> words = Arrays.stream(content.split("[,\n\r]+"))
                .map(String::trim)
                .filter(w -> !w.isEmpty())
                .collect(Collectors.toSet());
        for (String word : words) {
            textNotKnownWordsService.markAsLearned(word);
        }
        return ResponseEntity.ok().build();
    }
}
