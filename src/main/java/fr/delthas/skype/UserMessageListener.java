package fr.delthas.skype;


import fr.delthas.skype.message.Message;

/**
 * A listener for new messages sent to a Skype account.
 */
public interface UserMessageListener extends MessageListener {

  /**
   * Called when a message is sent from a user to the Skype account while it is connected.
   *
   * @param sender  The sender of the message.
   * @param message The message sent.
   */
  @Deprecated
  default void messageReceived(User sender, String message) {
  }

  default void messageReceived(User sender, Message message) {
  }

  default void messageEdited(User sender, Message message) {
  }

  default void messageRemoved(User sender, Message message) {
  }
}
