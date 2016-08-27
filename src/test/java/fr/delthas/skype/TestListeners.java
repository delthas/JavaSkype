package fr.delthas.skype;

import fr.delthas.skype.message.Message;
import org.junit.Assert;

import java.util.logging.Logger;

@SuppressWarnings({"javadoc", "static-method"})
public class TestListeners {

  public static Logger logger = Logger.getLogger("TestConnect");

  public void testConnect() {
    Skype skype = new Skype("username", "password");
    try {
      logger.info("Starting...");
      skype.connect();
      logger.info("Started");
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
    skype.setErrorListener(e -> {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    });
    skype.addGroupMessageListener(new GroupMessageListener() {
      @Override
      public void messageReceived(Group group, User sender, Message message) {
        logger.info("Received " + message);
      }

      @Override
      public void messageEdited(Group group, User sender, Message message) {
        logger.info("Edited " + message);
      }

      @Override
      public void messageRemoved(Group group, User sender, Message message) {
        logger.info("Removed " + message);
      }
    });

    skype.addUserMessageListener(new UserMessageListener() {
      @Override
      public void messageReceived(User sender, Message message) {
        logger.info("Received " + message);
      }

      @Override
      public void messageEdited(User sender, Message message) {
        logger.info("Edited " + message);
      }

      @Override
      public void messageRemoved(User sender, Message message) {
        logger.info("Removed " + message);
      }
    });
  }

  public static void main(String... strings) {
    new TestListeners().testConnect();
  }

}
