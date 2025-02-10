package com.bervan.englishtextstats.view;

import com.bervan.common.AbstractTableView;
import com.bervan.common.search.SearchRequest;
import com.bervan.common.service.AuthService;
import com.bervan.core.model.BervanLogger;
import com.bervan.englishtextstats.EnglishTextLayout;
import com.bervan.englishtextstats.Word;
import com.bervan.englishtextstats.service.ExtractedEbookTextRepository;
import com.bervan.englishtextstats.service.TextNotKnownWordsService;
import com.bervan.englishtextstats.service.WordService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractNotLearnedWordsView extends AbstractTableView<UUID, Word> {
    public static final String ROUTE_NAME = "english-ebook-words/not-learned-yet";
    protected HorizontalLayout dialogButtonsLayout;
    protected TextNotKnownWordsService textNotKnownWordsService;
    protected ExtractedEbookTextRepository extractedEbookTextRepository;
    protected UUID selectedEbookId;
    @Value("${file.service.storage.folder}")
    private String pathToFileStorage;
    @Value("${global-tmp-dir.file-storage-relative-path}")
    private String globalTmpDir;

    public AbstractNotLearnedWordsView(WordService service, ExtractedEbookTextRepository extractedEbookTextRepository, TextNotKnownWordsService textNotKnownWordsService, BervanLogger log) {
        super(new EnglishTextLayout(ROUTE_NAME), service, log, Word.class);
        this.extractedEbookTextRepository = extractedEbookTextRepository;
        this.textNotKnownWordsService = textNotKnownWordsService;
        renderCommonComponents();

        contentLayout.remove(addButton);
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
            log.error(e);
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
    protected Grid<Word> getGrid() {
        Grid<Word> grid = new Grid<>(Word.class, false);
        grid.addColumn(new ComponentRenderer<>(word -> formatTextComponent(word.getTableFilterableColumnValue())))
                .setHeader("Name").setKey("name").setResizable(true);
        grid.addColumn(new ComponentRenderer<>(word -> formatTextComponent(String.valueOf(word.getCount()))))
                .setHeader("Count").setKey("count").setResizable(true)
                .setSortable(true).setComparator(Comparator.comparing(Word::getCount));

        grid.getElement().getStyle().set("--lumo-size-m", 100 + "px");

        removeUnSortedState(grid, 1);

        return grid;
    }

    @Override
    protected void buildOnColumnClickDialogContent(Dialog dialog, VerticalLayout dialogLayout, HorizontalLayout headerLayout, String clickedColumn, Word item) {
        dialogButtonsLayout = new HorizontalLayout();

        TextArea field = new TextArea(clickedColumn);
        field.setWidth("100%");

        field.setValue(item.getTableFilterableColumnValue());

        Button saveButton = new Button("Mark as learned.");
        saveButton.addClassName("option-button");

        saveButton.addClickListener(e -> {
            data.remove(item);
            grid.getDataProvider().refreshAll();
            service.save(item);
            dialog.close();
        });

        dialogButtonsLayout.add(saveButton);

        dialogLayout.add(headerLayout, field, dialogButtonsLayout);
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

    @Override
    protected void newItemButtonClick() {
        throw new RuntimeException("Open dialog is invalid");
    }

    @Override
    protected void buildNewItemDialogContent(Dialog dialog, VerticalLayout dialogLayout, HorizontalLayout headerLayout) {
        throw new RuntimeException("Open dialog is invalid");
    }
}