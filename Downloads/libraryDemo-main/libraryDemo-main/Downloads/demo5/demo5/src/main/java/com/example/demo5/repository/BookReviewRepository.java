package com.example.demo5.repository;

import java.util.List;

import com.example.demo5.entity.BookReview;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookReviewRepository extends JpaRepository<BookReview, Long> {

    List<BookReview> findAllByOrderByCreatedAtDesc();
}
