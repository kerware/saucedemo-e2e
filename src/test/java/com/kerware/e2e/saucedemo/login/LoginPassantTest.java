package com.kerware.e2e.saucedemo.login;

import com.kerware.e2e.framework.extension.TestE2E;
import com.kerware.e2e.saucedemo.donnees.CasLoginPassant;
import com.kerware.e2e.saucedemo.pages.LoginPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestE2E
@Tag("login")
@DisplayName("Connexion - cas passants")
class LoginPassantTest {

    private static final Pattern URL_INVENTAIRE = Pattern.compile(".*/inventory\\.html.*");

    @ParameterizedTest(name = "[{index}] {0} : {1}")
    @CsvFileSource(resources = "/donnees/login/login-passants.csv", numLinesToSkip = 1, encoding = "UTF-8")
    @DisplayName("Un utilisateur autorisé est redirigé vers l'inventaire")
    void connexionReussie(@AggregateWith(CasLoginPassant.Agregateur.class) CasLoginPassant cas,
                          LoginPage loginPage) {
        // Étant donné la page de connexion affichée
        loginPage.ouvrir();


        // Quand l'utilisateur saisit des identifiants valides
        loginPage.seConnecter(cas.utilisateur(), cas.motDePasse());

        // Alors il est redirigé vers la page d'inventaire, sans message d'erreur
        assertThat(loginPage.page()).hasURL(URL_INVENTAIRE);
        assertThat(loginPage.messageErreur().racine()).isHidden();
    }
}
