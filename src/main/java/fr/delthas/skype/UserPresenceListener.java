package fr.delthas.skype;


/**
 * A listener for changes to a user presence.
 *
 * @see Presence
 *
 */
@FunctionalInterface
public interface UserPresenceListener {

  /**
   * Called when the presence of a user changes (becomes connected, or disconnected, ...)
   *
   * @param user The user whom presence changed.
   * @param oldPresence The presence the user had before it changed.
   * @param presence The new presence of the user.
   */
  void presenceChanged(User user, Presence oldPresence, Presence presence);

}
