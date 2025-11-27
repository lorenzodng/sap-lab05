package ttt_game_service_tests.steps;

import io.cucumber.java.en.*;
import ttt_game_service.application.AccountAlreadyPresentException;
import ttt_game_service.application.GameService;
import ttt_game_service.application.GameServiceImpl;
import ttt_game_service.infrastructure.InMemoryAccountRepository;
import ttt_game_service.infrastructure.InMemoryGameRepository;
import static org.assertj.core.api.Assertions.*;

/* test con "Cucumber"
sintassi:
Given (stato iniziale) -> When (azione eseguita) -> Then (verifica del risultato)
 */
public class RegistrationLoginSteps {

    private String currentPage = ""; //pagina corrente (simula dove si trova l'utente durante l'utilizzo dell'app)
    private String lastError = ""; //ultimo errore verificatosi
    private String lastInfo = ""; //ultimo messaggio di informazione
    private GameService gs;

    //costruttore
	public RegistrationLoginSteps(){
		gs = new GameServiceImpl(); 
		gs.bindAccountRepository(new InMemoryAccountRepository());
		gs.bindGameRepository(new InMemoryGameRepository());
	}

    //l'utente alla pagina di registrazione
    @Given("I am on the registration page")
    public void i_am_on_the_registration_page() {
        currentPage = "registration";
    }

    //l'utente non ha ancora un account
    @Given("I have not registered before")
    public void i_have_not_registered_before() {
    }

    //l'utente esegue la registrazione
    @When("I register with a unique username {string} and a valid password {string}")
    public void i_register_with_unique_username_and_valid_password(String username, String pwd) {
        assertThat(currentPage).isEqualTo("registration"); //verifica che l'utente si trovi sulla pagina di registrazione
        try {
            gs.registerUser(username, pwd); //prova a registrare l’utente
        } catch (AccountAlreadyPresentException ex) {
            lastError = "Username already taken"; //se l'username già presente, salva l'errore
            return;
        }
        lastInfo = "Account created"; //se tutto ok, salva l'informazione di successo
    }

    //l'utente si aspetta un risultato in seguito alla registrazione
    @Then("I should see a confirmation that my account was created")
    public void i_should_see_confirmation() {
        assertThat(lastInfo).isEqualTo("Account created"); //verifica che l'ultimo messaggio di informazione sia "Account created"
    }

    //esiste già un utente registrato con lo stesso username
    @Given("Someone already registered with a username {string}")
    public void someone_already_registered_with_username(String username) {
        try {
            gs.registerUser(username, "any");  //registra l’utente
        } catch (AccountAlreadyPresentException ex) {
            ex.printStackTrace(); //lancia l'eccezione
        }
    }

    //possibili risultati attesi (non utilizzati)

    @Then("I should see an error {string}")
    public void i_should_see_an_error(String message) {
        assertThat(lastError).isEqualTo(message);
    }

    @Then("my account should not be created")
    public void my_account_should_not_be_created() {
        assertThat(lastError).isNotBlank();
    }

}
