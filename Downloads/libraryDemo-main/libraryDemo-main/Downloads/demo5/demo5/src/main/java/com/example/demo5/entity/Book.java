package com.example.demo5.entity;

import java.time.LocalDateTime;

import com.example.demo5.enums.BookStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 80)
    private String author;

    @Column(nullable = false, length = 50)
    private String category;

    @Column
    private Integer stock;

    @Column
    private Integer recommendationIndex;

    @Column(columnDefinition = "LONGTEXT")
    private String image;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BookStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (status == null) {
            status = BookStatus.NORMAL;
        }
        if (stock == null) {
            stock = 0;
        }
        if (recommendationIndex == null) {
            recommendationIndex = 1;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getStock() {
        return stock == null ? 0 : stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getRecommendationIndex() {
        return recommendationIndex == null ? 1 : recommendationIndex;
    }

    public void setRecommendationIndex(Integer recommendationIndex) {
        this.recommendationIndex = recommendationIndex;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public BookStatus getStatus() {
        return status == null ? BookStatus.NORMAL : status;
    }

    public void setStatus(BookStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
