package fr.delthas.skype.message;

/**
 * Message with file
 */
public class FileMessage extends AbstractMessage {
  public FileMessage(String id, String html) {
    super(id, html);
  }

  public FileMessage(String id, String html, boolean isMe) {
    super(id, html, isMe);
  }
}
