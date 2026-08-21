package ru.kirzhq.wazzup.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "pending_contact_candidates",
        uniqueConstraints = @UniqueConstraint(columnNames = {"chat_type", "chat_id"})
)
public class PendingContactCandidate {
    @Id
    private String id;

    @Column(name = "chat_type", nullable = false, length = 32)
    private String chatType;

    @Column(name = "chat_id", nullable = false, length = 200)
    private String chatId;

    @Column(length = 200)
    private String name;

    @Column(length = 200)
    private String username;

    @Column(length = 32)
    private String phone;

    @Column(nullable = false, length = 32)
    private String source;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PendingContactCandidate() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getChatType() { return chatType; }
    public void setChatType(String chatType) { this.chatType = chatType; }
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
