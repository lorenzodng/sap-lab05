package ttt_game_service.infrastructure;

import java.util.HashMap;
import exagonal.Adapter;
import ttt_game_service.application.AccountRepository;
import ttt_game_service.domain.Account;

//implementazione della porta di uscita che collega l'architettura (applicazione) al db degli account
@Adapter
public class InMemoryAccountRepository implements AccountRepository {

	private HashMap<String, Account> userAccounts; //hashamp che associa l'utente all'account
	
	public InMemoryAccountRepository() {
		userAccounts = new HashMap<>();
	}

	//aggiunge un account
	public void addAccount(Account account) {
		userAccounts.put(account.getId(), account);
	}

	//verifica la presenza di un account
	public boolean isPresent(String userName) {
		return userAccounts.containsKey(userName);
	}
	
	//verifica l'autenticazione
	public boolean isValid(String userName, String password) {
		return (userAccounts.containsKey(userName) && userAccounts.get(userName).getPassword().equals(password));
	}
	
}
