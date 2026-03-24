package com.example.demo5.entity;

import java.time.LocalDateTime;

import com.example.demo5.enums.BorrowStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "borrow_records")
public class BorrowRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private LocalDateTime borrowedAt;

    @Column(nullable = false)
    private LocalDateTime dueDate;

    @Column
    private LocalDateTime returnedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private BorrowStatus status;

    @PrePersist
    void onCreate() {
        if (borrowedAt == null) {
            borrowedAt = LocalDateTime.now();
        }
        if (dueDate == null) {
            dueDate = borrowedAt.plusDays(14);
        }
        if (status == null) {
            status = BorrowStatus.BORROWED;
        }
    }

    public Long getId() { return id; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }

    public LocalDateTime getBorrowedAt() { return borrowedAt; }
    public void setBorrowedAt(LocalDateTime borrowedAt) { this.borrowedAt = borrowedAt; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDateTime returnedAt) { this.returnedAt = returnedAt; }

    public BorrowStatus getStatus() { return status; }
    public void setStatus(BorrowStatus status) { this.status = status; }
}
