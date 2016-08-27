package fr.delthas.skype.message;

/**
 * Message with unknown type
 */
public class UnknownMessage extends AbstractMessage {

  public UnknownMessage(String id, String html) {
    super(id, html);
  }

  public UnknownMessage(String id, String html, boolean isMe) {
    super(id, html, isMe);
  }

}
