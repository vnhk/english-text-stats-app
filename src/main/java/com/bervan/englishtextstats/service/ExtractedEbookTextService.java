package com.bervan.englishtextstats.service;

import com.bervan.common.search.SearchService;
import com.bervan.common.service.BaseService;
import com.bervan.englishtextstats.ExtractedEbookText;
import com.bervan.logging.JsonLogger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ExtractedEbookTextService extends BaseService<UUID, ExtractedEbookText> {
    private final JsonLogger log = JsonLogger.getLogger(getClass(), "english-ebook");

    @Value("${file.service.storage.folder.main}")
    private String pathToFileStorage;

    @Value("${ebook-not-known-words.file-storage-relative-path}")
    private String pathToConfigFolder;

    public ExtractedEbookTextService(ExtractedEbookTextRepository extractedEbookTextRepository, SearchService searchService) {
        super(extractedEbookTextRepository, searchService);
    }

    public static String extractText(String filePath) {
        StringBuilder textContent = new StringBuilder();
        String lowerPath = filePath.toLowerCase();
        if (lowerPath.endsWith(".epub")) {
            try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(filePath))) {
                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    String name = entry.getName().toLowerCase();
                    if (name.endsWith(".xhtml") || name.endsWith(".html") || name.endsWith(".htm") || name.endsWith(".xml")) {
                        byte[] bytes = zipInputStream.readAllBytes();
                        String html = new String(bytes, StandardCharsets.UTF_8);
                        String text = stripHtml(html);
                        textContent.append(text).append("\n");
                    }
                    zipInputStream.closeEntry();
                }
            } catch (IOException e) {
                throw new RuntimeException("Error reading epub: " + filePath, e);
            }
        } else if (lowerPath.endsWith(".pdf")) {
            try (PDDocument document = PDDocument.load(new File(filePath))) {
                PDFTextStripper pdfStripper = new PDFTextStripper();
                textContent.append(pdfStripper.getText(document));
            } catch (IOException e) {
                throw new RuntimeException("Error reading pdf: " + filePath, e);
            }
        } else if (lowerPath.endsWith(".vtt")) {
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3} --> \\d{2}:\\d{2}:\\d{2}\\.\\d{3}")) {
                        textContent.append(line).append(System.lineSeparator());
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Error reading vtt: " + filePath, e);
            }
        } else if (lowerPath.endsWith(".srt")) {
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.matches("\\d+") && !line.matches("\\d{2}:\\d{2}:\\d{2},\\d{3} --> \\d{2}:\\d{2}:\\d{2},\\d{3}")) {
                        textContent.append(line).append(System.lineSeparator());
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Error reading srt: " + filePath, e);
            }
        } else {
            throw new RuntimeException("File extension is unsupported: " + filePath);
        }

        return textContent.toString();
    }

    private static String stripHtml(String html) {
        if (html == null || html.isBlank()) return "";
        String bodyContent = html;
        int bodyStart = html.toLowerCase().indexOf("<body");
        if (bodyStart != -1) {
            int bodyTagEnd = html.indexOf(">", bodyStart);
            int bodyEnd = html.toLowerCase().lastIndexOf("</body>");
            if (bodyTagEnd != -1 && bodyEnd > bodyTagEnd) {
                bodyContent = html.substring(bodyTagEnd + 1, bodyEnd);
            } else if (bodyTagEnd != -1) {
                bodyContent = html.substring(bodyTagEnd + 1);
            }
        }

        // Strip scripts and styles
        bodyContent = bodyContent.replaceAll("(?is)<script.*?</script>", " ");
        bodyContent = bodyContent.replaceAll("(?is)<style.*?</style>", " ");

        // Replace HTML tags with space so words don't get merged
        String text = bodyContent.replaceAll("<[^>]+>", " ");

        // Decode common HTML entities
        text = text.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&#8217;", "'")
                .replace("&#8216;", "'")
                .replace("&#8220;", "\"")
                .replace("&#8221;", "\"")
                .replace("&rsquo;", "'")
                .replace("&lsquo;", "'")
                .replace("&rdquo;", "\"")
                .replace("&ldquo;", "\"");
        return text;
    }

    @Override
    public ExtractedEbookText save(ExtractedEbookText data) {
        String ebookName = data.getEbookName();
        if (data.getContent() == null || data.getContent().isBlank()) {
            data.setContent(getEbookText(ebookName));
        }
        return super.save(data);
    }

    public String getEbookText(String filename) {
        if (filename == null || filename.isBlank()) return "";

        List<File> candidates = new ArrayList<>();
        candidates.add(new File(filename));

        String relFolder = pathToConfigFolder != null ? pathToConfigFolder.replaceFirst("^[/\\\\]+", "") : "";
        if (pathToFileStorage != null) {
            candidates.add(new File(pathToFileStorage + File.separator + relFolder + File.separator + filename));
            candidates.add(new File(pathToFileStorage + File.separator + filename));
            if (pathToConfigFolder != null) {
                candidates.add(new File(pathToFileStorage + pathToConfigFolder + File.separator + filename));
            }
        }

        candidates.add(new File("files/epubs-config" + File.separator + filename));
        candidates.add(new File("files" + File.separator + filename));
        candidates.add(new File("epubs" + File.separator + filename));
        candidates.add(new File("english-text-stats-app/epubs" + File.separator + filename));
        candidates.add(new File("file-storage-app/epubs" + File.separator + filename));

        for (File candidate : candidates) {
            if (candidate.exists() && candidate.isFile()) {
                log.info("Found ebook file at: {}", candidate.getAbsolutePath());
                return extractText(candidate.getAbsolutePath());
            }
        }

        String fallbackPath = (pathToFileStorage != null ? pathToFileStorage : "")
                + File.separator + relFolder + File.separator + filename;
        log.warn("Ebook file not found in candidates, trying fallback: {}", fallbackPath);
        return extractText(fallbackPath);
    }
}
