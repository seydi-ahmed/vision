package com.exemple.controller;

import com.exemple.entity.Produit;
import com.exemple.entity.Quincaillerie;
import com.exemple.service.ProduitService;
import com.exemple.service.QuincaillerieService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produits")
public class ProduitController {

    private final ProduitService produitService;
    private final QuincaillerieService quincaillerieService;

    public ProduitController(ProduitService produitService, QuincaillerieService quincaillerieService) {
        this.produitService = produitService;
        this.quincaillerieService = quincaillerieService;
    }

    // Tous les visiteurs peuvent voir la liste
    @GetMapping
    public List<Produit> getAll() {
        return produitService.getAll();
    }

    @GetMapping("/{id}")
    public Produit getById(@PathVariable Long id) {
        return produitService.getById(id);
    }

    // Produits d'une quincaillerie
    @GetMapping("/quincaillerie/{quincaillerieId}")
    public List<Produit> getByQuincaillerie(@PathVariable Long quincaillerieId) {
        Quincaillerie q = quincaillerieService.getById(quincaillerieId);
        return produitService.getByQuincaillerie(q);
    }

    // Création produit (OWNER ou MANAGER)
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    @PostMapping
    public Produit create(@RequestBody Produit produit) {
        return produitService.save(produit);
    }

    // Mise à jour produit (OWNER ou MANAGER)
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    @PutMapping("/{id}")
    public Produit update(@PathVariable Long id, @RequestBody Produit produit) {
        produit.setId(id);
        return produitService.save(produit);
    }

    // Suppression produit (OWNER ou MANAGER)
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        produitService.delete(id);
    }

}
