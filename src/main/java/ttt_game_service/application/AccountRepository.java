package ttt_game_service.application;

import ddd.Repository;
import exagonal.OutBoundPort;
import ttt_game_service.domain.Account;

/*
interfaccia che collega l'architettura (applicazione) al db degli account
contiene tutti i metodi che l'architettura utilizza per interagire con il db degli account
 */
@OutBoundPort
public interface AccountRepository extends Repository {

	//aggiunge un account
	void addAccount(Account account);

	//verifica se un account è presente
	boolean isPresent(String userName);

	//verifica la validità dei dati inseriti per l'account
	boolean isValid(String userName, String password);
}
