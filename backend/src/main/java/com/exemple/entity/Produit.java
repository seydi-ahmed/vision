package com.exemple.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "produits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private Double prix;
    private Integer stock;

    @ManyToOne
    @JoinColumn(name = "quincaillerie_id", nullable = false)
    private Quincaillerie quincaillerie;
}
