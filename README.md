# JavaSkype

### As of 2019/04/09, MSNP24 is gone for good. This library stopped working and won't be updated anymore. Thanks to the [msndevs](https://github.com/msndevs) team, and to [dx](https://github.com/dequis) and [dos-freak](https://github.com/leecher1337) in particular for all their work on reverse-engineering the Skype client and making it possible to create libraries like this. Moving forward, you can now try to use libraries that support the new Skype Web API, for example [skype4pidgin](https://github.com/EionRobb/skype4pidgin/tree/master/skypeweb), or create an [official Skype bot](https://dev.skype.com/bots).

## Recent changes
- Now supports Microsoft accounts ([#24](https://github.com/Delthas/JavaSkype/issues/24))

## Introduction

JavaSkype is a lightweight and comprehensive API for Skype, using the MSNP24 protocol, and Java 8.

This API lets you:
- Send and receive private Skype messages
- Send and receive group chat Skype messages
- Send, receive, accept, and deny friend requests
- Get your contact list, know your contacts presence, and change yours
- Add/remove users to a group
- Block and unblock contacts
- Get information about your contacts, such as their name, and their avatar

## Install

JavaSkype requires Java >= 8 to run. You can get this library using Maven by adding this to your ```pom.xml```:

```xml
 <dependencies>
    <dependency>       
           <groupId>fr.delthas</groupId>
           <artifactId>javaskype</artifactId>
           <version>1.0.23</version>
    </dependency>
</dependencies>
```

## Quick example

This library is Object-oriented: the main Skype object will give you User and Group objects, on which you will call methods in order to do stuff. Let's have a look at how to interact with the library.

```java
Skype skype = new Skype("myusername", "mypassword");
try {
  // If you want to report a bug, enable logging
  // Skype.setDebug(path);
  skype.connect(); // Will block until we're connected
} catch (IOException e) {
  System.err.println("An error occured while connecting...");
  e.printStackTrace();
}

// Set the error callback (will be called if any exception is thrown)
// When it is called, you'll be automatically disconnected
skype.setErrorListener(Exception::printStackTrace);

// We're connected and ready to go

// Say hello to all your contacts
for (User user : skype.getContacts()) {
  user.sendMessage("Hi, " + user.getDisplayName() + ", what's up?");
}

// Better: let's say hello whenever a user connects
skype.addUserPresenceListener((user, oldPresence, presence) -> {
  if (oldPresence == Presence.OFFLINE) {
    user.sendMessage("Hi, " + user.getFirstname());
  }
});

// Create a simple time bot
skype.addUserMessageListener((user, message) -> {
  if (message.toLowerCase().contains("time")) {
    user.sendMessage("The current time is: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
  }
});

// Invite everyone to the groups you own/are admin on
for (Group group : skype.getGroups()) {
  if (group.isSelfAdmin()) {
    for (User user : skype.getContacts()) {
      group.addUser(user, Role.USER);
    }
  }
}

// Kick a user whenever he rants about Java
skype.addGroupMessageListener((group, user, message) -> {
  if (message.toLowerCase().contains("java") && message.toLowerCase().contains("bad")) {
    group.removeUser(user);
    group.changeTopic("No Java bashing allowed here!");
  }
});

// Let's disconnect
skype.disconnect();
```

## Documentation

The main entry points are :
- the Skype object
- the listeners you can put on the Skype object
- the User and Group objects you get from Skype or from the listeners

The javadoc for the API is located at: http://www.javadoc.io/doc/fr.delthas/javaskype/

Note that this API doesn't support multithreaded calls: if you want to go for some multithreading, you will have to handle the synchronization yourself. You can however run multiple Skype accounts simultaneously, on the same or different threads.

## Building

Simply run ```mvn install```.

## Misceallenous

### Tech

JavaSkype uses a very small set of libraries in order to run:

* [json](http://mvnrepository.com/artifact/org.json/json) - Parse JSON responses from the Skype API
* [JSoup](https://jsoup.org) - A lightweight and powerful library to parse HTML documents
* [JUnit](http://junit.org) - The famous testing library

Several libraries have been used during the making of this library:

* [skylogin](https://github.com/msndevs/skylogin) - I originally used this library via JNI but I've rewritten it in Java in UicConnector.java
* [pyskype](https://github.com/uunicorn/pyskype) - I've used this library to understand how to make a MSNP24 client
* [msndevs-wiki](https://github.com/msndevs/protocol-docs/wiki) - This wiki has helped me understand the MSNP24 protocol

### License

MIT
