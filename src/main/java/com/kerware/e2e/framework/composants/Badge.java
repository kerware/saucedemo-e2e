// Dernière génération/modification faite par l'IA Claude le 07/10/2026 15:52:12
package com.kerware.e2e.framework.composants;

import com.microsoft.playwright.Locator;

/**
 * Badge numérique (compteur), par exemple le nombre d'articles d'un panier.
 *
 * <p>Beaucoup d'applications <strong>retirent le badge du DOM</strong> quand le compteur vaut zéro
 * au lieu d'afficher « 0 ». Ce composant traite donc l'absence comme la valeur 0 :</p>
 * <ul>
 *     <li>{@link #valeur()} renvoie {@code 0} si le badge est absent ou masqué ;</li>
 *     <li>pour <em>vérifier</em> un compteur à zéro, utiliser
 *         {@code assertThat(badge.racine()).isHidden()} (ou {@code hasCount(0)}),
 *         jamais {@code hasText("0")} ;</li>
 *     <li>pour vérifier une valeur non nulle, utiliser
 *         {@code assertThat(badge.racine()).hasText("2")}, qui attend la mise à jour.</li>
 * </ul>
 *
 * <p>{@link #estAffiche()} et {@link #valeur()} sont des lectures immédiates, sans attente.</p>
 */
public class Badge extends Composant {

    public Badge(String nom, Locator racine) {
        super(nom, racine);
    }

    /** {@code true} si le badge est présent dans le DOM et visible (lecture immédiate). */
    public boolean estAffiche() {
        return racine.count() > 0 && racine.isVisible();
    }

    /**
     * Valeur entière affichée, {@code 0} si le badge est absent ou masqué (lecture immédiate).
     *
     * @throws IllegalStateException si le texte affiché n'est pas un entier
     */
    public int valeur() {
        if (!estAffiche()) {
            log.debug("Badge '{}' absent : valeur 0", nom);
            return 0;
        }
        String texte = racine.innerText().trim();
        try {
            int valeur = Integer.parseInt(texte);
            log.debug("Badge '{}' : valeur {}", nom, valeur);
            return valeur;
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Le badge '" + nom + "' n'affiche pas un entier : '" + texte + "'", e);
        }
    }
}
