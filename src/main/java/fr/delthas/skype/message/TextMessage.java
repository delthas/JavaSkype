package fr.delthas.skype.message;

/**
 * Simple text message
 */
public class TextMessage extends AbstractMessage {
  public TextMessage(String id, String html) {
    super(id, html);
  }

  public TextMessage(String id, String html, boolean isMe) {
    super(id, html, isMe);
  }
}
