# english-text-stats-app

Helps English learners analyze texts (ebooks, PDFs, subtitles) and track unknown vocabulary. Upload files, identify unfamiliar words by frequency, and optionally push them as flashcards to `learning-language-app`.

## Features

- **Multi-format upload**: `.epub`, `.pdf`, `.vtt`, `.srt`
- **Word frequency analysis**: Ranks unknown words by occurrence count
- **Smart matching**: Recognizes morphological variants (plurals, past tense, gerunds, adverbs) to avoid re-learning the same root word
- **Flashcard integration**: Push unknown words directly to `learning-language-app`
- **Bulk import**: CSV import to mark many words as known at once
- **Keyboard shortcuts**: `[` = mark learned, `]` = add as flashcard (auto-advances to next word)

## Key Entities

| Entity | Description |
|--------|-------------|
| `KnownWord` | A word the user has marked as learned |
| `ExtractedEbookText` | Extracted text content from an uploaded file |

## Routes

| Path | Purpose |
|------|---------|
| `english-ebook-words/available-ebooks` | Manage uploaded ebooks |
| `english-ebook-words/not-learned-yet` | Browse and act on unknown words |

## Architecture

- `TextNotKnownWordsService` — analyzes text, returns top N unknown words; caches known words per user in `ConcurrentHashMap`
- `ExtractedEbookTextService` — parses EPUB (ZIP/xhtml), PDF (PDFBox), VTT, SRT
- `AddAsFlashcardService` — integration point with `learning-language-app`

## Dependencies

- Apache PDFBox 2.0.29 (PDF parsing)
- `learning-language-app` (flashcard creation)

## Build

```bash
mvn clean install -DskipTests
```

Part of the `my-tools` multi-module Maven project. Requires `common` to be built first.
