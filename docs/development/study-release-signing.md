# Study edition release signing

The release application ID is `me.rerere.rikkahub.study`; debug builds use the `.debug` suffix. Release builds use the existing `local.properties` signing configuration. Keep the same private key for future in-place upgrades.

The current private key is stored outside Git at `/workspace/.setup/release-signing/rikkahub-study.p12`. The owner handoff is `/workspace/artifacts/RikkaHub-Study-private-signing-backup.zip`; it includes the alias and password. Keep this backup private. Neither the key nor its password belongs in a public release or commit.

Certificate SHA-256: `4b15d75da0e4461dc24506fb53e2aaa156b72c34b59a3fec3d56356525842909`.

For a fresh environment, restore the backup first and configure `storeFile`, `storePassword`, `keyAlias` and `keyPassword` in the ignored `local.properties`. `keyPassword` equals `storePassword`. Do not generate another key for an existing installation.

Build with `bash gradlew :app:assembleRelease`. Verify every APK with `apksigner verify --print-certs`, check the application ID/version and absence of `application-debuggable`, then create SHA-256 checksums.

The final-release workflow downloads already-signed APKs from a draft release, verifies their identity and certificate, and installs/launches the universal APK on Android 15 before making the release public. The private signing key stays in the build environment. When preparing a future version, update the version, draft tag and expected version in the workflow together, upload the signed draft assets, and then push the matching source revision. The workflow points the published tag at that validated source revision.
