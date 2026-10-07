<!-- Dernière génération/modification faite par l'IA Claude le 07/10/2026 17:16:32 -->
# Historique des versions

Format inspiré de [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/), versionnement [sémantique](https://semver.org/lang/fr/).

## [1.2.0] — 2026-10-07

Couche Gherkin / Cucumber, sans rupture : les 30 tests JUnit sont inchangés et passent toujours.

### Ajouté

- **Scénarios Gherkin en français** : `features/inventaire/tri.feature`, un plan de scénario sur les 4 tris des produits (tags `e2e`, `gherkin`, `inventaire`, `tri`).
- **Étapes** (`saucedemo.etapes`) : `EtapesConnexion`, `EtapesTri`, type de paramètre `{tri}` (`TypesParametres`, `TriChoisi`).
- **Point d'entrée** : suite `ScenariosGherkinTest` (JUnit Platform Suite → moteur Cucumber), lancée par Maven et par l'IDE.
- **Socle Cucumber générique** (`framework.cucumber`) : `HooksPlaywright` (cycle de vie Playwright des scénarios), `ContexteScenario` (état d'un scénario injecté dans les étapes).
- **Socle** : `framework.execution.CycleDeVieTest` (cycle de vie commun JUnit / Cucumber), `framework.pages.FabriquePages`, `framework.rapport.CanalArtefacts`.
- `CasTri.triDuLibelle(libelle)` : correspondance libellé → `Tri` lue dans `tris.csv`.
- Rapport Cucumber `target/rapport-e2e/cucumber.html`, en complément du rapport HTML du framework.
- Dépendances : `cucumber-java`, `cucumber-junit-platform-engine`, `cucumber-picocontainer` (BOM Cucumber 8.0.4), `junit-platform-suite`, `tools.jackson.core:jackson-databind` 3.2.3 (runtime, exigé par les rapports Cucumber 8).

### Modifié

- `PlaywrightExtension` délègue désormais à `CycleDeVieTest` et `FabriquePages`. Comportement et constantes publiques inchangés ; seul le nom du logger des lignes « DEBUT / FIN DU TEST » change (`CycleDeVieTest`).
- `RapportHtmlListener` : branche `CanalArtefacts` sur le test en cours ; range un test issu d'un moteur emboîté (Cucumber) sous sa fonctionnalité.
- `junit-platform.properties` : `cucumber.junit-platform.discovery.as-root-engine=false`.
- **Surefire 3.5.3 → 3.5.6** : nécessaire, car la 3.5.3 ne compte pas les scénarios et laisse le build vert quand un scénario échoue. Effet de bord : dans `target/surefire-reports/*.xml`, l'attribut `classname` vaut désormais le `@DisplayName` de la classe ; les noms de fichiers sont inchangés.
- Version du projet : `1.1.0-SNAPSHOT` → `1.2.0-SNAPSHOT`.
- Documentation : README, `UTILISATION.md` (section 7 « Scénarios Gherkin »), `ARCHITECTURE.md` (couches, cycle de vie commun, couche Gherkin, pièges de l'intégration).

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
