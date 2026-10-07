package com.kerware.e2e.framework.rapport;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Arborescence des sorties d'exécution (rapport, captures, traces, logs).
 *
 * <pre>
 * target/rapport-e2e/
 *   index.html          rapport HTML
 *   captures/           captures d'écran des tests en échec
 *   traces/             traces Playwright (.zip) des tests en échec
 *   logs/               execution.log + un fichier de log par test
 * </pre>
 *
 * <p>Le répertoire se change uniquement par {@code -Drapport.repertoire=...}
 * (également lu par logback-test.xml, pour que logs et rapport restent ensemble).</p>
 */
public final class RepertoireRapport {

    public static final String CAPTURES = "captures";
    public static final String TRACES = "traces";
    public static final String LOGS = "logs";

    private RepertoireRapport() {
    }

    public static Path racine() {
        return Path.of(System.getProperty("rapport.repertoire", "target/rapport-e2e"));
    }

    /** Chemin absolu d'un fichier de sortie, répertoire parent créé si besoin. */
    public static Path fichier(String sousRepertoire, String nomFichier) {
        Path dossier = racine().resolve(sousRepertoire);
        try {
            Files.createDirectories(dossier);
        } catch (IOException e) {
            throw new UncheckedIOException("Création impossible de " + dossier, e);
        }
        return dossier.resolve(nomFichier);
    }

    /** Chemin relatif à la racine du rapport, en séparateurs « / » (utilisable en lien HTML). */
    public static String relatif(String sousRepertoire, String nomFichier) {
        return sousRepertoire + "/" + nomFichier;
    }
}
