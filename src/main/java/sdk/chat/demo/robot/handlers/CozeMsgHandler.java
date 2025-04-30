package sdk.chat.demo.robot.handlers;

import sdk.chat.core.dao.Message;
import sdk.chat.core.handlers.MessageHandler;
import sdk.chat.core.manager.MessagePayload;
import sdk.chat.core.manager.TextMessagePayload;
import sdk.chat.core.types.MessageType;

public class CozeMsgHandler implements MessageHandler {
    @Override
    public MessagePayload payloadFor(Message message) {
        return new TextMessagePayload(message);
    }

    @Override
    public boolean isFor(MessageType type) {
        return type != null && type.is(MessageType.System);
    }
}
