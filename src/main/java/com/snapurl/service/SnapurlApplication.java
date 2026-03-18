package com.snapurl.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SnapurlApplication {

	public static void main(String[] args) {
		SpringApplication.run(SnapurlApplication.class, args);
	}

}
