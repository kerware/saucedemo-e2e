// Dernière génération/modification faite par l'IA Claude le 07/10/2026 15:52:12
package com.kerware.e2e.saucedemo.composants;

import com.kerware.e2e.framework.composants.Badge;
import com.kerware.e2e.framework.composants.Bouton;
import com.kerware.e2e.framework.composants.Composant;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * En-tête commun à toutes les pages connectées de SauceDemo (inventaire, panier, commande…).
 *
 * <p>Structure de référence (outer HTML simplifié, voir docs/ARCHITECTURE.md) :</p>
 * <pre>{@code
 * <div class="primary_header" data-test="primary-header">
 *   <div id="menu_button_container"> <button id="react-burger-menu-btn">Open Menu</button> ... </div>
 *   <div class="header_label"><div class="app_logo">Swag Labs</div></div>
 *   <div id="shopping_cart_container">
 *     <a class="shopping_cart_link" data-test="shopping-cart-link">
 *       <span class="shopping_cart_badge" data-test="shopping-cart-badge">1</span>   <- absent si panier vide
 *     </a>
 *   </div>
 * </div>
 * }</pre>
 *
 * <p>Le menu latéral (masqué par défaut) est hors périmètre : seul son bouton d'ouverture est déclaré.</p>
 */
public class EnTeteApplication extends Composant {

    private final Locator logo;
    private final Bouton boutonMenu;
    private final Bouton lienPanier;
    private final Badge badgePanier;

    /** @param racine en-tête principal ({@code data-test="primary-header"}) */
    public EnTeteApplication(Locator racine) {
        super("En-tête", racine);
        this.logo = racine.locator(".app_logo");
        this.boutonMenu = new Bouton("Ouvrir le menu", racine.getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Open Menu").setExact(true)));
        this.lienPanier = new Bouton("Panier", racine.getByTestId("shopping-cart-link"));
        this.badgePanier = new Badge("Nombre d'articles du panier", racine.getByTestId("shopping-cart-badge"));
    }

    /** Fabrique à partir de la page : l'en-tête est repéré par {@code data-test="primary-header"}. */
    public static EnTeteApplication de(Page page) {
        return new EnTeteApplication(page.getByTestId("primary-header"));
    }

    /** Clic sur le lien du panier. La page panier n'est pas encore modélisée : aucune page n'est renvoyée. */
    public void ouvrirPanier() {
        lienPanier.cliquer();
    }

    public Locator logo() {
        return logo;
    }

    public Bouton boutonMenu() {
        return boutonMenu;
    }

    public Bouton lienPanier() {
        return lienPanier;
    }

    /** Badge du panier : absent du DOM quand le panier est vide. */
    public Badge badgePanier() {
        return badgePanier;
    }
}
