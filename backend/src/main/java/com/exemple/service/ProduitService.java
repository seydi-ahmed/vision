package com.exemple.service;

import com.exemple.entity.Produit;
import com.exemple.entity.Quincaillerie;
import com.exemple.repository.ProduitRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProduitService {

    private final ProduitRepository produitRepository;

    public ProduitService(ProduitRepository produitRepository) {
        this.produitRepository = produitRepository;
    }

    public List<Produit> getAll() {
        return produitRepository.findAll();
    }

    public List<Produit> getByQuincaillerie(Quincaillerie quincaillerie) {
        return produitRepository.findByQuincaillerie(quincaillerie);
    }

    public Produit getById(Long id) {
        return produitRepository.findById(id).orElseThrow();
    }

    public Produit save(Produit produit) {
        return produitRepository.save(produit);
    }

    public void delete(Long id) {
        produitRepository.deleteById(id);
    }
}
