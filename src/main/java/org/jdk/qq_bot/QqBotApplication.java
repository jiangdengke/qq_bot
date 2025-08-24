package org.jdk.qq_bot;

import love.forte.simbot.spring.EnableSimbot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableSimbot
@SpringBootApplication
public class QqBotApplication {

  public static void main(String[] args) {
    SpringApplication.run(QqBotApplication.class, args);
  }
}
