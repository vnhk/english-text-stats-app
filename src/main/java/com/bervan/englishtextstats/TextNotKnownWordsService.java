package com.bervan.englishtextstats;

import com.bervan.common.service.FileBasedConfigUtils;
import com.bervan.core.model.BervanLogger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TextNotKnownWordsService {
    private final String pathToFileStorage;
    private final String appConfigFolder;
    private final BervanLogger logger;
    private final String pathToConfigFolder;
    private final Set<String> inMemoryWords = ConcurrentHashMap.newKeySet();
    private String extractedText = null;
    private String actualEbook = null;
    private String filePath;

    public TextNotKnownWordsService(String pathToFileStorage, String appConfigFolder, BervanLogger logger) {
        this.pathToFileStorage = pathToFileStorage;
        this.appConfigFolder = appConfigFolder;
        pathToConfigFolder = pathToFileStorage + appConfigFolder + File.separator;

        this.logger = logger;
    }

    public void loadIntoMemory() {
        try {
            Set<String> knownWords = new HashSet<>();
            FileBasedConfigUtils.loadFilesInStorageFolder(pathToFileStorage, appConfigFolder)
                    .stream().filter(File::isFile).filter(f -> f.getName().endsWith(".csv"))
                    .forEach(f -> {
                        knownWords.addAll(loadWordsFromCSV(f.toPath()));
                    });
            inMemoryWords.addAll(knownWords);
            logger.info("Loaded " + inMemoryWords.size() + " known words into memory.");
        } catch (Exception e) {
            logger.error("Could not load learned words.", e);
        }
    }

    public void markAsLearned(String word) {
        if (word == null || word.isBlank()) {
            return;
        }

        File file = new File(pathToFileStorage + appConfigFolder + File.separator + "app-knownWords.csv");
        FileWriter writer = null;

        try {
            word = word.trim();
            boolean fileExists = file.exists();
            writer = new FileWriter(file, true);

            if (fileExists) {
                writer.write("," + word);
            } else {
                writer.write(word);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    logger.error("Could not create or append file with known words!");
                }
            }
        }
        inMemoryWords.add(word);
    }

    public Set<Word> getNotLearnedWords(int howMany) {
        if (filePath == null) {
            return new HashSet<>();
        }

        if (inMemoryWords.isEmpty()) {
            loadIntoMemory();
        }

        if (extractedText == null) {
            try {
                extractedText = getEbookText(filePath);
            } catch (Exception e) {
                logger.error("Could not extract English words.", e);
                throw new RuntimeException("Could not extract English words.", e);
            }
        }

        try {

            logger.info("Extracted Ebook text length: " + extractedText.length());

            ConcurrentMap<String, Long> wordCounterComplete = Arrays.stream(extractedText.toLowerCase().split("\\W+"))
                    .parallel()
                    .filter(word -> word.length() > 0)
                    .map(String::trim)
                    .filter(word -> !isLearned(word, inMemoryWords))
                    .collect(Collectors.groupingByConcurrent(Function.identity(), Collectors.counting()));

            List<Map.Entry<String, Long>> sortedWordsComplete = wordCounterComplete.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .toList();

            Set<Word> resultReduced = sortedWordsComplete.stream()
                    .limit(howMany)
                    .map(entry -> new Word(entry.getKey(), entry.getValue(), null))
                    .collect(Collectors.toSet());

            logger.info("All Words Not Learned: " + sortedWordsComplete.size());

            return resultReduced;
        } catch (Exception e) {
            logger.error("Could not extract English words.", e);
            throw new RuntimeException("Could not extract English words.", e);
        }
    }

    private boolean isLearned(String word, Set<String> learnedWords) {
        try {
            Double.parseDouble(word);
            return true;
        } catch (NumberFormatException ignored) {
        }

        word = word.trim();
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

    private Set<String> loadWordsFromCSV(Path filePath) {
        try {
            return Files.lines(filePath)
                    .flatMap(line -> Arrays.stream(line.split(",")))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            logger.error("Could not load csv english words.", e);
        }

        return new HashSet<>();
    }


    private String getEbookText(String filePath) throws IOException {
        logger.info("Loading file: " + filePath);
        StringBuilder textContent = new StringBuilder();

        if (filePath.endsWith(".epub")) {
            try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(filePath))) {
                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    logger.info("Entry name in epub: " + entry.getName());
                    if (entry.getName().endsWith(".xhtml") || entry.getName().endsWith(".html") || entry.getName().endsWith(".htm")) {
                        textContent.append(extractTextFromEntry(zipInputStream));
                    }
                    zipInputStream.closeEntry();
                }
            } catch (IOException e) {
                logger.error(e);
            }
        } else if (filePath.endsWith(".pdf")) {
            try (PDDocument document = PDDocument.load(new File(filePath))) {
                PDFTextStripper pdfStripper = new PDFTextStripper();
                textContent.append(pdfStripper.getText(document));
            } catch (IOException e) {
                logger.error(e);
            }
        } else if (filePath.endsWith(".vtt")) {
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3} --> \\d{2}:\\d{2}:\\d{2}\\.\\d{3}")) {
                        textContent.append(line).append(System.lineSeparator());
                    }
                }
            } catch (IOException e) {
                logger.error(e);
            }
        } else if (filePath.endsWith(".srt")) {
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.matches("\\d+") && !line.matches("\\d{2}:\\d{2}:\\d{2},\\d{3} --> \\d{2}:\\d{2}:\\d{2},\\d{3}")) {
                        textContent.append(line).append(System.lineSeparator());
                    }
                }
            } catch (IOException e) {
                logger.error(e);
            }
        } else {
            throw new RuntimeException("File extension is unsupported!");
        }
        return textContent.toString();
    }

    private String extractTextFromEntry(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        StringBuilder content = new StringBuilder();
        String line;

        boolean insideBody = false;
        while ((line = reader.readLine()) != null) {
            if (line.contains("<body")) {
                insideBody = true;
            }
            if (insideBody) {
                content.append(line.replaceAll("<[^>]+>", "")).append("\n");
            }
            if (line.contains("</body>")) {
                insideBody = false;
            }
        }
        return content.toString();
    }

    public String getActualEbook() {
        return actualEbook;
    }

    public void setActualEbookAndUpdatePath(String actualEbook) {
        this.actualEbook = actualEbook;
        extractedText = null;
        filePath = pathToConfigFolder + actualEbook;
    }

    public void buildPath(String path) {
        if (!path.startsWith(File.separator)) {
            path = File.separator + path;
        }
        filePath = pathToFileStorage + path;
    }
}
