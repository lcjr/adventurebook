package com.test.adventurebook.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Difficulty {
        @JsonProperty("EASY") EASY,
        @JsonProperty("MEDIUM") MEDIUM,
        @JsonProperty("HARD") HARD
}
