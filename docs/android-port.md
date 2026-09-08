# Portage Android — analyse et plan

Mise à jour : les contrôles tactiles sont maintenant implémentés dans la version
`2.0.0-android-touch` (code 2). Voir [commandes et validation](android-touch.md).
Le bilan et l'empreinte ci-dessous décrivent le premier APK, avant cet ajout.

## Référence et objectif

Dépôt : https://github.com/mmatyas/supermariowar
Révision initiale : `051723d0ecfe0e6b66ff7e57a1f66b74a0e41715`.
Ressources : sous-module `1139d89ef7e38368536317afd7db54cea2488d5b`.
Branche locale : `android/initial-port`.

Objectif : exécuter le jeu sur Android, puis traiter les contrôles dans une étape distincte.
Premier périmètre : APK debug ARM64, Android 6/API 23 minimum, compilation et cible API 35,
NDK 27.2.12479018, jeu local avec bots. Clavier/manette physique pour les premiers essais.
Les éditeurs et le réseau sont exclus de cette première compilation.

## Faisabilité et points sensibles

La portabilité est bonne : moteur C++20, CMake et SDL2 déjà présents. Le rendu logique
640 × 480 est mis à l'échelle par SDL ; pas de réécriture du gameplay nécessaire.
L'ancien port Android est dans un dépôt séparé, référencé par le README ; le nouveau
socle réutilise le CMake courant et la couche Java officielle de SDL2 2.32.10.

| Domaine | Constat dans le code | Solution / validation |
| --- | --- | --- |
| Démarrage | CMake produit un exécutable desktop | Bibliothèque `libmain.so`, `SDLActivity` et point d'entrée exporté `SDL_main` |
| Données | `std::filesystem`, `stat`, flux et énumération de répertoires | Embarquer les données puis les extraire dans `files/game/data` avant SDL |
| Sauvegardes | Ancienne branche `ANDROID` utilise la carte SD | `SDL_GetPrefPath`, stockage privé sans permission de stockage |
| Initialisation | `RootDataDirectory` appelle SDL au chargement de la bibliothèque | Initialisation différée par l'argument `--datadir` fourni par Java |
| Images/audio | Ressources PNG, WAV, OGG | SDL_image avec PNG et SDL_mixer avec WAV/OGG ; autres codecs désactivés sur Android |
| Graphisme | SDL_Renderer, taille logique 640 × 480 | GLES, paysage ; vérifier bandes latérales et écrans avec encoche |
| Cycle de vie | SDL gère pause/reprise de base | Tester Home, verrouillage, interruption audio, retour et perte du contexte graphique |
| ABI | C++20, dépendances natives | ARM64 + libc++ partagée, alignement ELF 16 Kio ; vérifier aussi le conditionnement APK |
| Entrées | Clavier/joystick existants | Valider sur périphérique physique ; contrôles tactiles dans un jalon ultérieur |
| Réseau | ENet et implémentation expérimentale upstream | Désactivé au premier jalon ; audit et tests dédiés avant réactivation |

L'extraction initiale est effectuée hors du thread UI. Une erreur affiche un message et
une extraction interrompue est reprise au prochain lancement. Le marqueur dépend du
`versionCode`, à incrémenter quand les ressources changent. Cette première politique
recopie les ressources livrées lors des mises à jour ; la gestion de contenus importés
et la suppression de ressources obsolètes restent à concevoir avant leur prise en charge.
Les réglages sont séparés du répertoire des ressources.

## Jalons et critères d'acceptation

1. **Socle de compilation** : clone complet, branche isolée, cible CMake Android,
   activité SDL, packaging des ressources et procédure reproductible. APK ARM64 généré,
   `SDL_main` exporté, toutes les dépendances ELF présentes et correctement alignées.
2. **Premier démarrage réel** : installer sur un téléphone ARM64, démarrer jusqu'au menu,
   vérifier images, polices, musique et absence de crash dans logcat. Vérifier aussi
   le premier démarrage sans réseau, le deuxième démarrage et la persistance des options.
3. **Partie locale** : lancer une partie avec bots à l'aide d'un clavier/manette,
   jouer plusieurs manches, tester changement de carte et retour au menu. Mesurer
   cadence, consommation mémoire et latence audio sur le matériel cible.
4. **Robustesse Android** : répéter pause/reprise, verrouillage, retour système,
   relancement du processus, installation d'une mise à jour et extraction interrompue.
   Tester Android API 23 et Android récent, dont un système à pages de 16 Kio.
   Corriger les éventuels problèmes de textures et d'audio avant de déclarer le port fonctionnel.
5. **Contrôles** (ultérieur, avec l'utilisateur) : choix disposition/multitouch/manettes,
   navigation des menus, ergonomie, puis tests de gameplay.
6. **Distribution** : icône, version, signature et build release ; autres ABI et réseau
   selon besoin. Aucun engagement de publication dans ce premier jalon.

## Compilation

Prérequis : Linux x86_64, Git, CMake >= 3.24, Ninja, JDK 17, SDK Android plateforme 35,
NDK 27.2.12479018. Les dépendances natives sont téléchargées par CMake, aux versions
fixées dans `cmake/BundledDeps.cmake`. Gradle 8.13 et AGP 8.7.3 sont fixés dans le projet.

```sh
git submodule update --init --recursive
export ANDROID_HOME=/chemin/vers/android-sdk
export JAVA_HOME=/chemin/vers/jdk-17
./android/build.sh
```

Sortie : `android/app/build/outputs/apk/debug/app-debug.apk`.
Le projet Gradle utilise les bibliothèques produites par le script ; lancer celui-ci
après chaque modification native. Android Studio peut ouvrir `android/` après cette étape.

```sh
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n org.supermariowar.app/.MainActivity
adb logcat -s SDL SMW AndroidRuntime libc
```

## Sources techniques

- Dépôt et dépendances : https://github.com/mmatyas/supermariowar
- Ancien port référencé : https://github.com/mmatyas/supermariowar-android
- Intégration Android officielle SDL2, SDLActivity et contraintes de cycle de vie :
  https://wiki.libsdl.org/SDL2/README-android
- Pages mémoire Android : https://developer.android.com/guide/practices/page-sizes

## Validation de cette session

Validation effectuée le 7 septembre 2026 :

- Compilation complète NDK ARM64 du moteur et de SDL : réussie.
- Construction de l'APK debug par `android/build.sh` : réussie.
- Les 1 601 ressources du jeu ont été comparées octet par octet avec le contenu de l'APK.
- Les cinq bibliothèques natives sont ARM64, leurs segments ELF et leurs entrées ZIP
  sont alignés sur 16 Kio. Les dépendances ELF sont présentes ou fournies par Android.
- Le symbole `SDL_main` est exporté ; signature debug vérifiée avec `apksigner`.
- Manifest : application `org.supermariowar.app`, minimum API 23, cible API 35.
- `bash -n` et `git diff --check` : réussis.
- Android Lint a été exécuté mais **ne passe pas** : 31 erreurs signalées dans les
  sources Java SDL2 (26 permissions, 4 usages d'API, 1 enregistrement de récepteur),
  ainsi que 35 avertissements. Aucun de ces diagnostics n'a été masqué.
  Les branches Bluetooth et capture audio sont optionnelles ; le jeu ne demande
  aucune permission Bluetooth/microphone. Leur audit reste à faire avant activation.
  `GameActivity.registerReceiver` adapte l'appel SDL à Android 13+ avec un récepteur
  non exporté ; Lint continue à signaler l'appel original dans SDL. Validation sur
  appareil requise pour ce chemin USB et les branches d'API/haptique.
  Rapport détaillé de cette session : `/tmp/smw-android-lint-results.txt`.
  Pour reproduire : `cd android && ./gradlew :app:lintDebug`.

Aucun appareil n'est visible dans `adb devices -l`. Le menu, l'audio, une partie et le
cycle de vie **n'ont pas été testés en exécution**. Le jalon de compilation est réalisé ;
le port ne doit pas encore être déclaré fonctionnel sur téléphone. Prochaine étape :
installer l'APK sur un appareil ARM64, relever logcat et valider les jalons 2 à 4 avant
le travail sur les contrôles tactiles.

APK final de cette session : `android/app/build/outputs/apk/debug/app-debug.apk`,
51 949 294 octets (~49,5 Mio), reconstruit proprement et revérifié après assemblage.
SHA-256 : `34dcba2c2308a8913a3d514cc37628642cb7a36f68a7219b56618e24099bf9c2`.
