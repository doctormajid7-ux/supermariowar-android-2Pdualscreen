# Contrôles tactiles Android

Version de référence précédente : `2.0.0-android-touch`, code 2.
APK : `android/app/build/outputs/apk/debug/app-debug.apk`.

Le prototype de la deuxième étape est intégré : l'écran de sélection peut activer le
mode portrait à deux humains, les skins des bots peuvent être randomisés et les
touches peuvent être restaurées. En partie, l'écran est séparé en deux zones, celle
du haut étant retournée pour le joueur 2. Les deux moitiés ont leurs pointeurs et
leurs commandes ; le joueur 2 utilise W/A/S/D/E/Q. La simulation et la caméra restent
communes dans ce premier prototype.

Le second écran et les commandes du joueur 2 sont également actifs pendant la
sélection des personnages dès que `Portrait 2P: ON` est sélectionné.

Dans la sélection d'équipe, **Action** relance les skins de tous les bots à chaque
appui. Le tirage est immédiatement chargé et reste disponible après l'état `Ready`.

Le correctif de la version 3 inclut aussi le cache des miniatures de cartes : le dossier
`files/game/data/maps/cache` est créé automatiquement, et une miniature fraîche est
affichée immédiatement même si son écriture sur le stockage échoue. Les PNG absents,
vides, corrompus ou de mauvaises dimensions sont régénérés.

Les commandes sont affichées au-dessus du jeu en paysage. La croix est à gauche,
les trois boutons de jeu à droite, Retour en haut à gauche et OK / Pause en haut
à droite. Les zones sont translucides et s'éclairent lorsqu'elles sont maintenues.
La disposition tient compte de la taille de l'écran, de la densité et des marges
signalées par Android, y compris les encoches.

| Commande | Pendant une partie | Dans les menus |
| --- | --- | --- |
| Gauche / droite | Déplacement | Navigation / changement de valeur |
| Haut ou Saut | Saut (maintenir selon l'action) | Monter |
| Bas | Descendre / s'accroupir selon la situation | Descendre |
| Action | Courir, tirer ou utiliser l'action courante | Sélection aléatoire là où le jeu la propose |
| Objet | Utiliser le bonus en réserve | Défilement rapide, à maintenir avec une direction |
| OK / Pause | Mettre en pause / reprendre | Valider, passer l'écran d'accueil, confirmer un joueur prêt |
| Retour | Ouvrir le dialogue de sortie de partie | Annuler / revenir |

Plusieurs doigts peuvent être utilisés simultanément : direction + Action + Saut,
par exemple. Le glissement sur la croix permet les diagonales ; revenir au centre
ou sortir de la zone relâche la direction. Un doigt peut glisser d'un bouton à un autre.
Les appuis sont relâchés lors d'une annulation tactile, d'une perte de focus, de la
mise en pause de l'application, d'un changement de géométrie ou de la destruction de
l'activité. Les touches ne se répètent pas artificiellement à chaque mouvement.

## Démarrer une partie sans périphérique

1. Installer l'APK, ouvrir Super Mario War et toucher **OK / Pause** pour passer
   l'écran d'accueil après le chargement.
2. Utiliser la croix et **OK / Pause** pour sélectionner le mode de jeu.
3. Garder **Joueur 1 humain**, avec les contrôles **clavier par défaut**. Une nouvelle
   installation propose maintenant le joueur 2 CPU, les autres joueurs désactivés.
   Si une ancienne configuration conserve plusieurs humains, passer les adversaires
   en CPU dans le menu des joueurs pour éviter d'attendre leur validation.
4. Confirmer le joueur prêt, puis démarrer la partie depuis les réglages de match.

Cette première disposition pilote le joueur 1 via les commandes clavier existantes.
Les touches envoyées sont : flèches, Ctrl droit (Action), Shift droit (Objet),
Entrée et Échap. Action envoie aussi Espace et Objet aussi Shift gauche, afin de
couvrir respectivement l'aléatoire et le défilement rapide des menus.
**Une reconfiguration des touches clavier dans le jeu modifie donc le comportement
associé aux boutons tactiles.** Les réglages existants ne sont pas écrasés.
Le placement personnalisable, le masquage des boutons et un périphérique tactile
indépendant des raccourcis clavier ne sont pas implémentés dans cette version.

## Implémentation

- `GameActivity` ajoute une vue transparente à la surface SDL et gère le relâchement
  des touches lors des changements de cycle de vie.
- `TouchControlsView` dessine les boutons, lit les identifiants stables des pointeurs
  et transmet les changements à `SDLActivity.onNativeKeyDown` / `onNativeKeyUp`.
  Les événements tactiles sont consommés pour ne pas également créer des clics SDL.
- `TouchInput` calcule l'union des commandes détenues par chaque doigt. Un lot complet
  de pointeurs est traité avant d'émettre les changements ; deux doigts partageant
  le saut ne provoquent pas de relâchement prématuré ni de nouvel appui parasite.
- `TouchLayout` fournit la géométrie commune au dessin et à la détection des appuis.
- `CGameValues::init` utilise un adversaire CPU par défaut sur Android ; la lecture
  ultérieure des options sauvegardées conserve les choix de l'utilisateur.

## Vérification

```sh
JAVA_HOME=/chemin/vers/jdk-17 ./android/test-touch.sh
ANDROID_HOME=/chemin/vers/android-sdk JAVA_HOME=/chemin/vers/jdk-17 ./android/build.sh
```

Les tests Java sur l'hôte couvrent les combinaisons simultanées, les identifiants de
pointeurs non consécutifs, le saut partagé, le transfert d'appui entre doigts, les
glissements, les appuis courts, le relâchement global, les mouvements reçus sans focus
et la géométrie sur six tailles/densités d'écran avec marges.

Résultat : **156 vérifications réussies**. Ces tests valident la logique d'appui et
les zones de contact ; ils ne remplacent pas un essai de MotionEvent/JNI sur Android.
Aucun téléphone n'est visible dans ADB lors de cette session. À tester sur appareil :
navigation complète, maintien de la course avec sauts, usage d'objet, diagonales,
Pause/Retour, Home/reprise en maintenant une direction, affichage sur écran avec encoche.

Validation de l'APK final : compilation NDK/Gradle réussie, version 2 confirmée,
signature debug valide, 1 601 ressources comparées octet par octet et cinq bibliothèques
natives identiques aux sorties de compilation, alignement des entrées ZIP à 16 Kio.
Android Lint ne signale aucun diagnostic dans les trois nouvelles classes tactiles.
Il reste à 31 erreurs dans SDL2 et 35 avertissements, comme avant cette modification ;
le contrôle Lint global ne passe donc toujours pas. Rapport :
`android/app/build/reports/lint-results-debug.html`.

APK : 54 510 447 octets (~52 Mio).
SHA-256 : `a83e863113875ef408dda9f6d1d513ad7429d5583f5dede80b60235c2d216936`.
