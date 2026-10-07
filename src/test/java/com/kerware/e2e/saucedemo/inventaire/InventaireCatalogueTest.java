// Dernière génération/modification faite par l'IA Claude le 07/10/2026 15:52:12
package com.kerware.e2e.saucedemo.inventaire;

import com.kerware.e2e.framework.config.Configuration;
import com.kerware.e2e.framework.extension.TestE2E;
import com.kerware.e2e.saucedemo.composants.CarteProduit;
import com.kerware.e2e.saucedemo.donnees.CasProduit;
import com.kerware.e2e.saucedemo.pages.InventairePage;
import com.kerware.e2e.saucedemo.pages.LoginPage;
import com.kerware.e2e.saucedemo.pages.ProduitAffiche;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Test de fumée : la page d'inventaire affiche le catalogue attendu, panier vide. */
@TestE2E
@Tag("inventaire")
@Tag("smoke")
@DisplayName("Inventaire - catalogue")
class InventaireCatalogueTest {

    private InventairePage inventaire;

    @BeforeEach
    void seConnecter(LoginPage loginPage, Configuration config) {
        // Étant donné un utilisateur connecté, sur la page d'inventaire
        inventaire = loginPage.ouvrir()
                .seConnecterAvecSucces(config.get("utilisateur.defaut"), config.get("motdepasse.defaut"));
    }

    @Test
    @DisplayName("Les produits affichés correspondent exactement au catalogue (noms et prix)")
    void catalogueAffiche() {
        List<CasProduit> catalogue = CasProduit.catalogue();

        // Autant de cartes que de produits au catalogue
        assertThat(inventaire.produits().elements()).hasCount(catalogue.size());

        // Chaque produit du catalogue a sa carte, avec le bon prix
        for (CasProduit produit : catalogue) {
            CarteProduit carte = inventaire.produit(produit.nom());
            assertThat(carte.racine()).isVisible();
            assertThat(carte.prixAffiche()).hasText("$" + produit.prix().toPlainString());
            assertEquals(produit.prix(), carte.prix(), () -> "Prix de " + produit);
        }

        // Ni produit en trop, ni doublon : même ensemble et même taille
        List<ProduitAffiche> attendus = catalogue.stream().map(CasProduit::enProduitAffiche).toList();
        List<ProduitAffiche> affiches = inventaire.produitsAffiches();
        assertEquals(attendus.size(), affiches.size(), () -> "Produits affichés : " + affiches);
        assertEquals(new HashSet<>(attendus), new HashSet<>(affiches));
    }

    @Test
    @DisplayName("À l'arrivée, le panier est vide : pas de badge et tous les boutons proposent l'ajout")
    void panierVideALArrivee() {
        assertThat(inventaire.produits().elements()).hasCount(CasProduit.catalogue().size());

        // Panier vide = badge ABSENT du DOM (jamais un texte « 0 »)
        assertThat(inventaire.entete().badgePanier().racine()).hasCount(0);
        assertThat(inventaire.entete().badgePanier().racine()).isHidden();
        assertEquals(0, inventaire.entete().badgePanier().valeur());

        for (CarteProduit carte : inventaire.produits().tous()) {
            assertThat(carte.boutonAction().racine()).hasText(CarteProduit.LIBELLE_AJOUTER);
        }
    }
}
