package fr.delthas.skype;

import java.util.List;

/**
 * A listener for changed to a group properties, such as users added or removed.
 *
 */
public interface GroupPropertiesListener {

  /**
   * Called when users are added to a group the Skype account is in while it is connected.
   *
   * @param group The group in which the users have been added.
   * @param users The users which have been added.
   */
  void usersAdded(Group group, List<User> users);

  /**
   * Called when users are removed from a group the Skype account is in while it is connected.
   *
   * @param group The group from which the user has been removed.
   * @param users The user which have been removed.
   */
  void usersRemoved(Group group, List<User> users);

  /**
   * Called when the topic of a group is changed.
   *
   * @param group The group whose topic has been changed.
   * @param topic The new topic of the group.
   */
  void topicChanged(Group group, String topic);

  /**
   * Called when some users roles are changed in a group the Skype account is in while it is connected.
   *
   * @param group The group in which some users roles have changed.
   * @param newRoles The list of the new roles of the users whose roles have changed.
   */
  void usersRolesChanged(Group group, List<Pair<User, Role>> newRoles);

}
