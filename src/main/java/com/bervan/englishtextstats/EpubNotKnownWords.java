package com.bervan.englishtextstats;

import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class EpubNotKnownWords {

    private final Set<String> inMemoryWords = new HashSet<>();

    public void loadIntoMemory() {
        try {
            Set<String> a1_a2_words_dynamic = loadWordsFromCSV("./english-text-stats-app/knownWords_large.csv");
            a1_a2_words_dynamic.addAll(loadWordsFromCSV("./english-text-stats-app/knownWords1.csv"));
            a1_a2_words_dynamic.addAll(loadWordsFromCSV("./english-text-stats-app/knownWords2.csv"));
            inMemoryWords.addAll(a1_a2_words_dynamic);
        } catch (Exception e) {
            throw new RuntimeException("Could not load learned words.");
        }
    }

    public void markAsLearned(String word) {
        if (word == null || word.isBlank()) {
            return;
        }

        File file = new File("./english-text-stats-app/knownWords2.csv");
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

    public List<Word> getNotLearnedWords(int howMany) {
        if (inMemoryWords.isEmpty()) {
            loadIntoMemory();
        }
        List<Word> resultComplete = new ArrayList<>();

        try {
            // Extract text from EPUB file
            String extractedText = getEpubText("./english-text-stats-app/epubs/freud-dream-psychology.epub");

            // Preprocess text: remove non-alphabetic characters and convert to lower case
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
            translations.put("people", "ludzie");
            translations.put("history", "historia");
            translations.put("human", "człowiek");
            translations.put("society", "społeczeństwo");
            translations.put("development", "rozwój");
            translations.put("political", "polityczny");
            translations.put("system", "system");
            translations.put("economy", "gospodarka");

            // Prepare and print results
            for (Map.Entry<String, Long> entry : sortedWordsComplete) {
                String translation = translations.getOrDefault(entry.getKey(), "");
                resultComplete.add(new Word(entry.getKey(), entry.getValue(), translation));
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not extract english words.");
        }

        List<Word> resultReduced = new ArrayList<>();

        for (int i = 0; i < Math.min(howMany, resultComplete.size()); i++) {
            resultReduced.add(resultComplete.get(i));
        }
        System.out.println("All: " + resultComplete.size());

        return resultReduced;
    }

    private static Set<String> loadWordsFromCSV(String filePath) {
        try {
            return Files.lines(Paths.get(filePath))
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
            // Wyszukiwanie początku <body> i końca </body>
            if (line.contains("<body")) {
                insideBody = true;
            }
            if (insideBody) {
                // Usuwanie prostych znaczników HTML
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
}
