// Dernière génération/modification faite par l'IA Claude le 07/10/2026 15:52:12
package com.kerware.e2e.framework.donnees;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lecture d'un fichier CSV de données de test hors test paramétré.
 *
 * <p>Utile quand un test a besoin d'un jeu de données <em>complet</em> (par exemple le catalogue
 * attendu) et pas d'une ligne par invocation comme avec {@code @CsvFileSource}. Les règles
 * sont celles des CSV déjà utilisés par {@code @CsvFileSource} :</p>
 * <ul>
 *     <li>encodage UTF-8, séparateur virgule ;</li>
 *     <li>la première ligne utile est l'en-tête (noms de colonnes) ;</li>
 *     <li>les lignes vides et celles commençant par {@code #} sont ignorées ;</li>
 *     <li>une valeur peut être entourée de guillemets doubles (elle peut alors contenir
 *         des virgules ; {@code ""} représente un guillemet) ;</li>
 *     <li>les valeurs sont débarrassées des espaces en bordure ; les références {@code ${cle}}
 *         ne sont <em>pas</em> résolues ici (utiliser {@link DonneesTest#resoudre(String)}).</li>
 * </ul>
 */
public final class FichierCsv {

    private static final char SEPARATEUR = ',';
    private static final char GUILLEMET = '"';
    private static final String COMMENTAIRE = "#";

    private FichierCsv() {
    }

    /**
     * Lit une ressource du classpath et renvoie ses lignes, chacune sous forme
     * {@code colonne -> valeur} (ordre des colonnes conservé).
     *
     * @param ressource chemin dans le classpath, avec ou sans « / » initial
     *                  (ex. {@code /donnees/inventaire/produits.csv})
     */
    public static List<Map<String, String>> lire(String ressource) {
        String chemin = ressource.startsWith("/") ? ressource.substring(1) : ressource;
        ClassLoader chargeur = Thread.currentThread().getContextClassLoader();
        try (InputStream flux = chargeur.getResourceAsStream(chemin)) {
            if (flux == null) {
                throw new IllegalStateException("Fichier de données introuvable dans le classpath : " + ressource);
            }
            BufferedReader lecteur = new BufferedReader(new InputStreamReader(flux, StandardCharsets.UTF_8));
            List<String> entete = null;
            List<Map<String, String>> lignes = new ArrayList<>();
            int numero = 0;
            String ligne;
            while ((ligne = lecteur.readLine()) != null) {
                numero++;
                if (ligne.isBlank() || ligne.stripLeading().startsWith(COMMENTAIRE)) {
                    continue;
                }
                List<String> valeurs = decouper(ligne, ressource, numero);
                if (entete == null) {
                    entete = valeurs;
                    continue;
                }
                if (valeurs.size() != entete.size()) {
                    throw new IllegalStateException(ressource + " ligne " + numero + " : " + valeurs.size()
                            + " valeur(s) pour " + entete.size() + " colonne(s) " + entete);
                }
                Map<String, String> enregistrement = new LinkedHashMap<>();
                for (int i = 0; i < entete.size(); i++) {
                    enregistrement.put(entete.get(i), valeurs.get(i));
                }
                lignes.add(enregistrement);
            }
            return lignes;
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture impossible de " + ressource, e);
        }
    }

    /** Découpe une ligne CSV en valeurs, en respectant les guillemets. */
    static List<String> decouper(String ligne, String ressource, int numero) {
        List<String> valeurs = new ArrayList<>();
        StringBuilder courante = new StringBuilder();
        boolean entreGuillemets = false;
        for (int i = 0; i < ligne.length(); i++) {
            char c = ligne.charAt(i);
            if (entreGuillemets) {
                if (c == GUILLEMET && i + 1 < ligne.length() && ligne.charAt(i + 1) == GUILLEMET) {
                    courante.append(GUILLEMET);
                    i++;
                } else if (c == GUILLEMET) {
                    entreGuillemets = false;
                } else {
                    courante.append(c);
                }
            } else if (c == GUILLEMET) {
                entreGuillemets = true;
            } else if (c == SEPARATEUR) {
                valeurs.add(courante.toString().trim());
                courante.setLength(0);
            } else {
                courante.append(c);
            }
        }
        if (entreGuillemets) {
            throw new IllegalStateException(ressource + " ligne " + numero + " : guillemet non refermé");
        }
        valeurs.add(courante.toString().trim());
        return valeurs;
    }
}
