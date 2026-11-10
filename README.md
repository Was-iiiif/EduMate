# EduMate

> **Learn. Practice. Evaluate. Grow.**

EduMate is an Android learning companion designed to help students study
through AI-assisted conversations, receive structured feedback on their
answers, and keep track of their learning progress. It combines a
conversational learning experience with answer evaluation, chat history,
account-based cloud backup, and practical learning reminders.

------------------------------------------------------------------------

## Table of Contents

-   [Overview](#overview)
-   [Features](#features)
-   [Technology Stack](#technology-stack)
-   [Application Architecture](#application-architecture)
-   [AI and Gemini Integration](#ai-and-gemini-integration)
-   [Authentication and Cloud Data](#authentication-and-cloud-data)
-   [Chat Backup and Restoration](#chat-backup-and-restoration)
-   [Offline Behaviour](#offline-behaviour)
-   [Daily Learning Reminder](#daily-learning-reminder)
-   [Profile Picture](#profile-picture)
-   [Project Structure](#project-structure)
-   [Requirements](#requirements)
-   [Setup and Installation](#setup-and-installation)
-   [Firebase Configuration](#firebase-configuration)
-   [Cloudflare Worker Configuration](#cloudflare-worker-configuration)
-   [Build and Run](#build-and-run)
-   [Permissions](#permissions)
-   [Privacy and Security](#privacy-and-security)
-   [Troubleshooting](#troubleshooting)
-   [Development Notes](#development-notes)
-   [Contributing](#contributing)
-   [License](#license)

------------------------------------------------------------------------

## Overview

EduMate aims to make independent learning more structured and
accessible. A learner can ask questions, discuss academic topics, submit
answers for evaluation, review previous conversations, and return to
their learning history after signing in again.

The application uses Firebase Authentication to identify users, Cloud
Firestore to back up account-associated learning data, and a local
SQLite database to keep app data available on the device. AI requests
are routed through a Cloudflare Worker rather than exposing the Gemini
API secret in the Android application.

## Features

### AI-assisted learning

-   Conversational learning interface powered by Google Gemini.
-   Ask questions and explore academic topics through chat.
-   Receive AI-generated feedback and evaluations for submitted answers.
-   Review learning conversations from the app's history.
-   AI requests are routed through the configured Cloudflare Worker.

### Answer evaluation

-   Submit an answer for AI-assisted evaluation.
-   Receive structured feedback, including evaluation scores and written
    comments, according to the app's evaluation flow.
-   Store evaluation results alongside the associated learning session.
-   Review previous evaluations when their session is restored.

### Accounts and authentication

-   Firebase Authentication integration.
-   Supports the authentication methods enabled and implemented in the
    app, including email/password, Google sign-in, and anonymous guest
    access.
-   User-associated cloud data is scoped by the Firebase Authentication
    UID.

### Chat history and cloud backup

-   Store learning sessions, messages, and evaluations locally in
    SQLite.
-   Back up account-associated sessions, messages, and evaluations to
    Cloud Firestore.
-   Restore a previously signed-in account's cloud history after
    authentication.
-   Keep local data available when a network connection is unavailable.
-   Retry pending synchronization through the app's existing
    synchronization workflow.

Cloud backup depends on a successful Firebase sign-in, valid Firestore
configuration and rules, and network availability. A local record should
not be considered cloud-backed until its upload has succeeded.

### Connectivity awareness

-   Detect changes in network connectivity.
-   Show an offline message when internet access is unavailable.
-   Keep the existing synchronization/reconnection workflow separate
    from the offline alert mechanism.

A connected network does not always guarantee internet access. The app
should use validated internet connectivity when determining whether it
can reach online services.

### Daily learning reminder

-   Optional daily notification encouraging the learner to study and
    make progress.
-   Reminder can be enabled or disabled from Settings.
-   Scheduled through Android background work.
-   Android notification permission is required on Android 13 and newer.

### Profile picture

-   Choose a profile picture using the Android system photo
    picker/gallery selection flow.
-   Store the selected image reference locally on the device.
-   The profile picture feature does not require Firebase Storage unless
    cloud profile-image synchronization is added separately in a future
    version.

### Settings and About

Settings provides access to the app's supported preferences and
account-related options. The exact options shown depend on the current
implementation and device state.

------------------------------------------------------------------------

## Technology Stack

  -----------------------------------------------------------------------
  Component                           Technology
  ----------------------------------- -----------------------------------
  Platform                            Android

  Primary language                    Java

  Build system                        Gradle

  UI                                  Android Views and XML layouts

  Minimum SDK                         24

  Compile / Target SDK                36

  Java compatibility                  Java 17

  Authentication                      Firebase Authentication

  Cloud database                      Cloud Firestore

  Local persistence                   SQLite

  AI model service                    Google Gemini

  AI request gateway                  Cloudflare Workers

  Background tasks                    Android WorkManager

  Connectivity monitoring             Android connectivity APIs /
                                      BroadcastReceiver

  Image selection                     Android system photo picker or
                                      gallery picker
  -----------------------------------------------------------------------

> Dependency versions are managed in the Gradle build files. Consult
> those files for the exact versions used by the checked-out revision.

------------------------------------------------------------------------

## Application Architecture

EduMate uses a layered Android application structure. UI components
communicate with view models and repositories; repositories coordinate
local persistence and remote services.

``` text
Android UI (Activities / Fragments)
              |
              v
        ViewModels
              |
              v
          Repositories
          /         \
         v           v
  Local SQLite    Firebase / Cloud
         |           |
         v           v
  Sessions,       Authentication
  messages,       Cloud Firestore
  evaluations
                      |
                      v
               Cloudflare Worker
                      |
                      v
                 Gemini API
```

The diagram is conceptual. Refer to the source packages for the exact
class boundaries and call flow.

### Local-first learning data

The local database provides device-side persistence for sessions,
messages, and evaluation results. Cloud synchronization is a separate
operation. This allows the app to retain locally stored learning data
while offline and retry cloud operations later.

### Suggested data ownership

-   **SQLite:** device-side session, message, and evaluation records.
-   **Firebase Authentication:** account identity and authentication
    state.
-   **Cloud Firestore:** account-associated cloud backup and
    restoration.
-   **Cloudflare Worker:** server-side gateway for Gemini requests.
-   **Android preferences / local storage:** device-specific settings
    and profile-image reference.

------------------------------------------------------------------------

## AI and Gemini Integration

EduMate sends AI requests through a Cloudflare Worker endpoint:

``` text
https://edumate-gemini-proxy.wasifrahman121.workers.dev
```

The Android app should call the Worker using the request method and
payload expected by the deployed Worker. The Worker is responsible for
validating the request/authentication as configured and forwarding the
request to Google Gemini.

### Important security rule

**Do not put a Gemini API key in source code, resources, BuildConfig
values committed to Git, or the distributed APK.** Keep the Gemini
secret in the Cloudflare Worker environment as a secret (for example,
`GEMINI_API_KEY`).

The Worker health endpoint can be used to check whether the Worker is
reachable:

``` text
GET /health
```

A successful health response confirms Worker availability only. It does
not prove that Gemini requests, authentication, quota, or upstream model
access are working.

### Model configuration

The model ID and request configuration are part of the AI integration.
Change them only deliberately and test the full learning and evaluation
flows after any modification. The current project configuration is the
source of truth for the selected model; this README intentionally does
not hard-code a model ID.

------------------------------------------------------------------------

## Authentication and Cloud Data

Firebase Authentication provides the identity used to associate cloud
data with a user.

The authenticated Firebase UID is the ownership key for cloud records.
Do not use an email address or a locally generated guest identifier as a
substitute for the Firebase UID when reading or writing a user's
Firestore data.

### Firestore data layout

The app's cloud data is organized under the authenticated user's
document:

``` text
users/
  {firebaseAuthUid}/
    sessions/
      {sessionId}/
        messages/
          {messageId}
        evaluation/
          {evaluationId}
```

The implementation may use a fixed evaluation document ID or a
collection convention for evaluation records; follow the repository
implementation and deployed Firestore rules.

### Security rules

Firestore rules must enforce that a signed-in user can access only their
own user document and subcollections. The deployed rules must match the
fields and paths written by the Android application.

After changing Firestore rules, test all of the following:

1.  A signed-in user can create and update their own profile/session
    data.
2.  The same user can read their own sessions, messages, and
    evaluations.
3.  A different authenticated user cannot read or modify those records.
4.  Unauthenticated requests cannot access private user data.

Never loosen Firestore rules to public read/write access as a workaround
for a synchronization error.

------------------------------------------------------------------------

## Chat Backup and Restoration

### Backup

The app stores learning data locally first and then synchronizes
eligible records to Firestore. A successful backup requires:

-   A valid authenticated Firebase user.
-   The local session to be associated with that user's Firebase UID.
-   A working internet connection.
-   Correct Firestore collection paths and deployed security rules.
-   Successful completion of the Firestore write operations.

If an upload fails, retain the local data and allow the synchronization
workflow to retry. Do not mark records as successfully synchronized
before the remote writes have completed.

### Restore after login

When a previously signed-in account logs in:

1.  Firebase Authentication establishes the user's identity.
2.  EduMate obtains the authenticated Firebase UID.
3.  The app reads that UID's sessions from Firestore.
4.  It retrieves each session's messages and evaluation data.
5.  Cloud records that are not already present locally are inserted into
    SQLite.
6.  The history UI reloads from the local database.

Restoration must always be scoped to the currently authenticated UID.
This prevents one account's chat history from appearing in another
account.

### Important considerations

-   A guest account is restorable only if it is the same persistent
    Firebase anonymous account. A newly created anonymous account has a
    different UID.
-   Local records created before Firebase Authentication was available
    may have a local-only identifier and may need an explicit migration
    strategy before they can be associated with a signed-in account.
-   Data stored in an older Realtime Database instance is not
    automatically imported into Firestore.
-   Reinstalling the app removes ordinary app-local database data.
    Cloud-backed data can be restored after the user signs in again,
    provided the data was successfully uploaded.
-   Do not clear app data or uninstall the app as a troubleshooting step
    until pending local records have been backed up.

------------------------------------------------------------------------

## Offline Behaviour

EduMate distinguishes between local learning functionality and
cloud-dependent functionality.

When the device has no validated internet connection:

-   The app can continue to display data already stored locally.
-   The app can show an offline notification/message.
-   Gemini requests and Firestore synchronization require connectivity
    and may fail until the connection returns.
-   Pending cloud synchronization should be retried using the existing
    background/reconnection workflow.

When connectivity returns, the app should retry pending synchronization
without duplicating sessions, messages, or evaluations.

------------------------------------------------------------------------

## Daily Learning Reminder

The daily learning reminder is an optional notification intended to
encourage consistent study habits.

-   Enable or disable it from Settings.
-   Android may ask for notification permission on Android 13 (API 33)
    and newer.
-   Background scheduling is handled by WorkManager.
-   Battery optimization and manufacturer-specific background
    restrictions may affect the exact delivery time.
-   The reminder should not be treated as an alarm-clock-grade
    exact-time notification unless exact alarms are explicitly
    implemented and permissioned.

------------------------------------------------------------------------

## Profile Picture

The profile picture is selected through Android's system image selection
interface.

-   The user chooses an image from available device media.
-   The app stores a local image reference and displays it in the
    profile/settings UI.
-   Access should be limited to the selected image rather than
    requesting broad storage access where the system picker is
    available.
-   The selected image is device-local and is not automatically
    synchronized to other devices.

If cloud profile-image synchronization is introduced later, implement it
as a separate feature with explicit storage rules, upload handling,
deletion behaviour, and privacy disclosure. Do not assume Firestore
stores image bytes.

------------------------------------------------------------------------

## Project Structure

The exact package structure may evolve. The project follows the
conventional Android Gradle layout:

``` text
EduMate/
├── app/
│   ├── build.gradle
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/
│           │   └── com/edumate/app/
│           │       ├── background/
│           │       ├── data/
│           │       ├── database/
│           │       ├── repository/
│           │       ├── ui/
│           │       └── ...
│           └── res/
│               ├── drawable/
│               ├── layout/
│               ├── mipmap/
│               ├── values/
│               └── ...
├── gradle/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── firebase.json
├── firestore.rules
└── README.md
```

Package names and folders above are representative. Use the actual
source tree in the checked-out revision as the definitive structure.

------------------------------------------------------------------------

## Requirements

Before building EduMate, install:

-   Android Studio with Android SDK support.
-   JDK 17.
-   Android SDK Platform 36 (or the platform required by the current
    Gradle configuration).
-   A compatible Gradle version through the included Gradle Wrapper.
-   An Android device or emulator running Android 7.0 (API 24) or newer.
-   Internet access for Gradle dependency resolution, Firebase, and AI
    features.

A Firebase project and a deployed Cloudflare Worker are required for the
corresponding cloud and AI features.

------------------------------------------------------------------------

## Setup and Installation

### 1. Clone the repository

``` bash
git clone https://github.com/Was-iiiif/EduMate.git
cd EduMate
```

### 2. Open in Android Studio

Open the project root directory (the directory containing
`settings.gradle`).

Allow Android Studio to sync the Gradle project and install any required
SDK components.

### 3. Configure Firebase

1.  Open the Firebase Console.
2.  Select the Firebase project configured for EduMate.
3.  Register the Android application using the package name from
    `app/build.gradle`.
4.  Download the matching `google-services.json`.
5.  Place it in the `app/` directory.
6.  Enable the authentication providers used by the application.
7.  Create or configure the Cloud Firestore database.
8.  Publish the Firestore security rules that match the app's current
    schema.

Do not commit private service-account credentials or other secrets. The
Android `google-services.json` is client configuration, but it should
still be managed according to your project's repository policy.

### 4. Configure the Cloudflare Worker

The deployed Worker must have the Gemini API secret configured in its
environment. Keep the secret in Cloudflare's secret settings; do not add
it to the Android project or README.

Confirm that the configured Worker URL matches the endpoint used by the
app. Test the Worker health endpoint, then test an authenticated AI
request from the application.

### 5. Build and run

Use Android Studio's Run action, or execute:

**Windows**

``` powershell
.\gradlew.bat :app:assembleDebug
```

**macOS / Linux**

``` bash
./gradlew :app:assembleDebug
```

The debug APK is normally generated under:

``` text
app/build/outputs/apk/debug/
```

------------------------------------------------------------------------

## Firebase Configuration

The Firebase project currently associated with the app is:

``` text
edumate-e3b7f
```

Verify that this is still the intended project before deploying rules or
changing console settings.

The app relies on Firebase Authentication and Cloud Firestore. Keep the
existing authentication providers, Firestore database, document
structure, and security rules aligned with the checked-out application
code.

For a rules-only deployment, if the Firebase CLI is installed and
`firebase.json` maps the Firestore rules file correctly:

``` bash
firebase deploy --only firestore:rules --project edumate-e3b7f
```

Alternatively, publish the rules through Firebase Console → Firestore
Database → Rules.

**Do not deploy unrelated Firebase resources when only Firestore rules
need updating.** Firebase Cloud Functions are not required for the
described client-side Firestore synchronization flow.

------------------------------------------------------------------------

## Cloudflare Worker Configuration

The configured Worker endpoint is:

``` text
https://edumate-gemini-proxy.wasifrahman121.workers.dev
```

The Worker is an external deployment and is not built as part of the
Android Gradle project.

Operational checklist:

-   Keep the Gemini API key in Worker secrets.
-   Keep authentication verification enabled as required by the current
    Worker implementation.
-   Do not expose secrets in Logcat, client responses, Git commits,
    screenshots, or documentation.
-   Confirm the Worker accepts the HTTP method and payload used by the
    app.
-   Test error handling for invalid authentication, rate limits, and
    upstream Gemini failures.

------------------------------------------------------------------------

## Permissions

The application may request permissions depending on enabled features
and Android version.

  -----------------------------------------------------------------------
  Permission / capability             Purpose
  ----------------------------------- -----------------------------------
  Internet access                     Gemini requests, Firebase
                                      Authentication, and Firestore

  Network state access                Determine network connectivity

  Notifications (Android 13+)         Display daily learning reminders
                                      and applicable alerts

  Image selection                     Let the user choose a profile
                                      picture through the system picker
  -----------------------------------------------------------------------

The app should request runtime permissions only when needed and explain
why they are required. Prefer the Android system photo picker to broad
media/storage permissions.

------------------------------------------------------------------------

## Privacy and Security

EduMate handles account identity, learning conversations, submitted
answers, evaluation results, and device-specific preferences.

Follow these principles:

1.  Never hard-code API keys or private credentials in the Android
    client.
2.  Route Gemini requests through the configured secure gateway.
3.  Restrict Firestore access using authenticated UID ownership rules.
4.  Do not log access tokens, API keys, private chat content, or other
    sensitive data unnecessarily.
5.  Explain to users that AI prompts and submitted answers are sent to
    the configured backend and Google Gemini for processing.
6.  Keep local profile-image references separate from cloud profile data
    unless image backup is explicitly implemented.
7.  Preserve local data when cloud synchronization fails.
8.  Do not claim that a conversation is backed up until the Firestore
    write succeeds.
9.  Test account switching to ensure that data from one UID is never
    shown to another.
10. Provide clear account and data-management behaviour before adding
    destructive actions.

------------------------------------------------------------------------

## Troubleshooting

### AI response is unavailable

-   Check the device's internet connection.
-   Check the Worker health endpoint.
-   Confirm the Android app is calling the configured Worker URL using
    the expected HTTP method.
-   Inspect sanitized Worker and Android logs.
-   Check Gemini quota, model availability, authentication verification,
    and upstream errors.
-   Do not put the Gemini API key into the Android client as a
    workaround.

### Chat history does not appear after login

-   Confirm the user signed in with the same Firebase account.
-   Verify the Firebase UID is the expected UID.
-   Check that the original sessions were successfully uploaded to
    Firestore.
-   Confirm the Firestore rules permit that UID to read its own records.
-   Inspect the `users/{uid}/sessions` path and its message/evaluation
    subcollections.
-   Check Logcat for Firestore read, mapping, and SQLite insertion
    errors.
-   Confirm the history screen reloads after restoration completes.

### Chat backup fails

-   Confirm Firebase Authentication is active.
-   Confirm the local session belongs to the authenticated Firebase UID.
-   Check network connectivity.
-   Check the deployed Firestore rules and required document fields.
-   Inspect Firestore write errors in Logcat.
-   Keep local data intact while investigating.

### Reminder does not appear

-   Enable the reminder in Settings.
-   Grant notification permission on Android 13+.
-   Check the app's notification channel settings.
-   Check Android battery/background restrictions.
-   Remember that WorkManager is not an exact-alarm scheduler.

### Profile picture is not retained

-   Confirm the image picker returned a valid URI.
-   Check that the app persisted the selected URI or copied the image
    into app-private storage as implemented.
-   If the source image was moved or removed, select it again.
-   Remember that a local profile picture does not automatically
    transfer to another device.

### Offline alert does not appear

-   Check that the receiver is declared/registered correctly.
-   Verify the device actually loses validated internet access.
-   Check notification/channel or in-app alert behaviour implemented by
    the current version.
-   Avoid disabling the existing network callback used for
    synchronization retries.

------------------------------------------------------------------------

## Development Notes

-   Keep Gemini model selection and request changes isolated from
    unrelated UI or persistence work.
-   Treat Firebase Authentication UID as the source of truth for cloud
    ownership.
-   Keep Firestore schema changes coordinated with security-rule
    changes.
-   Preserve local records until cloud writes are confirmed.
-   Make restore operations idempotent to avoid duplicate sessions and
    messages.
-   Test switching between two accounts.
-   Test offline → online transitions.
-   Test notification behaviour on Android 12, Android 13+, and newer
    Android versions.
-   Test the image picker on devices with and without the latest system
    photo picker.
-   Avoid broad refactors when a targeted change is sufficient.

------------------------------------------------------------------------

## Contributing

Contributions and improvements are welcome.

A useful contribution workflow:

1.  Create a feature branch.
2.  Make a focused change.
3.  Build the project.
4.  Test the affected feature on an emulator or physical device.
5.  Verify that authentication, chat history, and cloud synchronization
    still work.
6.  Submit a pull request with a clear description and test notes.

Suggested branch naming:

``` text
feature/feature-name
fix/issue-description
docs/documentation-update
```

------------------------------------------------------------------------

## License

No license terms are specified in this README. Unless a license file is
added to the repository, all rights remain with the copyright holder and
reuse or redistribution should be treated as not expressly licensed.

------------------------------------------------------------------------

## Project

**EduMate** --- an AI-assisted learning companion.

Repository: https://github.com/Was-iiiif/EduMate

AI gateway: https://edumate-gemini-proxy.wasifrahman121.workers.dev

Firebase project ID: `edumate-e3b7f`
