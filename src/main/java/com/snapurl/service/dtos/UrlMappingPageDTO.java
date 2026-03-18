package com.snapurl.service.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrlMappingPageDTO {
    private List<UrlMappingDTO> items;
    private String nextCursor;
    private boolean hasNext;
}
