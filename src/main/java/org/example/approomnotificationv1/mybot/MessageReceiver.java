package org.example.roomnotificationtest_1.bot;

import lombok.RequiredArgsConstructor;
import org.example.roomnotificationtest_1.entity.Notification;
import org.example.roomnotificationtest_1.enums.ChatState;
import org.example.roomnotificationtest_1.enums.NotificationType;
import org.example.roomnotificationtest_1.enums.Room;
import org.example.roomnotificationtest_1.exception.MyExecuteException;
import org.example.roomnotificationtest_1.repository.NotificationRepository;
import org.example.roomnotificationtest_1.util.InlineKeyboardUtil;
import org.example.roomnotificationtest_1.util.SendMessageUtil;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MessageReceiver {
    private final TelegramBot telegramBot;
    private final NotificationRepository notificationRepository;

    public void onMessageReceived(Message message) {
        var chatStateMap = telegramBot.getChatStateMap();
        Long chatId = message.getChatId();

        if ("/start".equals(message.getText())) {
            notificationRepository.findByChatIdAndCompletedIsFalse(chatId).ifPresent(notificationRepository::delete);

            Message sentMessage = telegramBot.tryExecute(SendMessageUtil.simpleInlineKeyboardMarkupSendMessage(
                    chatId,
                    "Xonani tanlang",
                    Arrays.stream(Room.values()).map(Enum::name).toList()));

            Notification notification = Notification.builder()
                    .chatId(chatId)
                    .messageId(sentMessage.getMessageId())
                    .message("Xonani tanlang")
                    .build();

            notificationRepository.save(notification);

            chatStateMap.put(chatId, ChatState.ROOM);
        }


        if (!chatStateMap.containsKey(chatId)) {

            // maybe scanner
            if (message.hasText()) {
                String text = message.getText();

                if (text.matches("/start .+")) {
                    notificationRepository.findByChatIdAndCompletedIsFalse(chatId).ifPresent(notificationRepository::delete);
                    Room room = Room.valueOf(text.split(" ")[1]);

                    // todo DRY
                    Notification notification = new Notification();
                    notification.setChatId(chatId);
                    notification.setRoom(room);

                    String sm = """
                            Room: %s
                                                    
                            Muammo turini tanlang""".formatted(room);
                    Message sentMessage = telegramBot.tryExecute(SendMessage.builder()
                            .chatId(chatId)
                            .text(sm)
                            .replyMarkup(InlineKeyboardUtil.simpleInlineKeyboardMarkup(Arrays.stream(NotificationType.values()).map(Enum::name).toList()))
                            .build());


                    notification.setMessageId(sentMessage.getMessageId());
                    notificationRepository.save(notification);

                    chatStateMap.put(chatId, ChatState.NOTIFICATION_TYPE);

                    return;
                }
            }

            Message sentMessage = telegramBot.tryExecute(SendMessageUtil.simpleInlineKeyboardMarkupSendMessage(
                    chatId,
                    "Xonani tanlang",
                    Arrays.stream(Room.values()).map(Enum::name).toList()));

            Notification notification = Notification.builder()
                    .chatId(chatId)
                    .messageId(sentMessage.getMessageId())
                    .message("Xonani tanlang")
                    .build();

            notificationRepository.save(notification);

            chatStateMap.put(chatId, ChatState.ROOM);
        } else {
            ChatState chatState = chatStateMap.get(chatId);

            switch (chatState) {
                case MESSAGE -> {
                    if (message.hasText()) {
                        String text = message.getText();

                        Notification notification = notificationRepository.findByChatIdAndCompletedIsFalse(chatId).orElseThrow();
                        notification.setMessage(text);

                        DeleteMessage deleteMessage = DeleteMessage.builder()
                                .chatId(chatId)
                                .messageId(notification.getMessageId())
                                .build();
                        telegramBot.tryExecute(deleteMessage);

                        text = """
                                Xona: %s
                                Muammo turi: %s,
                                Xabaringiz: %s
                                                       
                                Xohlasangiz hozir rasm jonatishingiz mumkin""".formatted(notification.getRoom(),
                                notification.getType(), notification.getMessage());
                        Message sentMessage = telegramBot.tryExecute(SendMessage.builder()
                                .chatId(chatId)
                                .text(text)
                                .replyMarkup(InlineKeyboardUtil.simpleInlineKeyboardMarkup(List.of("✅ Tasdiqlash", "❌ Bekor qilish")))
                                .build());

                        notification.setMessageId(sentMessage.getMessageId());
                        notificationRepository.save(notification);

                        chatStateMap.put(chatId, ChatState.IMAGE_OR_LAST);
                    }
                }

                case IMAGE_OR_LAST -> {
                    if (message.hasPhoto()) {
                        try {
                            Notification notification = notificationRepository.findByChatIdAndCompletedIsFalse(chatId).orElseThrow();
                            // download
                            PhotoSize last = message.getPhoto().getLast();
                            GetFile getFile = new GetFile(last.getFileId());
                            File file = telegramBot.execute(getFile);
                            String pathname = "./src/main/resources/" + file.getFilePath();
                            var output = new java.io.File(pathname);
                            telegramBot.downloadFile(file, output);

                            telegramBot.tryExecute(DeleteMessage.builder()
                                    .chatId(chatId)
                                    .messageId(notification.getMessageId())
                                    .build());

                            SendPhoto sendPhoto = new SendPhoto();
                            sendPhoto.setChatId(chatId);
                            sendPhoto.setPhoto(new InputFile(output));
                            sendPhoto.setCaption("""
                                    Xona: %s,
                                    Xabar turi: %s,
                                    Xabar: %s
                                                                        
                                    Siz ushbu xabarni tasdiqlashingiz yoki bekor qilishingiz mumkin"""
                                    .formatted(notification.getRoom(), notification.getType(), notification.getMessage()));
                            sendPhoto.setReplyMarkup(InlineKeyboardUtil.simpleInlineKeyboardMarkup(List.of("✅ Tasdiqlash", "❌ Bekor qilish")));

                            Message executed = telegramBot.execute(sendPhoto);

                            notification.setImageUrl(pathname);
                            notification.setMessageId(executed.getMessageId());
                            notificationRepository.save(notification);
                        } catch (TelegramApiException e) {
                            throw new MyExecuteException(e);
                        }
                    }
                }
            }

        }

    }
}
