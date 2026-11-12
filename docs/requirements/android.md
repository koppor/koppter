# Android

Requirements and design that are specific to the Android application.
The screens are those of [ui.md](ui.md).

## Requirements

### Android application
`req~android-app~1`

The application runs on Android 8.0 (API level 26) and later with the same screens as on the desktop.

Covers:

* `feat~platforms~1`

Needs: dsn

### File of a document provider
`req~document-provider-file~1`

On Android, the user chooses the todo.txt file with the document picker of the system.
The file may belong to any document provider, including that of a cloud storage application.

Covers:

* `feat~sync-local-folder~1`

Needs: dsn

## Design

### Shared UI as an Android library
`dsn~android-shell~1`

The module `:app` has an Android library target next to the desktop target; the screens are compiled for both from `commonMain`.
The module `:android` contains the activity, which creates the controller and shows `App`.

Covers:

* `req~android-app~1`

Needs: impl

### Locations on Android
`dsn~android-locations~1`

The settings file is `settings.properties` in the private files directory of the application.
Without a configured location, the todo.txt file is `todo.txt` in the same directory.
A configured location that starts with `content:` is opened as a document, any other as a file.

Covers:

* `req~android-app~1`
* `req~persistent-settings~1`

Needs: impl

### Document storage
`dsn~document-storage~1`

`DocumentStorage` reads and writes the document behind a `content:` URI through the content resolver.
The version is the modification time together with the size, as reported by the document provider.

Covers:

* `req~document-provider-file~1`
* `req~external-changes~1`

Needs: impl

### Choose the file
`dsn~choose-file~1`

The settings dialog has a button that opens the file dialog of the platform and puts the chosen location into the field.
On Android, this is the document picker; the application takes the persistable read and write permission for the chosen document.
On the desktop, this is the file dialog of the operating system.

Covers:

* `req~document-provider-file~1`
* `req~choose-file~1`

Needs: impl
