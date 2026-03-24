package com.example.demo5.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import com.example.demo5.entity.Announcement;
import com.example.demo5.enums.AnnouncementStatus;
import com.example.demo5.repository.AnnouncementRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AnnouncementController {

    private final AnnouncementRepository announcementRepository;

    public AnnouncementController(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    @PostMapping("/admin/announcements")
    public ResponseEntity<?> createAnnouncement(@RequestBody AnnouncementRequest request) {
        String title = request.title() == null ? "" : request.title().trim();
        String content = request.content() == null ? "" : request.content().trim();
        AnnouncementStatus status = parseStatus(request.status());

        if (title.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiMessage("标题不能为空"));
        }
        if (content.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiMessage("内容不能为空"));
        }
        if (status == null) {
            return ResponseEntity.badRequest().body(new ApiMessage("状态不合法"));
        }

        Announcement announcement = new Announcement();
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setStatus(status);
        announcement.setPublishedAt(LocalDateTime.now());

        Announcement saved = announcementRepository.save(announcement);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/admin/announcements/{id}")
    public ResponseEntity<?> updateAnnouncement(@PathVariable Long id, @RequestBody AnnouncementRequest request) {
        Announcement announcement = announcementRepository.findById(id)
                .orElse(null);
        if (announcement == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiMessage("公告不存在"));
        }

        String title = request.title() == null ? "" : request.title().trim();
        String content = request.content() == null ? "" : request.content().trim();
        AnnouncementStatus status = parseStatus(request.status());

        if (title.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiMessage("标题不能为空"));
        }
        if (content.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiMessage("内容不能为空"));
        }
        if (status == null) {
            return ResponseEntity.badRequest().body(new ApiMessage("状态不合法"));
        }

        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setStatus(status);

        Announcement saved = announcementRepository.save(announcement);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/admin/announcements")
    public List<Announcement> listAnnouncementsForAdmin() {
        return announcementRepository.findAllByOrderByPublishedAtDesc();
    }

    @GetMapping("/announcements")
    public List<Announcement> listPublicAnnouncements() {
        return announcementRepository.findAllByOrderByPublishedAtDesc()
                .stream()
                .filter(item -> item.getStatus() == AnnouncementStatus.PUBLISHED)
                .toList();
    }

    private AnnouncementStatus parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return AnnouncementStatus.PUBLISHED;
        }
        try {
            return AnnouncementStatus.valueOf(rawStatus.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public record AnnouncementRequest(String title, String content, String status) {
    }

    public record ApiMessage(String message) {
    }
}
