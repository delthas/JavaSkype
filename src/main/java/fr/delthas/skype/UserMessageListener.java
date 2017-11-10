package fr.delthas.skype;

/**
 * A listener for new messages sent to a Skype account.
 */
@FunctionalInterface
public interface UserMessageListener {
  /**
   * Called when a message is sent from a user to the Skype account while it is connected.
   *
   * @param sender  The sender of the message.
   * @param message The message sent.
   */
  void messageReceived(User sender, String message);
}
