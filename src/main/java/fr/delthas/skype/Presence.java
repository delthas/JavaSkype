package fr.delthas.skype;

/**
 * A presence value, that respresents whether a user is connected, disconnected, away, ...
 */
public enum Presence {
  /**
   * Online / Available (green tick on Skype)
   */
  ONLINE("NLN"),
  /**
   * Away / Be Right Back (orange clock on Skype)
   */
  AWAY("AWY"),
  /**
   * Idle / Absent
   */
  IDLE("IDL"),
  /**
   * Busy / Do Not Disturb (red sign on Skype)
   */
  BUSY("BSY"),
  /**
   * Hidden (connected but not visible to others)
   * <p>
   * For all users except the Skype account you use, {@link #OFFLINE} will be shown instead of this value.
   */
  HIDDEN("HDN"),
  /**
   * Offline / Disconnected
   * <p>
   * The Skype account you use will never have this value. Other users, that are "hidden", will have this presence value instead of {@link #HIDDEN}.
   */
  OFFLINE("");
  private final String presenceString;
  
  Presence(String presenceString) {
    this.presenceString = presenceString;
  }
  
  static Presence getPresence(String presenceString) {
    for (int i = 0; i < Presence.values().length; i++) {
      if (Presence.values()[i].presenceString.equalsIgnoreCase(presenceString)) {
        return Presence.values()[i];
      }
    }
    throw new IllegalArgumentException("Unknown presence string: " + presenceString);
  }
  
  String getPresenceString() {
    return presenceString;
  }
}
