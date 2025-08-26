package com.exemple.repository;

import com.exemple.entity.Produit;
import com.exemple.entity.Quincaillerie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProduitRepository extends JpaRepository<Produit, Long> {
    List<Produit> findByQuincaillerie(Quincaillerie quincaillerie);
}
