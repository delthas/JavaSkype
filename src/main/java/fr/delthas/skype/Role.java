package fr.delthas.skype;

/**
 * A role of a user in a group.
 */
public enum Role {

  /**
   * An admin user: can add/remove users, change the topic, and change users roles.
   */
  ADMIN("admin"),
  /**
   * A regular user: can add users, change the topic, and remove himself from the group.
   */
  USER("user");
  private final String roleString;

  Role(String roleString) {
    this.roleString = roleString;
  }

  static Role getRole(String roleString) {
    for (int i = 0; i < Role.values().length; i++) {
      if (Role.values()[i].roleString.equalsIgnoreCase(roleString)) {
        return Role.values()[i];
      }
    }
    throw new IllegalArgumentException("Unknown role string: " + roleString);
  }

  String getRoleString() {
    return roleString;
  }

}
