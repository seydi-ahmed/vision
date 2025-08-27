package com.exemple.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuincaillerieRequest {
    private String nom;
    private String adresse;
    private Long proprietaireId;
    // getters/setters
}
