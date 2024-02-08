package org.example.roomnotificationtest_1.bot;

import lombok.RequiredArgsConstructor;
import org.example.roomnotificationtest_1.entity.Notification;
import org.example.roomnotificationtest_1.enums.ChatState;
import org.example.roomnotificationtest_1.enums.NotificationType;
import org.example.roomnotificationtest_1.enums.Room;
import org.example.roomnotificationtest_1.repository.NotificationRepository;
import org.example.roomnotificationtest_1.util.InlineKeyboardUtil;
import org.example.roomnotificationtest_1.util.SendMessageUtil;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.File;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class CallbackReceiver {
    private final NotificationRepository notificationRepository;
    private final TelegramBot telegramBot;

    public void onCallbackReceived(CallbackQuery callbackQuery) {
        var chatStateMap = telegramBot.getChatStateMap();
        Long chatId = callbackQuery.getFrom().getId();

        if (!chatStateMap.containsKey(chatId)) {
            telegramBot.tryExecute(SendMessageUtil.simpleInlineKeyboardMarkupSendMessage(
                    chatId,
                    "Xonani tanlang",
                    Arrays.stream(Room.values()).map(Enum::name).toList()));
            chatStateMap.put(chatId, ChatState.ROOM);
            return;
        }

        ChatState chatState = chatStateMap.get(chatId);
        final String data = callbackQuery.getData();
        switch (chatState) {
            case ROOM -> {
                Room room = Room.valueOf(data);
                Notification notification = notificationRepository.findByChatIdAndCompletedIsFalse(chatId).orElseThrow();
                notification.setRoom(room);

                DeleteMessage deleteMessage = DeleteMessage.builder()
                        .chatId(chatId)
                        .messageId(notification.getMessageId())
                        .build();
                telegramBot.tryExecute(deleteMessage);

                String text = """
                        Xona: %s
                                                
                        Muammo turini tanlang""".formatted(room);
                Message sentMessage = telegramBot.tryExecute(SendMessage.builder()
                        .chatId(chatId)
                        .text(text)
                        .replyMarkup(InlineKeyboardUtil.simpleInlineKeyboardMarkup(Arrays.stream(NotificationType.values()).map(Enum::name).toList()))
                        .build());


                notification.setMessageId(sentMessage.getMessageId());
                notificationRepository.save(notification);

                chatStateMap.put(chatId, ChatState.NOTIFICATION_TYPE);
            }

            case NOTIFICATION_TYPE -> {
                NotificationType type = NotificationType.valueOf(data);
                Notification notification = notificationRepository.findByChatIdAndCompletedIsFalse(chatId).orElseThrow();
                notification.setType(type);

                DeleteMessage deleteMessage = DeleteMessage.builder()
                        .chatId(chatId)
                        .messageId(notification.getMessageId())
                        .build();
                telegramBot.tryExecute(deleteMessage);

                String text = """
                        Xona: %s
                        Muammo turi: %s
                                               
                        Xabaringizni yozing""".formatted(notification.getRoom(), notification.getType());
                Message sentMessage = telegramBot.tryExecute(SendMessage.builder()
                        .chatId(chatId)
                        .text(text)
                        .build());

                notification.setMessageId(sentMessage.getMessageId());
                notificationRepository.save(notification);

                chatStateMap.put(chatId, ChatState.MESSAGE);
            }

            case IMAGE_OR_LAST -> {
                Notification notification = notificationRepository.findByChatIdAndCompletedIsFalse(chatId).orElseThrow();

                if (data.equals("✅ Tasdiqlash")) {

                    String text = """
                            Xona: %s
                            Muammo turi: %s
                            Xabaringiz: %s
                            """.formatted(notification.getRoom(), notification.getType(), notification.getMessage());


                    // sent to admin todo all admin
                    if (notification.getImageUrl() == null) {
                        telegramBot.tryExecute(SendMessage.builder()
                                .chatId(telegramBot.getBotConfig().getAdminChatId())
                                .text(text)
                                .build());
                    } else {
                        telegramBot.tryExecute(SendPhoto.builder()
                                .chatId(telegramBot.getBotConfig().getAdminChatId())
                                .caption(text)
                                .photo(new InputFile(new File(notification.getImageUrl())))
                                .build());
                    }

                    telegramBot.tryExecute(SendMessage.builder()
                            .chatId(chatId)
                            .text("✅ Xabaringiz muvaffaqiyatli jo'nalildi")
                            .build());

                    notification.setCompleted(true);
                    notificationRepository.save(notification);

                    chatStateMap.remove(chatId);
                } else {
                    notificationRepository.delete(notification);
                    telegramBot.tryExecute(SendMessage.builder()
                            .chatId(chatId)
                            .text("Xabaringiz bekor qilindi.")
                            .build());

                    chatStateMap.remove(chatId);
                }
            }
        }
    }
}
