package com.kerware.e2e.saucedemo.login;

import com.kerware.e2e.framework.extension.TestE2E;
import com.kerware.e2e.saucedemo.pages.LoginPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Test de fumée : la page de connexion s'affiche avec tous ses composants. */
@TestE2E
@Tag("login")
@Tag("smoke")
@DisplayName("Connexion - affichage de la page")
class LoginAffichageTest {

    private LoginPage loginPage;

    @BeforeEach
    void ouvrirLaPage(LoginPage loginPage) {
        this.loginPage = loginPage.ouvrir();
    }

    @Test
    @DisplayName("Le titre et le logo Swag Labs sont affichés")
    void titreEtLogo() {
        assertThat(loginPage.page()).hasTitle("Swag Labs");
        assertThat(loginPage.logo()).hasText("Swag Labs");
    }

    @Test
    @DisplayName("Les champs et le bouton de connexion sont présents et vides")
    void composantsDuFormulaire() {
        assertThat(loginPage.champUtilisateur().racine()).isVisible();
        assertThat(loginPage.champUtilisateur().racine()).hasAttribute("placeholder", "Username");
        assertThat(loginPage.champUtilisateur().racine()).isEmpty();

        assertThat(loginPage.champMotDePasse().racine()).isVisible();
        assertThat(loginPage.champMotDePasse().racine()).hasAttribute("placeholder", "Password");
        assertThat(loginPage.champMotDePasse().racine()).hasAttribute("type", "password");

        assertThat(loginPage.boutonConnexion().racine()).isEnabled();
        assertThat(loginPage.boutonConnexion().racine()).hasValue("Login");

        assertThat(loginPage.messageErreur().racine()).isHidden();
    }
}
