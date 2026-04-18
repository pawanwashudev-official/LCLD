# Releasing

This document describes how to publish a new release for FMD.

Preparation:

1. Update the `versionCode` and `versionName` in `app/build.gradle`.
1. Write a changelog and put it in `metadata/en-US/changelogs/{versionCode}.txt`
1. Commit and push

Build the APK:

1. Build the unsigned releases: `./gradlew clean assembleProdRelease assembleEdgeRelease`
1. Sign the the APKs.
1. Test the signed APKs.
   (The release flavor has optimisations that the debug flavor does not have!)
1. Test that fdroidserver can build the commit.
1. Verify that the APKs are reproducible.

F-Droid repo:

1. Copy the signed APKs into the F-Droid repo.
1. Update the `CurrentVersionCode` in the repo metadata.
1. Sign a new F-Droid repo index.
1. Upload the new repo to <https://packages.fmd-foss.org>.

Publishing:

3. Tag the new release: `git tag v0.0.0` and push the tag: `git push --tags`
4. Create a new release on Gitlab: <https://gitlab.com/fmd-foss/fmd-android/-/releases>
5. Wait for F-Droid to pick it up!

## Testing

Before a new release, manually test the following things:

- Design:
  - Dark mode & light mode of new features
  - Edge-to-Edge on different navigation modes (gesture navigation, 3-button navigation)
- Basic commands on all transports: `fmd mypin help`
- Case insensitivity of trigger word and commands: `FMD locATE`
- FMD Server:
  - Registration, Logout, Login, Account deletion
  - Web: login, pushing commands
  - Background location upload
