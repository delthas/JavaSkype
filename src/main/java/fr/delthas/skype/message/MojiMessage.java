package fr.delthas.skype.message;

/**
 * Message with moji
 */
public class MojiMessage extends AbstractMessage {

  public MojiMessage(String id, String html) {
    super(id, html);
  }

  public MojiMessage(String id, String html, boolean isMe) {
    super(id, html, isMe);
  }

}
