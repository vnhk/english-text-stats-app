package com.bervan.englishtextstats.service;

import com.bervan.common.search.SearchService;
import com.bervan.common.service.BaseService;
import com.bervan.englishtextstats.ExtractedEbookText;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Slf4j
public class ExtractedEbookTextService extends BaseService<UUID, ExtractedEbookText> {

    @Value("${file.service.storage.folder}")
    private String pathToFileStorage;

    @Value("${ebook-not-known-words.file-storage-relative-path}")
    private String pathToConfigFolder;


    public ExtractedEbookTextService(ExtractedEbookTextRepository extractedEbookTextRepository, SearchService searchService) {
        super(extractedEbookTextRepository, searchService);
    }

    @Override
    public ExtractedEbookText save(ExtractedEbookText data) {
        String ebookName = data.getEbookName();

        String path = ebookName;
        data.setContent(getEbookText(path));

        return super.save(data);
    }

    private String getEbookText(String filename) {
        String filePath = pathToFileStorage + pathToConfigFolder + File.separator + filename;
        log.info("Loading file: " + filePath);
        return extractText(filePath);
    }


    public static String extractText(String filePath) {
        StringBuilder textContent = new StringBuilder();
        if (filePath.endsWith(".epub")) {
            try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(filePath))) {
                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    if (entry.getName().endsWith(".xhtml") || entry.getName().endsWith(".html") || entry.getName().endsWith(".htm")) {
                        textContent.append(extractTextFromEntry(zipInputStream));
                    }
                    zipInputStream.closeEntry();
                }
            } catch (IOException e) {
                throw new RuntimeException("Error!", e);
            }
        } else if (filePath.endsWith(".pdf")) {
            try (PDDocument document = PDDocument.load(new File(filePath))) {
                PDFTextStripper pdfStripper = new PDFTextStripper();
                textContent.append(pdfStripper.getText(document));
            } catch (IOException e) {
                throw new RuntimeException("Error!", e);
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
                throw new RuntimeException("Error!", e);
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
                throw new RuntimeException("Error!", e);
            }
        } else {
            throw new RuntimeException("File extension is unsupported!");
        }

        return textContent.toString();
    }


    private static String extractTextFromEntry(InputStream inputStream) throws IOException {
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
}
