package org.jdk.qq_bot.listener;

import lombok.extern.slf4j.Slf4j;
import love.forte.simbot.component.onebot.v11.core.event.message.OneBotFriendMessageEvent;
import love.forte.simbot.event.ChatGroupMessageEvent;
import love.forte.simbot.quantcat.common.annotations.Listener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MyListener {

  /** 监听群消息事件 */
  @Listener
  public void onGroupMessage(ChatGroupMessageEvent event) {
    String nickName = event.getAuthor().getName();
    String plainText = event.getMessageContent().getPlainText();
    String groupName = event.getContent().getName();
    // 打印日志，xx在xx群说了xx
    log.info("🗣️ {} 在「{}」群说：{}", nickName, groupName, plainText);
  }

  /** 监听好友消息事件 */
  @Listener
  public void onFriendMessage(OneBotFriendMessageEvent event) {
    System.out.println("OneBotFriendMessageEvent: " + event);
    String name = event.getContent().getName();
    String plainText = event.getMessageContent().getPlainText();
    log.info("💌 {} 私聊说：{}", name, plainText);
  }
}
