package fr.delthas.skype;


/**
 * A listener for new messages sent to a Skype account.
 *
 */
@FunctionalInterface
public interface GroupMessageListener {

  /**
   * Called when a message is sent from a user to a group the Skype account is in while it is connected.
   *
   * @param group The group in which the message has been sent.
   * @param sender The sender of the message.
   * @param message The message sent.
   */
  void messageReceived(Group group, User sender, String message);

}
