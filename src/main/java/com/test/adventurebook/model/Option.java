package com.test.adventurebook.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Option {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;
    @JsonProperty("id")
    @Column(name = "story_option_id")
    private Integer optionId;

    private String description;

    @JsonProperty("gotoId")
    private Integer gotoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    @JsonIgnore
    private Section section;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "option_id")
    private List<Consequence> consequence = new ArrayList<>();

    @JsonProperty("consequence")
    public void setConsequence(Consequence c) {
        if (c != null) {
            this.consequence.add(c);
        }
    }

    @JsonProperty("consequence")
    public List<Consequence> getConsequences() {
        return consequence;
    }
}