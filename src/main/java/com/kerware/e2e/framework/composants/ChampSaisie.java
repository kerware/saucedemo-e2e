package com.kerware.e2e.framework.composants;

import com.microsoft.playwright.Locator;

/**
 * Champ de saisie texte (input, textarea).
 *
 * <p>Un champ « sensible » (mot de passe, jeton…) voit sa valeur masquée dans les logs.</p>
 */
public class ChampSaisie extends Composant {

    private static final String VALEUR_MASQUEE = "********";

    private final boolean sensible;

    public ChampSaisie(String nom, Locator racine) {
        this(nom, racine, false);
    }

    public ChampSaisie(String nom, Locator racine, boolean sensible) {
        super(nom, racine);
        this.sensible = sensible;
    }

    /** Fabrique pour un champ dont la valeur ne doit jamais apparaître dans les logs. */
    public static ChampSaisie sensible(String nom, Locator racine) {
        return new ChampSaisie(nom, racine, true);
    }

    /** Remplace le contenu du champ. {@code null} est traité comme une chaîne vide. */
    public void saisir(String valeur) {
        String texte = valeur == null ? "" : valeur;
        log.info("Saisie dans '{}' : {}", nom, pourLog(texte));
        racine.fill(texte);
    }

    public void vider() {
        log.info("Effacement du champ '{}'", nom);
        racine.clear();
    }

    public String valeur() {
        return racine.inputValue();
    }

    public String placeholder() {
        return racine.getAttribute("placeholder");
    }

    public boolean estSensible() {
        return sensible;
    }

    private String pourLog(String texte) {
        if (texte.isEmpty()) {
            return "(vide)";
        }
        return sensible ? VALEUR_MASQUEE : "'" + texte + "'";
    }
}
