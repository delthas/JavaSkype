package fr.delthas.skype.message;

/**
 * Interface for messages`
 */
public interface Message {

  public static String X_URL_THUMBNAIL = "/URIObject/@url_thumbnail";
  public static String X_URL = "/URIObject/@uri";
  public static String X_TYPE = "/URIObject/@type";
  public static String X_ORIGINAL_NAME = "/URIObject/OriginalName/@v";

  String getId();

  MessageType getType();
}
