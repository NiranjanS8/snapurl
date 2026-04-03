package com.snapurl.service.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClickEventMessage {
    private String eventId;
    private Long urlMappingId;
    private Long userId;
    private String shortUrl;
    private LocalDateTime clickedAt;
}
