# Contrôles tactiles Android

Version actuelle : `2.0.0-stage2-touchfix`, code 6.
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
| Saut | Saut (maintenir selon l'action) | Monter |
| Haut de la croix | Masqué et inactif | Monter |
| Bas | Descendre / s'accroupir selon la situation | Descendre |
| Action | Courir, tirer ou utiliser l'action courante | Sélection aléatoire là où le jeu la propose |
| Objet | Utiliser le bonus en réserve | Défilement rapide, à maintenir avec une direction |
| OK / Pause | Mettre en pause / reprendre | Valider, passer l'écran d'accueil, confirmer un joueur prêt |
| Retour | Ouvrir le dialogue de sortie de partie | Annuler / revenir |

Plusieurs doigts peuvent être utilisés simultanément : direction + Action + Saut,
par exemple. La flèche haute est conservée dans les menus, pendant la pause et dans le dialogue
de sortie. En partie, un glissement vers un coin supérieur conserve gauche/droite
sans déclencher de saut. Le glissement sur la croix permet les diagonales basses ; revenir au centre
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
  Le haut de la croix peut être désactivé sans désactiver le bouton Saut.
- `TouchRouter` garde chaque contact affecté au joueur de départ, applique la rotation
  de la moitié supérieure et ignore les mouvements périmés après une annulation.
- `TouchKeys` fusionne les touches clavier partagées entre les joueurs. Relâcher
  Action, Objet ou une validation ne coupe plus une touche encore détenue ailleurs.
  La touche physique à relâcher est celle choisie à l'appui, même après un changement
  de menu. Les transferts dans un même événement ne produisent pas de nouvel appui.
- L'état menu/jeu est actualisé au toucher, au dessin et par la vérification périodique
  de l'activité ; les contacts sont relâchés lors d'une transition.
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

Résultat actuel : **330 vérifications réussies** sur l'hôte. Les scénarios 2P
couvrent 4, 6 et 8 contacts, les deux moitiés (dont celle retournée), 100 mises à jour
successives par scénario, les identifiants non consécutifs, l'ordre inverse des
pointeurs, les relâchements séparés par joueur, plusieurs doigts sur Saut, les touches
partagées, l'annulation, la perte de focus et les transitions menu/jeu.

Ces tests exécutent la logique utilisée par la vue, sans runtime Android. Ils ne
valident pas le matériel tactile, la livraison des MotionEvent ni la chaîne JNI/SDL
sur un téléphone. Aucun appareil ADB n'était connecté lors de cette modification.

À vérifier sur l'appareil destiné au mode 2P :

1. Les deux joueurs maintiennent une direction et Saut : 4 contacts.
2. Chacun ajoute Action : 6 contacts ; les directions et les sauts restent actifs.
3. Chacun ajoute Objet : 8 contacts ; relâcher les doigts dans différents ordres,
   en particulier tous ceux d'un joueur avant ceux de l'autre.
4. Glisser vers l'ancien emplacement de Haut : aucun saut ; vérifier ensuite Haut
   dans un menu, puis reprendre la partie avec le bouton Saut.
5. Passer en arrière-plan avec des contacts maintenus et revenir : aucune touche
   bloquée. Vérifier aussi les gestes système et la détection des paumes de l'appareil.

La capacité effective à détecter huit contacts dépend de l'écran et de son système.
Le code n'impose pas de limite à quatre, six ou huit contacts.

## APK validé pour ce correctif

- Compilation native ARM64 et assemblage Gradle réussis.
- Version `2.0.0-stage2-touchfix`, code 6 ; signature debug valide (v1/v2).
- Alignement ZIP vérifié, dont les cinq bibliothèques natives à 16 Kio ; leur contenu
  correspond aux sorties compilées.
- Taille : 51 957 305 octets.
- SHA-256 : `24d4c706544d62ff93e456535dca756c023f8ffca2c54836c23c2d1ee98b116b`.
- Lint global reste en échec : 31 erreurs dans SDL et 35 avertissements, comme le
  relevé antérieur. Aucun diagnostic dans les classes tactiles modifiées. Le rapport
  local est `android/app/build/reports/lint-results-debug.html`.

## Correctif de réactivité

Les requêtes JNI indiquant l’état menu/partie ne sont plus exécutées pendant chaque
`MotionEvent` ni pendant chaque dessin. Elles sont lues périodiquement par l’activité
et mises en cache ; le chemin tactile ne fait donc plus d’appel natif bloquant lorsqu’un
joueur maintient ou déplace plusieurs doigts. Le code Java a été recompilé avec succès.

Les touches physiques encore maintenues sont aussi réémises toutes les 100 ms. Cette
réémission ne crée pas de répétition dans le jeu lorsque `fDown` est intacte, mais
répare rapidement l’état si le moteur réinitialise ses contrôles pendant une transition
ou un événement de focus sans que les doigts aient bougé.

Le mode portrait 2P est maintenant conservé pendant une partie. Une valeur native
transitoirement incomplète ne peut plus déclencher une rotation de l’activité ; cela
évite la perte de focus Android commune aux deux joueurs. Le retour au mode paysage
reste possible depuis les menus.
