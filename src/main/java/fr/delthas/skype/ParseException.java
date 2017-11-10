package fr.delthas.skype;

import java.io.IOException;

/**
 * An error while parsing the Skype input stream.
 */
@SuppressWarnings("javadoc")
public class ParseException extends IOException {
  private static final long serialVersionUID = 5609974238032149730L;
  
  public ParseException() {
    super();
  }
  
  public ParseException(String message, Throwable cause) {
    super(message, cause);
  }
  
  public ParseException(String message) {
    super(message);
  }
  
  public ParseException(Throwable cause) {
    super(cause);
  }
}
