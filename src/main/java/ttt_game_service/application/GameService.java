package ttt_game_service.application;

import exagonal.InBoundPort;
import ttt_game_service.domain.Account;
import ttt_game_service.domain.InvalidJoinException;
import ttt_game_service.domain.TTTSymbol;
import ttt_game_service.domain.UserId;

//interfaccia che contiene tutti i metodi che il client può richiamare per interagire con il sistema
@InBoundPort
public interface GameService  {

	//registra un utente al servizio
	Account registerUser(String userName, String password) throws AccountAlreadyPresentException;

	//esegue il login di un utente al servizio
	UserSession login(String userName, String password) throws LoginFailedException;

	//recupera la sessione dell'utente
	UserSession getUserSession(String sessionId);

	//recupera la sessione del giocatore
	PlayerSession getPlayerSession(String sessionId);

	//crea una nuova partita
	void createNewGame(String gameId) throws GameAlreadyPresentException;

	//esegue il join di un utente ad un partita
	PlayerSession joinGame(UserId userId, String gameId, TTTSymbol symbol, PlayerSessionEventObserver observer) throws InvalidJoinException;

	//definisce un repository per gli account
    void bindAccountRepository(AccountRepository repo);

	//definisce un repository per le partite
    void bindGameRepository(GameRepository repo);
}
