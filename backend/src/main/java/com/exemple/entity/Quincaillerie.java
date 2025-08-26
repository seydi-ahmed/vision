package com.exemple.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "quincailleries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quincaillerie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    private String adresse;

    // Propriétaire
    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    // Gérant (un seul)
    @OneToOne
    @JoinColumn(name = "manager_id")
    private User manager;

    // Produits de la quincaillerie
    @OneToMany(mappedBy = "quincaillerie", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Produit> produits;
}
