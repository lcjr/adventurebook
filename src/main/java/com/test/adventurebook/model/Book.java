package com.test.adventurebook.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "books")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Book {
        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private String id;

        private String title;
        private String author;

        @Enumerated(EnumType.STRING)
        private Difficulty difficulty;

        @ElementCollection
        @CollectionTable(name = "book_categories", joinColumns = @JoinColumn(name = "book_id"))
        @Column(name = "category")
        private List<String> categories = new ArrayList<>();

        @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<Section> sections = new ArrayList<>();

}
