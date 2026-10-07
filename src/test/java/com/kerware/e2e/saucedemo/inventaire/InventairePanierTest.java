// Dernière génération/modification faite par l'IA Claude le 07/10/2026 15:52:12
package com.kerware.e2e.saucedemo.inventaire;

import com.kerware.e2e.framework.composants.Badge;
import com.kerware.e2e.framework.config.Configuration;
import com.kerware.e2e.framework.extension.TestE2E;
import com.kerware.e2e.saucedemo.composants.CarteProduit;
import com.kerware.e2e.saucedemo.donnees.CasProduit;
import com.kerware.e2e.saucedemo.pages.InventairePage;
import com.kerware.e2e.saucedemo.pages.LoginPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestE2E
@Tag("inventaire")
@Tag("panier")
@DisplayName("Inventaire - panier")
class InventairePanierTest {

    private InventairePage inventaire;
    private Badge badge;

    @BeforeEach
    void seConnecter(LoginPage loginPage, Configuration config) {
        // Étant donné un utilisateur connecté, sur la page d'inventaire, panier vide
        inventaire = loginPage.ouvrir()
                .seConnecterAvecSucces(config.get("utilisateur.defaut"), config.get("motdepasse.defaut"));
        badge = inventaire.entete().badgePanier();
        assertThat(badge.racine()).isHidden();
    }

    @ParameterizedTest(name = "[{index}] {0} : {1}")
    @CsvFileSource(resources = CasProduit.FICHIER, numLinesToSkip = 1, encoding = "UTF-8")
    @DisplayName("Ajout d'un produit au panier")
    void ajoutDUnProduit(@AggregateWith(CasProduit.Agregateur.class) CasProduit produit) {
        CarteProduit carte = inventaire.produit(produit.nom());

        // Quand l'utilisateur ajoute le produit au panier
        carte.ajouterAuPanier();

        // Alors le badge affiche 1 et le bouton de la carte propose le retrait
        assertThat(badge.racine()).hasText("1");
        assertThat(carte.boutonAction().racine()).hasText(CarteProduit.LIBELLE_RETIRER);
        assertTrue(carte.estDansLePanier(), () -> produit + " devrait être dans le panier");

        // Et les autres produits proposent toujours l'ajout
        for (CasProduit autre : CasProduit.catalogue()) {
            if (!autre.nom().equals(produit.nom())) {
                assertThat(inventaire.produit(autre.nom()).boutonAction().racine())
                        .hasText(CarteProduit.LIBELLE_AJOUTER);
            }
        }
    }

    @Test
    @DisplayName("Le badge cumule les ajouts successifs : 1, 2 puis 3")
    void badgeCumulatif() {
        List<CasProduit> aAjouter = CasProduit.catalogue().subList(0, 3);

        for (int i = 0; i < aAjouter.size(); i++) {
            inventaire.produit(aAjouter.get(i).nom()).ajouterAuPanier();
            assertThat(badge.racine()).hasText(String.valueOf(i + 1));
        }
        assertEquals(aAjouter.size(), badge.valeur());
    }

    @Test
    @DisplayName("Retirer des produits décrémente le badge, qui disparaît quand le panier est vide")
    void retraitDuPanier() {
        List<CasProduit> catalogue = CasProduit.catalogue();
        // Premier et dernier produits du catalogue (le dernier a des points et des parenthèses dans son nom)
        CarteProduit premier = inventaire.produit(catalogue.getFirst().nom());
        CarteProduit second = inventaire.produit(catalogue.getLast().nom());

        // Étant donné deux produits dans le panier
        premier.ajouterAuPanier();
        second.ajouterAuPanier();
        assertThat(badge.racine()).hasText("2");

        // Quand l'utilisateur en retire un, le badge vaut 1
        premier.retirerDuPanier();
        assertThat(badge.racine()).hasText("1");
        assertThat(premier.boutonAction().racine()).hasText(CarteProduit.LIBELLE_AJOUTER);

        // Quand il retire le second, le badge disparaît du DOM
        second.retirerDuPanier();
        assertThat(badge.racine()).hasCount(0);
        assertThat(badge.racine()).isHidden();
        assertEquals(0, badge.valeur());
        assertFalse(second.estDansLePanier());
    }
}
