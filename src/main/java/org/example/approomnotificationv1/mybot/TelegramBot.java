package org.example.roomnotificationtest_1.bot;

import lombok.Getter;
import org.example.roomnotificationtest_1.config.BotConfig;
import org.example.roomnotificationtest_1.enums.ChatState;
import org.example.roomnotificationtest_1.exception.MyExecuteException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Getter
public class TelegramBot extends TelegramLongPollingBot {
    private final BotConfig botConfig;
    private final Map<Long, ChatState> chatStateMap = new ConcurrentHashMap<>();
    private final MessageReceiver messageReceiver;
    private final CallbackReceiver callbackReceiver;

    public TelegramBot(BotConfig botConfig, @Lazy MessageReceiver messageReceiver, @Lazy CallbackReceiver callbackReceiver) {
        super(botConfig.getToken());
        this.botConfig = botConfig;
        this.messageReceiver = messageReceiver;
        this.callbackReceiver = callbackReceiver;
    }

    @Override
    public void onUpdateReceived(Update update) {
        /*SendPhoto sendPhoto = new SendPhoto();

        sendPhoto.setChatId(update.getMessage().getChatId());

        sendPhoto.setCaption("This is caption");

        File file = new File("./src/main/resources/photos/file_3.jpg");
        sendPhoto.setPhoto(new InputFile(file));

        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton("AA");
        inlineKeyboardButton.setCallbackData("AA");
        sendPhoto.setReplyMarkup(new InlineKeyboardMarkup(List.of(List.of(inlineKeyboardButton))));

        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

        List<PhotoSize> photo = update.getMessage().getPhoto();
        PhotoSize last = photo.getLast();
        GetFile getFile = new GetFile(last.getFileId());
        try {
            File file = execute(getFile);
            java.io.File output = new java.io.File("./src/main/resources/" + file.getFilePath());
            downloadFile(file, output);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }*/

        if (update.hasMessage())
            messageReceiver.onMessageReceived(update.getMessage());

        if (update.hasCallbackQuery())
            callbackReceiver.onCallbackReceived(update.getCallbackQuery());
    }

    public Message tryExecute(SendMessage sendMessage) {
        try {
            return execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new MyExecuteException(e);
        }
    }


    public Message tryExecute(SendPhoto sendPhoto) {
        try {
            return execute(sendPhoto);
        } catch (TelegramApiException e) {
            throw new MyExecuteException(e);
        }
    }

    public void tryExecute(DeleteMessage deleteMessage) {
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            throw new MyExecuteException(e);
        }
    }

    @Override
    public String getBotUsername() {
        return botConfig.getUsername();
    }


}
