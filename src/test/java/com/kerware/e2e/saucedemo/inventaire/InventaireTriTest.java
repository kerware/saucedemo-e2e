// Dernière génération/modification faite par l'IA Claude le 07/10/2026 15:52:12
package com.kerware.e2e.saucedemo.inventaire;

import com.kerware.e2e.framework.config.Configuration;
import com.kerware.e2e.framework.extension.TestE2E;
import com.kerware.e2e.saucedemo.donnees.CasProduit;
import com.kerware.e2e.saucedemo.donnees.CasTri;
import com.kerware.e2e.saucedemo.pages.InventairePage;
import com.kerware.e2e.saucedemo.pages.LoginPage;
import com.kerware.e2e.saucedemo.pages.ProduitAffiche;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.util.HashSet;
import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestE2E
@Tag("inventaire")
@Tag("tri")
@DisplayName("Inventaire - tri des produits")
class InventaireTriTest {

    private InventairePage inventaire;

    @BeforeEach
    void seConnecter(LoginPage loginPage, Configuration config) {
        // Étant donné un utilisateur connecté, sur la page d'inventaire
        inventaire = loginPage.ouvrir()
                .seConnecterAvecSucces(config.get("utilisateur.defaut"), config.get("motdepasse.defaut"));
    }

    @ParameterizedTest(name = "[{index}] {0} : {2}")
    @CsvFileSource(resources = CasTri.FICHIER, numLinesToSkip = 1, encoding = "UTF-8")
    @DisplayName("Le tri sélectionné réordonne les produits")
    void triDesProduits(@AggregateWith(CasTri.Agregateur.class) CasTri cas) {
        // Et un autre tri déjà appliqué : le tri testé n'est pas actif
        inventaire.trierPar(cas.triPrealable());
        assertThat(inventaire.listeTri().racine()).hasValue(cas.triPrealable().codeOption());
        assertThat(inventaire.libelleTriActif()).not().hasText(cas.libelle());

        // Quand l'utilisateur choisit le tri testé
        inventaire.trierPar(cas.tri());

        // Alors le libellé du tri actif change (attente automatique AVANT toute lecture de la liste)
        assertThat(inventaire.libelleTriActif()).hasText(cas.libelle());
        assertThat(inventaire.listeTri().racine()).hasValue(cas.tri().codeOption());
        assertEquals(cas.tri().codeOption(), inventaire.listeTri().valeurSelectionnee());
        assertEquals(cas.libelle(), inventaire.listeTri().libelleSelectionne());

        // Et les produits sont dans l'ordre défini par le tri. La copie est triée de façon stable :
        // les ex-aequo (même prix) gardent leur ordre affiché, leur ordre relatif n'est donc pas imposé.
        List<ProduitAffiche> affiches = inventaire.produitsAffiches();
        List<ProduitAffiche> ordreAttendu = affiches.stream().sorted(cas.tri().comparateur()).toList();
        assertEquals(ordreAttendu, affiches, () -> "Ordre incorrect pour " + cas);

        // Et le tri n'a ni perdu ni dupliqué de produit
        List<ProduitAffiche> catalogue = CasProduit.catalogue().stream().map(CasProduit::enProduitAffiche).toList();
        assertEquals(catalogue.size(), affiches.size(), () -> "Produits affichés : " + affiches);
        assertEquals(new HashSet<>(catalogue), new HashSet<>(affiches));
    }
}
