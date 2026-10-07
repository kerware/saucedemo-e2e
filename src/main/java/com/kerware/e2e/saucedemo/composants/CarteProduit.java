// Dernière génération/modification faite par l'IA Claude le 07/10/2026 15:52:12
package com.kerware.e2e.saucedemo.composants;

import com.kerware.e2e.framework.composants.Bouton;
import com.kerware.e2e.framework.composants.Composant;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Carte d'un produit de l'inventaire : composant composite ancré sur {@code data-test="inventory-item"}.
 *
 * <p>Structure de référence (outer HTML simplifié) :</p>
 * <pre>{@code
 * <div class="inventory_item" data-test="inventory-item">
 *   ...
 *   <div data-test="inventory-item-name">Sauce Labs Backpack</div>
 *   <div data-test="inventory-item-desc">carry.allTheThings() ...</div>
 *   <div class="pricebar">
 *     <div data-test="inventory-item-price">$29.99</div>
 *     <button data-test="add-to-cart-sauce-labs-backpack">Add to cart</button>   <- ou "remove-…" / Remove
 *   </div>
 * </div>
 * }</pre>
 *
 * <p>Le {@code data-test} du bouton est construit à partir du nom du produit et change quand
 * le produit entre dans le panier : il n'est <strong>jamais</strong> utilisé. Le bouton est
 * localisé <em>à l'intérieur de la carte</em> par son rôle et son libellé.</p>
 *
 * <p>Écart constaté avec la capture HTML de référence : sur le site réel, les liens image et titre
 * portent {@code role="button"} et {@code aria-label="View details for <nom>"}. Un simple
 * {@code getByRole(BUTTON)} dans la carte trouve donc 3 éléments ; le libellé est indispensable.</p>
 */
public class CarteProduit extends Composant {

    /** Libellé du bouton quand le produit n'est pas dans le panier. */
    public static final String LIBELLE_AJOUTER = "Add to cart";
    /** Libellé du bouton quand le produit est dans le panier. */
    public static final String LIBELLE_RETIRER = "Remove";

    private static final Pattern LIBELLES_ACTION = Pattern.compile("^(" + LIBELLE_AJOUTER + "|" + LIBELLE_RETIRER + ")$");
    private static final String SYMBOLE_MONETAIRE = "$";

    private final Locator nomProduit;
    private final Locator description;
    private final Locator prix;
    private final Bouton boutonAction;
    private final Bouton boutonAjouter;
    private final Bouton boutonRetirer;

    /** @param racine carte produit ({@code data-test="inventory-item"}) */
    public CarteProduit(String nom, Locator racine) {
        super(nom, racine);
        this.nomProduit = racine.getByTestId("inventory-item-name");
        this.description = racine.getByTestId("inventory-item-desc");
        this.prix = racine.getByTestId("inventory-item-price");
        // Le site réel donne aussi role="button" aux deux liens « View details for … » de la carte :
        // le bouton d'action est donc distingué par ses deux libellés possibles.
        this.boutonAction = new Bouton("Action de " + nom, racine.getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(LIBELLES_ACTION)));
        this.boutonAjouter = new Bouton("Ajouter au panier " + nom, racine.getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(LIBELLE_AJOUTER).setExact(true)));
        this.boutonRetirer = new Bouton("Retirer du panier " + nom, racine.getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(LIBELLE_RETIRER).setExact(true)));
    }

    // ------------------------------------------------------------------ lectures (immédiates)

    /**
     * Nom du produit <em>affiché</em> sur la carte (lecture immédiate).
     *
     * <p>Redéfinit {@link Composant#nom()} : pour une carte produit, le nom métier pertinent est
     * le nom du produit. Le nom utilisé dans les logs reste celui donné à la construction.</p>
     */
    @Override
    public String nom() {
        return nomProduit.innerText().trim();
    }

    public String description() {
        return description.innerText().trim();
    }

    /** Prix affiché, converti sans dépendre de la locale (« $29.99 » → 29.99). */
    public BigDecimal prix() {
        return convertirPrix(prix.innerText());
    }

    /** {@code true} si le bouton de la carte propose de retirer le produit du panier. */
    public boolean estDansLePanier() {
        String libelle = boutonAction.libelle();
        return switch (libelle) {
            case LIBELLE_RETIRER -> true;
            case LIBELLE_AJOUTER -> false;
            default -> throw new IllegalStateException(
                    "Libellé de bouton inattendu sur " + this + " : '" + libelle + "'");
        };
    }

    // ------------------------------------------------------------------ actions métier

    /** Clique sur « Add to cart » (échoue si le produit est déjà dans le panier). */
    public void ajouterAuPanier() {
        log.info("Ajout au panier : {}", nom);
        boutonAjouter.cliquer();
    }

    /** Clique sur « Remove » (échoue si le produit n'est pas dans le panier). */
    public void retirerDuPanier() {
        log.info("Retrait du panier : {}", nom);
        boutonRetirer.cliquer();
    }

    // ------------------------------------------------------------------ composants exposés

    /** Bouton de la carte, quel que soit son libellé : {@code assertThat(carte.boutonAction().racine()).hasText(...)}. */
    public Bouton boutonAction() {
        return boutonAction;
    }

    public Locator nomProduit() {
        return nomProduit;
    }

    public Locator prixAffiche() {
        return prix;
    }

    // ------------------------------------------------------------------ utilitaire

    /**
     * Convertit un prix affiché (« $29.99 ») en {@link BigDecimal}.
     *
     * <p>{@code new BigDecimal(String)} attend toujours un point décimal : la conversion ne dépend
     * pas de la locale du poste ni du navigateur. Aucun {@code double} n'intervient.</p>
     *
     * @throws IllegalArgumentException si le texte n'est pas un montant en dollars
     */
    public static BigDecimal convertirPrix(String texteAffiche) {
        String texte = texteAffiche == null ? "" : texteAffiche.trim();
        if (!texte.startsWith(SYMBOLE_MONETAIRE)) {
            throw new IllegalArgumentException("Prix affiché inattendu : '" + texteAffiche + "'");
        }
        try {
            return new BigDecimal(texte.substring(SYMBOLE_MONETAIRE.length()).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Prix affiché inattendu : '" + texteAffiche + "'", e);
        }
    }
}
