package fr.delthas.skype.message;

import static fr.delthas.skype.Constants.*;

/**
 * Skype message types
 */
public enum MessageType {
  TEXT("Text", HEADER_RICH_TEXT, TextMessage.class),
  PICTURE("Picture", HEADER_PICTURE, PictureMessage.class),
  FILE("File", HEADER_FILE, FileMessage.class),
  VIDEO("Video", HEADER_VIDEO, VideoMessage.class),
  CONTACT("Contact", HEADER_CONTACT, ContactMessage.class),
  MOJI("Moji", HEADER_MOJI, MojiMessage.class),
  UNKNOWN("Unknown", "Unknown", UnknownMessage.class);

  private String name;
  private String headerType;
  private Class<? extends Message> clazz;

  MessageType(String name, String headerType, Class<? extends Message> clazz) {
    this.name = name;
    this.headerType = headerType;
    this.clazz = clazz;
  }

  public String getHeaderType() {
    return headerType;
  }

  public String getName() {
    return name;
  }

  public Class<? extends Message> getClazz() {
    return clazz;
  }

  public static MessageType getTypeByClass(String className) {
    for (MessageType type : MessageType.values()) {
      if (type.getClazz().getSimpleName().equals(className)) {
        return type;
      }
    }
    return UNKNOWN;
  }

  public static MessageType getTypeByClass(Message message) {
    if (message != null) {
      String className = message.getClass().getSimpleName();
      return getTypeByClass(className);
    }
    return UNKNOWN;
  }

  public static MessageType getTypeByHeaderType(String headerType) {
    if (HEADER_TEXT.equals(headerType)) return TEXT;
    for (MessageType type : MessageType.values()) {
      if (type.getHeaderType().equals(headerType)) {
        return type;
      }
    }
    return UNKNOWN;
  }
}
