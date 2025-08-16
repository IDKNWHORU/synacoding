package com.platform.coding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// Spring의 스케줄러 기능을 활성화
@EnableScheduling
@SpringBootApplication
public class LearningManagementSystem {

  public static void main(String[] args) {
    SpringApplication.run(LearningManagementSystem.class, args);
  }

}
