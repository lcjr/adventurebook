package com.test.adventurebook.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "consequence_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Consequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;
    private String type;

    @Column(name = "consequence_value")
    private String value;
    private String text;

}