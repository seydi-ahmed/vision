package com.exemple.service;

import com.exemple.entity.Quincaillerie;
import com.exemple.entity.User;
import com.exemple.repository.QuincaillerieRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuincaillerieService {

    private final QuincaillerieRepository quincaillerieRepository;

    public QuincaillerieService(QuincaillerieRepository quincaillerieRepository) {
        this.quincaillerieRepository = quincaillerieRepository;
    }

    public List<Quincaillerie> getAll() {
        return quincaillerieRepository.findAll();
    }

    public List<Quincaillerie> getByOwner(User owner) {
        return quincaillerieRepository.findByOwner(owner);
    }

    public Quincaillerie getById(Long id) {
        return quincaillerieRepository.findById(id).orElseThrow();
    }

    public Quincaillerie save(Quincaillerie q) {
        return quincaillerieRepository.save(q);
    }

    public void delete(Long id) {
        quincaillerieRepository.deleteById(id);
    }
}
