package fr.delthas.skype.message;

/**
 * Message with contact
 */
public class ContactMessage extends AbstractMessage {

  public ContactMessage(String id, String html) {
    super(id, html);
  }

  public ContactMessage(String id, String html, boolean isMe) {
    super(id, html, isMe);
  }

}
