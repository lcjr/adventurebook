package com.test.adventurebook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.adventurebook.dto.BookSummaryDTO;
import com.test.adventurebook.model.Book;
import com.test.adventurebook.model.Difficulty;
import com.test.adventurebook.model.PlayerState;
import com.test.adventurebook.model.Section;
import com.test.adventurebook.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api")
@Tag(name = "Adventure Game API", description = "Choose a book and start your adventure!")
public class GameController {

    @Autowired
    private GameService gameService;

    @Operation(summary = "Upload a new book", description = "Upload a JSON file to add a new adventure to the library.")
    @PostMapping(value = "/books/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BookSummaryDTO> uploadBook(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            Book book = mapper.readValue(file.getInputStream(), Book.class);

            Book savedBook = gameService.createBook(book);

            BookSummaryDTO dto = new BookSummaryDTO(
                    savedBook.getId(),
                    savedBook.getTitle(),
                    savedBook.getAuthor(),
                    new HashSet<>(savedBook.getCategories()),
                    savedBook.getDifficulty()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(dto);

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid JSON format: "  + e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @Operation(summary = "List all books", description = "Search by title, author, category, or difficulty.")
    @GetMapping("/books")
    public List<BookSummaryDTO> getAllBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Difficulty difficulty) {


        List<Book> books = gameService.findBooks(title, author, category, difficulty);

        return books.stream()
                .map(b -> new BookSummaryDTO(
                        b.getId(),
                        b.getTitle(),
                        b.getAuthor(),
                        b.getCategories() != null ? new HashSet<>(b.getCategories()) : Set.of(),
                        b.getDifficulty()
                ))
                .toList();

    }

    @Operation(summary = "Get book details", description = "Read a book.")
    @GetMapping("/books/{id}")
    public Book getBook(@PathVariable String id) {
        return gameService.getBook(id);
    }

    @GetMapping("/books/{bookId}/sections/{sectionId}")
    public Section previewSection(@PathVariable String bookId, @PathVariable Integer sectionId) {
        Book book = gameService.getBook(bookId);

        return book.getSections().stream()
                .filter(s -> s.getSectionId().equals(sectionId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
    }

    @Operation(summary = "Manage categories", description = "Add or remove a category from a book.")
    @PatchMapping("/books/{id}/categories")
    public ResponseEntity<Void> updateCategories(
            @PathVariable String id,
            @RequestParam String action, // "ADD" or "REMOVE"
            @RequestParam String category) {
        gameService.updateCategories(id, action, category);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Start a new adventure", description = "Initializes a game.")
    @PostMapping("/play/{bookId}/start")
    public PlayerState startNewGame(@PathVariable String bookId) {
        return gameService.startSession(bookId);
    }

    @Operation(summary = "Resume/Get session progress", description = "Resume a session.")
    @GetMapping("/play/session/{sessionId}")
    public PlayerState getSession(@PathVariable String sessionId) {
        return gameService.getPlayerState(sessionId);
    }

    @Operation(summary = "Make a choice", description = "Mke a move and deal with the consequences.")
    @PostMapping("/play/session/{sessionId}/choose")
    public PlayerState makeChoice(
            @PathVariable String sessionId,
            @RequestBody Integer targetSectionId) {
        return gameService.makeChoice(sessionId, targetSectionId);
    }


}
