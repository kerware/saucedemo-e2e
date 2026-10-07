<!-- Dernière génération/modification faite par l'IA Claude le 07/10/2026 15:52:12 -->
# Utilisation

## 1. Lancer les tests

```bash
mvn clean verify                                  # tous les tests, environnement "recette"
mvn clean verify -Denv=local                      # navigateur visible, ralenti, traces systématiques
mvn clean verify -Denv=ci                         # délais élargis pour l'intégration continue
mvn clean verify -Dnavigateur=firefox             # chromium | firefox | webkit
mvn clean verify -Dheadless=false -Dslowmo=500    # surcharge ponctuelle
```

### Sélectionner les tests

```bash
mvn clean verify -Dgroups=smoke                          # par tag (@Tag)
mvn clean verify -Dgroups=login -DexcludedGroups=smoke   # inclusion / exclusion
mvn clean verify -Dtest=LoginPassantTest                 # une classe
mvn clean verify "-Dtest=LoginNonPassantTest#connexionRefusee"   # une méthode
mvn clean verify -Dgroups=inventaire                     # toute la page d'inventaire
mvn clean verify -Dgroups=panier                         # ajout / retrait au panier
mvn clean verify -Dgroups=tri                            # les 4 tris de produits
mvn clean verify -Dgroups=inventaire -DexcludedGroups=tri
```

Tags disponibles :

| Tag | Tests |
|---|---|
| `e2e` | tous (posé par `@TestE2E`) |
| `smoke` | `LoginAffichageTest`, `InventaireCatalogueTest` |
| `login` | `LoginAffichageTest`, `LoginPassantTest`, `LoginNonPassantTest` |
| `inventaire` | `InventaireCatalogueTest`, `InventairePanierTest`, `InventaireTriTest` |
| `panier` | `InventairePanierTest` |
| `tri` | `InventaireTriTest` |

### Ne pas bloquer le build en cas d'échec

```bash
mvn clean verify -Dmaven.test.failure.ignore=true
```

> **PowerShell** : entourer chaque argument `-D` de guillemets : `mvn clean verify "-Dgroups=smoke"`.

## 2. Configuration

### Priorité des sources

1. `-Dcle=valeur` (ligne de commande Maven ou *VM options* de l'IDE)
2. variable d'environnement `E2E_CLE` (points et tirets remplacés par `_`, en majuscules). Exemple : `E2E_BASE_URL`
3. `src/test/resources/config/<env>.properties` (choisi par `-Denv`, `recette` par défaut)
4. `src/test/resources/config/commun.properties`

### Clés disponibles

| Clé | Défaut | Rôle |
|---|---|---|
| `env` | `recette` | Fichier d'environnement à charger |
| `base.url` | `https://www.saucedemo.com` | URL de base (navigations relatives) |
| `navigateur` | `chromium` | `chromium`, `firefox`, `webkit` |
| `navigateur.canal` | — | `chrome` ou `msedge` : navigateur installé sur le poste |
| `navigateur.executable` | — | Chemin d'un exécutable de navigateur |
| `headless` | `true` | Navigateur invisible |
| `slowmo` | `0` | Pause (ms) entre chaque action Playwright |
| `viewport.largeur` / `viewport.hauteur` | `1366` / `768` | Taille de la fenêtre |
| `locale` | `fr-FR` | Langue du navigateur |
| `testid.attribut` | `data-test` | Attribut utilisé par `getByTestId()` |
| `timeout.action.ms` | `10000` | Délai max d'une action (clic, saisie…) |
| `timeout.navigation.ms` | `30000` | Délai max d'une navigation |
| `timeout.assertion.ms` | `15000` | Délai max d'une assertion `assertThat(...)` |
| `traces.mode` | `echec` | `echec`, `toujours` ou `jamais` |
| `motdepasse.defaut` | `secret_sauce` | Mot de passe référencé dans les CSV par `${motdepasse.defaut}` |
| `utilisateur.defaut` | `standard_user` | Utilisateur de la précondition « connecté » des tests d'inventaire (mot de passe : `motdepasse.defaut`) |
| `rapport.repertoire` | `target/rapport-e2e` | Dossier des sorties (**uniquement par `-D`**) |
| `log.niveau` | `DEBUG` | Niveau des logs du framework (**uniquement par `-D`**) |

### Ajouter un environnement

Créer `src/test/resources/config/preprod.properties` avec les seules clés qui diffèrent, puis lancer `mvn clean verify -Denv=preprod`.

## 3. Données de test

Les jeux de données sont dans `src/test/resources/donnees/`, un dossier par fonctionnalité :

| Fichier | Colonnes | Utilisé par |
|---|---|---|
| `login/login-passants.csv` | `id,description,utilisateur,motDePasse` | `LoginPassantTest` |
| `login/login-non-passants.csv` | `id,description,utilisateur,motDePasse,messageAttendu` | `LoginNonPassantTest` |
| `inventaire/produits.csv` | `id,nom,prix` | `InventaireCatalogueTest`, `InventairePanierTest`, `InventaireTriTest` |
| `inventaire/tris.csv` | `id,codeOption,libelle,triPrealable` | `InventaireTriTest` |

### Connexion

**`login-passants.csv`**, colonnes `id,description,utilisateur,motDePasse` :

```csv
LP-01,Utilisateur standard,standard_user,${motdepasse.defaut}
```

**`login-non-passants.csv`**, colonnes `id,description,utilisateur,motDePasse,messageAttendu` :

```csv
LNP-04,Mot de passe vide,standard_user,,"Epic sadface: Password is required"
```

Règles :

- **Ajouter un cas** revient à ajouter une ligne. Aucune modification de code n'est nécessaire.
- **Cellule vide** signifie que le champ est laissé vide.
- **Valeurs contenant une virgule** : les entourer de guillemets doubles.
- **Lignes commençant par `#`** : ce sont des commentaires.
- **`${cle}`** est remplacé par la valeur de configuration `cle`. Cela permet de garder les secrets hors des fichiers versionnés. En CI, il suffit de définir `E2E_MOTDEPASSE_DEFAUT` dans les secrets du pipeline.
- **Les colonnes `id` et `description`** apparaissent dans le nom du test (rapport, IDE, console). Exemple : `[4] LNP-04 : Mot de passe vide`.

Chaque ligne CSV est convertie en objet typé (`CasLoginNonPassant`) par un `ArgumentsAggregator`. Le test manipule ainsi `cas.messageAttendu()` plutôt que des index de colonnes.

### Inventaire

**`produits.csv`**, colonnes `id,nom,prix` : le catalogue attendu.

```csv
PR-06,Test.allTheThings() T-Shirt (Red),15.99
```

- `nom` doit correspondre **exactement** au nom affiché (casse, points, parenthèses) : la carte produit est retrouvée par ce nom.
- `prix` s'écrit sans symbole, avec un point décimal et deux décimales (`7.99`), comme à l'affichage. Il est converti en `BigDecimal`.
- Le fichier sert de deux façons : une ligne par invocation (`@CsvFileSource`, test d'ajout au panier) et catalogue complet (`CasProduit.catalogue()`, lu avec `FichierCsv`) pour vérifier qu'aucun produit ne manque ou n'est en double.
- Les tests « badge cumulatif » et « retrait » prennent leurs produits dans ce fichier (les 3 premiers, puis le premier et le dernier) : aucun nom de produit n'est écrit dans le code.

**`tris.csv`**, colonnes `id,codeOption,libelle,triPrealable` :

```csv
TRI-01,az,Name (A to Z),za
```

- `codeOption` et `triPrealable` sont des valeurs d'option HTML : `az`, `za`, `lohi`, `hilo` (converties en enum `Tri`).
- `libelle` est le texte attendu dans le libellé du tri actif.
- `triPrealable` est appliqué **avant** le tri testé et doit en être différent (contrôlé au chargement). « Name (A to Z) » est actif par défaut : le sélectionner directement ne prouverait rien.
- Nom du test : `[1] TRI-01 : Name (A to Z)`.

### Précondition « utilisateur connecté »

Les tests d'inventaire se connectent par l'IHM dans un `@BeforeEach`, avec les clés `utilisateur.defaut` et `motdepasse.defaut` :

```java
inventaire = loginPage.ouvrir()
        .seConnecterAvecSucces(config.get("utilisateur.defaut"), config.get("motdepasse.defaut"));
```

Pour lancer les tests d'inventaire avec un autre profil : `mvn clean verify "-Dgroups=inventaire" "-Dutilisateur.defaut=visual_user"`.

## 4. Résultats d'exécution

Tout est produit dans `target/rapport-e2e/` :

```
target/rapport-e2e/
├── index.html                 rapport HTML (à ouvrir dans un navigateur)
├── captures/                  captures d'écran pleine page des tests en échec
├── traces/                    traces Playwright des tests en échec (ou toutes si traces.mode=toujours)
└── logs/
    ├── execution.log          log complet de l'exécution
    ├── hors-test.log          logs émis hors d'un test (démarrage, arrêt)
    └── <Classe>_<methode>_<n>.log   un fichier par test
```

### Le rapport HTML

- **Synthèse** : nombre de tests, succès, échecs, ignorés, taux de réussite et durée.
- **Environnement** : URL, navigateur, mode headless, versions de Java et de l'OS.
- **Filtres** : Tous, Échecs, Succès, Ignorés.
- **Détail par test en échec** (déplié automatiquement) :
  - message d'erreur ;
  - URL au moment de l'échec ;
  - capture d'écran (cliquable) ;
  - lien vers la trace Playwright ;
  - log détaillé du test ;
  - pile d'appels.

### Le log d'un test (extrait)

```
10:42:01.120 INFO  PlaywrightExtension - ========== DEBUT DU TEST : [4] LNP-04 : Mot de passe vide ==========
10:42:01.512 INFO  LoginPage           - Ouverture de la page https://www.saucedemo.com/
10:42:01.803 INFO  e2e.navigateur      - Navigation vers https://www.saucedemo.com/
10:42:01.950 INFO  LoginPage           - Tentative de connexion avec l'utilisateur 'standard_user'
10:42:01.951 INFO  ChampSaisie         - Saisie dans 'Nom d'utilisateur' : 'standard_user'
10:42:01.990 INFO  ChampSaisie         - Saisie dans 'Mot de passe' : (vide)
10:42:02.020 INFO  Bouton              - Clic sur 'Login'
10:42:02.105 INFO  PlaywrightExtension - [SUCCES] [4] LNP-04 : Mot de passe vide
```

Les champs déclarés sensibles (`ChampSaisie.sensible(...)`) apparaissent masqués (`********`).

### Ouvrir une trace Playwright

La trace permet de rejouer le test pas à pas : DOM, réseau, console et capture à chaque action.

```bash
mvn exec:java "-Dexec.args=show-trace target/rapport-e2e/traces/LoginNonPassantTest_connexionRefusee_01.zip"
```

On peut aussi glisser le fichier `.zip` sur https://trace.playwright.dev. La trace reste locale au navigateur.

### Rapport Surefire standard (complément)

Les fichiers XML JUnit restent produits dans `target/surefire-reports/`, pour intégration dans Jenkins, GitLab ou Squash TM. Un rapport HTML Surefire peut aussi être généré :

```bash
mvn surefire-report:report-only
```

## 5. Étendre le framework

### Ajouter un composant au référentiel

Un composant **générique** (réutilisable sur n'importe quelle application) va dans `framework/composants` et hérite de `Composant` :

```java
public class CaseACocher extends Composant {
    public CaseACocher(String nom, Locator racine) { super(nom, racine); }

    public void cocher() {
        log.info("Cochage de '{}'", nom);
        racine.check();
    }
}
```

Un composant **applicatif** (propre à SauceDemo, souvent composite) va dans `saucedemo/composants`. Il assemble des composants génériques sous sa racine, comme `CarteProduit` :

```java
public CarteProduit(String nom, Locator racine) {
    super(nom, racine);
    this.prix = racine.getByTestId("inventory-item-price");
    this.boutonAjouter = new Bouton("Ajouter au panier " + nom, racine.getByRole(AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName(LIBELLE_AJOUTER).setExact(true)));
}
```

Pour des éléments répétés, utiliser `ListeDeComposants` avec le constructeur du composant comme fabrique :

```java
produits = new ListeDeComposants<>("Produit", page.getByTestId("inventory-item"), CarteProduit::new);
produits.nombre();                                          // lecture immédiate
assertThat(produits.elements()).hasCount(6);                // assertion avec attente
produits.elementContenantTexte("Sauce Labs Onesie");        // par contenu exact (stable après un tri)
```

Règles du référentiel :

- un nom métier ;
- des locators relatifs à la racine ;
- une action journalisée ;
- aucune assertion ;
- `framework` n'importe jamais une classe de `saucedemo`.

### Ajouter une page

```java
public class PanierPage extends BasePage {
    public static final String CHEMIN = "/cart.html";

    private final EnTeteApplication entete;

    public PanierPage(Page page, Configuration config) {   // constructeur obligatoire
        super(page, config);
        this.entete = EnTeteApplication.de(page);           // en-tête commun : réutilisé, pas recopié
    }

    public EnTeteApplication entete() {
        return entete;
    }
}
```

La page est alors injectable dans n'importe quel test : `void monTest(PanierPage panier) { ... }`.

Pour le **chaînage de pages**, une action qui mène à une autre page renvoie cette page une fois affichée :

```java
public InventairePage seConnecterAvecSucces(String utilisateur, String motDePasse) {
    seConnecter(utilisateur, motDePasse);
    return new InventairePage(page, config).attendreAffichage();
}
```

### Ajouter une classe de test

```java
@TestE2E
@DisplayName("Ma fonctionnalité")
class MaFonctionnaliteTest {
    @Test
    void monScenario(LoginPage loginPage) { ... }
}
```

`@TestE2E` suffit : navigateur, contexte isolé, logs, captures et rapport sont gérés automatiquement.

## 6. Intégration continue

Le fichier `.github/workflows/e2e.yml` installe le JDK 21 et Chromium, lance `mvn verify -Denv=ci`, puis publie `target/rapport-e2e/` en artefact, même en cas d'échec.

Le principe est le même pour GitLab CI ou Jenkins :

1. `mvn exec:java "-Dexec.args=install --with-deps chromium"` ;
2. `mvn verify -Denv=ci` ;
3. archiver `target/rapport-e2e/**` et `target/surefire-reports/*.xml`.

On peut aussi utiliser l'image Docker officielle `mcr.microsoft.com/playwright/java:v1.55.0-noble`, qui embarque le JDK, Maven et les navigateurs.

## 7. Dépannage

| Symptôme | Piste |
|---|---|
| `TimeoutError` sur `hasURL(...inventory...)` avec `performance_glitch_user` | Augmenter `timeout.assertion.ms` (connexion volontairement lente, environ 5 s). |
| Test instable | Lancer avec `-Denv=local` (visible + ralenti + traces), puis ouvrir la trace. |
| `ParameterResolutionException ... constructeur public (Page, Configuration)` | La page injectée doit déclarer exactement ce constructeur. |
| Pas de `index.html` | Vérifier `src/main/resources/META-INF/services/org.junit.platform.launcher.TestExecutionListener`. |
| Caractères accentués illisibles dans la console Windows | Lancer `chcp 65001` avant Maven. Les fichiers de log sont toujours en UTF-8. |
