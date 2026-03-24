package com.example.demo5.entity;

import java.time.LocalDateTime;

import com.example.demo5.enums.AnnouncementStatus;

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
@Table(name = "announcements")
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 4000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AnnouncementStatus status;

    @Column(nullable = false)
    private LocalDateTime publishedAt;

    @PrePersist
    void onCreate() {
        if (status == null) {
            status = AnnouncementStatus.PUBLISHED;
        }
        if (publishedAt == null) {
            publishedAt = LocalDateTime.now();
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public AnnouncementStatus getStatus() {
        return status == null ? AnnouncementStatus.PUBLISHED : status;
    }

    public void setStatus(AnnouncementStatus status) {
        this.status = status;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
