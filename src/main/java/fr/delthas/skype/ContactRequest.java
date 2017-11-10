package fr.delthas.skype;

/**
 * A contact request from a Skype account.
 */
public class ContactRequest {
  private Skype skype;
  private String username;
  private String greeting;
  private boolean processed = false;
  
  ContactRequest(Skype skype, String username, String greeting) {
    this.skype = skype;
    this.username = username;
    this.greeting = greeting;
  }
  
  /**
   * Accepts the contact request. Ignored if already accepted or declined.
   */
  public void accept() {
    if (processed) {
      return;
    }
    skype.acceptContactRequest(this);
    processed = true;
  }
  
  /**
   * Declines the contact request. Ignored if already accepted or declined.
   */
  public void decline() {
    if (processed) {
      return;
    }
    skype.declineContactRequest(this);
    processed = true;
  }
  
  /**
   * @return The user that sent the contact request.
   */
  public User getUser() {
    return skype.getUser(username);
  }
  
  /**
   * @return The message of the user sending the contact request.
   */
  public String getGreeting() {
    return greeting;
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
    if (!(obj instanceof ContactRequest)) {
      return false;
    }
    ContactRequest other = (ContactRequest) obj;
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
    return "Contact Request: User: " + username + " Greeting: " + greeting;
  }
}
