package com.test.adventurebook.model;

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
@Table(name = "player_states")
public class PlayerState {
    @Id
    private String sessionId;

    private String bookId;
    private Integer currentSectionId;
    private int hp = 10;
    private boolean isGameOver = false;

    @ElementCollection
    @CollectionTable(name = "player_logs", joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "log_entry")
    @OrderColumn
    private List<String> log = new ArrayList<>();

    public void addToLog(String message) {
        this.log.add(message);
    }


}
