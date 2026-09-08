# Deuxième étape Android : deux joueurs sur un écran portrait

Cette étape sera activable depuis la sélection des joueurs. Le comportement actuel
reste le comportement par défaut : un joueur tactile en paysage contre des bots,
avec une seule zone de commandes. Le mode portrait ne s'active que lorsqu'on choisit
explicitement deux joueurs humains sur le même appareil.

État d'avancement : le prototype est intégré. L'interface affiche désormais
le choix `Portrait 2P: ON/OFF` lorsque deux humains sont sélectionnés, la commande
Action/Random permet de relancer plusieurs fois les skins des bots dans l'écran équipe,
y compris après leur état Ready, et le menu
de touches contient `Restore defaults`. Le prototype bascule l'activité en portrait
dès l'activation, rend deux viewports inversés et affecte les touches du haut au
joueur 2 (W/A/S/D/E/Q). La caméra et l'image sont encore communes : une prochaine
étape pourra rendre deux caméras indépendantes.

## Décisions de produit

Dans la sélection des joueurs, ajouter une option **Deux joueurs sur le même écran**.
Elle est visible et modifiable uniquement quand exactement deux joueurs humains sont
actifs. Si l'un des deux repasse en CPU ou si un troisième humain est activé, l'option
est désactivée automatiquement. Elle n'est pas proposée pour le réseau, les éditeurs,
les modes non compatibles ou les parties à un seul joueur.

L'option ne doit pas être confondue avec le choix « humain / CPU » existant. La
sélection des joueurs choisit les participants ; l'option de partie choisit la
présentation et l'affectation des contrôles. Le réglage sera réinitialisé à désactivé
si ses conditions ne sont plus remplies. Il sera enregistré dans la configuration
Android afin de conserver le dernier choix, avec migration sûre des anciens fichiers
`options.bin`.

Ajouter à côté de chaque joueur CPU une commande **Skin aléatoire**. Elle doit être
accessible avant que les bots soient déclarés prêts et rester accessible après leur
initialisation. Elle positionne `randomskin[player]` et affiche « Random » ; elle ne
doit pas attendre la validation d'un deuxième périphérique. Le bouton aléatoire destiné
au joueur humain conserve son fonctionnement actuel. Le tirage doit utiliser la liste
des skins valides, comme le fait déjà la logique de sélection, et éviter les skins
impossibles à charger.

Dans le menu de configuration des touches, ajouter **Restaurer les touches par défaut**
pour chaque joueur et chaque périphérique. Une confirmation est nécessaire avant
d'écraser une configuration. Le bouton remet les huit commandes de jeu et les huit
commandes de menu depuis la table `controlkeys`, met à jour l'affichage et écrit la
configuration au prochain enregistrement. Il doit aussi proposer une restauration
globale des quatre joueurs, avec le même avertissement.

## Contrôles tactiles et sécurité des réglages

Le tactile actuel envoie des codes clavier SDL fixes : flèches, Ctrl droit, Shift
droit, Entrée et Échap. Les touches modifiées dans le menu peuvent donc rendre une
commande tactile incohérente. La solution retenue est de faire passer les commandes
tactiles par une couche virtuelle native :

```text
Touch player 1 -> virtual COutputControl[0]
Touch player 2 -> virtual COutputControl[1]
                      |
                 gameplay/menu
```

Cette couche convertit directement gauche, droite, saut, bas, turbo, objet, démarrage
et annulation en `COutputControl`, sans dépendre de `inputConfiguration`. Les claviers,
manettes et le tactile gardent ainsi des chemins séparés. Le bouton de restauration
reste utile pour les périphériques physiques et pour la navigation des menus, mais il
n'est plus une condition de bon fonctionnement du tactile.

Avant cette séparation, ajouter un test qui modifie toutes les touches du joueur 1,
puis vérifie que les commandes tactiles produisent toujours les huit actions attendues.
Après la séparation, vérifier qu'un même appui ne produit pas deux événements lorsque
le clavier et le tactile sont utilisés ensemble.

## Affichage portrait à deux joueurs

Le moteur est actuellement une vue unique 640×480. Le mode à deux joueurs demande un
vrai rendu divisé, pas seulement une rotation des boutons Android :

```text
┌──────────────────────────────┐
│ joueur 2                     │  vue tournée de 180°
│ contrôles du joueur 2        │
├──────────────────────────────┤
│ contrôles du joueur 1        │
│ joueur 1                     │  vue normale
└──────────────────────────────┘
```

Le premier prototype doit conserver une caméra commune et rendre le jeu dans deux
moitiés du même écran, avec la moitié supérieure tournée de 180 degrés. Cela donne
aux joueurs assis face à face une orientation lisible sans modifier la simulation.
Une caméra indépendante par joueur serait une évolution séparée : elle impose de
rendre deux fois la scène, de gérer les objets hors caméra et de vérifier les effets
de bord du multijoueur.

Le rendu SDL doit donc passer par une cible portrait, deux rectangles de viewport et
une transformation pour la moitié du joueur 2. La sélection des joueurs et des
personnages bénéficie donc aussi des deux zones tactiles. Le mode revient en paysage
quand l'option est désactivée ou au retour au menu. Il faut valider la rotation avec les
textures, le texte, les particules, le score, les effets de fondu et les captures.

## Contrôles des deux joueurs

Chaque moitié de l'écran possède sa propre vue de commandes. Les commandes du joueur 1
sont orientées vers le bas de l'écran ; celles du joueur 2 sont tournées de 180 degrés
et placées vers le haut. Les zones tactiles sont bornées à leur moitié pour empêcher
un doigt de piloter l'autre joueur. Le multitouch reste nécessaire pour direction +
saut + action.

Le système de pointeurs doit associer chaque doigt à un joueur dès le premier contact,
puis conserver cette association pendant les mouvements. Une perte de focus relâche
les deux joueurs. Les tests couvriront les contacts simultanés dans les deux moitiés,
les croisements de doigts, la rotation, les diagonales et le passage portrait/paysage.

## Découpage des livrables

1. **Option de sélection** : état, visibilité conditionnelle, validation des deux
   joueurs humains, configuration et migration.
2. **Bots** : commande Skin aléatoire, skin valide, affichage et tests de sélection.
3. **Touches** : restauration individuelle/globale, confirmation, sauvegarde et test
   de régression. Découplage du tactile des touches configurables.
4. **Rendu portrait** : orientation de l'activité, cible de rendu, deux viewports,
   rotation du joueur 2, retour paysage et tests visuels sur plusieurs proportions.
5. **Contrôles séparés** : deux zones tactiles, affectation des pointeurs, commandes
   virtuelles P1/P2, pause et relâchement global.
6. **Validation appareil** : partie locale complète à deux, menus, choix de skin bot,
   restauration des touches, reprise après Home/verrouillage, appareils avec encoche,
   densités différentes et absence de régression du mode solo.

## Critères d'acceptation

- Sans activation, le mode solo contre bots se comporte comme aujourd'hui.
- L'option est impossible avec zéro, un ou plus de deux humains.
- Deux humains activent le portrait et chacun ne reçoit que ses propres commandes.
- Le joueur 2 voit sa moitié orientée vers lui et peut jouer face au joueur 1.
- Un bot peut recevoir un skin aléatoire avant le démarrage et celui-ci est visible.
- Restaurer les touches remet exactement les valeurs par défaut sans modifier les
  autres joueurs.
- Modifier toutes les touches physiques ne casse aucune commande tactile.
- Pause, retour système, rotation contrôlée et destruction de l'activité relâchent
  tous les boutons.
- Le mode portrait est explicitement désactivé pour le réseau jusqu'à validation dédiée.

## Risques à traiter avant l'implémentation

- Le jeu utilise beaucoup d'états globaux de rendu ; il faudra isoler les rectangles
  sans changer la logique de collision.
- Les effets qui supposent 640×480 devront être audités dans les deux viewports.
- La rotation Android doit être pilotée par l'activité uniquement quand le mode est
  actif, sinon le mode paysage actuel reste inchangé.
- Le format binaire des options est fragile : ajouter un champ nécessite une version
  ou un bloc optionnel, jamais une insertion silencieuse au milieu du fichier.
- Le réseau et les menus de configuration ne doivent pas hériter accidentellement du
  mode portrait.
