package fr.delthas.skype;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

/**
 * A conversation between some Skype users.
 * <p>
 * All information will be updated as updates are received (this object <b>is NOT</b> an immutable view/snapshot of a group).
 *
 */
public class Group {

  private final Skype skype;
  private final String id;
  private List<Pair<User, Role>> users;
  private String topic;

  Group(Skype skype, String id) {
    this.skype = skype;
    this.id = id;
    users = Collections.emptyList();
    topic = "";
  }

  /**
   * Sends a message to the group.
   *
   * @param message The message to send.
   */
  public void sendMessage(String message) {
    skype.sendGroupMessage(this, message);
  }

  public String getId() {
    return id;
  }

  /**
   * @return The topic of the group.
   */
  public String getTopic() {
    return topic;
  }

  /**
   * Changes the topic of this group
   *
   * @param topic The topic to set.
   */
  public void changeTopic(String topic) {
    skype.changeGroupTopic(this, topic);
  }

  void setTopic(String topic) {
    this.topic = topic;
  }

  /**
   * @return The list of users in the group with their roles.
   */
  public List<Pair<User, Role>> getUsersWithRoles() {
    return users;
  }

  /**
   * @return The list of users in the group.
   */
  public List<User> getUsers() {
    return users.stream().map(Pair::getFirst).collect(Collectors.toList());
  }

  void setUsers(List<Pair<User, Role>> users) {
    this.users = users;
  }

  /**
   * Adds a user to this group and gives him the specified role. Group admin rights are needed if the role is {@link Role#ADMIN}.
   *
   * @param user The user to add to this group.
   * @param role The role of the newly added user.
   *
   * @return true if the user wasn't in the group, and the Skype account has group admin rights if needed.
   */
  public boolean addUser(User user, Role role) {
    if (!isSelfAdmin() && role == Role.ADMIN) {
      return false;
    }
    // we need to make sure the user isn't in the group to avoid getting an error
    for (Pair<User, Role> pair : users) {
      if (user.equals(pair.getFirst())) {
        return false;
      }
    }
    users.add(new Pair<>(user, role));
    skype.addUserToGroup(user, role, this);
    return true;
  }

  /**
   * Removes a user from this group. Group admin rights are needed.
   *
   * @param user The user to remove from this group.
   *
   * @return true if the Skype account has admin rights, and the user was in the group.
   *
   * @see #isSelfAdmin()
   */
  public boolean removeUser(User user) {
    if (!isSelfAdmin()) {
      return false;
    }
    // we need to make sure the user is in the group to avoid getting an error
    Iterator<Pair<User, Role>> it = users.iterator();
    while (it.hasNext()) {
      Pair<User, Role> pair = it.next();
      if (user.equals(pair.getFirst())) {
        it.remove();
        skype.removeUserFromGroup(user, this);
        return true;
      }
    }
    return true;
  }

  /**
   * Changes the role of a user in this group. Group admin rights are needed.
   *
   * @param user The user whose role is to be changed
   * @param role The new role of the user.
   *
   * @return true if the Skype account has admin rights, and the user was in the group and didn't have this role already.
   *
   * @see #isSelfAdmin()
   */
  public boolean changeUserRole(User user, Role role) {
    if (!isSelfAdmin()) {
      return false;
    }
    // we need to make sure the user is in the group to avoid getting an error
    ListIterator<Pair<User, Role>> it = users.listIterator();
    while (it.hasNext()) {
      Pair<User, Role> pair = it.next();
      if (user.equals(pair.getFirst())) {
        // need to return if it already has the same role to avoid getting an error
        if (role.equals(pair.getSecond())) {
          return false;
        }
        it.remove();
        it.add(new Pair<>(user, role));
        skype.changeUserRole(user, role, this);
        return true;
      }
    }
    return false;
  }

  /**
   * @return true if the Skype account has admin rights on this group.
   */
  public boolean isSelfAdmin() {
    User self = skype.getSelf();
    for (Pair<User, Role> pair : users) {
      if (pair.getFirst().equals(self)) {
        return pair.getSecond() == Role.ADMIN;
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (id == null ? 0 : id.hashCode());
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
    if (!(obj instanceof Group)) {
      return false;
    }
    Group other = (Group) obj;
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    return true;
  }

}
