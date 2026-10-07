// Dernière génération/modification faite par l'IA Claude le 07/10/2026 17:16:32
package com.kerware.e2e.saucedemo.etapes;

import com.kerware.e2e.framework.cucumber.ContexteScenario;
import com.kerware.e2e.saucedemo.donnees.CasProduit;
import com.kerware.e2e.saucedemo.pages.InventairePage;
import com.kerware.e2e.saucedemo.pages.ProduitAffiche;
import io.cucumber.java.fr.Alors;
import io.cucumber.java.fr.Etantdonné;
import io.cucumber.java.fr.Quand;

import java.util.HashSet;
import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Étapes du tri des produits de l'inventaire.
 *
 * <p>Mêmes vérifications que {@code InventaireTriTest} : seule la façon d'écrire le scénario change.
 * Chaque étape délègue à {@link InventairePage} ; aucune ne manipule de sélecteur.</p>
 */
public class EtapesTri {

    private final ContexteScenario contexte;

    public EtapesTri(ContexteScenario contexte) {
        this.contexte = contexte;
    }

    private InventairePage inventaire() {
        return contexte.pageCourante(InventairePage.class);
    }

    /** Applique un tri et attend qu'il soit actif : point de départ connu du scénario. */
    @Etantdonné("les produits triés par {tri}")
    public void lesProduitsTriesPar(TriChoisi prealable) {
        inventaire().trierPar(prealable.tri());
        assertThat(inventaire().listeTri().racine()).hasValue(prealable.tri().codeOption());
        assertThat(inventaire().libelleTriActif()).hasText(prealable.libelle());
    }

    /**
     * Sélectionne le tri. Garde-fou : le tri demandé ne doit pas être déjà actif, sinon le scénario
     * ne prouverait pas que le tri agit (« Name (A to Z) » est actif au chargement).
     */
    @Quand("je trie les produits par {tri}")
    public void jeTrieLesProduitsPar(TriChoisi tri) {
        assertThat(inventaire().libelleTriActif()).not().hasText(tri.libelle());
        inventaire().trierPar(tri.tri());
    }

    /** Attente automatique du libellé AVANT toute lecture de la liste par les étapes suivantes. */
    @Alors("le tri actif affiché est {tri}")
    public void leTriActifAfficheEst(TriChoisi tri) {
        assertThat(inventaire().libelleTriActif()).hasText(tri.libelle());
        assertThat(inventaire().listeTri().racine()).hasValue(tri.tri().codeOption());
        assertEquals(tri.libelle(), inventaire().listeTri().libelleSelectionne());
    }

    /**
     * Liste affichée égale à sa copie triée de façon stable par le comparateur du tri :
     * les ex-aequo de prix gardent leur ordre affiché, leur ordre relatif n'est pas imposé.
     */
    @Alors("les produits sont dans l'ordre {tri}")
    public void lesProduitsSontDansLOrdre(TriChoisi tri) {
        assertThat(inventaire().libelleTriActif()).hasText(tri.libelle());
        List<ProduitAffiche> affiches = inventaire().produitsAffiches();
        List<ProduitAffiche> ordreAttendu = affiches.stream().sorted(tri.tri().comparateur()).toList();
        assertEquals(ordreAttendu, affiches, () -> "Ordre incorrect pour le tri " + tri);
    }

    /** Ni perte ni doublon par rapport au catalogue de {@code produits.csv}. */
    @Alors("tous les produits du catalogue sont présents")
    public void tousLesProduitsDuCatalogueSontPresents() {
        List<ProduitAffiche> catalogue = CasProduit.catalogue().stream().map(CasProduit::enProduitAffiche).toList();
        List<ProduitAffiche> affiches = inventaire().produitsAffiches();
        assertEquals(catalogue.size(), affiches.size(), () -> "Produits affichés : " + affiches);
        assertEquals(new HashSet<>(catalogue), new HashSet<>(affiches));
    }
}
