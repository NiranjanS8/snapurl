package com.snapurl.service.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_url_mapping_short_url", columnNames = "shortUrl")
        },
        indexes = {
                @Index(name = "idx_url_mapping_user_created", columnList = "user_id, createdAt"),
                @Index(name = "idx_url_mapping_user_clicks", columnList = "user_id, clickCount"),
                @Index(name = "idx_url_mapping_user_last_accessed", columnList = "user_id, lastAccessed"),
                @Index(name = "idx_url_mapping_user_expires", columnList = "user_id, expiresAt"),
                @Index(name = "idx_url_mapping_short_url", columnList = "shortUrl"),
                @Index(name = "idx_url_mapping_original_url", columnList = "originalUrl")
        }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    private String originalUrl;
    @Column(nullable = false, unique = true, length = 32)
    private String shortUrl;
    private int clickCount = 0;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessed;
    private LocalDateTime expiresAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private  Users user;

    @OneToMany(mappedBy = "urlMapping")
    @ToString.Exclude
    @JsonIgnore
    private List<ClickEvent> clickEvents;
}
