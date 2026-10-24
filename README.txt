BUILD THE APK (no installs needed)
1. Create a free GitHub account, make a new repository, upload everything in this folder
   (keep the .github folder). 
2. Open the Actions tab > "Build APK" > Run workflow.
3. When it finishes (~3 min), open the run and download the "ZLauncher-apk" artifact (zip, contains the .apk).
4. Copy the APK to the Android device and install it (allow "Install unknown apps").

Or in Android Studio: File > Open this folder > Build > Build APK(s).

--- v2.4: WayneTech BatCam added ---
BatCam is a card in the right column (camera preview, speed/time overlay, snapshots).
First tap on "Start" shows Android's camera permission prompt: choose Allow.
Settings (gear): show/hide BatCam, auto-start camera, speed alert limit, clear snapshots.
Back button closes the snapshot viewer / settings / expanded card.
