// Dernière génération/modification faite par l'IA Claude le 07/10/2026 15:52:12
package com.kerware.e2e.saucedemo.pages;

import com.kerware.e2e.framework.composants.ListeDeComposants;
import com.kerware.e2e.framework.composants.ListeDeroulante;
import com.kerware.e2e.framework.config.Configuration;
import com.kerware.e2e.framework.pages.BasePage;
import com.kerware.e2e.saucedemo.composants.CarteProduit;
import com.kerware.e2e.saucedemo.composants.EnTeteApplication;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Page d'inventaire de SauceDemo (liste des produits), affichée après une connexion réussie.
 *
 * <p>Structure de référence (outer HTML simplifié, voir docs/ARCHITECTURE.md) :</p>
 * <pre>{@code
 * <div data-test="primary-header"> ... </div>                         -> EnTeteApplication
 * <div data-test="secondary-header">
 *   <span data-test="title">Products</span>
 *   <span data-test="active-option">Name (A to Z)</span>
 *   <select data-test="product-sort-container"> az | za | lohi | hilo </select>
 * </div>
 * <div id="inventory_container" data-test="inventory-container">      <- id dupliqué : jamais utilisé
 *   <div data-test="inventory-list">
 *     <div data-test="inventory-item"> ... </div>   x 6                -> CarteProduit
 *   </div>
 * </div>
 * }</pre>
 *
 * <p>L'en-tête (menu, logo, panier et son badge) est commun aux pages connectées :
 * il est modélisé par {@link EnTeteApplication}, pas par cette page.</p>
 */
public class InventairePage extends BasePage {

    public static final String CHEMIN = "/inventory.html";
    public static final String TITRE = "Products";

    private static final Pattern URL_INVENTAIRE = Pattern.compile(".*/inventory\\.html.*");

    private final EnTeteApplication entete;
    private final Locator titreAffiche;
    private final ListeDeroulante listeTri;
    private final Locator libelleTriActif;
    private final Locator listeProduits;
    private final ListeDeComposants<CarteProduit> produits;

    public InventairePage(Page page, Configuration config) {
        super(page, config);

        // Référentiel de composants de la page inventaire

        Locator enTeteSecondaire = page.getByTestId("secondary-header");

        this.entete = EnTeteApplication.de(page);
        this.titreAffiche = enTeteSecondaire.getByTestId("title");
        this.listeTri = new ListeDeroulante("Tri des produits", enTeteSecondaire.getByTestId("product-sort-container"));
        this.libelleTriActif = enTeteSecondaire.getByTestId("active-option");
        this.listeProduits = page.getByTestId("inventory-list");
        this.produits = new ListeDeComposants<>("Produit",
                listeProduits.getByTestId("inventory-item"), CarteProduit::new);
    }

    // ------------------------------------------------------------------ actions métier

    /** Attend l'URL {@value #CHEMIN} et le titre « {@value #TITRE} ». */
    public InventairePage attendreAffichage() {
        log.debug("Attente de l'affichage de la page d'inventaire");
        page.waitForURL(URL_INVENTAIRE);
        titreAffiche.and(page.getByText(TITRE, new Page.GetByTextOptions().setExact(true)))
                .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        return this;
    }

    /**
     * Sélectionne un tri. Ne présume pas de son effet : le test attend le libellé du tri actif
     * ({@link #libelleTriActif()}) avant de lire la liste.
     */
    public InventairePage trierPar(Tri tri) {
        log.info("Tri des produits : {} ({})", tri, tri.codeOption());
        listeTri.choisirParValeur(tri.codeOption());
        return this;
    }

    // ------------------------------------------------------------------ lectures (immédiates)

    /** Noms des produits, dans l'ordre d'affichage. */
    public List<String> nomsAffiches() {
        return listeProduits.getByTestId("inventory-item-name").allInnerTexts().stream()
                .map(String::trim)
                .toList();
    }

    /** Prix des produits, dans l'ordre d'affichage, en {@link BigDecimal}. */
    public List<BigDecimal> prixAffiches() {
        return listeProduits.getByTestId("inventory-item-price").allInnerTexts().stream()
                .map(CarteProduit::convertirPrix)
                .toList();
    }

    /** Couples (nom, prix) dans l'ordre d'affichage. */
    public List<ProduitAffiche> produitsAffiches() {
        List<String> noms = nomsAffiches();
        List<BigDecimal> prix = prixAffiches();
        if (noms.size() != prix.size()) {
            throw new IllegalStateException(noms.size() + " nom(s) pour " + prix.size() + " prix affiché(s)");
        }
        List<ProduitAffiche> affiches = new ArrayList<>(noms.size());
        for (int i = 0; i < noms.size(); i++) {
            affiches.add(new ProduitAffiche(noms.get(i), prix.get(i)));
        }
        return affiches;
    }

    // ------------------------------------------------------------------ composants exposés

    public EnTeteApplication entete() {
        return entete;
    }

    /**
     * Titre affiché dans l'en-tête secondaire (« Products »).
     *
     * <p>{@link BasePage#titre()} renvoie déjà le titre du <em>document</em> (« Swag Labs ») sous forme
     * de {@code String} : ce titre-ci est exposé sous un autre nom, en {@link Locator} pour les assertions.</p>
     */
    public Locator titreAffiche() {
        return titreAffiche;
    }

    public ListeDeroulante listeTri() {
        return listeTri;
    }

    /** Libellé du tri actif affiché au-dessus de la liste déroulante ({@code data-test="active-option"}). */
    public Locator libelleTriActif() {
        return libelleTriActif;
    }

    /** Toutes les cartes produit, dans l'ordre d'affichage. */
    public ListeDeComposants<CarteProduit> produits() {
        return produits;
    }

    /**
     * Carte du produit dont le nom affiché vaut exactement {@code nomExact}.
     *
     * <p>La carte est repérée par son sous-élément {@code inventory-item-name} : elle reste la même
     * si la liste est triée, et aucun {@code data-test} n'est construit à partir du nom.</p>
     */
    public CarteProduit produit(String nomExact) {
        Locator nomCorrespondant = page.getByTestId("inventory-item-name")
                .and(page.getByText(nomExact, new Page.GetByTextOptions().setExact(true)));
        return produits.elementAyant("'" + nomExact + "'", nomCorrespondant);
    }
}
