<!-- Dernière génération/modification faite par l'IA Claude le 07/10/2026 15:52:12 -->
# Historique des versions

Format inspiré de [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/), versionnement [sémantique](https://semver.org/lang/fr/).

## [1.1.0] — 2026-10-07

Page d'inventaire : ajout de fonctionnalités, sans rupture. Les 16 tests de connexion sont inchangés.

### Ajouté

- **Référentiel de composants génériques** (`framework.composants`) :
  - `ListeDeroulante` : choix par valeur ou libellé, lecture de la sélection et des options ;
  - `Badge` : compteur numérique, valeur 0 quand il est absent du DOM ;
  - `ListeDeComposants<T>` : collection typée d'éléments répétés (nombre, parcours, accès par position, premier élément qui satisfait un prédicat, élément par contenu exact).
- **Données** (`framework.donnees`) : `FichierCsv`, lecture d'un CSV complet hors test paramétré (catalogue attendu).
- **Composants applicatifs** (`saucedemo.composants`, nouveau package) :
  - `EnTeteApplication` : en-tête commun des pages connectées (logo, menu, lien et badge du panier) ;
  - `CarteProduit` : carte produit composite (nom, description, prix en `BigDecimal`, ajout et retrait du panier).
- **Pages** : `InventairePage`, enum `Tri` (4 tris et leur comparateur de vérification), record `ProduitAffiche`.
- **Chaînage de pages** : `LoginPage.seConnecterAvecSucces(utilisateur, motDePasse)` renvoie l'`InventairePage` affichée.
- **Données de test** : `donnees/inventaire/produits.csv`, `donnees/inventaire/tris.csv` ; records `CasProduit`, `CasTri`.
- **Configuration** : clé `utilisateur.defaut` (`standard_user`).
- **Tests** (14, package `saucedemo.inventaire`) :
  - `InventaireCatalogueTest` (`inventaire`, `smoke`) : catalogue affiché, panier vide à l'arrivée ;
  - `InventairePanierTest` (`inventaire`, `panier`) : ajout de chaque produit, badge cumulatif, retrait jusqu'à disparition du badge ;
  - `InventaireTriTest` (`inventaire`, `tri`) : les 4 tris, avec tri préalable et tolérance des prix ex-aequo.
- **Documentation** : DOM de référence de l'inventaire, table des composants, schéma de navigation, section « Pièges du DOM et parades », ce CHANGELOG.

### Modifié

- Version du projet : `1.0.0-SNAPSHOT` → `1.1.0-SNAPSHOT`.
- `LoginPage` : ajout de `seConnecterAvecSucces(...)` ; `seConnecter(...)` est inchangée (signature et comportement).

### Inchangé

- Versions des dépendances et des plugins.
- `PlaywrightExtension`, rapport HTML, configuration des logs : les nouveaux tests en bénéficient sans modification.

## [1.0.0] — 2026-10-07

### Ajouté

- Socle : `Configuration`, `SessionNavigateur`, `PlaywrightExtension`, `@TestE2E`, rapport HTML, log par test.
- Référentiel : `Composant`, `ChampSaisie`, `Bouton`, `MessageErreur` ; `BasePage`, `DonneesTest`.
- Page de connexion et 16 tests (`LoginAffichageTest`, `LoginPassantTest`, `LoginNonPassantTest`).
