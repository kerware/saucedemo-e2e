package com.kerware.e2e.framework.composants;

import com.microsoft.playwright.Locator;

/** Bouton ou élément cliquable assimilé (input submit, lien d'action). */
public class Bouton extends Composant {

    public Bouton(String nom, Locator racine) {
        super(nom, racine);
    }

    public void cliquer() {
        log.info("Clic sur '{}'", nom);
        racine.click();
    }

    public boolean estActif() {
        return racine.isEnabled();
    }

    /** Libellé affiché : texte du bouton ou attribut value d'un input. */
    public String libelle() {
        String texte = racine.innerText();
        if (texte != null && !texte.isBlank()) {
            return texte.trim();
        }
        String valeur = racine.getAttribute("value");
        return valeur == null ? "" : valeur.trim();
    }
}
