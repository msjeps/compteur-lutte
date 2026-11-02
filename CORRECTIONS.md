# Compteur Lutte — Corrections

Audit du 2026-11-02 (v0.1). Etat des corrections.

---

## Corrections effectuees (commit `4d175d2`)

### 1. `async/await` incompatible tablettes (CRITIQUE)

- **Fichier** : `index.html` ligne ~2199
- **Probleme** : `async function requestWakeLock()` et `await` sont du ES2017. Le WebView fige des Lenovo TB-X606F (Android 10, pas de mise a jour) ne supporte pas cette syntaxe. Le Wake Lock n'etait jamais acquis sur les tablettes cibles.
- **Correction** : remplace par `navigator.wakeLock.request('screen').then(function(lock) { ... }).catch(function() {})` (ES5).

### 2. Pause pendant SALUT saute le decompte restant

- **Fichier** : `index.html`, fonction `toggleTimer()`
- **Probleme** : si on met en pause pendant la phase "SALUEZ-VOUS", la reprise sautait directement en COMBAT. Le delai de salut restant etait perdu.
- **Correction** : nouveau champ `timer.pausedFrom` memorise l'etat avant pause. La reprise retourne en SALUT si la pause a eu lieu pendant SALUT.

### 3. Scoring actif apres la fin du chrono

- **Fichier** : `index.html`, fonction `majScoringEnabled()`
- **Probleme** : quand le timer etait TERMINE, les boutons de scoring restaient actifs. Les eleves pouvaient ajouter des points apres le buzzer.
- **Correction** : TERMINE retire de la condition `timerActif`. Les boutons sont desormais gris et inactifs quand le temps est ecoule.

### 4. Wake Lock non acquis dans `lancerCombat()`

- **Fichier** : `index.html`, fonction `lancerCombat()`
- **Probleme** : `navigator.wakeLock.request('screen')` retournait une Promise jamais awaitee. Le wake lock n'etait pas acquis et l'ecran pouvait se mettre en veille pendant un combat.
- **Correction** : appelle `requestWakeLock()` (la fonction partagee, corrigee en ES5).

---

## Corrections a faire

### Fonctionnalite

#### 5. Pas d'export CSV (MOYENNE)

- **Impact** : les conventions communes EPS exigent un export CSV (separateur `;`, BOM UTF-8). Seul JSON est disponible.
- **Solution** : ajouter un bouton "Exporter CSV" generant un fichier `CompteurLutte_Classe_Date.csv` avec classement et detail des combats. Detecter `AndroidDownload` pour le wrapper APK.

#### 6. AudioContext cree a chaque bip (MOYENNE)

- **Fichier** : `index.html`, fonction `beep()`
- **Impact** : `new AudioContext()` a chaque appel. Sur un combat avec beaucoup d'actions, peut epuiser les ressources audio du P22T.
- **Solution** : creer un seul AudioContext global, le reutiliser pour tous les bips.

#### 7. Pas de feedback sur doublon d'eleve (BASSE)

- **Fichier** : `index.html`, fonction `ajouterEleve()`
- **Impact** : quand on entre un prenom deja present, seul un border rouge apparait. Pas de message texte. L'eleve ne comprend pas pourquoi l'ajout echoue.
- **Solution** : afficher un message sous l'input ("Ce prenom existe deja").

#### 8. `.en-cours` defini mais jamais utilise (BASSE)

- **Fichier** : `index.html`, CSS ligne ~166 + fonction `afficherListeMatchs()`
- **Impact** : la classe CSS `.match-card.en-cours` existe mais n'est jamais appliquee. Apres un "retour sans valider", le match en cours n'est pas distingue visuellement.
- **Solution** : appliquer la classe au match dont l'index est `etat.currentMatch` dans `afficherListeMatchs()`.

#### 9. Pas de scroll-to-top au retour de l'ecran combat (BASSE)

- **Fichier** : `index.html`, fonctions `retourSansValider()` et `confirmerValidation()`
- **Impact** : apres retour a l'ecran config, la page reste a une position de scroll aleatoire.
- **Solution** : ajouter `window.scrollTo(0, 0)` au retour.

#### 10. Import JSON sans validation de structure (MOYENNE)

- **Fichier** : `index.html`, fonction `importerJSON()`
- **Impact** : `etat = data.seance` assigne directement sans verifier la structure. Un JSON malicieux ou corrompu peut casser l'app.
- **Solution** : valider que `data.seance` contient les champs attendus (`groupes` est un tableau, `matchs` a les bons champs, etc.). Rejeter avec un message clair sinon.

#### 11. Nom de groupe modifiable sans validation (BASSE)

- **Fichier** : `index.html`, fonction `afficherGroupes()`, handler `onchange`
- **Impact** : le nom de groupe accepte une chaine vide ou des doublons.
- **Solution** : valider que le nom n'est pas vide et n'existe pas deja.

#### 12. `for...in` sans `hasOwnProperty` (BASSE)

- **Fichier** : `index.html`, fonction `afficherClassement()`
- **Impact** : `for (var nom in stats)` itere sans filtre. Risque faible mais present si `Object.prototype` est etendu.
- **Solution** : ajouter `if (!stats.hasOwnProperty(nom)) continue;`

### Design / Ergonomie

#### 13. Mode sombre absent (MOYENNE)

- **Impact** : le Design System EPS l'exige. Non implemente.
- **Solution** : ajouter les variables CSS pour le mode sombre sous `@media (prefers-color-scheme: dark)` et un toggle manuel.

#### 14. Boutons scoring < 44px sur petits ecrans (MOYENNE)

- **Fichier** : `index.html`, CSS `@media (max-width: 480px)`
- **Impact** : `min-height: 40px` pour les `.btn-action` est sous le minimum tactile de 44px.
- **Solution** : passer a `min-height: 44px` dans le media query mobile.

#### 15. Pas de page d'aide integree (MOYENNE)

- **Impact** : aucune aide pour les eleves en autonomie. Pas de reference pour les actions de scoring ni la procedure d'arbitrage.
- **Solution** : ajouter une modale "Aide" accessible depuis un bouton dans l'en-tete.

#### 16. Classement ne signale pas les disqualifies (BASSE)

- **Impact** : les joueurs disqualifies apparaissent avec 0 points sans indicateur visuel.
- **Solution** : ajouter un indicateur "DQ" ou une ligne barree dans le classement.

#### 17. Pas de fermeture modale par Escape ou clic overlay (BASSE)

- **Impact** : les modales ne se ferment qu'avec les boutons. UX standard manquant.
- **Solution** : ajouter un listener `keydown` pour Escape et un `onclick` sur l'overlay.

#### 18. Pas de guide PDF (MOYENNE)

- **Impact** : attendu dans la checklist de publication.
- **Solution** : creer un PDF avec regles, utilisation, lien APK.

### RGPD

#### 19. QR code exporte les prenoms (HAUTE)

- **Fichier** : `index.html`, fonction `exporterQR()`
- **Impact** : le payload QR contient les prenoms des eleves (mineurs) avec leurs scores. N'importe qui scannant le QR voit ces donnees. Contredit le principe "aucune sortie de la tablette".
- **Solution** : remplacer les prenoms par des identifiants anonymises dans le QR (initiales ou numeros), ou limiter le QR aux scores de groupe sans noms.

#### 20. Pas de mention RGPD (BASSE)

- **Impact** : aucune notice d'information sur le traitement des donnees dans l'app.
- **Solution** : ajouter une mention dans l'aide ("Les donnees restent sur cette tablette et ne sont pas transmises").

#### 21. Pas de purge automatique (BASSE)

- **Impact** : les donnees persistent indefiniment dans localStorage.
- **Solution** : proposer un rappel de nettoyage apres X jours ou en debut de trimestre.

### Securite

#### 22. Pas de Content Security Policy (BASSE)

- **Impact** : pas de meta CSP. Risque faible car tout est inline et offline.
- **Solution** : ajouter `<meta http-equiv="Content-Security-Policy" content="default-src 'self'; script-src 'unsafe-inline'; style-src 'unsafe-inline'">`.

---

## Checklist avant prochaine publication

```
[ ] Corrections 5-22 ci-dessus triees et appliquees
[ ] CACHE_NAME du sw.js incremente
[ ] Version affichee mise a jour
[ ] Test sur tablette Lenovo TB-X606F
[ ] Export/import JSON round-trip verifie
[ ] QR code teste avec payload reel
[ ] Responsive portrait + paysage sur tablette 10"
[ ] CLAUDE.md mis a jour
```
