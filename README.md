# Gist for Android

Gist gives you access to a library of ready built micro-experiences that can be easily dropped into your application without writing a line of code.

## Installation
```gradle
implementation 'sh.bourbon:gist:1.+'

repositories {
    maven {
        url 'http://maven.gist.build'
    }
}
```

Note: Make sure to use an x86_64 emulator. x86 is not supported.

## Setup
Initialize Gist inside the application’s `onCreate` method. A shared Gist instance can be fetched using `GistSdk.getInstance()`.
When using Kotlin, the shared instanced can also be accessed directly by calling the `GistSdk` object. 
The Organization Id property can be retrieved from the Gist dashboard.

```kotlin
import android.app.Application
import sh.bourbon.gist.presentation.GistSdk

class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Gist with organization id
        GistSdk.getInstance().init(this, "your-organization-id")
    }
}
```

### User Token
If your app is relying on Gist’s web hook service to trigger in-app messages, a user token must be set. This user token should be generated by your services and set at any point during runtime, ex: login or registration.

```kotlin
GistSdk.getInstance().setUserToken("unique-user-token")
```
To clear the user token:
```kotlin
GistSdk.getInstance().clearUserToken()
```

### Broadcasts
Broadcasts enable you to receive messages based on topics the client is subscribed to.

### Subscribing
```kotlin
GistSdk.getInstance().subscribeToTopic("announcements")
```

### Unsubscribe
```kotlin
GistSdk.getInstance().unsubscribeFromTopic("announcements")
```

### Clear All Topics
```kotlin
GistSdk.getInstance().clearTopics()
```

## Manually Triggering In-App Messages
Gist gives you the option to programmatically trigger in-app messaging flows within your app.

### Show Message
```kotlin
val message = Message("artists")
GistSdk.getInstance().showMessage(message)
```

### Adding Message Properties
```kotlin
val mainRouteProperties = mutableMapOf<String, Any?>()
mainRouteProperties["property-key"] = "Hello"
val message = Message(messageId = "artists", properties = mainRouteProperties)
```
Note: Properties also support `data` classes

### Dismiss Message
```kotlin
GistSdk.getInstance().dismissMessage()
```

## Event Handling
The library exposes a listener which you can hook into, this gives you the option to know when a message is shown, dismissed or when an action occurs within the message.

```kotlin
interface GistListener {
    fun onMessageShown(message: Message)
    fun onMessageDismissed(message: Message)
    fun onError(message: Message)
    fun onAction(currentRoute: String, action: String)
}
```

## Integrations
Gist enables you to plug in external integrations that trigger in-app messages from external sources.

### A list of available integrations can be found below:
- [Gist Firebase](https://gitlab.com/bourbonltd/gist-firebase-android)
