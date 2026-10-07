<!-- Dernière génération/modification faite par l'IA Claude le 07/10/2026 15:52:12 -->
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
| **Injection de dépendances** | Une extension JUnit injecte toute page (`LoginPage`, `InventairePage`), `Page` ou `Configuration` directement en paramètre de test. |
| **Isolation des tests** | Un navigateur partagé et un `BrowserContext` neuf par test (cookies et stockage vierges). |
| **Données externalisées** | Fichiers CSV (`src/test/resources/donnees/`) consommés par des tests paramétrés `@CsvFileSource`, ou lus en entier (`FichierCsv`) pour un catalogue attendu. Les secrets sont résolus par `${cle}`. |
| **Configuration multi-environnements** | `config/<env>.properties`, surchargeable par variable `E2E_*` ou par `-D`. |
| **Logs détaillés** | Logback : console, `execution.log` global et un fichier de log par test. Les mots de passe sont masqués. |
| **Diagnostic des échecs** | Capture d'écran pleine page, trace Playwright (.zip) et URL courante. |
| **Rapport HTML** | `target/rapport-e2e/index.html` : synthèse, filtres, captures, logs et piles d'appels intégrés. |
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

**30 tests** (16 sur la connexion, 14 sur l'inventaire) :

| Classe | Tags | Tests |
|---|---|---|
| `LoginAffichageTest` | `login`, `smoke` | 2 : titre et logo, composants du formulaire |
| `LoginPassantTest` | `login` | 5 (paramétré, `login-passants.csv`) |
| `LoginNonPassantTest` | `login` | 9 : 8 paramétrés (`login-non-passants.csv`) + fermeture du message d'erreur |
| `InventaireCatalogueTest` | `inventaire`, `smoke` | 2 : catalogue affiché (noms et prix), panier vide à l'arrivée |
| `InventairePanierTest` | `inventaire`, `panier` | 8 : ajout de chaque produit (6, paramétré sur `produits.csv`), badge cumulatif, retrait |
| `InventaireTriTest` | `inventaire`, `tri` | 4 (paramétré, `tris.csv`) : les 4 tris |

Tous portent aussi le tag `e2e` (posé par `@TestE2E`). Exemple : `mvn clean verify "-Dgroups=inventaire"`.

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
    │   │   ├── extension/
    │   │   │   ├── PlaywrightExtension.java       contexte par test, injection, capture, trace, logs
    │   │   │   └── TestE2E.java                   annotation composée à poser sur les classes de test
    │   │   ├── composants/                        RÉFÉRENTIEL DE COMPOSANTS GÉNÉRIQUES
    │   │   │   ├── Composant.java
    │   │   │   ├── ChampSaisie.java
    │   │   │   ├── Bouton.java
    │   │   │   ├── MessageErreur.java
    │   │   │   ├── ListeDeroulante.java           <select> : choix par valeur ou libellé
    │   │   │   ├── Badge.java                     compteur, 0 si absent du DOM
    │   │   │   └── ListeDeComposants.java         collection typée d'éléments répétés
    │   │   ├── pages/BasePage.java                base des Page Objects
    │   │   ├── donnees/
    │   │   │   ├── DonneesTest.java               résolution des ${cle} dans les données
    │   │   │   └── FichierCsv.java                lecture d'un CSV complet (hors test paramétré)
    │   │   └── rapport/                           rapport HTML (listener JUnit Platform)
    │   │       ├── RapportHtmlListener.java
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
        │   └── inventaire/
        │       ├── InventaireCatalogueTest.java   smoke : catalogue affiché, panier vide
        │       ├── InventairePanierTest.java      ajout, badge cumulatif, retrait
        │       └── InventaireTriTest.java         paramétré : les 4 tris
        └── resources/
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
| Ouvrir une trace | `mvn exec:java "-Dexec.args=show-trace target/rapport-e2e/traces/<fichier>.zip"` |

Sous **PowerShell**, mettez les arguments `-D...` entre guillemets : `mvn clean verify "-Denv=local"`.

## Documentation

- [Installation](docs/INSTALLATION.md)
- [Utilisation](docs/UTILISATION.md)
- [Architecture et conventions](docs/ARCHITECTURE.md)
- [Historique des versions](CHANGELOG.md)
