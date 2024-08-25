package com.bervan.englishtextstats;

import com.bervan.common.service.FileBasedConfigUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class EpubNotKnownWordsService {
    @Value("${file.service.storage.folder}")
    private String pathToFileStorage;

    @Value("${ebook-not-known-words.path-in-file-storage}")
    private String appConfigFolder;

    private final Set<String> inMemoryWords = new HashSet<>();
    private String actualEbook = null;

    public void loadIntoMemory() {
        try {
            Set<String> knownWords = new HashSet<>();
            FileBasedConfigUtils.loadFilesInStorageFolder(pathToFileStorage, appConfigFolder)
                    .stream().filter(File::isFile).filter(f -> f.getName().endsWith(".csv"))
                    .forEach(f -> {
                        knownWords.addAll(loadWordsFromCSV(f.toPath()));
                    });
            inMemoryWords.addAll(knownWords);
        } catch (Exception e) {
            throw new RuntimeException("Could not load learned words.");
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
                    e.printStackTrace();
                }
            }
        }
        inMemoryWords.add(word);
    }

    public Set<Word> getNotLearnedWords(int howMany) {
        if (actualEbook == null) {
            return new HashSet<>();
        }

        if (inMemoryWords.isEmpty()) {
            loadIntoMemory();
        }

        List<Word> resultComplete = new ArrayList<>();

        try {
            String extractedText = getEpubText(pathToFileStorage + File.separator + appConfigFolder + File.separator + actualEbook);

            List<String> words = filterWords(
                    Arrays.stream(extractedText.toLowerCase().split("\\W+"))
                            .filter(word -> word.length() >= 3)
                            .collect(Collectors.toList()),
                    inMemoryWords
            );

            // Count word frequencies
            Map<String, Long> wordCounterComplete = words.stream()
                    .collect(Collectors.groupingBy(w -> w, Collectors.counting()));

            // Get words that appear more than 3 times
            Map<String, Long> commonWordsComplete = wordCounterComplete.entrySet().stream()
                    .filter(entry -> entry.getValue() > 3)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            // Sort words by frequency
            List<Map.Entry<String, Long>> sortedWordsComplete = commonWordsComplete.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .toList();

            // Translate words to Polish
            Map<String, String> translations = new HashMap<>();

            // Prepare and print results
            for (Map.Entry<String, Long> entry : sortedWordsComplete) {
                String translation = translations.getOrDefault(entry.getKey(), "");
                resultComplete.add(new Word(entry.getKey(), entry.getValue(), translation));
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not extract english words.");
        }

        Set<Word> resultReduced = new HashSet<>();

        for (int i = 0; i < Math.min(howMany, resultComplete.size()); i++) {
            resultReduced.add(resultComplete.get(i));
        }
        System.out.println("All: " + resultComplete.size());

        return resultReduced;
    }

    private static Set<String> loadWordsFromCSV(Path filePath) {
        try {
            return Files.lines(filePath)
                    .flatMap(line -> Arrays.stream(line.split(",")))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new HashSet<>();
    }


    private String getEpubText(String filePath) {
        StringBuilder textContent = new StringBuilder();

        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(filePath))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().endsWith(".xhtml") || entry.getName().endsWith(".html")) {
                    textContent.append(extractTextFromEntry(zipInputStream));
                }
                zipInputStream.closeEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
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

    private List<String> filterWords(List<String> words, Set<String> learnedWords) {
        List<String> filteredWords = new ArrayList<>();
        for (String word : words) {
            word = word.trim();
            if ((word.endsWith("s") && learnedWords.contains(word.substring(0, word.length() - 1))) ||
                    (word.endsWith("ed") && learnedWords.contains(word.substring(0, word.length() - 1))) ||
                    (word.endsWith("er") && learnedWords.contains(word.substring(0, word.length() - 1))) ||
                    (word.endsWith("ing") && learnedWords.contains(word.substring(0, word.length() - 3))) ||
                    (word.endsWith("er") && learnedWords.contains(word.substring(0, word.length() - 2))) ||
                    (word.endsWith("ed") && learnedWords.contains(word.substring(0, word.length() - 2))) ||
                    (word.endsWith("ly") && learnedWords.contains(word.substring(0, word.length() - 2))) ||
                    (word.endsWith("ies") && learnedWords.contains(word.substring(0, word.length() - 3) + "y")) ||
                    word.equals("true")) {
                continue;
            }
            if (!learnedWords.contains(word)) {
                filteredWords.add(word);
            }
        }
        return filteredWords;
    }

    public String getActualEpub() {
        return actualEbook;
    }

    public void setActualEpub(String actualEbook) {
        this.actualEbook = actualEbook;
    }
}
