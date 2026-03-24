package com.example.demo5.controller;

import java.time.LocalDateTime;
import java.util.List;

import com.example.demo5.entity.AppUser;
import com.example.demo5.entity.Book;
import com.example.demo5.entity.BorrowRecord;
import com.example.demo5.enums.BorrowStatus;
import com.example.demo5.repository.AppUserRepository;
import com.example.demo5.repository.BookRepository;
import com.example.demo5.repository.BorrowRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BorrowController {

    private final BorrowRepository borrowRepository;
    private final BookRepository bookRepository;
    private final AppUserRepository appUserRepository;

    public BorrowController(BorrowRepository borrowRepository, BookRepository bookRepository,
                            AppUserRepository appUserRepository) {
        this.borrowRepository = borrowRepository;
        this.bookRepository = bookRepository;
        this.appUserRepository = appUserRepository;
    }

    /** 借阅图书 */
    @Transactional
    @PostMapping("/borrow")
    public ResponseEntity<?> borrowBook(@RequestBody BorrowRequest request) {
        String username = request.username() == null ? "" : request.username().trim();
        Long bookId = request.bookId();

        if (username.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiMessage("用户名不能为空"));
        }
        if (bookId == null) {
            return ResponseEntity.badRequest().body(new ApiMessage("请选择要借阅的图书"));
        }

        AppUser user = appUserRepository.findByUsernameIgnoreCase(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiMessage("用户不存在"));
        }

        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiMessage("图书不存在"));
        }

        if (book.getStock() == null || book.getStock() <= 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiMessage("该图书暂无库存，无法借阅"));
        }

        if (borrowRepository.existsByUserAndBookAndStatus(user, book, BorrowStatus.BORROWED)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiMessage("您已借阅该图书，请归还后再借"));
        }

        book.setStock(book.getStock() - 1);
        bookRepository.save(book);

        BorrowRecord record = new BorrowRecord();
        record.setUser(user);
        record.setBook(book);
        BorrowRecord saved = borrowRepository.save(record);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    /** 用户归还图书 */
    @Transactional
    @PostMapping("/borrow/{id}/return")
    public ResponseEntity<?> returnBook(@PathVariable Long id, @RequestBody ReturnRequest request) {
        String username = request.username() == null ? "" : request.username().trim();

        BorrowRecord record = borrowRepository.findById(id).orElse(null);
        if (record == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiMessage("借阅记录不存在"));
        }

        if (!record.getUser().getUsername().equalsIgnoreCase(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiMessage("您无权归还此借阅记录"));
        }

        if (record.getStatus() == BorrowStatus.RETURNED) {
            return ResponseEntity.badRequest().body(new ApiMessage("该图书已归还"));
        }

        record.setStatus(BorrowStatus.RETURNED);
        record.setReturnedAt(LocalDateTime.now());
        Book book = record.getBook();
        book.setStock(book.getStock() + 1);
        bookRepository.save(book);
        borrowRepository.save(record);

        return ResponseEntity.ok(new ApiMessage("归还成功"));
    }

    /** 查询用户借阅记录 */
    @GetMapping("/users/{username}/borrows")
    public ResponseEntity<?> getUserBorrows(@PathVariable String username) {
        AppUser user = appUserRepository.findByUsernameIgnoreCase(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiMessage("用户不存在"));
        }
        List<BorrowRecord> records = borrowRepository.findByUserOrderByBorrowedAtDesc(user);
        return ResponseEntity.ok(records.stream().map(this::toResponse).toList());
    }

    /** 管理员查看所有借阅记录 */
    @GetMapping("/admin/borrows")
    public List<BorrowRecordResponse> adminListBorrows() {
        return borrowRepository.findAllByOrderByBorrowedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    /** 管理员强制归还 */
    @Transactional
    @PostMapping("/admin/borrow/{id}/return")
    public ResponseEntity<?> adminReturnBook(@PathVariable Long id) {
        BorrowRecord record = borrowRepository.findById(id).orElse(null);
        if (record == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiMessage("借阅记录不存在"));
        }
        if (record.getStatus() == BorrowStatus.RETURNED) {
            return ResponseEntity.badRequest().body(new ApiMessage("该图书已归还"));
        }
        record.setStatus(BorrowStatus.RETURNED);
        record.setReturnedAt(LocalDateTime.now());
        Book book = record.getBook();
        book.setStock(book.getStock() + 1);
        bookRepository.save(book);
        borrowRepository.save(record);
        return ResponseEntity.ok(new ApiMessage("归还成功"));
    }

    private BorrowRecordResponse toResponse(BorrowRecord r) {
        boolean overdue = r.getStatus() == BorrowStatus.BORROWED
                && r.getDueDate() != null
                && LocalDateTime.now().isAfter(r.getDueDate());
        BorrowStatus displayStatus = overdue ? BorrowStatus.OVERDUE : r.getStatus();
        return new BorrowRecordResponse(
                r.getId(),
                r.getUser().getUsername(),
                r.getBook().getId(),
                r.getBook().getTitle(),
                r.getBook().getAuthor(),
                r.getBorrowedAt(),
                r.getDueDate(),
                r.getReturnedAt(),
                displayStatus,
                overdue
        );
    }

    record BorrowRequest(String username, Long bookId) {}
    record ReturnRequest(String username) {}
    record ApiMessage(String message) {}

    record BorrowRecordResponse(
            Long id,
            String username,
            Long bookId,
            String bookTitle,
            String bookAuthor,
            LocalDateTime borrowedAt,
            LocalDateTime dueDate,
            LocalDateTime returnedAt,
            BorrowStatus status,
            boolean overdue
    ) {}
}
