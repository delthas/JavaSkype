# JavaSkype

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
           <version>1.0.2</version>
    </dependency>
</dependencies>
```

## Quick example

This library is Object-oriented: the main Skype object will give you User and Group objects, on which you will call methods in order to do stuff. Let's have a look at how to interact with the library.

```java
Skype skype = new Skype("myusername", "mypassword");
try {
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

The javadoc for the API is located at: https://skype.delthas.fr/

Note that this API doesn't support multithreaded calls: if you want to go for some multithreading, you will have to handle the synchronization yourself. You can however run multiple Skype accounts simultaneously, on the same or different threads.

## Building

### Compiling the Java part of the library
Run ```maven package```.

### Compiling the C part of the library (with JNI) (the hard part)
There are 4 possible targets: linux 32 bits, linux 64 bits, windows 32 bits, windows 64 bits. Each of these will produce a shared library in their corresponding target directory. Here are the instructions for a 64 bits linux environment. No need to have a JDK: the headers from Java 8 are already in ./skylogin/java.
```sh
# Get in the skylogin directory
cd skylogin

# Install the basic utils needed for compiling, for example, on Debian:
sudo apt-get install binutils libc6-dev-i36 libc6-dev gcc binutils

# Install gcc-mingw-w64. for example, on Debian:
sudo apt-get install gcc-mingw-w64 --no-install-recommends

# Download and extract OpenSSL (skylogin needs it)
# I have only tried with 1.0.2h (the latest version at the current time)
wget https://www.openssl.org/source/openssl-1.0.2h.tar.gz
# Extract it to the folder "openssl"
tar xf openssl-1.0.2h.tar.gz -C openssl --strip-components=1
rm openssl-*.tar.gz

# Now you're ready to make
# Either make all four libraries (might take quite some time):
make all
# Or make only one of them (choose among linux32, linux64, windows32, windows64)
make windows64

# make clean works if you want to clean up everything
make clean
```

## Misceallenous

### Version
1.0.2

### Tech

JavaSkype uses a very small set of libraries in order to run:

* [json](http://mvnrepository.com/artifact/org.json/json) - Parse JSON responses from the Skype API
* [JSoup](https://jsoup.org) - A lightweight and powerful library to parse HTML documents
* [JUnit](http://junit.org) - The famous testing library
* [skylogin](https://github.com/msndevs/skylogin) - A **C** API to compute a token for Skype authentication (called with JNI)
* [OpenSSL](https://www.openssl.org) - You should know what this is

### Todos

 - Write Tests
 - Add user search
 - Add better error handling and recovery

License
----

MIT
