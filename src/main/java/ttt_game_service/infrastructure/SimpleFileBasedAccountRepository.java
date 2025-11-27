package ttt_game_service.infrastructure;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.logging.Logger;
import exagonal.Adapter;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import ttt_game_service.application.AccountRepository;
import ttt_game_service.domain.Account;

//implementazione 2 della porta di uscita che collega l'architettura (applicazione) al db degli account (salva i dati in un file .json)
@Adapter
public class SimpleFileBasedAccountRepository implements AccountRepository {

	static Logger logger = Logger.getLogger("[MyDB]");
	static final String DB_USERS = "users.json"; //nome del file usato come db
	private HashMap<String, Account> userAccounts; //hashmap che associa l'username (id) dell'account all'account
	
	public SimpleFileBasedAccountRepository() {
		userAccounts = new HashMap<>();
		initFromDB();
	}

	//inizializza lo stato degli utenti registrati
	private void initFromDB() {
		try {
			var usersDB = new BufferedReader(new FileReader(DB_USERS)); //apre il file db (FileReader) in lettura (BufferedReader)
			var sb = new StringBuffer(); //crea una stringa "contenitore" di testo
			while (usersDB.ready()) { //per ogni riga...
				sb.append(usersDB.readLine()+"\n"); //legge e lo aggiunge a db
			}
			usersDB.close(); //chiude il file db
			var array = new JsonArray(sb.toString()); //trasforma la stringa in un array json
			for (int i = 0; i < array.size(); i++) { //per ogni elemento...
				var user = array.getJsonObject(i); //memorizza l'elemento
				Account acc = new Account(user.getString("userName"), user.getString("password")); //registra l'utente utilizzando i valori dei campi "userId" e "userName"
				userAccounts.put(acc.getId(), acc); //aggiunge l'account
			}
		} catch (Exception ex) {
			logger.info("DB not found, creating an empty one.");
			saveOnDB(); //crea un nuovo file json
		}
	}

	//memorizza nel db gli utenti registrati
	private void saveOnDB() {
		try {
			JsonArray list = new JsonArray(); //crea un array json
			for (Account ac: userAccounts.values()) { //per ogni utente registrato
				var obj = new JsonObject(); //crea un oggetto json
				obj.put("userName", ac.getUserName()); //aggiunge l'username dell'utente all'oggetto
				obj.put("password", ac.getPassword()); //aggiunge la password dell'utente all'oggetto
				list.add(obj); //aggiunge l'oggetto alla lista
			}
			var usersDB = new FileWriter(DB_USERS); //apre il file db in scrittura
			usersDB.append(list.encodePrettily()); //converte l'array in una stringa (encodePrettily) e scrive sul file db
			usersDB.flush(); //forza la scrittura
			usersDB.close(); //chiude il file db
		} catch (Exception ex) {
			ex.printStackTrace(); //lancia un'eccezione
		}	
	}

	//verifica l'autenticazione
	@Override
	public boolean isValid(String userName, String password) {
		return (userAccounts.containsKey(userName) && userAccounts.get(userName).getPassword().equals(password));
	}

	//aggiunge un account e lo memorizza nel db
	public void addAccount(Account account) {
		userAccounts.put(account.getId(), account);
		saveOnDB();
	}

	//verifica la presenza di un account
	public boolean isPresent(String userName) {
		return userAccounts.containsKey(userName);
	}
	
}
