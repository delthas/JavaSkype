package fr.delthas.skype.message;

/**
 * Message with video
 */
public class VideoMessage extends AbstractMessage {

  public VideoMessage(String id, String html) {
    super(id, html);
  }

  public VideoMessage(String id, String html, boolean isMe) {
    super(id, html, isMe);
  }

}
