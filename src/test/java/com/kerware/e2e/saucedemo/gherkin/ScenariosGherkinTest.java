// Dernière génération/modification faite par l'IA Claude le 07/10/2026 17:16:32
package com.kerware.e2e.saucedemo.gherkin;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.JUNIT_PLATFORM_SHORT_NAMING_STRATEGY_EXAMPLE_NAME_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PUBLISH_QUIET_PROPERTY_NAME;

/**
 * Point d'entrée des scénarios Gherkin ({@code src/test/resources/features/**}.feature).
 *
 * <p>Les features sont sélectionnées comme un package ({@code features}) : c'est ce qu'attend
 * Cucumber 8 (un sélecteur de ressource sur un dossier est refusé avec un avertissement).</p>
 *
 * <p>Suite JUnit Platform qui délègue au moteur Cucumber : lancée par Maven (le nom finit par
 * {@code Test}) et par l'IDE comme n'importe quelle classe de test. Les tags Gherkin
 * ({@code @tri}, {@code @gherkin}…) sont des tags JUnit : {@code -Dgroups=tri} fonctionne.</p>
 *
 * <ul>
 *     <li>glue : hooks du framework (cycle de vie Playwright) + étapes SauceDemo ;</li>
 *     <li>nom des exemples d'un plan de scénario : le nom du scénario, paramètres remplacés
 *         (« Trier par « Name (A to Z) » (TRI-01) ») ;</li>
 *     <li>rapport Cucumber en complément du rapport HTML du framework :
 *         {@code target/rapport-e2e/cucumber.html}.</li>
 * </ul>
 */
@Suite
@SuiteDisplayName("Scénarios Gherkin")
@IncludeEngines("cucumber")
@SelectPackages("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME,
        value = "com.kerware.e2e.framework.cucumber,com.kerware.e2e.saucedemo.etapes")
@ConfigurationParameter(key = JUNIT_PLATFORM_SHORT_NAMING_STRATEGY_EXAMPLE_NAME_PROPERTY_NAME, value = "pickle")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "html:target/rapport-e2e/cucumber.html")
@ConfigurationParameter(key = PLUGIN_PUBLISH_QUIET_PROPERTY_NAME, value = "true")
class ScenariosGherkinTest {
}
