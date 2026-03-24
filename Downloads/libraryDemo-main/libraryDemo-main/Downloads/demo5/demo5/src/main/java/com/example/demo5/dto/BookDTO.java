package com.example.demo5.dto;

import java.time.LocalDateTime;

import com.example.demo5.entity.Book;
import com.example.demo5.enums.BookStatus;

public record BookDTO(
    Long id,
    String title,
    String author,
    String category,
    Integer stock,
    Integer recommendationIndex,
    BookStatus status,
    LocalDateTime createdAt
) {

    public static BookDTO from(Book book) {
        return new BookDTO(
            book.getId(),
            book.getTitle(),
            book.getAuthor(),
            book.getCategory(),
            book.getStock(),
            book.getRecommendationIndex(),
            book.getStatus(),
            book.getCreatedAt()
        );
    }
}
