# Release hygiene

When publishing via CI (`latest` release):

- Remove previous jar assets from the `latest` GitHub Release so only the current build remains.
- The Build workflow does this automatically before uploading the new jar.
- Manual versioned tags (`v*`) are left alone.
