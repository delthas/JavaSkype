package fr.delthas.skype;

import fr.delthas.skype.message.Message;

/**
 * Interface for chats
 */
public interface Chat {

  /**
   * Get Skype client
   *
   * @return skype object
   */
  Skype getSkype();

  /**
   * Get identity of a chat
   *
   * @return string
   */
  String getIdentity();

  /**
   * Type of chat
   *
   * @return ChatType enum item
   */
  ChatType getType();

  /**
   * Send message to chat
   *
   * @param message object of one of messages types
   */
  void sendMessage(Message message);

  /**
   * Edit message to chat.
   *
   * Work only with text message.
   *
   * @param message object of one of messages types
   */
  void editMessage(Message message);

  /**
   * Remove message to chat
   *
   * @param message object of one of messages types
   */
  void removeMessage(Message message);

  /**
   * ChatType enum
   */
  public enum ChatType {
    GROUP,
    USER
  }
}
