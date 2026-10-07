package com.kerware.e2e.framework.rapport;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Résultat d'un test (ou d'un conteneur en erreur) collecté pour le rapport HTML. */
final class ResultatTest {

    enum Statut {
        SUCCES("succes", "Succès"),
        ECHEC("echec", "Échec"),
        INTERROMPU("interrompu", "Interrompu"),
        IGNORE("ignore", "Ignoré");

        final String code;
        final String libelle;

        Statut(String code, String libelle) {
            this.code = code;
            this.libelle = libelle;
        }
    }

    final String idUnique;
    final String suite;
    final String nom;
    final String source;

    Statut statut = Statut.SUCCES;
    Duration duree = Duration.ZERO;
    Throwable erreur;
    String raison;
    final Map<String, String> artefacts = new LinkedHashMap<>();

    ResultatTest(String idUnique, String suite, String nom, String source) {
        this.idUnique = idUnique;
        this.suite = suite;
        this.nom = nom;
        this.source = source;
    }
}
