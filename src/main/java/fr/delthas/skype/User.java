package fr.delthas.skype;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A Skype account.
 * <p>
 * All information will be updated as updates are received (this object <b>is NOT</b> an immutable view/snapshot of a user account).
 */
public class User {
  private final Skype skype;
  private final String username;
  private String firstname;
  private String lastname;
  private String mood;
  private String country;
  private String city;
  private String displayName;
  private String avatarUrl;
  private String liveUsername;
  private Presence presence = Presence.OFFLINE;
  
  User(Skype skype, String username) {
    this.skype = skype;
    this.username = username;
  }
  
  /**
   * Blocks this user (without reporting the account).
   */
  public void block() {
    skype.block(this);
  }
  
  /**
   * Unblocks this user.
   */
  public void unblock() {
    skype.unblock(this);
  }
  
  /**
   * Sends a contact request to this account.
   *
   * @param greeting The message to send in the contact request.
   */
  public void sendContactRequest(String greeting) {
    skype.sendContactRequest(this, greeting);
  }
  
  /**
   * Removes this user from the list of contacts of the Skype account. If the user isn't a contact, nothing happens.
   */
  public void removeFromContacts() {
    skype.removeFromContacts(this);
  }
  
  /**
   * Sends a message to this user.
   *
   * @param message The message to send to this user.
   */
  public void sendMessage(String message) {
    skype.sendUserMessage(this, message);
  }
  
  /**
   * @return The username of this user.
   */
  public String getUsername() {
    return username;
  }
  
  /**
   * @return The display name ("pretty" name) of this user.
   */
  public String getDisplayName() {
    if (displayName != null) {
      return displayName;
    }
    if (firstname != null) {
      if (lastname != null) {
        return firstname + " " + lastname;
      }
      return firstname;
    }
    return username;
  }
  
  void setDisplayName(String displayName) {
    if (displayName == null || displayName.isEmpty()) {
      return;
    }
    this.displayName = displayName;
  }
  
  /**
   * @return The first name of this user, or null if not visible.
   */
  public String getFirstname() {
    return firstname;
  }
  
  /**
   * @return The last name of this user, or null if not visible.
   */
  public String getLastname() {
    return lastname;
  }
  
  /**
   * @return The mood (status) of this user, or null if not visible.
   */
  public String getMood() {
    return mood;
  }
  
  void setMood(String mood) {
    if (mood == null || mood.isEmpty()) {
      return;
    }
    this.mood = mood;
  }
  
  /**
   * @return Two letters identifying this user's country (e.g. us), or null if not visible.
   */
  public String getCountry() {
    return country;
  }
  
  void setCountry(String country) {
    if (country == null || country.isEmpty()) {
      return;
    }
    this.country = country;
  }
  
  /**
   * @return The city of this user, or null if not visible.
   */
  public String getCity() {
    return city;
  }
  
  void setCity(String city) {
    if (city == null || city.isEmpty()) {
      return;
    }
    this.city = city;
  }
  
  /**
   * Fetches and returns the avatar of the user, or null if the avatar is not visible.
   * <p>
   * The avatar is the data of a jpeg (compressed) image that can be transformed into a Image by several methods such as {@link java.awt.Toolkit#createImage(byte[])}, {@link ImageIO#read(InputStream)}, {@link javax.swing.ImageIcon#ImageIcon(byte[])}.
   * <p>
   * Use {@link #getAvatarImage()} to get a {@link BufferedImage} directly (uses {@link ImageIO} internally).
   *
   * @return The avatar (account picture) of this user, as a byte array, or null if not visible.
   * @see #getAvatarImage()
   */
  public byte[] getAvatar() {
    return skype.getAvatar(this);
  }
  
  /**
   * Fetches and returns the avatar of the user as a {@link BufferedImage}, or null if the avatar is not visible.
   * <p>
   * This simply calls {@link #getAvatar()} and parses it as a {@link BufferedImage} with {@link ImageIO#read(InputStream)}.
   *
   * @return The avatar (account picture) of this user, or null if not visible or if there's an image parsing error.
   * @see #getAvatar()
   */
  public BufferedImage getAvatarImage() {
    byte[] bytes = getAvatar();
    if (bytes == null) {
      return null;
    }
    try (ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
      return ImageIO.read(is);
    } catch (IOException ignore) {
      return null;
    }
  }
  
  /**
   * Returns an URL (http/https) to fetch the avatar of this user (with a GET request), or null if it is not visible.
   * <p>
   * The server will respond with a 200 response if the avatar is visible, with the bytes of a jpeg image in it. <b>Sometimes, in very rare cases, the URL will not be null, but the server will respond with a 403 error code. This will always happen for the same accounts but is very rare.</b>
   *
   * @return The url for the avatar (account picture) of this user, or null if not visible from this account with an URL.
   * @see #getAvatar()
   * @see #getAvatarImage()
   */
  public String getAvatarUrl() {
    return avatarUrl;
  }
  
  void setAvatarUrl(String avatarUrl) {
    if (avatarUrl == null || avatarUrl.isEmpty()) {
      return;
    }
    this.avatarUrl = avatarUrl;
  }
  
  /**
   * @return The presence of this user
   * @see Presence
   */
  public Presence getPresence() {
    return presence;
  }
  
  void setPresence(Presence presence) {
    setPresence(presence, true);
  }
  
  void setPresence(String presenceString) {
    setPresence(Presence.getPresence(presenceString), true);
  }
  
  void setFirstName(String firstname) {
    if (firstname == null || firstname.isEmpty()) {
      return;
    }
    this.firstname = firstname;
  }
  
  void setLastName(String lastname) {
    if (lastname == null || lastname.isEmpty()) {
      return;
    }
    this.lastname = lastname;
  }
  
  void setPresence(Presence presence, boolean triggerListeners) {
    if (presence != this.presence) {
      Presence oldPresence = this.presence;
      this.presence = presence;
      if (triggerListeners) {
        skype.userPresenceChanged(this, oldPresence, presence);
      }
    }
  }
  
  String getLiveUsername() {
    return liveUsername;
  }

  void setLiveUsername(String liveUsername) {
    this.liveUsername = liveUsername;
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (username == null ? 0 : username.hashCode());
    return result;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof User)) {
      return false;
    }
    User other = (User) obj;
    if (username == null) {
      if (other.username != null) {
        return false;
      }
    } else if (!username.equals(other.username)) {
      return false;
    }
    return true;
  }
  
  @Override
  public String toString() {
    return "User: " + getUsername();
  }
}
