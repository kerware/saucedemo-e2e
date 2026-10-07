package com.kerware.e2e.framework.donnees;

import com.kerware.e2e.framework.config.Configuration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitaires pour les jeux de données externes.
 *
 * <p>Une cellule de données peut référencer une clé de configuration avec
 * {@code ${cle}} : la valeur réelle est résolue à l'exécution (propriété système,
 * variable d'environnement {@code E2E_...} ou fichier de configuration).
 * Les secrets restent ainsi hors des fichiers de données.</p>
 */
public final class DonneesTest {

    private static final Pattern REFERENCE = Pattern.compile("\\$\\{([^}]+)}");

    private DonneesTest() {
    }

    /** Résout les références {@code ${cle}} ; une cellule vide ou absente devient {@code ""}. */
    public static String resoudre(String valeur) {
        if (valeur == null) {
            return "";
        }
        Matcher matcher = REFERENCE.matcher(valeur);
        StringBuilder resultat = new StringBuilder();
        while (matcher.find()) {
            String cle = matcher.group(1).trim();
            matcher.appendReplacement(resultat, Matcher.quoteReplacement(Configuration.instance().get(cle)));
        }
        matcher.appendTail(resultat);
        return resultat.toString();
    }
}
