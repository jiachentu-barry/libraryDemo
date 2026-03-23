package com.example.demo5.book;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BookController {

    private final BookRepository bookRepository;

    public BookController(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @GetMapping("/books")
    public List<Book> listBooks() {
        return bookRepository.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/books/{id}")
    public ResponseEntity<?> getBook(@PathVariable Long id) {
        Book book = bookRepository.findById(id).orElse(null);
        if (book == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiMessage("图书不存在"));
        }
        return ResponseEntity.ok(book);
    }

    @GetMapping("/books/{id}/image")
    public ResponseEntity<?> getBookImage(@PathVariable Long id) {
        Book book = bookRepository.findById(id).orElse(null);
        if (book == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiMessage("图书不存在"));
        }
        return ResponseEntity.ok(new ImageResponse(book.getImage()));
    }

    @PostMapping("/books")
    public ResponseEntity<?> createBook(@RequestBody BookRequest request) {
        String title = trim(request.title());
        String author = trim(request.author());
        String category = trim(request.category());
        String image = trim(request.image());
        Integer stock = request.stock();
        Integer recommendationIndex = request.recommendationIndex();
        BookStatus status = parseStatus(request.status());

        String validationError = validate(title, author, category, stock, recommendationIndex, status);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(new ApiMessage(validationError));
        }

        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setCategory(category);
        book.setStock(stock);
        book.setRecommendationIndex(recommendationIndex);
        book.setImage(image);
        book.setStatus(status);
        book.setCreatedAt(LocalDateTime.now());

        Book saved = bookRepository.save(book);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/books/{id}")
    public ResponseEntity<?> updateBook(@PathVariable Long id, @RequestBody BookRequest request) {
        Book book = bookRepository.findById(id).orElse(null);
        if (book == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiMessage("图书不存在"));
        }

        String title = trim(request.title());
        String author = trim(request.author());
        String category = trim(request.category());
        String image = trim(request.image());
        Integer stock = request.stock();
        Integer recommendationIndex = request.recommendationIndex();
        BookStatus status = parseStatus(request.status());

        String validationError = validate(title, author, category, stock, recommendationIndex, status);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(new ApiMessage(validationError));
        }

        book.setTitle(title);
        book.setAuthor(author);
        book.setCategory(category);
        book.setStock(stock);
        book.setRecommendationIndex(recommendationIndex);
        book.setImage(image);
        book.setStatus(status);

        Book saved = bookRepository.save(book);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/books/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable Long id) {
        if (!bookRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiMessage("图书不存在"));
        }
        bookRepository.deleteById(id);
        return ResponseEntity.ok(new ApiMessage("删除成功"));
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String validate(String title, String author, String category, Integer stock, Integer recommendationIndex, BookStatus status) {
        if (title.isEmpty()) {
            return "书名不能为空";
        }
        if (author.isEmpty()) {
            return "作者不能为空";
        }
        if (category.isEmpty()) {
            return "分类不能为空";
        }
        if (stock == null || stock < 0) {
            return "库存数必须大于等于 0";
        }
        if (recommendationIndex == null || recommendationIndex < 1 || recommendationIndex > 5) {
            return "推荐指数必须在 1-5 之间";
        }
        if (status == null) {
            return "状态不合法";
        }
        return null;
    }

    private BookStatus parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return BookStatus.NORMAL;
        }
        try {
            return BookStatus.valueOf(rawStatus.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public record BookRequest(String title, String author, String category, Integer stock, Integer recommendationIndex, String image, String status) {
    }

    public record ApiMessage(String message) {
    }

    public record ImageResponse(String image) {
    }
}
