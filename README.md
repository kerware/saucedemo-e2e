<!-- Dernière génération/modification faite par l'IA Claude le 07/10/2026 17:16:32 -->
# SauceDemo E2E — Playwright Java · JUnit 6 · JDK 21

Socle d'automatisation de tests end-to-end sur [SauceDemo](https://www.saucedemo.com).
Il applique les bonnes pratiques des frameworks d'automatisation :

| Pratique | Mise en œuvre |
|---|---|
| **Référentiel de composants** | `framework.composants` : `Composant`, `ChampSaisie`, `Bouton`, `MessageErreur`, `ListeDeroulante`, `Badge`. Ce sont des briques réutilisables ancrées sur un `Locator` racine. |
| **Composants composites** | Un composant peut en assembler d'autres : `CarteProduit` (nom, prix, bouton d'action) et `EnTeteApplication` (menu, logo, lien et badge du panier), dans `saucedemo.composants`. L'en-tête commun n'est modélisé qu'une fois. |
| **Collections typées** | `ListeDeComposants<T>` représente des éléments répétés (`ListeDeComposants<CarteProduit>`) : nombre, parcours, accès par position ou par contenu exact. |
| **Page Object Model** | `saucedemo.pages` : `LoginPage` et `InventairePage` assemblent les composants et exposent des actions métier (`seConnecter()`, `trierPar(Tri)`…). |
| **Chaînage de pages** | `loginPage.ouvrir().seConnecterAvecSucces(...)` renvoie l'`InventairePage` affichée. |
| **Injection de dépendances** | Une extension JUnit injecte toute page (`LoginPage`, `InventairePage`), `Page` ou `Configuration` directement en paramètre de test. Côté Cucumber, un `ContexteScenario` est injecté par constructeur dans les classes d'étapes. |
| **Scénarios Gherkin (BDD)** | Fonctionnalités en français (`src/test/resources/features/`) exécutées par Cucumber 8 sur la JUnit Platform, à côté des tests JUnit. Les étapes réutilisent les mêmes pages, composants et données. Même cycle de vie (`CycleDeVieTest`) : contexte isolé, log, capture, trace et rapport. |
| **Isolation des tests** | Un navigateur partagé et un `BrowserContext` neuf par test (cookies et stockage vierges). |
| **Données externalisées** | Fichiers CSV (`src/test/resources/donnees/`) consommés par des tests paramétrés `@CsvFileSource`, ou lus en entier (`FichierCsv`) pour un catalogue attendu. Les secrets sont résolus par `${cle}`. |
| **Configuration multi-environnements** | `config/<env>.properties`, surchargeable par variable `E2E_*` ou par `-D`. |
| **Logs détaillés** | Logback : console, `execution.log` global et un fichier de log par test. Les mots de passe sont masqués. |
| **Diagnostic des échecs** | Capture d'écran pleine page, trace Playwright (.zip) et URL courante. |
| **Rapport HTML** | `target/rapport-e2e/index.html` : synthèse, filtres, captures, logs et piles d'appels intégrés, pour les tests JUnit comme pour les scénarios. En complément, rapport Cucumber `target/rapport-e2e/cucumber.html`. |
| **Assertions web-first** | `PlaywrightAssertions.assertThat(...)` avec attente automatique, sans `Thread.sleep`. |
| **Montants exacts** | Les prix sont manipulés en `BigDecimal`, convertis sans dépendre de la locale. |
| **CI** | Workflow GitHub Actions fourni (`.github/workflows/e2e.yml`). |

## Démarrage rapide

```bash
# 1. Installer les navigateurs Playwright (une seule fois)
mvn compile exec:java "-Dexec.args=install"

# 2. Lancer les tests
mvn clean verify

# 3. Ouvrir le rapport
#    target/rapport-e2e/index.html
```

Prérequis : JDK 21 et Maven 3.9 ou plus récents. La procédure complète est dans [docs/INSTALLATION.md](docs/INSTALLATION.md).

## Tests

**34 tests** : 30 tests JUnit (16 sur la connexion, 14 sur l'inventaire) et 4 scénarios Gherkin.

| Classe / fonctionnalité | Tags | Tests |
|---|---|---|
| `LoginAffichageTest` | `login`, `smoke` | 2 : titre et logo, composants du formulaire |
| `LoginPassantTest` | `login` | 5 (paramétré, `login-passants.csv`) |
| `LoginNonPassantTest` | `login` | 9 : 8 paramétrés (`login-non-passants.csv`) + fermeture du message d'erreur |
| `InventaireCatalogueTest` | `inventaire`, `smoke` | 2 : catalogue affiché (noms et prix), panier vide à l'arrivée |
| `InventairePanierTest` | `inventaire`, `panier` | 8 : ajout de chaque produit (6, paramétré sur `produits.csv`), badge cumulatif, retrait |
| `InventaireTriTest` | `inventaire`, `tri` | 4 (paramétré, `tris.csv`) : les 4 tris |
| `features/inventaire/tri.feature` (Gherkin) | `gherkin`, `inventaire`, `tri` | 4 : plan de scénario « Trier par … », un exemple par tri |

Tous portent aussi le tag `e2e` (posé par `@TestE2E`, ou `@e2e` dans la feature). Exemples : `mvn clean verify "-Dgroups=inventaire"`, `mvn clean verify "-Dgroups=gherkin"`.

Le tri est volontairement couvert deux fois, en JUnit et en Gherkin, pour comparer les deux styles sur le même besoin.

## Arborescence

```
saucedemo-e2e/
├── pom.xml
├── README.md
├── CHANGELOG.md                        historique des versions
├── docs/
│   ├── INSTALLATION.md                 installation pas à pas (Windows / Linux / macOS)
│   ├── UTILISATION.md                  lancer, configurer, lire le rapport, étendre
│   └── ARCHITECTURE.md                 couches, conventions, DOM de référence, pièges du DOM
├── .github/workflows/e2e.yml           intégration continue
└── src/
    ├── main/java/com/kerware/e2e/
    │   ├── framework/                  ── socle réutilisable, indépendant de l'application
    │   │   ├── config/Configuration.java          configuration multi-sources
    │   │   ├── navigateur/SessionNavigateur.java  cycle de vie Playwright / navigateur
    │   │   ├── execution/CycleDeVieTest.java      cycle de vie d'un test (contexte, log, capture, trace), commun JUnit / Cucumber
    │   │   ├── extension/
    │   │   │   ├── PlaywrightExtension.java       adaptateur JUnit : session, injection, délègue à CycleDeVieTest
    │   │   │   └── TestE2E.java                   annotation composée à poser sur les classes de test
    │   │   ├── cucumber/                          adaptateur Cucumber
    │   │   │   ├── HooksPlaywright.java           hooks @Before/@After/@AfterAll, délèguent à CycleDeVieTest
    │   │   │   └── ContexteScenario.java          état d'un scénario injecté dans les étapes (page, pages, page courante)
    │   │   ├── composants/                        RÉFÉRENTIEL DE COMPOSANTS GÉNÉRIQUES
    │   │   │   ├── Composant.java
    │   │   │   ├── ChampSaisie.java
    │   │   │   ├── Bouton.java
    │   │   │   ├── MessageErreur.java
    │   │   │   ├── ListeDeroulante.java           <select> : choix par valeur ou libellé
    │   │   │   ├── Badge.java                     compteur, 0 si absent du DOM
    │   │   │   └── ListeDeComposants.java         collection typée d'éléments répétés
    │   │   ├── pages/
    │   │   │   ├── BasePage.java                  base des Page Objects
    │   │   │   └── FabriquePages.java             instanciation (Page, Configuration), pour JUnit et Cucumber
    │   │   ├── donnees/
    │   │   │   ├── DonneesTest.java               résolution des ${cle} dans les données
    │   │   │   └── FichierCsv.java                lecture d'un CSV complet (hors test paramétré)
    │   │   └── rapport/                           rapport HTML (listener JUnit Platform)
    │   │       ├── RapportHtmlListener.java
    │   │       ├── CanalArtefacts.java            artefacts des scénarios Cucumber vers le rapport
    │   │       ├── GenerateurRapportHtml.java
    │   │       ├── ResultatTest.java
    │   │       └── RepertoireRapport.java
    │   └── saucedemo/                  ── spécifique à l'application testée
    │       ├── composants/                        composants applicatifs (composites)
    │       │   ├── EnTeteApplication.java         en-tête commun des pages connectées
    │       │   └── CarteProduit.java              carte d'un produit de l'inventaire
    │       ├── pages/                             PAGE OBJECTS
    │       │   ├── LoginPage.java
    │       │   ├── InventairePage.java
    │       │   ├── Tri.java                       tris disponibles + comparateur de vérification
    │       │   └── ProduitAffiche.java            instantané (nom, prix) d'un produit affiché
    │       └── donnees/                           modèles des jeux de données
    │           ├── CasLoginPassant.java
    │           ├── CasLoginNonPassant.java
    │           ├── CasProduit.java
    │           └── CasTri.java
    ├── main/resources/META-INF/services/          enregistrement du listener de rapport
    └── test/
        ├── java/com/kerware/e2e/saucedemo/
        │   ├── login/
        │   │   ├── LoginPassantTest.java          paramétré : 5 utilisateurs valides → /inventory.html
        │   │   ├── LoginNonPassantTest.java       paramétré : 8 cas refusés + message d'erreur ; fermeture du message
        │   │   └── LoginAffichageTest.java        smoke : composants de la page
        │   ├── inventaire/
        │   │   ├── InventaireCatalogueTest.java   smoke : catalogue affiché, panier vide
        │   │   ├── InventairePanierTest.java      ajout, badge cumulatif, retrait
        │   │   └── InventaireTriTest.java         paramétré : les 4 tris
        │   ├── etapes/                            ÉTAPES GHERKIN (step definitions)
        │   │   ├── EtapesConnexion.java           « un utilisateur connecté sur la page d'inventaire »
        │   │   ├── EtapesTri.java                 trier, tri actif affiché, ordre, catalogue complet
        │   │   ├── TypesParametres.java           type {tri} : libellé → Tri (via tris.csv)
        │   │   └── TriChoisi.java                 libellé + Tri
        │   └── gherkin/ScenariosGherkinTest.java  point d'entrée (@Suite) des scénarios Gherkin
        └── resources/
            ├── features/inventaire/tri.feature    scénarios Gherkin en français
            ├── config/                            commun / recette / local / ci .properties
            ├── donnees/login/                     login-passants.csv, login-non-passants.csv
            ├── donnees/inventaire/                produits.csv, tris.csv
            ├── logback-test.xml
            └── junit-platform.properties
```

## Exemple de test

```java
@TestE2E
@Tag("inventaire")
@Tag("panier")
@DisplayName("Inventaire - panier")
class InventairePanierTest {

    private InventairePage inventaire;
    private Badge badge;

    @BeforeEach
    void seConnecter(LoginPage loginPage, Configuration config) {
        inventaire = loginPage.ouvrir()
                .seConnecterAvecSucces(config.get("utilisateur.defaut"), config.get("motdepasse.defaut"));
        badge = inventaire.entete().badgePanier();
    }

    @ParameterizedTest(name = "[{index}] {0} : {1}")
    @CsvFileSource(resources = CasProduit.FICHIER, numLinesToSkip = 1, encoding = "UTF-8")
    void ajoutDUnProduit(@AggregateWith(CasProduit.Agregateur.class) CasProduit produit) {
        CarteProduit carte = inventaire.produit(produit.nom());
        carte.ajouterAuPanier();

        assertThat(badge.racine()).hasText("1");
        assertThat(carte.boutonAction().racine()).hasText(CarteProduit.LIBELLE_RETIRER);
    }
}
```

Le test ne contient aucun sélecteur, aucune gestion du navigateur et aucune donnée en dur.

## Exemple de scénario Gherkin

```gherkin
# language: fr
@e2e @gherkin @inventaire @tri
Fonctionnalité: Tri des produits de l'inventaire

  Contexte:
    Étant donné un utilisateur connecté sur la page d'inventaire

  Plan du scénario: Trier par « <tri> » (<id>)
    Étant donné les produits triés par "<préalable>"
    Quand je trie les produits par "<tri>"
    Alors le tri actif affiché est "<tri>"
    Et les produits sont dans l'ordre "<tri>"
    Et tous les produits du catalogue sont présents

    Exemples:
      | id     | tri                 | préalable           |
      | TRI-01 | Name (A to Z)       | Name (Z to A)       |
      | TRI-03 | Price (low to high) | Price (high to low) |
```

Chaque étape est une méthode Java courte qui appelle les pages, par exemple `inventaire().trierPar(tri.tri())`. Voir [docs/UTILISATION.md](docs/UTILISATION.md#7-scénarios-gherkin-cucumber).

## Commandes utiles

| Besoin | Commande |
|---|---|
| Tous les tests | `mvn clean verify` |
| Navigateur visible et ralenti | `mvn clean verify -Denv=local` |
| Firefox | `mvn clean verify -Dnavigateur=firefox` |
| Tests de fumée uniquement | `mvn clean verify -Dgroups=smoke` |
| Tests de l'inventaire | `mvn clean verify -Dgroups=inventaire` |
| Tests du panier / des tris | `mvn clean verify -Dgroups=panier` · `mvn clean verify -Dgroups=tri` |
| Une seule classe | `mvn clean verify -Dtest=LoginNonPassantTest` |
| Scénarios Gherkin uniquement | `mvn clean verify -Dgroups=gherkin` (ou `-Dtest=ScenariosGherkinTest`) |
| Tests JUnit uniquement | `mvn clean verify -DexcludedGroups=gherkin` |
| Ouvrir une trace | `mvn exec:java "-Dexec.args=show-trace target/rapport-e2e/traces/<fichier>.zip"` |

Sous **PowerShell**, mettez les arguments `-D...` entre guillemets : `mvn clean verify "-Denv=local"`.

## Documentation

- [Installation](docs/INSTALLATION.md)
- [Utilisation](docs/UTILISATION.md)
- [Architecture et conventions](docs/ARCHITECTURE.md)
- [Historique des versions](CHANGELOG.md)
