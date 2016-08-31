package fr.delthas.skype.message;

/**
 * Simple text message
 */
public class TextMessage extends AbstractMessage {
  private boolean isMe;
  private boolean hasQuotes;

  public TextMessage(String id, String html) {
    super(id, html);
  }

  public TextMessage(String id, String html, boolean isMe) {
    this(id, html);
    this.isMe = isMe;
  }

  public TextMessage(String id, String html, boolean isMe, boolean hasQuotes) {
    this(id, html, isMe);
    this.hasQuotes = hasQuotes;
  }

  public boolean hasQuotes() {
    return hasQuotes;
  }

  public void setHasQuotes(boolean hasQuotes) {
    this.hasQuotes = hasQuotes;
  }

  public boolean isMe() {
    return isMe;
  }

  @Override
  public String toString() {
    return "TextMessage{" +
        "isMe=" + isMe +
        ", hasQuotes=" + hasQuotes +
        "} " + super.toString();
  }
}
