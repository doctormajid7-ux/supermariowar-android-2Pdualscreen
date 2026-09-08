# Miniatures des cartes Android

Le premier accès à la liste des cartes pouvait échouer avec un message indiquant que
`files/game/data/maps/cache/0smw.png` ne pouvait pas être ouvert. Le jeu générait bien
la miniature, mais le dossier `maps/cache` n'existait pas encore et le résultat de
`IMG_SavePNG` n'était pas vérifié. Il essayait ensuite de charger le fichier inexistant.

La correction est dans `src/common/gfx/ThumbnailCache.*` et `MI_MapBrowser.cpp` :

- le dossier parent est créé avec `std::filesystem::create_directories` ;
- un cache absent, vide, corrompu ou qui n'est pas une image 160×120 est traité comme
  un cache manquant ;
- la miniature nouvellement générée est conservée et affichée immédiatement ;
- une erreur d'écriture est avertie dans le journal SDL sans faire quitter le jeu ;
- `CMap::saveThumbnail` utilise le même chemin robuste pour la génération complète
  depuis le menu des options.

Les miniatures ne sont pas ajoutées à l'APK : elles sont propres à l'appareil et sont
créées dans son stockage privé. Une réinstallation ou une mise à jour peut donc les
recréer sans agrandir l'APK.

Validation : `tests/thumbnail-cache/test_thumbnail_cache.cpp` couvre la première
création sans dossier, le rechargement, les PNG corrompus et vides, les mauvaises
dimensions, un chemin bloqué par un fichier et la recréation après suppression.
Le test passe avec CTest. L'APK Android ARM64 final a été reconstruit avec succès.
