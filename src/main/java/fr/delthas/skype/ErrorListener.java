package fr.delthas.skype;

import java.io.IOException;

/**
 * An IO exception listener for the Skype interface.
 *
 */
public interface ErrorListener {

  /**
   * Called whenever an error happens in the Skype interface.
   * <p>
   * Note: This method will immediately be called, and in the same thread, of any error that occurs in the Skype interface.
   *
   * @param e The error thrown by the Skype interface.
   */
  public void error(IOException e);

}
