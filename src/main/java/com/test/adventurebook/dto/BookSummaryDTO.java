package com.test.adventurebook.dto;

import com.test.adventurebook.model.Difficulty;
import java.util.Set;

//Used record instead a standard DTO
public record BookSummaryDTO(
        String id,
        String title,
        String author,
        Set<String> categories,
        Difficulty difficulty
) {}
