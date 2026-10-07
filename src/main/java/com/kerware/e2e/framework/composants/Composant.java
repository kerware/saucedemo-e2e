package com.kerware.e2e.framework.composants;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base du référentiel de composants métier.
 *
 * <p>Règles :</p>
 * <ul>
 *     <li>un composant est ancré sur un {@link Locator} racine et ne cherche que <em>sous</em> lui ;</li>
 *     <li>il porte un nom métier, utilisé dans les logs (« Saisie dans 'Mot de passe' ») ;</li>
 *     <li>il expose des actions métier et ses {@link Locator} (pour les assertions Playwright
 *         à attente automatique), mais ne contient <strong>aucune assertion</strong> ;</li>
 *     <li>les locators sont paresseux : rien n'est cherché dans le DOM à la construction.</li>
 * </ul>
 */
public abstract class Composant {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final String nom;
    protected final Locator racine;

    protected Composant(String nom, Locator racine) {
        this.nom = nom;
        this.racine = racine;
    }

    /** Locator racine, à utiliser dans les assertions : {@code assertThat(composant.racine()).isVisible()}. */
    public Locator racine() {
        return racine;
    }

    public String nom() {
        return nom;
    }

    public boolean estVisible() {
        return racine.isVisible();
    }

    public void attendreVisible() {
        log.debug("Attente de l'affichage de '{}'", nom);
        racine.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    }

    public void attendreMasque() {
        log.debug("Attente de la disparition de '{}'", nom);
        racine.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + nom + "]";
    }
}
