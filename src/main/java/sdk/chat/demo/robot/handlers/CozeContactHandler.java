package sdk.chat.demo.robot.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Completable;
import sdk.chat.core.dao.User;
import sdk.chat.core.handlers.ContactHandler;
import sdk.chat.core.session.ChatSDK;
import sdk.chat.core.types.ConnectionType;

public class CozeContactHandler implements ContactHandler {

    @Override
    public List<User> contacts() {
        List<User> users = new ArrayList<>();
        User robot = ChatSDK.db().fetchOrCreateEntityWithEntityID(User.class, "robot1");
        users.add(robot);
//        robot.setName("人工智信");
//        robot.setHeaderURL("https://media.istockphoto.com/id/1003676294/zh/%E5%90%91%E9%87%8F/%E5%9C%96%E7%95%AB%E5%9F%BA%E7%9D%A3%E5%BE%92%E5%8D%81%E5%AD%97%E6%9E%B6%E5%BE%A9%E6%B4%BB%E7%AF%80%E8%83%8C%E6%99%AF%E5%9C%A8%E9%87%91%E5%AD%90%E5%8F%A3%E6%B0%A3.jpg?s=2048x2048&w=is&k=20&c=ZQNgr7w53jaLIKx_YousXMgTZrv5ycqdThyejAdaqXg=");
        return users;
    }

    @Override
    public boolean exists(User user) {
        return false;
    }

    @Override
    public List<User> contactsWithType(ConnectionType type) {
        return Collections.emptyList();
    }

    @Override
    public Completable addContact(User user, ConnectionType type) {
        return null;
    }

    @Override
    public Completable deleteContact(User user, ConnectionType type) {
        return null;
    }

    @Override
    public Completable addContactLocal(User user, ConnectionType type) {
        return null;
    }

    @Override
    public void deleteContactLocal(User user, ConnectionType type) {

    }

    @Override
    public Completable addContacts(ArrayList<User> users, ConnectionType type) {
        return null;
    }

    @Override
    public Completable deleteContacts(ArrayList<User> users, ConnectionType type) {
        return null;
    }
}
