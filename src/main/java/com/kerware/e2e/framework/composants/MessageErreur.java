package com.kerware.e2e.framework.composants;

import com.microsoft.playwright.Locator;

/**
 * Bandeau de message d'erreur, éventuellement refermable.
 *
 * <p>Pour vérifier le texte, préférer l'assertion à attente automatique
 * {@code assertThat(message.racine()).hasText(...)} plutôt que {@link #texte()}.</p>
 */
public class MessageErreur extends Composant {

    private final Bouton boutonFermer;

    public MessageErreur(String nom, Locator racine, Locator boutonFermer) {
        super(nom, racine);
        this.boutonFermer = new Bouton("Fermer " + nom, boutonFermer);
    }

    /** Texte affiché (lecture immédiate, sans attente). */
    public String texte() {
        return racine.innerText().trim();
    }

    public void fermer() {
        boutonFermer.cliquer();
    }

    public Bouton boutonFermer() {
        return boutonFermer;
    }
}
