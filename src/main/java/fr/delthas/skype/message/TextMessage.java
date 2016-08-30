package fr.delthas.skype.message;

/**
 * Simple text message
 */
public class TextMessage extends AbstractMessage {
  private boolean isMe;
  private String quote;

  public TextMessage(String id, String html) {
    super(id, html);
  }

  public TextMessage(String id, String html, boolean isMe) {
    this(id, html);
    this.isMe = isMe;
  }

  public TextMessage(String id, String html, boolean isMe, String quote) {
    this(id, html, isMe);
    this.quote = quote;
  }

  public String getQuote() {
    return quote;
  }

  public void setQuote(String quote) {
    this.quote = quote;
  }

  public boolean isMe() {
    return isMe;
  }
}
