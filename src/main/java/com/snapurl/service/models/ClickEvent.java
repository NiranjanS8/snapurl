package com.snapurl.service.models;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_click_event_event_id", columnNames = "eventId")
        },
        indexes = {
                @Index(name = "idx_click_event_event_id", columnList = "eventId")
        }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    @Column(nullable = false, length = 64)
    private String eventId;
    private LocalDateTime clickTime;

    @ManyToOne
    @JoinColumn(name = "url_mapping_id")
    @ToString.Exclude
    private UrlMapping urlMapping;

}
