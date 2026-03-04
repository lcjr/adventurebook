package com.test.adventurebook.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.adventurebook.model.Book;
import com.test.adventurebook.repository.BookRepository;
import com.test.adventurebook.service.GameService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(GameService service, BookRepository bookRepository) {
        return args -> {
            ObjectMapper mapper = new ObjectMapper();
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:books/*.json");

            for (Resource resource : resources) {
                try {
                    Book book = mapper.readValue(resource.getInputStream(), Book.class);

                    // Check the book already exists
                    if (!bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(book.getTitle(), book.getAuthor()).isEmpty()) {
                        System.out.println("SKIPPING: Book '" + book.getTitle() + "' already exists.");
                    } else {
                        service.createBook(book);
                        System.out.println("LOADED: " + book.getTitle());
                    }
                } catch (Exception e) {
                    System.err.println("FAILED to load " + resource.getFilename() + ": " + e.getMessage());
                }
            }
        };
    }
}
