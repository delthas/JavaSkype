package fr.delthas.skype.message;

import org.jsoup.Jsoup;

/**
 * Abstract class for all type of message with all common field and methods
 */
public abstract class AbstractMessage implements Message {

  private String id;
  private MessageType type;
  private String html;
  private String text;
  private boolean isMe;

  public AbstractMessage(String id, String html) {
    this.id = id;
    this.html = html;
    this.text = Jsoup.parseBodyFragment(html, "").text();
    this.type = MessageType.getTypeByClass(getClass().getSimpleName());
  }

  public AbstractMessage(String id, String html, boolean isMe) {
    this(id, html);
    this.isMe = isMe;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public MessageType getType() {
    return type;
  }

  protected void setType(MessageType type) {
    this.type = type;
  }

  public String getHtml() {
    return html;
  }

  public void setHtml(String html) {
    this.html = html;
  }

  public String getText() {
    return text;
  }

  public boolean isMe() {
    return isMe;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " {" +
        "id='" + id + '\'' +
        ", type=" + type +
        ", html='" + html + '\'' +
        ", text='" + text + '\'' +
        ", isMe=" + isMe +
        '}';
  }
}
