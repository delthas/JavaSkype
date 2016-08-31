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


  public AbstractMessage(String id, String html) {
    this.id = id;
    this.html = html;
    this.text = Jsoup.parseBodyFragment(html, "").text();
    this.type = MessageType.getTypeByClass(getClass().getSimpleName());
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
    this.text = Jsoup.parseBodyFragment(html, "").text();
  }

  public String getText() {
    return text;
  }

  public void empty() {
    setHtml("");
  }

  public boolean isEmpty() {
    return html.isEmpty();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " {" +
        "id='" + id + '\'' +
        ", type=" + type +
        ", html='" + html + '\'' +
        ", text='" + text + '\'' +
        '}';
  }
}
