package fr.delthas.skype;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A Skype interface to receive and send messages via a Skype account.
 * <p>
 * All IO exceptions that might be thrown by any action are catched and passed to the registered error listener and disconnect the interface
 * immediately.
 *
 */
public class Skype {

  private final String username;
  private final String password;

  private List<UserMessageListener> userMessageListeners = new LinkedList<>();
  private List<GroupMessageListener> groupMessageListeners = new LinkedList<>();
  private List<UserPresenceListener> userPresenceListeners = new LinkedList<>();
  private List<GroupPropertiesListener> groupPropertiesListeners = new LinkedList<>();
  private ErrorListener errorListener;

  private NotifConnector notifConnector;
  private WebConnector webConnector;

  private Map<String, Group> groups;
  private List<User> contacts;
  private Map<String, User> users;
  private List<ContactRequest> contactRequests;

  private boolean connected = false;
  private boolean connecting = false;
  private IOException exceptionDuringConnection;

  // --- Public API (except listeners add/remove methods) --- //

  /**
   * Builds a new Skype connection without connecting to anything.
   *
   * @param username The username of the Skype account to connect to.
   * @param password The password of the Skype account to connect to.
   */
  public Skype(String username, String password) {
    this.username = username;
    this.password = password;
  }

  /**
   * Calls {@code connect(Presence.CONNECTED)}.
   *
   * @throws IOException If an error is thrown while connecting.
   * @throws InterruptedException If the connection is interrupted.
   *
   * @see #connect(Presence)
   */
  public void connect() throws IOException, InterruptedException {
    connect(Presence.ONLINE);
  }


  /**
   * Connects the Skype interface. Will block until connected.
   *
   * @param presence The initial presence of the Skype account after connection. Cannot be {@link Presence#OFFLINE}.
   *
   * @throws IOException If an error is thrown while connecting.
   * @throws InterruptedException If the connection is interrupted.
   */
  public void connect(Presence presence) throws IOException, InterruptedException {
    if (presence == Presence.OFFLINE) {
      throw new IllegalArgumentException("Presence can't be set to offline. Use HIDDEN if you want to connect without being visible.");
    }
    if (connecting || connected) {
      return;
    }
    connected = true;
    connecting = true;

    reset();
    // notifConnector depends on webConnector information so load webConnector first
    webConnector.start();

    getSelf().setPresence(presence, false);
    // will block until connected
    notifConnector.connect();

    connecting = false;

    if (exceptionDuringConnection != null) {
      // an exception has been thrown during connection
      throw new IOException(exceptionDuringConnection);
    }
  }

  /**
   * Disconnects the Skype interface.
   * <p>
   * All User, Group, and ContactRequest objects will remain valid for the next connections. Make sure to reconnect (start) before triggering actions
   * from these, however, or they will throw an IllegalStateException.
   */
  public void disconnect() {
    if (connecting || !connected) {
      return;
    }
    connected = false;

    notifConnector.disconnect();
    for (Map.Entry<String, User> user : users.entrySet()) {
      user.getValue().setPresence(Presence.OFFLINE, false);
    }
    reset();
  }


  /**
   * @return The current list of contact requests to this Skype account (snapshot, won't be updated).
   */
  public List<ContactRequest> getContactRequests() {
    ensureConnected();
    return Collections.unmodifiableList(new ArrayList<>(contactRequests));
  }

  /**
   * @return The groups (or threads) the Skype account is currently in (as a snapshot: the list won't be updated).
   */
  public List<Group> getGroups() {
    ensureConnected();
    return Collections.unmodifiableList(new ArrayList<>(groups.values()));
  }

  /**
   * @return The User object representing the Skype account.
   */
  public User getSelf() {
    ensureConnected();
    return getUser(username);
  }

  /**
   * @return The current list of contacts of the account (snapshot, won't be updated).
   *
   */
  public List<User> getContacts() {
    ensureConnected();
    return Collections.unmodifiableList(new ArrayList<>(contacts));
  }

  /**
   * Changes the presence of the Skype account.
   * <p>
   * All presence values are valid except {@link Presence#OFFLINE} : to disconnect, use {@link #disconnect()}.
   *
   * @param presence The new presence of the Skype account.
   * @see Presence
   */
  public void changePresence(Presence presence) {
    if (presence == Presence.OFFLINE) {
      throw new IllegalArgumentException("Presence can't be set to offline. Use HIDDEN if you want to connect without being visible.");
    }
    ensureConnected();
    try {
      notifConnector.changePresence(presence);
    } catch (IOException e) {
      error(e);
    }
  }

  /**
   * @return true if the Skype interface is connected.
   */
  public boolean isConnected() {
    return connected;
  }

  // --- Package-private methods --- //

  User getUser(String username) {
    User user = users.get(username);
    if (user == null) {
      user = new User(this, username);
      users.put(username, user);
    }
    return user;
  }

  Group getGroup(String id) {
    Group group = groups.get(id);
    if (group == null) {
      group = new Group(this, id);
      groups.put(id, group);
    }
    return group;
  }

  void addContact(String username) {
    contacts.add(getUser(username));
  }

  void error(IOException e) {
    if (errorListener != null) {
      errorListener.error(e);
    }
    if (connecting) {
      exceptionDuringConnection = e;
    } else {
      disconnect();
    }
  }

  private void ensureConnected() throws IllegalStateException {
    if (!connected) {
      throw new IllegalStateException("Not connected to Skype!");
    }
  }

  private void reset() {
    notifConnector = new NotifConnector(this, username, password);
    webConnector = new WebConnector(this, username, password);
    groups = new HashMap<>();
    contacts = new LinkedList<>();
    users = new HashMap<>();
    contactRequests = new LinkedList<>();
    exceptionDuringConnection = null;
  }

  // --- Package-private methods that simply call the web connector --- //

  void block(User user) {
    ensureConnected();
    try {
      webConnector.block(user);
    } catch (IOException e) {
      error(e);
    }
  }

  void unblock(User user) {
    ensureConnected();
    try {
      webConnector.unblock(user);
    } catch (IOException e) {
      error(e);
    }
  }

  void sendContactRequest(User user, String greeting) {
    ensureConnected();
    try {
      webConnector.sendContactRequest(user, greeting);
    } catch (IOException e) {
      error(e);
    }
  }

  void removeFromContacts(User user) {
    ensureConnected();
    try {
      webConnector.removeFromContacts(user);
      contacts.remove(user);
    } catch (IOException e) {
      error(e);
    }
  }

  byte[] getAvatar(User user) {
    ensureConnected();
    try {
      return webConnector.getAvatar(user);
    } catch (IOException e) {
      error(e);
      return null;
    }
  }

  void updateUser(User user) {
    if (!users.containsKey(user.getUsername())) {
      try {
        webConnector.updateUser(user);
      } catch (IOException e) {
        error(e);
      }
    }
  }

  void acceptContactRequest(ContactRequest contactRequest) {
    ensureConnected();
    try {
      webConnector.acceptContactRequest(contactRequest);
      contactRequests.remove(contactRequest);
    } catch (IOException e) {
      error(e);
    }
  }

  void declineContactRequest(ContactRequest contactRequest) {
    ensureConnected();
    try {
      webConnector.declineContactRequest(contactRequest);
      contactRequests.remove(contactRequest);
    } catch (IOException e) {
      error(e);
    }
  }

  // --- Package-private methods that simply call the notification connector --- //

  void sendUserMessage(User user, String message) {
    ensureConnected();
    try {
      notifConnector.sendUserMessage(user, message);
    } catch (IOException e) {
      error(e);
    }
  }

  void sendGroupMessage(Group group, String message) {
    ensureConnected();
    try {
      notifConnector.sendGroupMessage(group, message);
    } catch (IOException e) {
      error(e);
    }
  }

  void addUserToGroup(User user, Role role, Group group) {
    ensureConnected();
    try {
      notifConnector.addUserToGroup(user, role, group);
    } catch (IOException e) {
      error(e);
    }
  }

  void removeUserFromGroup(User user, Group group) {
    ensureConnected();
    try {
      notifConnector.removeUserFromGroup(user, group);
    } catch (IOException e) {
      error(e);
    }
  }

  void changeUserRole(User user, Role role, Group group) {
    ensureConnected();
    try {
      notifConnector.changeUserRole(user, role, group);
    } catch (IOException e) {
      error(e);
    }
  }

  void changeGroupTopic(Group group, String topic) {
    ensureConnected();
    try {
      notifConnector.changeGroupTopic(group, topic);
    } catch (IOException e) {
      error(e);
    }
  }

  // --- Listeners call methods --- //

  void userMessageReceived(User sender, String message) {
    updateUser(sender);
    for (UserMessageListener listener : userMessageListeners) {
      listener.messageReceived(sender, message);
    }
  }

  void groupMessageReceived(Group group, User sender, String message) {
    for (GroupMessageListener listener : groupMessageListeners) {
      listener.messageReceived(group, sender, message);
    }
  }

  void userPresenceChanged(User user, Presence oldPresence, Presence presence) {
    for (UserPresenceListener listener : userPresenceListeners) {
      listener.presenceChanged(user, oldPresence, presence);
    }
  }

  void usersAddedToGroup(List<User> users, Group group) {
    for (GroupPropertiesListener listener : groupPropertiesListeners) {
      listener.usersAdded(group, users);
    }
  }

  void usersRemovedFromGroup(List<User> users, Group group) {
    for (GroupPropertiesListener listener : groupPropertiesListeners) {
      listener.usersRemoved(group, users);
    }
  }

  void usersRolesChanged(Group group, List<Pair<User, Role>> newRoles) {
    for (GroupPropertiesListener listener : groupPropertiesListeners) {
      listener.usersRolesChanged(group, newRoles);
    }
  }

  void groupTopicChanged(Group group, String topic) {
    for (GroupPropertiesListener listener : groupPropertiesListeners) {
      listener.topicChanged(group, topic);
    }
  }

  // --- Listeners change methods ---

  /**
   * Adds a user message listener.
   *
   * @param userMessageListener The user message listener to add.
   */
  public void addUserMessageListener(UserMessageListener userMessageListener) {
    userMessageListeners.add(userMessageListener);
  }

  /**
   * Removes a user message listener.
   *
   * @param userMessageListener The user message listener to remove.
   */
  public void removeUserMessageListener(UserMessageListener userMessageListener) {
    userMessageListeners.remove(userMessageListener);
  }

  /**
   * Adds a group message listener.
   *
   * @param groupMessageListener The group message listener to add.
   */
  public void addGroupMessageListener(GroupMessageListener groupMessageListener) {
    groupMessageListeners.add(groupMessageListener);
  }

  /**
   * Removes a group message listener.
   *
   * @param groupMessageListener The group message listener to remove.
   */
  public void removeGroupMessageListener(GroupMessageListener groupMessageListener) {
    groupMessageListeners.remove(groupMessageListener);
  }

  /**
   * Adds a user presence listener.
   *
   * @param userPresenceListener The user presence listener to add.
   */
  public void addUserPresenceListener(UserPresenceListener userPresenceListener) {
    userPresenceListeners.add(userPresenceListener);
  }

  /**
   * Removes a user presence listener.
   *
   * @param userPresenceListener The user presence listener to remove.
   */
  public void removeUserPresenceListener(UserPresenceListener userPresenceListener) {
    userPresenceListeners.remove(userPresenceListener);
  }

  /**
   * Adds a group properties listener.
   *
   * @param groupPropertiesListener The group properties listener to add.
   */
  public void addGroupPropertiesListener(GroupPropertiesListener groupPropertiesListener) {
    groupPropertiesListeners.add(groupPropertiesListener);
  }

  /**
   * Removes a group properties listener.
   *
   * @param groupPropertiesListener The group properties listener to remove.
   */
  public void removeGroupPropertiesListener(GroupPropertiesListener groupPropertiesListener) {
    groupPropertiesListeners.remove(groupPropertiesListener);
  }

  /**
   * Sets an error listener for the Skype interface.
   *
   * @param errorListener The error listener to set.
   */
  public void setErrorListener(ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

}
