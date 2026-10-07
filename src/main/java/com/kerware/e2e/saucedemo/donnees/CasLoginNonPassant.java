package com.kerware.e2e.saucedemo.donnees;

import com.kerware.e2e.framework.donnees.DonneesTest;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.aggregator.ArgumentsAggregator;

/**
 * Ligne du fichier {@code donnees/login/login-non-passants.csv}.
 *
 * <p>Colonnes : id, description, utilisateur, motDePasse, messageAttendu.
 * Une cellule vide représente un champ laissé vide.</p>
 */
public record CasLoginNonPassant(String id, String description, String utilisateur, String motDePasse,
                                 String messageAttendu) {

    @Override
    public String toString() {
        return id + " - " + description + " (" + utilisateur + ") -> \"" + messageAttendu + "\"";
    }

    public static class Agregateur implements ArgumentsAggregator {
        @Override
        public Object aggregateArguments(ArgumentsAccessor ligne, ParameterContext contexte) {
            return new CasLoginNonPassant(
                    ligne.getString(0),
                    ligne.getString(1),
                    DonneesTest.resoudre(ligne.getString(2)),
                    DonneesTest.resoudre(ligne.getString(3)),
                    DonneesTest.resoudre(ligne.getString(4)));
        }
    }
}
