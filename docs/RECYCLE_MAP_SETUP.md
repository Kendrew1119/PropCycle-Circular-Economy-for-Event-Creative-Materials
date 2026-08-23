# PropCycle Recycling Centre Map Setup

This guide is for Phase 2D. It explains how to make the **Recycle Center** page
show a real Google map and real nearby place results.

The app can still build without a Maps key. Without a key, the page shows a
setup message and can pass a manual search to an installed map app. It never
shows fake centres.

## 1. What this phase uses

The page uses these Android services:

- Maps SDK for Android: displays the in-app map.
- Places API (New): searches for recycling centres by text.
- Fused Location Provider: gets the user's current foreground location once.

The app does **not** save the user's location. It does not use background
location, routes, navigation, or location history.

## 2. Before you start

You need:

1. Android Studio installed.
2. This PropCycle repository on your computer.
3. Access to the Google Cloud project used by the team.
4. Permission to enable APIs, manage billing, and create API keys.

For the shared development app, use the Google Cloud project connected to the
Firebase project `propcycle-e5f14`. Do not create a random key in another
project unless the team deliberately wants separate development and release
projects.

## 3. Enable billing

Google Maps Platform normally requires a billing account even when your use is
within a free monthly allowance.

1. Open [Google Cloud Console](https://console.cloud.google.com/).
2. Select the correct PropCycle project at the top of the page.
3. Open **Billing**.
4. Link an active billing account.
5. Set a small budget and billing alerts that are suitable for the course demo.

Do not continue with a different project by mistake. The selected project name
is shown in the top bar of Google Cloud Console.

## 4. Enable the two required APIs

1. In Google Cloud Console, open **APIs & Services** > **Library**.
2. Search for **Maps SDK for Android**.
3. Open it and click **Enable**.
4. Go back to the API Library.
5. Search for **Places API (New)**.
6. Open it and click **Enable**.

Do not enable Directions, Routes, Geocoding, or other APIs for this phase.

## 5. Get the app SHA-1 value

Google uses the package name and signing SHA-1 to make sure the key works only
with the PropCycle Android app.

Open PowerShell in the repository root:

```powershell
cd C:\Users\B2B\Desktop\mobileapp
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat signingReport
```

Find the `debug` section and copy its **SHA1** value. Every teammate can have a
different debug SHA-1 because each computer can have a different debug
keystore. Add every approved teammate SHA-1 to the key restriction. Do not
share the private keystore file.

The Android package name is:

```text
com.propcycle.app
```

For a final signed APK, also add the SHA-1 of the final release signing key.

## 6. Create and restrict the API key

1. Open **APIs & Services** > **Credentials**.
2. Click **Create credentials** > **API key**.
3. Copy the new key temporarily. Do not paste it into chat, GitHub, a report,
   Java source, XML source, or `local.defaults.properties`.
4. Click **Edit API key**.
5. Give it a clear name such as `PropCycle Android development`.
6. Under **Application restrictions**, choose **Android apps**.
7. Click **Add an item**.
8. Enter package name `com.propcycle.app`.
9. Enter the debug SHA-1 copied from `signingReport`.
10. Add another item for each approved teammate SHA-1 and the release SHA-1
    when it is ready.
11. Under **API restrictions**, choose **Restrict key**.
12. Select only **Maps SDK for Android** and **Places API (New)**.
13. Save the key.

Key restriction changes can take a few minutes to become active.

Important: keeping a key outside Git is useful, but an Android APK can still be
inspected. The Android package/SHA-1 and API restrictions are the real security
controls.

## 7. Add the key only on your computer

In the repository root, create a file named:

```text
secrets.properties
```

Add one line:

```properties
MAPS_API_KEY=PASTE_YOUR_RESTRICTED_ANDROID_KEY_HERE
```

Replace the example value with the real restricted key. Do not add quotes or
spaces around the value.

`secrets.properties` is ignored by Git. Check this before committing:

```powershell
git check-ignore -v secrets.properties
```

The command should show a `.gitignore` rule. If it shows nothing, stop and do
not commit.

The checked-in `local.defaults.properties` contains only `SETUP_REQUIRED`.
Never put the real key in that file.

## 8. Sync and build the project

In Android Studio:

1. Open the repository root folder.
2. Click **File** > **Sync Project with Gradle Files**.
3. Wait for Gradle sync to finish.
4. Use the `app` run configuration.
5. Choose an Android device or emulator that includes Google Play services.
6. Run the app.

PowerShell build check:

```powershell
cd C:\Users\B2B\Desktop\mobileapp
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug testDebugUnitTest
```

## 9. Test the live page

Open **Recycle Center** from the app menu or from an AI result.

Complete every check below:

1. The map loads and shows the Google attribution.
2. Enter `Petaling Jaya` and tap **Search area**.
3. The page shows only real Places results, or a clear empty/error message.
4. At most ten results appear.
5. Tap a list row. Its map marker becomes selected and the map moves to it.
6. Tap a map marker. The matching list row becomes selected.
7. Tap **Open in map app**. The selected place opens in an installed map app.
8. Return to PropCycle and tap **Use location**.
9. Test **Approximate** permission. Search must still work.
10. Test **Precise** permission. Search must work.
11. Remove location permission in Android Settings and try again. Manual area
    search must still work.
12. Turn location services off. The page must explain that location is
    unavailable and keep manual area search usable.
13. Turn the internet off. The page must show an offline/retry message and must
    not show fake results.
14. Confirm the app never asks for background location.
15. Confirm the list says distance is approximate when a current location was
    used.
16. Contact or check a centre before travelling; the app does not claim which
    materials a centre accepts.

## 10. Test the safe missing-key build

Temporarily move `secrets.properties` outside the repository. Do not delete it
if it is your only copy of the key.

Then build again:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat clean assembleDebug
```

Expected result:

- The build succeeds.
- The page shows a Maps setup message.
- Manual search can open an installed map app.
- The app does not claim that an in-app Places search succeeded.

Put your ignored `secrets.properties` file back after this check.

## 11. Common problems

### The map is blank or shows an authorization error

Check all of these:

1. The correct Google Cloud project is selected.
2. Billing is active.
3. Maps SDK for Android is enabled.
4. The key application restriction uses `com.propcycle.app`.
5. The SHA-1 matches the exact debug or release signing key used for the APK.
6. The key API restrictions include Maps SDK for Android.
7. `secrets.properties` uses the exact name `MAPS_API_KEY`.

### The map loads but place search is denied

Check that **Places API (New)** is enabled and included in the key's API
restrictions. Also check billing and quota.

### It works for one teammate but not another

Run `signingReport` on the teammate's computer and add that teammate's debug
SHA-1 to the Android application restriction. Wait a few minutes and retry.

### Current location is unavailable

Turn on device location, keep PropCycle in the foreground, and try again. If it
still fails, use manual city/area search. The app does not continuously watch
for a location.

### Search results are not a real recycling service

Places Text Search is not a verified material-acceptance database. Try a more
specific area and always contact the place before travelling.

## 12. Rules for the team

- Never commit `secrets.properties` or a real Maps key.
- Never put the key in Java, XML, screenshots, chat, or the assignment report.
- Keep Android package/SHA-1 and API restrictions enabled.
- Review quota and billing before a class demo.
- Do not add routes, background location, or another location API without a
  separately approved phase.
