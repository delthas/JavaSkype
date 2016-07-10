package fr.delthas.skype;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.net.ssl.SSLSocketFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

class NotifConnector {

  private static class Packet {

    public final String command;
    public final String params;
    public final String body;

    public Packet(String command, String params, String body) {
      this.command = command;
      this.params = params;
      this.body = body;
    }

    @Override
    public String toString() {
      return String.format("Command: %s Params: %s Body: %s", command, params, body);
    }

  }

  private static final String EPID = generateEPID(); // generate EPID at runtime
  private static final String DEFAULT_SERVER_HOSTNAME = "s.gateway.messenger.live.com";
  private static final int DEFAULT_SERVER_PORT = 443;
  private static final Pattern patternFirstLine = Pattern.compile("([A-Z]+|\\d+) \\d+ ([A-Z]+(?:\\\\[A-Z]+)?) (\\d+)");
  private static final Pattern patternHeaders = Pattern.compile("\\A(?:(?:Set-Registration: (.+)|[A-Za-z\\-]+: .+)\\R)*\\R");
  private static final Pattern patternXFR = Pattern.compile("([a-zA-Z0-9\\.]+):(\\d+)");
  private static final long pingInterval = 30 * 1000000000L; // seconds
  private long lastMessageSentTime;
  private Thread pingThread;
  private final DocumentBuilder documentBuilder;
  private final Skype skype;
  private final String username, password;
  private boolean disconnectRequested = false;
  private Socket socket;
  private BufferedWriter writer;
  private BufferedInputStream inputStream;
  private int sequenceNumber;
  private String registration;
  private CountDownLatch connectLatch = new CountDownLatch(1);

  public NotifConnector(Skype skype, String username, String password) {
    this.skype = skype;
    this.username = username;
    this.password = password;
    try {
      documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      // Should never happen, throw RE if it does
      throw new RuntimeException(e);
    }
  }

  private void processPacket(Packet packet) throws IOException {
    // System.out.println("<<<" + packet.command + " " + packet.params + "\n" + packet.body);
    switch (packet.command) {
      case "GET":
        if (packet.params.equals("MSGR")) {
          String mainNode;
          try {
            Document doc = getDocument(packet.body);
            mainNode = doc.getFirstChild().getNodeName();
          } catch (ParseException e) {
            break;
          }
          switch (mainNode) {
            case "recentconversations-response":
              NodeList conversationNodes;
              try {
                Document doc = getDocument(packet.body);
                conversationNodes = doc.getElementsByTagName("conversation");
              } catch (ParseException e) {
                break;
              }
              StringBuilder sb = new StringBuilder("<threads>");
              outer: for (int i = 0; i < conversationNodes.getLength(); i++) {
                Node conversation = conversationNodes.item(i);
                String id = null;
                boolean isThread = false;
                NodeList conversationChildren = conversation.getChildNodes();
                for (int j = 0; j < conversationChildren.getLength(); j++) {
                  Node child = conversationChildren.item(j);
                  if (child.getNodeName().equals("id")) {
                    id = child.getTextContent();
                  } else if (child.getNodeName().equals("thread")) {
                    isThread = true;
                    NodeList threadNodes = child.getChildNodes();
                    for (int k = 0; k < threadNodes.getLength(); k++) {
                      Node threadNode = threadNodes.item(k);
                      // if there's a node named "lastleaveat", we've left this group
                      // do not show it in our group list
                      if (threadNode.getNodeName().equals("lastleaveat")) {
                        continue outer;
                      }
                    }
                  } else if (child.getNodeName().equals("messages")) {
                    if (!child.hasChildNodes()) {
                      // messages is empty, this probably means we've left this group
                      // do not show it in our group list
                      continue outer;
                    }
                  }
                }
                if (id == null || !isThread) {
                  continue outer;
                }
                Group group = (Group) parseEntity(id);
                sb.append("<thread><id>19:").append(group.getId()).append("@thread.skype</id></thread>");
              }
              String body = sb.append("</threads>").toString();
              sendPacket("GET", "MSGR\\THREADS", body);
              break;
            case "threads-response":
              NodeList threadNodes;
              try {
                Document doc = getDocument(packet.body);
                threadNodes = doc.getElementsByTagName("thread");
              } catch (ParseException e) {
                break;
              }
              for (int i = 0; i < threadNodes.getLength(); i++) {
                updateThread(threadNodes.item(i));
              }
              connectLatch.countDown(); // stop blocking: we're connected
              break;
            default:
          }
        }
        break;
      case "SDG":
        if (!packet.body.isEmpty()) {
          FormattedMessage formatted;
          try {
            formatted = FormattedMessage.parseMessage(packet.body);
          } catch (IllegalArgumentException e) {
            // weird message with no interesting content
            break;
          }
          String messageType = formatted.headers.get("Message-Type");
          if (messageType == null) {
            break;
          }
          Object sender = parseEntity(formatted.sender);
          Object receiver = parseEntity(formatted.receiver);
          switch (messageType) {
            case "Text":
            case "RichText":
              if (!(sender instanceof User)) {
                break;
              }
              if (receiver instanceof Group) {
                skype.groupMessageReceived((Group) receiver, (User) sender, getPlaintext(formatted.body));
              } else {
                skype.userMessageReceived((User) sender, getPlaintext(formatted.body));
              }
              break;
            case "ThreadActivity/AddMember":
              List<String> usernames = getXMLFields(formatted.body, "target");
              skype.usersAddedToGroup(usernames.stream().map(username -> (User) parseEntity(username)).collect(Collectors.toList()), (Group) sender);
              break;
            case "ThreadActivity/DeleteMember":
              usernames = getXMLFields(formatted.body, "target");
              skype.usersAddedToGroup(usernames.stream().map(username -> (User) parseEntity(username)).collect(Collectors.toList()), (Group) sender);
              break;
            case "ThreadActivity/TopicUpdate":
              skype.groupTopicChanged((Group) sender, getPlaintext(getXMLField(formatted.body, "value")));
              break;
            case "ThreadActivity/RoleUpdate":
              Document doc = getDocument(formatted.body);
              NodeList targetNodes = doc.getElementsByTagName("target");
              List<Pair<User, Role>> roles = new ArrayList<>(targetNodes.getLength());
              for (int i = 0; i < targetNodes.getLength(); i++) {
                Node targetNode = targetNodes.item(i);
                User user = null;
                Role role = null;
                for (int j = 0; j < targetNode.getChildNodes().getLength(); j++) {
                  Node targetPropertyNode = targetNode.getChildNodes().item(j);
                  if (targetPropertyNode.getNodeName().equals("id")) {
                    user = (User) parseEntity(targetPropertyNode.getTextContent());
                    skype.updateUser(user);
                  } else if (targetPropertyNode.getNodeName().equals("role")) {
                    role = Role.getRole(targetPropertyNode.getTextContent());
                  }
                }
                if (user != null && role != null) {
                  roles.add(new Pair<>(user, role));
                }
              }
              skype.usersRolesChanged((Group) sender, roles);
              break;
            default:
              break;
          }
        }
        break;
      case "NFY":
        switch (packet.params) {
          case "MSGR\\DEL":
            FormattedMessage formatted = FormattedMessage.parseMessage(packet.body);
            ((User) parseEntity(formatted.sender)).setPresence(Presence.OFFLINE);
            break;
          case "MSGR\\PUT":
            formatted = FormattedMessage.parseMessage(packet.body);
            String presenceString = getXMLField(formatted.body, "Status");
            if (presenceString == null) {
              // happens when a user switches from offline to "hidden"
              presenceString = "";
            }
            ((User) parseEntity(formatted.sender)).setPresence(presenceString);
            break;
          case "MSGR\\THREAD":
            Document document = getDocument(packet.body);
            updateThread(document);
            break;
          default:
            break;
        }
        break;
      case "XFR":
        String newAddress = getXMLField(packet.body, "target");
        if (newAddress == null) {
          throw new ParseException();
        }
        Matcher matcherXFR = patternXFR.matcher(newAddress);
        if (!matcherXFR.matches()) {
          throw new ParseException();
        }
        String hostname = matcherXFR.group(1);
        String portString = matcherXFR.group(2);
        int port;
        try {
          port = Integer.parseInt(portString);
        } catch (NumberFormatException e) {
          throw new ParseException(e);
        }
        connectTo(hostname, port);
        break;
      case "CNT":
        String nonce = getXMLField(packet.body, "nonce");
        if (nonce == null) {
          throw new ParseException();
        }
        // we *need* to make sure these are not null to avoid going APPCRASH
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);
        Objects.requireNonNull(nonce);
        String uic = SkyLoginConnector.getUIC(username, password, nonce);
        sendPacket("ATH", "CON\\USER", "<user><uic>" + uic + "</uic><id>" + username + "</id></user>");
        break;
      case "ATH":
        sendPacket("BND", "CON\\MSGR",
            "<msgr><ver>2</ver><client><name>.</name><ver>.</ver><networks>skype</networks></client><epid>" + EPID + "</epid></msgr>");
        break;
      case "BND":
        // apparently no challenge is needed anymore, but skype may put it back in the future
        String challenge = getXMLField(packet.body, "nonce");
        if (challenge != null) {
          String query = Challenge.createQuery(challenge);
          sendPacket("PUT", "MSGR\\CHALLENGE",
              "<challenge><appId>" + Challenge.PRODUCT_ID + "</appId><response>" + query + "</response></challenge>");
        }
        String formattedPublicationBody = String.format(
            "<user><s n=\"IM\"><Status>%s</Status></s><sep n=\"IM\" epid=\"{%s}\"><Capabilities>0:4194560</Capabilities></sep><s n=\"SKP\"><Mood/><Skypename>%s</Skypename></s><sep n=\"SKP\" epid=\"{%s}\"><Version>.</Version><Seamless>true</Seamless></sep></user>",
            skype.getSelf().getPresence().getPresenceString(), EPID, EPID, username, EPID);
        String formattedPublicationMessage = FormattedMessage.format("8:" + username + ";epid={" + EPID + "}", "8:" + username, "Publication: 1.0",
            formattedPublicationBody, "Uri: /user", "Content-Type: application/user+xml");
        sendPacket("PUT", "MSGR\\PRESENCE", formattedPublicationMessage);
        sendPacket("PUT", "MSGR\\SUBSCRIPTIONS",
            "<subscribe><presence><buddies><all /></buddies></presence><messaging><im /><conversations /></messaging></subscribe>");
        List<User> contacts = skype.getContacts();
        StringBuilder contactsStringBuilder = new StringBuilder("<ml l=\"1\"><skp>");
        for (User contact : contacts) {
          contactsStringBuilder.append("<c n=\"");
          contactsStringBuilder.append(contact.getUsername());
          contactsStringBuilder.append("\" t=\"8\"><s l=\"3\" n=\"IM\"/><s l=\"3\" n=\"SKP\"/></c>");
        }
        contactsStringBuilder.append("</skp></ml>");
        String contactsString = contactsStringBuilder.toString();
        sendPacket("PUT", "MSGR\\CONTACTS", contactsString);
        sendPacket("GET", "MSGR\\RECENTCONVERSATIONS", "<recentconversations><start>0</start><pagesize>100</pagesize></recentconversations>");
        break;
      case "OUT":
        // we got disconnected
        skype.error(new IOException("Disconnected: " + packet.body));
        break;
      case "PUT":
        break;
      case "PNG":
        // ignore pong
        break;
      default:
        System.out.println("Received unknown message: " + packet);
    }
  }

  private Packet readPacket() throws IOException, ParseException {
    StringBuilder firstLineBuilder = new StringBuilder();
    byte[] oneByteBuffer = new byte[1];
    boolean crFlag = false;
    while (true) {
      if (inputStream.read(oneByteBuffer) == -1) {
        return null;
      }
      char character = (char) (oneByteBuffer[0] & 0xFF);
      if (crFlag) {
        if (character == '\n') {
          break;
        }
        throw new ParseException();
      }
      if (character == '\n') {
        break;
      }
      if (character == '\r') {
        crFlag = true;
      } else {
        firstLineBuilder.append(character);
      }
    }
    String firstLine = firstLineBuilder.toString();
    Matcher matcherFirstLine = patternFirstLine.matcher(firstLine);
    if (!matcherFirstLine.matches()) {
      throw new ParseException("Error matching message first line: " + firstLine + " ");
    }
    String command = matcherFirstLine.group(1);
    String parameters = matcherFirstLine.group(2);
    String payloadSizeString = matcherFirstLine.group(3);
    int payloadSize;
    try {
      payloadSize = Integer.parseInt(payloadSizeString);
    } catch (NumberFormatException e) {
      throw new ParseException(e);
    }
    byte[] payloadRaw = new byte[payloadSize];
    int bytesRead = 0;
    while (true) {
      int n = inputStream.read(payloadRaw, bytesRead, payloadSize - bytesRead);
      if (n == -1) {
        throw new ParseException();
      }
      bytesRead += n;
      if (bytesRead == payloadSize) {
        break;
      }
    }

    String payload = new String(payloadRaw, StandardCharsets.UTF_8);

    if (command.matches("\\d+")) {
      throw new ParseException("Error message received:\n" + new Packet(command, parameters, payload).toString());
    }

    Matcher matcherHeaders = patternHeaders.matcher(payload);
    if (!matcherHeaders.find()) {
      throw new ParseException();
    }
    String newRegistration = matcherHeaders.group(1);
    if (newRegistration != null) {
      registration = newRegistration;
    }
    String body = payload.substring(matcherHeaders.end());
    return new Packet(command, parameters, body);
  }

  public void connect() throws IOException, InterruptedException {
    lastMessageSentTime = System.nanoTime();
    connectTo(DEFAULT_SERVER_HOSTNAME, DEFAULT_SERVER_PORT);
    new Thread(() -> {
      while (!disconnectRequested) {
        try {
          Packet packet = readPacket();
          if (packet == null) {
            continue;
          }
          processPacket(packet);
        } catch (IOException e) {
          if (disconnectRequested) {
            // there may be errors reading from the closed stream when disconnecting
            // quit without throwing
            return;
          }
          skype.error(e);
          connectLatch.countDown();
          break;
        }
      }
    }, "Skype-Receiver-Thread").start();

    pingThread = new Thread(() -> {
      while (!disconnectRequested) {
        if (System.nanoTime() - lastMessageSentTime > pingInterval) {
          try {
            sendPacket("PNG", "CON", "");
          } catch (IOException e) {
            skype.error(e);
            break;
          }
        }
        try {
          Thread.sleep(pingInterval / 1000000);
        } catch (InterruptedException e) {
          // stop sleeping early
        }
      }
    }, "Skype-Ping-Thread");

    connectLatch.await(); // block until connected

    pingThread.start();
  }

  public void sendUserMessage(User user, String message) throws IOException {
    sendMessage("8:" + user.getUsername(), getSanitized(message));
  }

  public void sendGroupMessage(Group group, String message) throws IOException {
    sendMessage("19:" + group.getId() + "@thread.skype", getSanitized(message));
  }

  public void addUserToGroup(User user, Role role, Group group) throws IOException {
    String body = String.format("<thread><id>19:%s@thread.skype</id><members><member><mri>8:%s</mri><role>%s</role></member></members></thread>",
        group.getId(), user.getUsername(), role.getRoleString());
    sendPacket("PUT", "MSGR\\THREAD", body);
  }

  public void removeUserFromGroup(User user, Group group) throws IOException {
    String body = String.format("<thread><id>19:%s@thread.skype</id><members><member><mri>8:%s</mri></member></members></thread>", group.getId(),
        user.getUsername());
    sendPacket("DEL", "MSGR\\THREAD", body);
  }

  public void changeGroupTopic(Group group, String topic) throws IOException {
    String body =
        String.format("<thread><id>19:%s@thread.skype</id><properties><topic>%s</topic></properties></thread>", group.getId(), getSanitized(topic));
    sendPacket("PUT", "MSGR\\THREAD", body);
  }

  public void changeUserRole(User user, Role role, Group group) throws IOException {
    String body = String.format("<thread><id>19:%s@thread.skype</id><members><member><mri>8:%s</mri><role>%s</role></member></members></thread>",
        group.getId(), user.getUsername(), role.getRoleString());
    sendPacket("PUT", "MSGR\\THREAD", body);
  }

  public void changePresence(Presence presence) throws IOException {
    String formattedPublicationBody = String.format("<user><s n=\"IM\"><Status>" + presence.getPresenceString() + "</Status></s></user>");
    String formattedPublicationMessage = FormattedMessage.format("8:" + username + ";epid={" + EPID + "}", "8:" + username, "Publication: 1.0",
        formattedPublicationBody, "Uri: /user", "Content-Type: application/user+xml");
    sendPacket("PUT", "MSGR\\PRESENCE", formattedPublicationMessage);
  }

  private void sendMessage(String entity, String message) throws IOException {
    String body = FormattedMessage.format("8:" + username + ";epid={" + EPID + "}", entity, "Messaging: 2.0", message,
        "Content-Type: application/user+xml", "Message-Type: RichText");
    sendPacket("SDG", "MSGR", body);
  }

  public synchronized void disconnect() {
    try {
      sendPacket("OUT", "CON", "");
    } catch (IOException e) {
      // we're closing anyway
    }
    disconnectRequested = true;
    pingThread.interrupt();
    connectLatch.countDown();
    if (socket != null) {
      try {
        socket.close();
      } catch (IOException e) {
        // ignore any error during close
      }
    }
  }

  private synchronized void sendPacket(String command, String parameters, String body) throws IOException {
    String headerString = registration != null ? "Registration: " + registration + "\r\n" : "";
    String messageString = String.format("%s %d %s %d\r\n%s\r\n%s", command, ++sequenceNumber, parameters,
        body.getBytes(StandardCharsets.UTF_8).length + 2 + headerString.length(), headerString, body);
    writer.write(messageString);
    writer.flush();
    lastMessageSentTime = System.nanoTime();
    // System.out.println(">>>" + messageString);
  }

  private void connectTo(String hostname, int port) throws IOException {
    if (socket != null) {
      socket.close();
    }
    socket = SSLSocketFactory.getDefault().createSocket(hostname, port);
    writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    inputStream = new BufferedInputStream(socket.getInputStream());
    sequenceNumber = 0;
    sendPacket("CNT", "CON", "<connect><ver>2</ver><agent><os>.</os><osVer>.</osVer><proc>.</proc><lcid>en-us</lcid></agent></connect>");
  }

  private void updateThread(Node threadNode) {
    Group group = null;
    String topic = null;
    Node members = null;
    for (int i = 0; i < threadNode.getChildNodes().getLength(); i++) {
      Node node = threadNode.getChildNodes().item(i);
      if (node.getNodeName().equals("id")) {
        group = (Group) parseEntity(node.getTextContent());
      } else if (node.getNodeName().equals("members")) {
        members = node;
      } else if (node.getNodeName().equals("properties")) {
        for (int j = 0; j < node.getChildNodes().getLength(); j++) {
          Node topicNode = node.getChildNodes().item(j);
          if (topicNode.getNodeName().equals("topic")) {
            topic = topicNode.getTextContent();
          }
        }
      }
    }
    if (group == null || topic == null || members == null) {
      return;
    }
    List<Pair<User, Role>> users = new ArrayList<>(members.getChildNodes().getLength());
    for (int i = 0; i < members.getChildNodes().getLength(); i++) {
      Node memberNode = members.getChildNodes().item(i);
      if (!memberNode.getNodeName().equals("member")) {
        continue;
      }
      User user = null;
      Role role = null;
      for (int j = 0; j < memberNode.getChildNodes().getLength(); j++) {
        Node memberPropertyNode = memberNode.getChildNodes().item(j);
        if (memberPropertyNode.getNodeName().equals("mri")) {
          user = (User) parseEntity(memberPropertyNode.getTextContent());
          skype.updateUser(user);
        } else if (memberPropertyNode.getNodeName().equals("role")) {
          role = Role.getRole(memberPropertyNode.getTextContent());
        }
      }
      if (user != null && role != null) {
        users.add(new Pair<>(user, role));
      }
    }
    group.setTopic(topic);
    group.setUsers(users);
  }

  private Object parseEntity(String rawEntity) {
    // returns a user or a group
    int senderBegin = rawEntity.indexOf(':');
    int network = Integer.parseInt(rawEntity.substring(0, senderBegin));
    int end0 = rawEntity.indexOf('@');
    int end1 = rawEntity.indexOf(';');
    if (end0 == -1) {
      end0 = Integer.MAX_VALUE;
    }
    if (end1 == -1) {
      end1 = Integer.MAX_VALUE;
    }
    int senderEnd = Math.min(end0, end1);
    String name;
    if (senderEnd == Integer.MAX_VALUE) {
      name = rawEntity.substring(senderBegin + 1);
    } else {
      name = rawEntity.substring(senderBegin + 1, senderEnd);
    }
    if (network == 8) {
      return skype.getUser(name);
    } else if (network == 19) {
      return skype.getGroup(name);
    } else {
      throw new IllegalArgumentException();
    }
  }

  private Document getDocument(String XML) throws ParseException {
    try {
      return documentBuilder.parse(new InputSource(new StringReader(XML)));
    } catch (IOException | SAXException e) {
      // IOException should never happen, but treat as ParseException anyway
      throw new ParseException(e);
    }
  }

  private List<String> getXMLFields(String XML, String fieldName) throws ParseException {
    NodeList nodes = getDocument(XML).getElementsByTagName(fieldName);
    List<String> fields = new ArrayList<>(nodes.getLength());
    for (int i = 0; i < nodes.getLength(); i++) {
      fields.add(nodes.item(i).getTextContent());
    }
    return fields;
  }

  private String getXMLField(String XML, String fieldName) throws ParseException {
    List<String> fields = getXMLFields(XML, fieldName);
    if (fields.size() > 1) {
      throw new ParseException();
    }
    if (fields.size() == 0) {
      return null;
    }
    return fields.get(0);
  }

  private static String generateEPID() {
    char[] hexCharacters = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    // EPID format: XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
    char[] EPIDchars = new char[36];
    EPIDchars[8] = '-';
    EPIDchars[13] = '-';
    EPIDchars[18] = '-';
    EPIDchars[23] = '-';
    Random random = new Random();
    for (int i = 0; i < 8; i++) {
      EPIDchars[i] = hexCharacters[random.nextInt(hexCharacters.length)];
    }
    for (int i = 9; i < 13; i++) {
      EPIDchars[i] = hexCharacters[random.nextInt(hexCharacters.length)];
    }
    for (int i = 14; i < 18; i++) {
      EPIDchars[i] = hexCharacters[random.nextInt(hexCharacters.length)];
    }
    for (int i = 19; i < 23; i++) {
      EPIDchars[i] = hexCharacters[random.nextInt(hexCharacters.length)];
    }
    for (int i = 24; i < 36; i++) {
      EPIDchars[i] = hexCharacters[random.nextInt(hexCharacters.length)];
    }
    return new String(EPIDchars);
  }

  private static String getPlaintext(String string) {
    return Jsoup.parseBodyFragment(string).text();
  }

  private static String getSanitized(String raw) {
    if (raw.isEmpty())
      return raw;
    StringBuilder sb = new StringBuilder(raw.length());
    boolean crFlag = false;
    for (int i = 0; i < raw.length(); i++) {
      char c = raw.charAt(i);
      if (c <= 0x1F || (c >= 0x7F && c <= 0x9F)) {
        if (c == '\r') {
          if (crFlag) {
            sb.append('\r').append('\n');
          }
          crFlag = true;
        } else if (c == '\n') {
          sb.append('\r').append('\n');
          crFlag = false;
        } else if (crFlag) {
          sb.append('\r').append('\n');
          crFlag = false;
        }
      } else {
        if (crFlag) {
          sb.append('\r').append('\n');
        }
        crFlag = false;
        sb.append(c);
      }
    }
    if (crFlag) {
      sb.append('\r').append('\n');
    }
    return sb.toString();
  }

}
