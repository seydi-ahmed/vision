package com.exemple.controller;

import com.exemple.entity.Quincaillerie;
import com.exemple.service.QuincaillerieService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quincailleries")
public class QuincaillerieController {

    private final QuincaillerieService quincaillerieService;

    public QuincaillerieController(QuincaillerieService quincaillerieService) {
        this.quincaillerieService = quincaillerieService;
    }

    // Tous les visiteurs peuvent voir la liste
    @GetMapping
    public List<Quincaillerie> getAll() {
        return quincaillerieService.getAll();
    }

    @GetMapping("/{id}")
    public Quincaillerie getById(@PathVariable Long id) {
        return quincaillerieService.getById(id);
    }

    // Création d’une quincaillerie (OWNER seulement)
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping
    public Quincaillerie create(@RequestBody Quincaillerie q) {
        return quincaillerieService.save(q);
    }

    // Mise à jour (OWNER seulement)
    @PreAuthorize("hasRole('OWNER')")
    @PutMapping("/{id}")
    public Quincaillerie update(@PathVariable Long id, @RequestBody Quincaillerie q) {
        q.setId(id);
        return quincaillerieService.save(q);
    }

    // Suppression (OWNER seulement)
    @PreAuthorize("hasRole('OWNER')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        quincaillerieService.delete(id);
    }

}
