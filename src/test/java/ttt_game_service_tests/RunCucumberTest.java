package ttt_game_service_tests;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

@Suite //indica che la classe è una suite di test JUnit (raggruppa ed eseguire più test insieme)
@IncludeEngines("cucumber") //dice a JUnit di usare l’engine di Cucumber per eseguire i test
@SelectPackages("ttt") //specifica il package in cui cercare le classi di test Cucumber (ttt)

//parametri di configurazione Cucumber (per messaggi su console)

@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "ttt") //specifica il package in cui cerca i metodi annotati con @Given, @When, @Then

//viene eseguito "registration.feature"
public class RunCucumberTest{}

