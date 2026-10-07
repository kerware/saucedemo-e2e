package com.kerware.e2e.saucedemo.login;

import com.kerware.e2e.framework.extension.TestE2E;
import com.kerware.e2e.saucedemo.donnees.CasLoginNonPassant;
import com.kerware.e2e.saucedemo.pages.LoginPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestE2E
@Tag("login")
@DisplayName("Connexion - cas non passants")
class LoginNonPassantTest {

    private static final Pattern URL_INVENTAIRE = Pattern.compile(".*/inventory\\.html.*");

    @ParameterizedTest(name = "[{index}] {0} : {1}")
    @CsvFileSource(resources = "/donnees/login/login-non-passants.csv", numLinesToSkip = 1, encoding = "UTF-8")
    @DisplayName("Une connexion invalide est refusée avec le message attendu")
    void connexionRefusee(@AggregateWith(CasLoginNonPassant.Agregateur.class) CasLoginNonPassant cas,
                          LoginPage loginPage) {
        // Étant donné la page de connexion affichée
        loginPage.ouvrir();

        // Quand l'utilisateur tente de se connecter avec des identifiants invalides
        loginPage.seConnecter(cas.utilisateur(), cas.motDePasse());

        // Alors le message d'erreur attendu s'affiche et l'utilisateur reste sur la page de connexion
        assertThat(loginPage.messageErreur().racine()).isVisible();
        assertThat(loginPage.messageErreur().racine()).hasText(cas.messageAttendu());
        assertThat(loginPage.page()).not().hasURL(URL_INVENTAIRE);
    }

    @Test
    @DisplayName("Le message d'erreur peut être refermé")
    void fermetureDuMessageDErreur(LoginPage loginPage) {
        loginPage.ouvrir();
        loginPage.seConnecter("", "");
        assertThat(loginPage.messageErreur().racine()).isVisible();

        loginPage.messageErreur().fermer();

        assertThat(loginPage.messageErreur().racine()).isHidden();
    }
}
