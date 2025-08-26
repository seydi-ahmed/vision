package com.exemple.repository;

import com.exemple.entity.Quincaillerie;
import com.exemple.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuincaillerieRepository extends JpaRepository<Quincaillerie, Long> {
    List<Quincaillerie> findByOwner(User owner);
    List<Quincaillerie> findByManager(User manager);
}
