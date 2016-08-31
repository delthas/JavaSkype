package fr.delthas.skype;

import fr.delthas.skype.message.Message;

/**
 * A listener for new messages sent to a Skype account.
 */
public interface GroupMessageListener extends MessageListener {

  /**
   * Called when a message is sent from a user to a group the Skype account is in while it is connected.
   *
   * @param group   The group in which the message has been sent.
   * @param sender  The sender of the message.
   * @param message The text of message sent.
   */
  @Deprecated
  default void messageReceived(Group group, User sender, String message) {
  }

  /**
   * Called when a message is sent from a user to a group the Skype account is in while it is connected.
   *
   * @param group   The group in which the message has been sent.
   * @param sender  The sender of the message.
   * @param message The object of message sent.
   */
  default void messageReceived(Group group, User sender, Message message) {
  }

  /**
   * Called when a message is edited by a user in a group the Skype account is in while it is connected.
   *
   * @param group   The group in which the message has been edit.
   * @param sender  The sender of the message.
   * @param message The object of message sent.
   */
  default void messageEdited(Group group, User sender, Message message) {
  }

  /**
   * Called when a message is removed by a user in a group the Skype account is in while it is connected.
   *
   * @param group   The group in which the message has been remove.
   * @param sender  The sender of the message.
   * @param message The object of message sent.
   */
  default void messageRemoved(Group group, User sender, Message message) {
  }


}
