package com.example.demo5.repository;

import java.util.List;

import com.example.demo5.entity.Book;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findAllByOrderByCreatedAtDesc();
}
