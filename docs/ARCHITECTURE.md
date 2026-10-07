<!-- Dernière génération/modification faite par l'IA Claude le 07/10/2026 15:52:12 -->
# Architecture et conventions

## Couches

```
┌───────────────────────────────────────────────────────────────┐
│ TESTS  (src/test/java)                                        │
│   login/      LoginPassantTest, LoginNonPassantTest,          │
│               LoginAffichageTest                              │
│   inventaire/ InventaireCatalogueTest, InventairePanierTest,  │
│               InventaireTriTest                               │
│   → scénarios Étant donné / Quand / Alors, assertions         │
└──────────────┬────────────────────────────────┬───────────────┘
               │ injecte / chaîne               │ lit
┌──────────────▼──────────────┐   ┌─────────────▼───────────────┐
│ PAGES (Page Object Model)   │   │ DONNÉES                     │
│   LoginPage → InventairePage│   │   CSV + Cas* + ${cle}       │
│   Tri, ProduitAffiche       │   │   FichierCsv (catalogue)    │
└──────────────┬──────────────┘   └─────────────────────────────┘
               │ assemble
┌──────────────▼────────────────────────────────────────────────┐
│ COMPOSANTS APPLICATIFS (saucedemo.composants)                 │
│   EnTeteApplication, CarteProduit  (composites)               │
└──────────────┬────────────────────────────────────────────────┘
               │ assemblent
┌──────────────▼────────────────────────────────────────────────┐
│ RÉFÉRENTIEL DE COMPOSANTS GÉNÉRIQUES (framework.composants)   │
│   Composant ← ChampSaisie, Bouton, MessageErreur,             │
│               ListeDeroulante, Badge                          │
│   ListeDeComposants<T extends Composant>                      │
└──────────────┬────────────────────────────────────────────────┘
               │ s'appuie sur
┌──────────────▼────────────────────────────────────────────────┐
│ SOCLE TECHNIQUE                                               │
│   PlaywrightExtension · SessionNavigateur · Configuration     │
│   Logback (log par test) · RapportHtmlListener                │
└───────────────────────────────────────────────────────────────┘
```

Le package `framework` ne référence **jamais** le package `saucedemo`. Le socle peut donc être extrait tel quel dans un module Maven partagé par plusieurs applications. Un composant ne va dans `framework.composants` que s'il est générique (un `<select>`, un compteur, une collection) ; ce qui décrit un écran de SauceDemo (en-tête, carte produit) va dans `saucedemo.composants`.

## Responsabilités

| Élément | Fait | Ne fait pas |
|---|---|---|
| Test | Enchaîne des actions métier et vérifie le résultat | Manipuler un sélecteur, le navigateur ou une donnée en dur |
| Page | Assemble des composants, expose des actions métier et des lectures, renvoie la page suivante (chaînage) | Faire des assertions |
| Composant applicatif | Assemble des composants génériques sous sa racine (composite) | Connaître la page qui le contient ou faire des assertions |
| Composant générique | Encapsule un élément d'IHM et journalise ses actions | Connaître l'application testée ou faire des assertions |
| Extension | Gère le cycle de vie, l'injection, les captures, les traces et les logs | Contenir de la logique métier |

## Cycle de vie d'un test

```
1er test de l'exécution
 └─ SessionNavigateur.demarrer()          Playwright + Browser (store racine JUnit)

pour chaque test
 ├─ beforeEach  → MDC (log par test), BrowserContext neuf, tracing.start, Page
 ├─ résolution  → LoginPage(page, config) injectée en paramètre
 ├─ @BeforeEach → (inventaire) connexion par l'IHM, InventairePage obtenue par chaînage
 ├─ test        → actions + assertions
 └─ afterEach   → si échec : capture + trace + URL ; fermeture du contexte ;
                  publication des artefacts (report entries)

fin de l'exécution
 ├─ fermeture automatique de SessionNavigateur (AutoCloseable dans le store)
 └─ RapportHtmlListener → target/rapport-e2e/index.html
```

## Navigation entre pages

```
                      ouvrir()
 (navigateur) ───────────────────────► LoginPage  (/)
                                          │
              seConnecter(u, mdp)         │  seConnecterAvecSucces(u, mdp)
              (aucune page renvoyée :     │  = seConnecter + attente de l'URL
               le test vérifie l'échec    │    /inventory.html et du titre « Products »
               ou le succès)              ▼
                                       InventairePage  (/inventory.html)
                                          │
                                          │  entete().ouvrirPanier()   (clic seulement :
                                          ▼                             page panier non modélisée)
                                       (/cart.html)
```

`seConnecter(...)` reste la méthode des tests de connexion (elle ne présume pas du résultat) ; `seConnecterAvecSucces(...)` sert de précondition aux tests des pages connectées. Une page reçue par chaînage est identique à une page injectée : même `Page`, même `Configuration`.

## Conventions

- **Sélecteurs** : `getByTestId()` sur l'attribut `data-test` en priorité, puis `getByRole` ou `getByLabel`, puis CSS en dernier recours. XPath positionnel interdit. **Jamais de `data-test` construit par concaténation** (voir les pièges ci-dessous).
- **Attentes** : uniquement les attentes automatiques de Playwright (`assertThat`, `waitFor`, `waitForURL`). Jamais de `Thread.sleep`. Une lecture immédiate (`nomsAffiches()`, `valeur()`) ne se fait qu'après une assertion qui a attendu l'état voulu.
- **Nommage** : tout est en français métier (`seConnecter`, `messageErreur`, `ajouterAuPanier`). Les identifiants de cas (`LP-xx`, `LNP-xx`, `PR-xx`, `TRI-xx`) assurent la traçabilité vers l'outil de gestion des tests.
- **Données** : aucune donnée en dur dans le code de test (ni identifiant, ni nom de produit). Les secrets passent par `${cle}`.
- **Montants** : `BigDecimal`, jamais `double`.
- **Indépendance** : chaque test part d'un contexte vierge et ne dépend d'aucun autre (panier vide à chaque test).

## DOM de référence : page de connexion

Les locators de `LoginPage` s'appuient sur la structure publique de https://www.saucedemo.com (outer HTML simplifié) :

```html
<div class="login_container">
  <div class="login_logo">Swag Labs</div>
  <div class="login_wrapper">
    <div class="login_wrapper-inner">
      <div id="login_button_container" class="form_column">
        <div class="login-box">
          <form>
            <div class="form_group">
              <input class="input_error form_input" placeholder="Username" type="text"
                     data-test="username" id="user-name" name="user-name" autocorrect="off" autocapitalize="none" value="">
            </div>
            <div class="form_group">
              <input class="input_error form_input" placeholder="Password" type="password"
                     data-test="password" id="password" name="password" autocorrect="off" autocapitalize="none" value="">
            </div>
            <!-- vide tant qu'aucune erreur ; en cas d'erreur : -->
            <div class="error-message-container error">
              <h3 data-test="error">
                <button class="error-button" data-test="error-button"><svg>…</svg></button>
                Epic sadface: Username is required
              </h3>
            </div>
            <input type="submit" class="submit-button btn_action"
                   data-test="login-button" id="login-button" name="login-button" value="Login">
          </form>
        </div>
      </div>
    </div>
  </div>
</div>
```

| Composant métier | Type | Locator |
|---|---|---|
| Logo | `Locator` | `page.locator(".login_logo")` |
| Nom d'utilisateur | `ChampSaisie` | `formulaire.getByTestId("username")` |
| Mot de passe | `ChampSaisie` (sensible) | `formulaire.getByTestId("password")` |
| Bouton Login | `Bouton` | `formulaire.getByTestId("login-button")` |
| Message d'erreur | `MessageErreur` | `formulaire.getByTestId("error")` |
| Fermer le message | `Bouton` | `formulaire.getByTestId("error-button")` |

`formulaire` désigne `page.locator("#login_button_container")`.

Si le DOM de votre version diffère, seule `LoginPage` est à adapter : les tests ne changent pas.

## DOM de référence : page d'inventaire

Structure de https://www.saucedemo.com/inventory.html (outer HTML simplifié : images, styles et 5 des 6 cartes retirés). **Attention** : capture faite avec « Sauce Labs Backpack » déjà dans le panier (badge à 1, bouton « Remove ») ; après une connexion neuve, le badge n'existe pas et tous les boutons affichent « Add to cart ».

```html
<div id="header_container" class="header_container" data-test="header-container">
  <div class="primary_header" data-test="primary-header">                 <!-- EnTeteApplication -->
    <div id="menu_button_container">
      <div class="bm-burger-button">
        <button type="button" id="react-burger-menu-btn">Open Menu</button>
        <img class="bm-icon" alt="Open Menu" data-test="open-menu">
      </div>
      <div class="bm-menu-wrap" aria-hidden="true" hidden="true">          <!-- menu latéral : hors périmètre -->
        <nav class="bm-item-list">
          <a data-test="inventory-sidebar-link">All Items</a>
          <a data-test="about-sidebar-link">About</a>
          <a data-test="logout-sidebar-link">Logout</a>
          <a data-test="reset-sidebar-link">Reset App State</a>
        </nav>
      </div>
    </div>
    <div class="header_label"><div class="app_logo">Swag Labs</div></div>
    <div id="shopping_cart_container" class="shopping_cart_container">
      <a class="shopping_cart_link" data-test="shopping-cart-link">
        <span class="shopping_cart_badge" data-test="shopping-cart-badge">1</span>   <!-- ABSENT si panier vide -->
      </a>
    </div>
  </div>
  <div class="header_secondary_container" data-test="secondary-header">
    <span class="title" data-test="title">Products</span>
    <div class="right_component">
      <span class="select_container">
        <span class="active_option" data-test="active-option">Name (A to Z)</span>
        <select class="product_sort_container" data-test="product-sort-container">
          <option value="az">Name (A to Z)</option>
          <option value="za">Name (Z to A)</option>
          <option value="lohi">Price (low to high)</option>
          <option value="hilo">Price (high to low)</option>
        </select>
      </span>
    </div>
  </div>
</div>
<div id="inventory_container">                                             <!-- id dupliqué -->
  <div id="inventory_container" class="inventory_container" data-test="inventory-container">
    <div class="inventory_list" data-test="inventory-list">
      <div class="inventory_item" data-test="inventory-item">               <!-- CarteProduit -->
        <div class="inventory_item_img">
          <a href="#" id="item_4_img_link" data-test="item-4-img-link"> <img alt="Sauce Labs Backpack"> </a>
        </div>
        <div class="inventory_item_description" data-test="inventory-item-description">
          <div class="inventory_item_label">
            <a href="#" id="item_4_title_link" data-test="item-4-title-link">
              <div class="inventory_item_name " data-test="inventory-item-name">Sauce Labs Backpack</div>
            </a>
            <div class="inventory_item_desc" data-test="inventory-item-desc">carry.allTheThings() with …</div>
          </div>
          <div class="pricebar">
            <div class="inventory_item_price" data-test="inventory-item-price">$29.99</div>
            <button class="btn btn_secondary btn_small btn_inventory " data-test="remove-sauce-labs-backpack"
                    id="remove-sauce-labs-backpack">Remove</button>               <!-- "add-to-cart-…" / Add to cart -->
          </div>
        </div>
      </div>
      <!-- … 5 autres cartes … -->
    </div>
  </div>
</div>
```

Écart constaté au premier lancement sur le site réel : les deux liens `<a>` de chaque carte portent en plus `role="button"` et `aria-label="View details for <nom>"`. Ces attributs n'apparaissent pas dans la capture de référence. Conséquence : `getByRole(BUTTON)` dans une carte renvoie 3 éléments ; le bouton d'action doit être filtré par son libellé.

### Composants de la page d'inventaire

| Composant métier | Type | Locator |
|---|---|---|
| En-tête | `EnTeteApplication` | `page.getByTestId("primary-header")` |
| ↳ Logo | `Locator` | `entete.locator(".app_logo")` |
| ↳ Ouvrir le menu | `Bouton` | `entete.getByRole(BUTTON, name="Open Menu", exact)` |
| ↳ Panier | `Bouton` | `entete.getByTestId("shopping-cart-link")` |
| ↳ Nombre d'articles du panier | `Badge` | `entete.getByTestId("shopping-cart-badge")` |
| Titre « Products » | `Locator` | `enTeteSecondaire.getByTestId("title")` |
| Tri des produits | `ListeDeroulante` | `enTeteSecondaire.getByTestId("product-sort-container")` |
| Libellé du tri actif | `Locator` | `enTeteSecondaire.getByTestId("active-option")` |
| Produits | `ListeDeComposants<CarteProduit>` | `page.getByTestId("inventory-list").getByTestId("inventory-item")` |
| Produit « X » | `CarteProduit` | `produits.filter(has: getByTestId("inventory-item-name") AND getByText("X", exact))` |
| ↳ Nom | `Locator` | `carte.getByTestId("inventory-item-name")` |
| ↳ Description | `Locator` | `carte.getByTestId("inventory-item-desc")` |
| ↳ Prix | `Locator` → `BigDecimal` | `carte.getByTestId("inventory-item-price")` |
| ↳ Ajouter au panier | `Bouton` | `carte.getByRole(BUTTON, name="Add to cart", exact)` |
| ↳ Retirer du panier | `Bouton` | `carte.getByRole(BUTTON, name="Remove", exact)` |
| ↳ Bouton d'action (quel que soit l'état) | `Bouton` | `carte.getByRole(BUTTON, name=/^(Add to cart\|Remove)$/)` |

`enTeteSecondaire` désigne `page.getByTestId("secondary-header")`.

## Pièges du DOM et parades

| Piège | Risque | Parade |
|---|---|---|
| Capture faite avec 1 produit au panier (badge « 1 », bouton « Remove ») | Écrire des tests qui supposent cet état | Chaque test part d'une connexion neuve, panier vide ; l'état initial est vérifié (`InventaireCatalogueTest`, `@BeforeEach` du panier) |
| Badge absent quand le panier est vide | Attendre un texte « 0 » qui n'existe jamais | `assertThat(badge.racine()).isHidden()` / `hasCount(0)` ; `Badge.valeur()` renvoie 0 si absent |
| `id="inventory_container"` présent deux fois | Sélecteur `#inventory_container` ambigu (violation du mode strict) | `data-test="inventory-list"` uniquement |
| `data-test` des boutons dérivé du nom (`add-to-cart-test.allthethings()-t-shirt-(red)`) et changeant avec l'état (`remove-…`) | Chaînes fragiles, caractères spéciaux à échapper, locator invalide après un clic | Carte repérée par son nom exact, puis bouton **dans la carte** par rôle + libellé |
| Liens image et titre en `role="button"` (site réel, absent de la capture) | `getByRole(BUTTON)` ambigu dans la carte | Toujours préciser le libellé du bouton |
| Noms proches (« Bolt T-Shirt » / « T-Shirt (Red) ») | Correspondance partielle qui prend la mauvaise carte | `getByText(nom, exact=true)` |
| Prix ex-aequo ($15.99 deux fois) | Test de tri par prix qui impose un ordre arbitraire entre ex-aequo | Comparateur sur le seul prix + copie triée **stable** : seule la monotonie des prix est vérifiée |
| « Name (A to Z) » actif au chargement | Un test du tri « az » qui passerait sans que le tri agisse | `triPrealable` (différent, contrôlé) appliqué d'abord, et vérification que le libellé attendu n'est **pas** affiché avant le tri testé |
| Prix affichés « $29.99 » | Conversion dépendante de la locale, erreurs d'arrondi en `double` | `CarteProduit.convertirPrix` : retrait du `$`, `new BigDecimal(String)` |
| Mise à jour asynchrone de la liste après un tri | Lire la liste avant le nouveau rendu | Assertion `hasText` sur le libellé du tri actif **avant** toute lecture de liste |
| En-tête commun à toutes les pages connectées | Le recopier dans chaque page | Composant `EnTeteApplication`, obtenu par `entete()` |
| Menu latéral masqué (`hidden="true"`) | Cliquer sur des liens invisibles | Hors périmètre ; seul le bouton d'ouverture est déclaré |

## Évolutions possibles

- **Module partagé** : extraire `framework` en artefact Maven `kerware-e2e-framework`, versionné sémantiquement.
- **Parallélisme** : passer à une `SessionNavigateur` par thread (`ThreadLocal`) et activer `junit.jupiter.execution.parallel.enabled`.
- **Vidéo** : `Browser.NewContextOptions.setRecordVideoDir(...)` sur échec.
- **Authentification réutilisable** : `BrowserContext.storageState()` pour sauter l'écran de login dans les tests des autres pages (aujourd'hui, la connexion passe par l'IHM à chaque test d'inventaire).
- **Page panier** : `PanierPage` renvoyée par `EnTeteApplication.ouvrirPanier()`, en réutilisant `EnTeteApplication` et `ListeDeComposants`.
- **Remontée vers Squash TM** : exploiter les XML de `target/surefire-reports` ou les identifiants `LP-xx`, `LNP-xx`, `PR-xx`, `TRI-xx`.
