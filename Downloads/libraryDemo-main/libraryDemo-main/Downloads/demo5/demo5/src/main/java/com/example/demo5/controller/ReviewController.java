package com.example.demo5.controller;

import java.time.LocalDateTime;
import java.util.List;

import com.example.demo5.entity.AppUser;
import com.example.demo5.entity.Book;
import com.example.demo5.entity.BookReview;
import com.example.demo5.repository.AppUserRepository;
import com.example.demo5.repository.BookRepository;
import com.example.demo5.repository.BookReviewRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReviewController {

    private final BookReviewRepository bookReviewRepository;
    private final BookRepository bookRepository;
    private final AppUserRepository appUserRepository;

    public ReviewController(BookReviewRepository bookReviewRepository,
                            BookRepository bookRepository,
                            AppUserRepository appUserRepository) {
        this.bookReviewRepository = bookReviewRepository;
        this.bookRepository = bookRepository;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping("/reviews")
    public List<ReviewResponse> listReviews() {
        return bookReviewRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/reviews")
    public ResponseEntity<?> createReview(@RequestBody CreateReviewRequest request) {
        String username = trim(request.username());
        Long bookId = request.bookId();
        Integer rating = request.rating();
        String comment = trim(request.comment());

        if (username.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiMessage("用户名不能为空"));
        }
        if (bookId == null) {
            return ResponseEntity.badRequest().body(new ApiMessage("请选择图书"));
        }
        if (rating == null || rating < 1 || rating > 5) {
            return ResponseEntity.badRequest().body(new ApiMessage("评分需为 1-5 分"));
        }
        if (comment.length() > 400) {
            return ResponseEntity.badRequest().body(new ApiMessage("评价内容不能超过 400 字"));
        }

        AppUser user = appUserRepository.findByUsernameIgnoreCase(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiMessage("用户不存在"));
        }

        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiMessage("图书不存在"));
        }

        BookReview review = new BookReview();
        review.setUser(user);
        review.setBook(book);
        review.setRating(rating);
        review.setComment(comment);
        BookReview saved = bookReviewRepository.save(review);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    private ReviewResponse toResponse(BookReview r) {
        return new ReviewResponse(
                r.getId(),
                r.getBook().getId(),
                r.getBook().getTitle(),
                r.getUser().getUsername(),
                r.getRating(),
                r.getComment(),
                r.getCreatedAt()
        );
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    record CreateReviewRequest(String username, Long bookId, Integer rating, String comment) {
    }

    record ReviewResponse(Long id, Long bookId, String bookTitle, String username, Integer rating, String comment,
                          LocalDateTime createdAt) {
    }

    record ApiMessage(String message) {
    }
}
