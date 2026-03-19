package com.snapurl.service.dtos;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String code;
    private String password;
}
