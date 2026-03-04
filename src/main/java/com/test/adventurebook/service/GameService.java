package com.test.adventurebook.service;

import com.test.adventurebook.exceptions.InvalidBookException;
import com.test.adventurebook.model.*;
import com.test.adventurebook.repository.BookRepository;
import com.test.adventurebook.repository.PlayerStateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class GameService {
    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PlayerStateRepository playerRepository;

    private static final int MAX_HP = 10;

    public List<Book> findBooks(String title, String author, String category, Difficulty difficulty) {
        return bookRepository.findAll().stream()
                .filter(b -> title == null || b.getTitle().toLowerCase().contains(title.toLowerCase()))
                .filter(b -> author == null || b.getAuthor().toLowerCase().contains(author.toLowerCase()))
                .filter(b -> category == null || b.getCategories().contains(category.toUpperCase()))
                .filter(b -> difficulty == null || b.getDifficulty() == difficulty)
                .toList();
    }

    public Book getBook(String id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
    }

    @Transactional
    public void updateCategories(String id, String action, String category) {
        Book book = getBook(id);
        if ("ADD".equalsIgnoreCase(action)) {
            book.getCategories().add(category.toUpperCase());
        } else {
            book.getCategories().remove(category.toUpperCase());
        }
        bookRepository.save(book);
    }

    @Transactional
    public Book createBook(Book book) {
        validateBook(book);

        if (book.getSections() != null) {
            for (Section section : book.getSections()) {
                section.setBook(book); // Set the back-reference
                if (section.getOptions() != null) {
                    for (Option option : section.getOptions()) {
                        option.setSection(section); // Set the back-reference
                    }
                }
            }
        }
        return bookRepository.save(book);
    }

    private void validateBook(Book book) {
        List<Section> sections = book.getSections();

        long startCount = sections.stream()
                .filter(s -> s.getType() != null && s.getType().name().equalsIgnoreCase("BEGIN"))
                .count();
        if (startCount != 1) throw new InvalidBookException("Book must have exactly one BEGIN section.");

        boolean hasEnd = sections.stream()
                .anyMatch(s -> s.getType() != null && s.getType().name().equalsIgnoreCase("END"));
        if (!hasEnd) throw new InvalidBookException("Book has no ending.");

        Set<Integer> validIds = new HashSet<>();
        sections.forEach(s -> validIds.add(s.getSectionId()));

        for (Section s : sections) {
            boolean isEnd = s.getType() != null && s.getType().name().equalsIgnoreCase("END");
            if (!isEnd) {
                if (s.getOptions() == null || s.getOptions().isEmpty()) {
                    throw new InvalidBookException("Section " + s.getSectionId() + " has no options.");
                }
                for (Option o : s.getOptions()) {
                    if (!validIds.contains(o.getGotoId())) {
                        throw new InvalidBookException("Invalid next section id: " + o.getGotoId());
                    }
                }
            }
        }
    }

    @Transactional
    public PlayerState startSession(String bookId) {
        Book book = getBook(bookId);
        Section start = book.getSections().stream()
                .filter(s -> "BEGIN".equalsIgnoreCase(s.getType().name()))
                .findFirst().get();

        PlayerState state = new PlayerState();
        state.setSessionId(UUID.randomUUID().toString());
        state.setBookId(bookId);
        state.setCurrentSectionId(start.getSectionId());
        state.setHp(10); // Default health
        state.getLog().add("Started: " + book.getTitle());

        return playerRepository.save(state);
    }

    public PlayerState getPlayerState(String sessionId) {
        return playerRepository.findById(sessionId)
                .map(state -> {
                    return state;
                })
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Session " + sessionId + " not found or has expired."
                ));
    }

    @Transactional
    public PlayerState makeChoice(String sessionId, Integer targetId) {

        PlayerState state = playerRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (state.isGameOver()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Game is over.");
        }

        Book book = getBook(state.getBookId());
        List<Section> sections = book.getSections();

        Section current = sections.stream()
                .filter(s -> s.getSectionId().equals(state.getCurrentSectionId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Current section not found in book"));

        Option chosenOption = current.getOptions().stream()
                .filter(o -> o.getGotoId().equals(targetId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid choice"));


        System.out.println("Checking consequences for option: " + chosenOption.getDescription());
        if (chosenOption.getConsequences() == null || chosenOption.getConsequences().isEmpty()) {
            System.out.println("DEBUG: No consequences found for this option!");
        } else {
            for (Consequence c : chosenOption.getConsequences()) {
                System.out.println("DEBUG: Found consequence type: " + c.getType() + " with value: " + c.getValue());
                applyConsequence(state, c);
            }
        }

        state.setCurrentSectionId(targetId);

        Section nextSection = sections.stream()
                .filter(s -> s.getSectionId().equals(targetId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Next section " + targetId + " not found"));

        updateGameStatus(state, nextSection);

        return playerRepository.save(state);
    }

    private void applyConsequence(PlayerState state, Consequence c) {
        if (c == null || c.getType() == null) return;

        try {
            ConsequenceType type = ConsequenceType.valueOf(c.getType().trim().toUpperCase());
            int amount = (c.getValue() != null && !c.getValue().isEmpty())
                    ? Integer.parseInt(c.getValue().trim()) : 0;

            int oldHp = state.getHp();

            switch (type) {
                case LOSE_HEALTH -> {
                    state.setHp(Math.max(0, oldHp - amount));
                    state.getLog().add(String.format("CONSEQUENCE: %s (HP -%d)", c.getText(), amount));
                }
                case GAIN_HEALTH -> {
                    int newHp = Math.min(MAX_HP, oldHp + amount);
                    state.setHp(newHp);
                    state.getLog().add(String.format("CONSEQUENCE: %s (HP +%d)", c.getText(), (newHp - oldHp)));
                }
                case INSTANT_DEATH -> {
                    state.setHp(0);
                    state.setGameOver(true);
                    state.getLog().add("FATAL CONSEQUENCE: " + c.getText());
                }
            }
            if (state.getHp() <= 0) {
                state.setGameOver(true);
            }

        } catch (IllegalArgumentException e) {
            System.out.println("WARN: Unknown consequence type: " + c.getType());
        } catch (Exception e) {
            System.out.println("ERROR: Failed to apply consequence: " + e.getMessage());
        }
    }

    private void updateGameStatus(PlayerState state, Section nextSection) {
        if (state.getHp() <= 0) {
            state.setGameOver(true);
            state.getLog().add("You have died. Game Over.");
        } else if (nextSection.getType() != null && "END".equalsIgnoreCase(nextSection.getType().name())) {
            state.setGameOver(true);
            state.getLog().add("Adventure Completed!");
        }
    }
}
