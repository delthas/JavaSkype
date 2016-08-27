package fr.delthas.skype.message;

/**
 * Message with picture
 */
public class PictureMessage extends AbstractMessage {

  public PictureMessage(String id, String html) {
    super(id, html);
  }

  public PictureMessage(String id, String html, boolean isMe) {
    super(id, html, isMe);
  }

}
