# Gist for Android

Gist gives you access to a library of ready built micro-experiences that can be easily dropped into your application without writing a line of code.

## Installation
```gradle
implementation 'sh.bourbon:gist:1.+'
```

## Setup
Initialize Gist inside the application’s `onCreate` method. The Organization Id property can be retrieved from the Gist dashboard.

```kotlin
import android.app.Application
import sh.bourbon.gist.presentation.GistSdk

class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Gist with organization id
        GistSdk.init(this, "Your-Key")
    }
}
```

### User Token
If your app is relying on Gist’s web hook service to trigger in-app messages, a user token must be set. This user token should be generated by your services and set at any point during runtime, ex: login or registration.

```kotlin
GistSdk.setUserToken("Unique-User-Token")
```

## Manually Triggering In-App Messages
Gist gives you the option to programmatically trigger in-app messaging flows within your app.

### Show Message
```kotlin
GistSdk.showMessage("message-id")
```

### Dismiss Message
```kotlin
GistSdk.dismissMessage()
```

## Event Handling
The library exposes a listener which you can hook into, this gives you the option to know when a message is shown, dismissed or when an action occurs within the message.

```kotlin
interface GistListener {
    fun onMessageShown(messageId: String)
    fun onMessageDismissed(messageId: String)
    fun onAction(action: String)
    fun onError(messageId: String)
}
```

## Integrations
Gist enables you to plug in external integrations that trigger in-app messages from external sources.

### A list of available integrations can be found below:
- [Gist Firebase](https://gitlab.com/bourbonltd/gist-firebase-android)