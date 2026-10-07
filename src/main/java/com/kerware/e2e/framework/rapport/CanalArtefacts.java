// Dernière génération/modification faite par l'IA Claude le 07/10/2026 17:16:32
package com.kerware.e2e.framework.rapport;

import java.util.function.BiConsumer;

/**
 * Canal de publication des artefacts (capture, trace, log, URL) vers le rapport HTML,
 * pour les moteurs qui ne publient pas de « report entries » JUnit.
 *
 * <p>Les tests JUnit Jupiter publient leurs artefacts par {@code ExtensionContext.publishReportEntry}.
 * Le moteur Cucumber n'offre pas cette possibilité aux hooks : ils passent par ce canal.
 * {@link RapportHtmlListener} le branche sur le test en cours au démarrage de chaque test
 * et le débranche à la fin.</p>
 *
 * <p>Limite assumée : un seul test en cours à la fois, ce qui correspond à l'exécution
 * séquentielle imposée par le framework (Playwright n'est pas thread-safe).</p>
 */
public final class CanalArtefacts {

    private static final BiConsumer<String, String> AUCUNE_DESTINATION = (cle, valeur) -> {
    };

    private static volatile BiConsumer<String, String> destination = AUCUNE_DESTINATION;

    private CanalArtefacts() {
    }

    /** Associe un artefact au test en cours ; sans effet si aucun rapport n'écoute. */
    public static void publier(String cle, String valeur) {
        destination.accept(cle, valeur);
    }

    static void brancher(BiConsumer<String, String> nouvelleDestination) {
        destination = nouvelleDestination;
    }

    static void debrancher() {
        destination = AUCUNE_DESTINATION;
    }
}
