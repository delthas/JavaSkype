package fr.delthas.skype;

/**
 * A root listener for new messages sent to a Skype account.
 */
public interface MessageListener {
  enum MessageEvent {
    RECEIVED,
    EDITED,
    REMOVED
  }
}
