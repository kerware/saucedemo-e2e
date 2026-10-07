// Dernière génération/modification faite par l'IA Claude le 07/10/2026 15:52:12
package com.kerware.e2e.framework.composants;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.SelectOption;

import java.util.List;

/**
 * Liste déroulante native ({@code <select>}).
 *
 * <p>Deux façons de choisir une option :</p>
 * <ul>
 *     <li>{@link #choisirParValeur(String)} : par l'attribut {@code value} de l'option, stable
 *         et indépendant de la langue d'affichage (à privilégier) ;</li>
 *     <li>{@link #choisirParLibelle(String)} : par le texte affiché de l'option.</li>
 * </ul>
 *
 * <p>Les lectures ({@link #valeurSelectionnee()}, {@link #libelleSelectionne()}, {@link #options()})
 * sont immédiates, sans attente. Pour vérifier une sélection, préférer l'assertion à attente
 * automatique {@code assertThat(liste.racine()).hasValue(...)}.</p>
 */
public class ListeDeroulante extends Composant {

    public ListeDeroulante(String nom, Locator racine) {
        super(nom, racine);
    }

    /** Sélectionne l'option dont l'attribut {@code value} vaut {@code valeur}. */
    public void choisirParValeur(String valeur) {
        log.info("Choix de la valeur '{}' dans '{}'", valeur, nom);
        racine.selectOption(new SelectOption().setValue(valeur));
    }

    /** Sélectionne l'option dont le texte affiché vaut exactement {@code libelle}. */
    public void choisirParLibelle(String libelle) {
        log.info("Choix du libellé '{}' dans '{}'", libelle, nom);
        racine.selectOption(new SelectOption().setLabel(libelle));
    }

    /** Attribut {@code value} de l'option sélectionnée (lecture immédiate). */
    public String valeurSelectionnee() {
        return racine.inputValue();
    }

    /** Texte affiché de l'option sélectionnée, ou chaîne vide si aucune (lecture immédiate). */
    public String libelleSelectionne() {
        Object libelle = racine.evaluate(
                "liste => liste.selectedIndex < 0 ? '' : liste.options[liste.selectedIndex].text");
        return libelle == null ? "" : libelle.toString().trim();
    }

    /** Textes affichés de toutes les options, dans l'ordre du DOM (lecture immédiate). */
    public List<String> options() {
        return racine.locator("option").allTextContents().stream()
                .map(String::trim)
                .toList();
    }
}
