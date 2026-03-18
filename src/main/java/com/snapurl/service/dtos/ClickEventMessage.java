package com.snapurl.service.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClickEventMessage {
    private Long urlMappingId;
    private String shortUrl;
    private LocalDateTime clickedAt;
}
