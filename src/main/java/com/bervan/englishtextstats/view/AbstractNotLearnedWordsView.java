package com.bervan.englishtextstats.view;

import com.bervan.common.config.BervanViewConfig;
import com.bervan.common.search.SearchRequest;
import com.bervan.common.service.AuthService;
import com.bervan.englishtextstats.EnglishTextLayout;
import com.bervan.englishtextstats.Word;
import com.bervan.englishtextstats.service.ExtractedEbookTextRepository;
import com.bervan.englishtextstats.service.TextNotKnownWordsService;
import com.bervan.englishtextstats.service.WordService;
import com.bervan.languageapp.service.AddAsFlashcardService;
import com.bervan.logging.JsonLogger;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractNotLearnedWordsView extends AbstractNotLearnedWordsBaseView {
    public static final String ROUTE_NAME = "english-ebook-words/not-learned-yet";
    protected final ExtractedEbookTextRepository extractedEbookTextRepository;
    protected final TextNotKnownWordsService textNotKnownWordsService;
    private final JsonLogger log = JsonLogger.getLogger(getClass(), "english-ebook");
    protected UUID selectedEbookId;
    @Value("${file.service.storage.folder.main}")
    private String pathToFileStorage;
    @Value("${global-tmp-dir.file-storage-relative-path}")
    private String globalTmpDir;

    public AbstractNotLearnedWordsView(WordService service, ExtractedEbookTextRepository extractedEbookTextRepository, TextNotKnownWordsService textNotKnownWordsService,
                                       AddAsFlashcardService addAsFlashcardService, BervanViewConfig bervanViewConfig) {
        super(service, new EnglishTextLayout(ROUTE_NAME), addAsFlashcardService, "EN", bervanViewConfig);
        this.extractedEbookTextRepository = extractedEbookTextRepository;
        this.textNotKnownWordsService = textNotKnownWordsService;
        renderCommonComponents();

        newItemButton.setVisible(false);
        HorizontalLayout horizontalLayout = new HorizontalLayout(JustifyContentMode.BETWEEN, buildEbookComboBox(), new VerticalLayout(new H4("Upload Known Words:"), buildUploadKnownWords()));
        horizontalLayout.setWidthFull();
        add(horizontalLayout);
    }

    private Upload buildUploadKnownWords() {
        MemoryBuffer buffer = new MemoryBuffer();
        Upload uploadKnownWords = new Upload(buffer);
        uploadKnownWords.setAcceptedFileTypes(".csv");

        uploadKnownWords.addSucceededListener(event -> {
            String fileName = event.getFileName();
            InputStream inputStream = buffer.getInputStream();
            try {
                importData(inputStream, fileName);
                showSuccessNotification("File uploaded successfully: " + fileName);
            } catch (Exception e) {
                showErrorNotification("Failed to upload file: " + fileName);
            }
        });

        uploadKnownWords.addFailedListener(event ->
                showErrorNotification("Upload failed"));
        return uploadKnownWords;
    }

    private void importData(InputStream inputStream, String fileName) throws IOException {
        File file = null;
        try {
            File uploadFolder = new File(pathToFileStorage + globalTmpDir);
            if (!uploadFolder.exists()) {
                uploadFolder.mkdirs();
            }
            file = new File(pathToFileStorage + globalTmpDir + File.separator + fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[10240];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            log.info("CSV Known Words tmp file saved.");
            log.info("CSV Known Words processing:");
            Set<String> dataToImport = Files.lines(file.toPath())
                    .flatMap(line -> Arrays.stream(line.split(",")))
                    .collect(Collectors.toSet());
            log.info("Extracted: " + dataToImport.size());

            for (String knownWord : dataToImport) {
                textNotKnownWordsService.markAsLearned(knownWord);
            }

        } catch (Exception e) {
            log.error("Exception!", e);
        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }

    private ComboBox buildEbookComboBox() {
        ComboBox<ExtractedEbookTextRepository.EbookSummary> ebookComboBox = new ComboBox<>("Select an Ebook");
        ebookComboBox.setWidth("800px");
        List<ExtractedEbookTextRepository.EbookSummary> ebooks = extractedEbookTextRepository.findAllAvailable(AuthService.getLoggedUserId());
        ebookComboBox.setItems(ebooks);

        ebookComboBox.setItemLabelGenerator(ExtractedEbookTextRepository.EbookSummary::getName);

        ebookComboBox.addValueChangeListener(event -> {
            ExtractedEbookTextRepository.EbookSummary selectedEbook = event.getValue();
            if (selectedEbook != null) {
                selectedEbookId = (selectedEbook.getId());
                refreshData();
            } else {
                selectedEbookId = (null);
            }
        });

        return (ebookComboBox);
    }

    @Override
    protected List<Word> loadData() {
        if (selectedEbookId != null) {
            return ((WordService) service).loadNotKnownWords(selectedEbookId);
        }
        return new ArrayList<>();
    }

    @Override
    protected long countAll(SearchRequest request, Collection<Word> collect) {
        return collect.size();
    }
}