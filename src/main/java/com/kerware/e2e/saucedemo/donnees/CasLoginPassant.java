package com.kerware.e2e.saucedemo.donnees;

import com.kerware.e2e.framework.donnees.DonneesTest;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.aggregator.ArgumentsAggregator;

/**
 * Ligne du fichier {@code donnees/login/login-passants.csv}.
 *
 * <p>Colonnes : id, description, utilisateur, motDePasse.</p>
 */
public record CasLoginPassant(String id, String description, String utilisateur, String motDePasse) {

    /** Le mot de passe n'apparaît jamais dans les logs ni le rapport. */
    @Override
    public String toString() {
        return id + " - " + description + " (" + utilisateur + ")";
    }

    /** Transforme une ligne CSV en {@link CasLoginPassant}, références {@code ${...}} résolues. */
    public static class Agregateur implements ArgumentsAggregator {
        @Override
        public Object aggregateArguments(ArgumentsAccessor ligne, ParameterContext contexte) {
            return new CasLoginPassant(
                    ligne.getString(0),
                    ligne.getString(1),
                    DonneesTest.resoudre(ligne.getString(2)),
                    DonneesTest.resoudre(ligne.getString(3)));
        }
    }
}
