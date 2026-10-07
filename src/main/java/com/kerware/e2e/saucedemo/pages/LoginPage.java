// Dernière génération/modification faite par l'IA Claude le 07/10/2026 15:52:12
package com.kerware.e2e.saucedemo.pages;

import com.kerware.e2e.framework.composants.Bouton;
import com.kerware.e2e.framework.composants.ChampSaisie;
import com.kerware.e2e.framework.composants.MessageErreur;
import com.kerware.e2e.framework.config.Configuration;
import com.kerware.e2e.framework.pages.BasePage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Page de connexion de SauceDemo (Swag Labs).
 *
 * <p>Structure de référence (outer HTML simplifié, voir docs/ARCHITECTURE.md) :</p>
 * <pre>{@code
 * <div class="login_logo">Swag Labs</div>
 * <div id="login_button_container" class="form_column">
 *   <form>
 *     <input data-test="username" id="user-name" placeholder="Username" type="text">
 *     <input data-test="password" id="password" placeholder="Password" type="password">
 *     <div class="error-message-container error">
 *       <h3 data-test="error"><button data-test="error-button" class="error-button"/>Epic sadface: ...</h3>
 *     </div>
 *     <input data-test="login-button" id="login-button" type="submit" value="Login">
 *   </form>
 * </div>
 * }</pre>
 *
 * <p>Les sélecteurs s'appuient sur l'attribut {@code data-test}, stable et dédié aux tests
 * (déclaré via {@code testid.attribut} dans la configuration).</p>
 */
public class LoginPage extends BasePage {

    public static final String CHEMIN = "/";

    private final Locator logo;
    private final ChampSaisie champUtilisateur;
    private final ChampSaisie champMotDePasse;
    private final Bouton boutonConnexion;
    private final MessageErreur messageErreur;

    public LoginPage(Page page, Configuration config) {
        super(page, config);

        // Référentiel de composants de la page login

        Locator formulaire = page.locator("#login_button_container");

        this.logo = page.locator(".login_logo");
        this.champUtilisateur = new ChampSaisie("Nom d'utilisateur", formulaire.getByTestId("username"));
        this.champMotDePasse = ChampSaisie.sensible("Mot de passe", formulaire.getByTestId("password"));
        this.boutonConnexion = new Bouton("Login", formulaire.getByTestId("login-button"));
        this.messageErreur = new MessageErreur("Message d'erreur",
                formulaire.getByTestId("error"),
                formulaire.getByTestId("error-button"));
    }

    // ------------------------------------------------------------------ actions métier

    public LoginPage ouvrir() {
        naviguer(CHEMIN);
        return attendreAffichage();
    }

    public LoginPage attendreAffichage() {
        boutonConnexion.attendreVisible();
        return this;
    }

    /**
     * Saisit les identifiants et valide le formulaire.
     *
     * <p>Ne présume pas du résultat : le test vérifie ensuite l'URL (succès)
     * ou le message d'erreur (échec).</p>
     */
    public void seConnecter(String utilisateur, String motDePasse) {
        log.info("Tentative de connexion avec l'utilisateur '{}'", utilisateur);
        champUtilisateur.saisir(utilisateur);
        champMotDePasse.saisir(motDePasse);
        boutonConnexion.cliquer();
    }

    /**
     * Se connecte avec des identifiants valides et renvoie la page d'inventaire affichée
     * (chaînage de pages du Page Object Model).
     *
     * <p>Réservé aux préconditions « utilisateur connecté » : si la connexion échoue,
     * l'attente de la page d'inventaire échoue avec un message explicite (URL attendue).
     * Pour tester un refus de connexion, utiliser {@link #seConnecter(String, String)}.</p>
     */
    public InventairePage seConnecterAvecSucces(String utilisateur, String motDePasse) {
        seConnecter(utilisateur, motDePasse);
        return new InventairePage(page, config).attendreAffichage();
    }

    // ------------------------------------------------------------------ composants exposés

    public Locator logo() {
        return logo;
    }

    public ChampSaisie champUtilisateur() {
        return champUtilisateur;
    }

    public ChampSaisie champMotDePasse() {
        return champMotDePasse;
    }

    public Bouton boutonConnexion() {
        return boutonConnexion;
    }

    public MessageErreur messageErreur() {
        return messageErreur;
    }
}
