package com.example.demo5.repository;

import java.util.List;

import com.example.demo5.entity.AppUser;
import com.example.demo5.entity.Book;
import com.example.demo5.entity.BorrowRecord;
import com.example.demo5.enums.BorrowStatus;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BorrowRepository extends JpaRepository<BorrowRecord, Long> {

    List<BorrowRecord> findByUserOrderByBorrowedAtDesc(AppUser user);

    List<BorrowRecord> findAllByOrderByBorrowedAtDesc();

    boolean existsByUserAndBookAndStatus(AppUser user, Book book, BorrowStatus status);
}
