# English Text Stats App - Project Notes

> **IMPORTANT**: Keep this file updated when making significant changes to the codebase. This file serves as persistent memory between Claude Code sessions.

## Overview
Helps English language learners analyze texts (ebooks, PDFs, subtitles) and track unknown vocabulary. Users upload files, identify words they haven't learned, and optionally add them as flashcards to the learning-language-app.

## Key Architecture

### Entities

#### KnownWord
- `id: UUID`, `value: String` (max 100), `creationDate`, `modificationDate`, `deleted: Boolean`
- Tracks words the user has marked as learned

#### ExtractedEbookText
- `id: UUID`, `ebookName: String` (max 255), `content: String` (max 500MB LONGTEXT)
- `creationDate`, `modificationDate`, `deleted: Boolean`
- Stores extracted text from uploaded files

#### Word (in-memory, not persisted)
- `uuid`, `name`, `count: Long`, `translation`

### Services

#### TextNotKnownWordsService (Core)
- `getNotLearnedWords(int howMany, UUID ebookId)` - analyzes text, returns top N unknown words
- `markAsLearned(String word)` - adds word + morphological variants (plural -s, -ed, -ing, -ly, -ies)
- `loadIntoMemory()` - caches all known words per user in ConcurrentHashMap
- Smart word matching filters numbers and handles morphological variations
- Uses parallel streams for performance

#### ExtractedEbookTextService
- Supported formats: `.epub` (ZIP/xhtml), `.pdf` (PDFBox), `.vtt`, `.srt`
- Files stored in `${file.service.storage.folder.main}`

### Views

#### AbstractEbooksView
- Route: `english-ebook-words/available-ebooks`
- Table of all uploaded ebooks

#### AbstractNotLearnedWordsView / AbstractNotLearnedWordsBaseView
- Route: `english-ebook-words/not-learned-yet`
- ComboBox to select ebook, grid of unknown words with frequency
- Keyboard shortcuts: `[` = mark learned, `]` = add as flashcard
- Auto-advances to next word after action
- CSV import to bulk-mark known words

## Configuration
- `src/main/resources/autoconfig/ExtractedEbookText.yml` - form config (ebookName field)
- `src/main/resources/autoconfig/KnownWord.yml` - form config (value field)
- Pre-loaded word lists: `knownWords*.csv` in root

## Dependencies
- Apache PDFBox 2.0.29 (PDF parsing)
- Integrates with `learning-language-app` for flashcard creation (`AddAsFlashcardService`)

## Important Notes
1. Known words cached per user in ConcurrentHashMap for performance
2. Morphological matching: recognizes word variations to avoid re-learning same word
3. Soft deletes on all entities
4. Multi-tenancy via `BervanOwnedBaseEntity`
