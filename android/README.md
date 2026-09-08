# Super Mario War pour Android

Voir [l'analyse et le plan de portage](../docs/android-port.md).

```sh
export ANDROID_HOME=/chemin/vers/android-sdk
export JAVA_HOME=/chemin/vers/jdk-17
./build.sh
```

APK : `app/build/outputs/apk/debug/app-debug.apk`.

Cette version cible ARM64, Android 6 ou supérieur. Elle comprend le jeu local,
les ressources et les contrôles tactiles pour un ou deux joueurs (clavier par défaut).
Voir [les commandes et les tests tactiles](../docs/android-touch.md).
Une nouvelle installation propose un humain face à un bot ; les options déjà sauvegardées
restent prioritaires. Les essais sur téléphone restent à effectuer.

Tests de la logique tactile : `JAVA_HOME=/chemin/vers/jdk-17 ./test-touch.sh`.

Le script compile d'abord C++/SDL avec le NDK puis assemble l'application Java avec
Gradle. Relancer ce script après une modification native. `deps/SDL` fournit à la fois
la bibliothèque SDL2 et les classes Java correspondantes ; ne pas mélanger les versions.
Les fichiers générés, caches, SDK et clés de signature restent hors du dépôt.
