package ttt_game_service.infrastructure;

import java.util.logging.Level;
import java.util.logging.Logger;
import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.*;
import io.vertx.ext.web.*;
import io.vertx.ext.web.handler.StaticHandler;
import ttt_game_service.application.AccountAlreadyPresentException;
import ttt_game_service.application.GameAlreadyPresentException;
import ttt_game_service.application.GameService;
import ttt_game_service.application.LoginFailedException;
import ttt_game_service.domain.InvalidJoinException;
import ttt_game_service.domain.InvalidMoveException;
import ttt_game_service.domain.TTTSymbol;

/*
controller di backend (intermediario client <-> servizio principale):
client -> controller che utilizza un server -> servizio principale
 */
public class VertxGameServiceController extends VerticleBase  {

	private int port; //porta su cui il server ascolta le richieste http
	static Logger logger = Logger.getLogger("[TicTacToe Backend]");
	private GameService gameService; //servizio principale
	
	public VertxGameServiceController(GameService service, int port) {
		this.port = port;
		logger.setLevel(Level.INFO);
		this.gameService = service;
	}

	//avvia il server (eseguito automaticamente alla chiamata "vertx.deployVerticle(server)")
	public Future<?> start() {
		logger.log(Level.INFO, "TTT Game Service initializing...");
		HttpServer server = vertx.createHttpServer(); //crea un'istanza del server http
		
		Router router = Router.router(vertx); //crea una rotta che gestisce le richiesta http
		router.route(HttpMethod.POST, "/api/registerUser").handler(this::registerUser); //rotta per registrare un nuovo utente
		router.route(HttpMethod.POST, "/api/login").handler(this::login); //rotta per il login di un utente
		router.route(HttpMethod.POST, "/api/createGame").handler(this::createNewGame); //rotta per creare una nuova partita
		router.route(HttpMethod.POST, "/api/joinGame").handler(this::joinGame); //rotta per far entrare l'utente in una partita
		router.route(HttpMethod.POST, "/api/makeAMove").handler(this::makeAMove); //rotta per eseguire una mossa
		this.handleEventSubscription(server); //registra un websocket handler al server per ascoltare le richieste del client

		router.route("/public/*").handler(StaticHandler.create()); //gestisce le richieste del client che iniziano con "public", relative all'aspetto della pagina web

		var fut = server.requestHandler(router).listen(port); //avvia il server sulla porta specificata
		fut.onSuccess(res -> { //in caso di avvio con successo
			logger.log(Level.INFO, "TTT Game Service ready - port: " + port); //stampa un messaggio di log
		});

		return fut; //restituisce la future
	}

	//registra un nuovo utente
	protected void registerUser(RoutingContext context) { //context è l'oggetto che rappresenta la richiesta http
		logger.log(Level.INFO, "RegisterUser request");
		context.request().handler(buf -> { //prende il body del messaggio http inviato dal client
			JsonObject userInfo = buf.toJsonObject(); //converte il body in un oggetto json
			logger.log(Level.INFO, "Payload: " + userInfo);
			var userName = userInfo.getString("userName"); //estrae il valore del campo "userName"
			var password = userInfo.getString("password"); //estrae il valore del campo "password"
			var reply = new JsonObject(); //crea un oggetto json di risposta al client
			try {
				gameService.registerUser(userName, password); //registra l'utente nel db
				reply.put("result", "ok"); //popola l'oggetto con un'informazione di successo
				sendReply(context.response(), reply); //invia la risposta al client
			} catch (AccountAlreadyPresentException ex) {
				reply.put("result", "error"); //popola l'oggetto con un'informazione di errore
				reply.put("error", ex.getMessage()); //popola l'oggetto con la specifica dell'errore
				sendReply(context.response(), reply); //invia la risposta al client
			} catch (Exception ex1) {
				sendError(context.response()); //invia un errore al client
			}
		});
	}

	//esegue il login di un utente
	protected void login(RoutingContext context) {
		logger.log(Level.INFO, "Login request");
		context.request().handler(buf -> { //prende il body del messaggio http inviato dal client
			JsonObject userInfo = buf.toJsonObject(); //converte il body in un oggetto json
			logger.log(Level.INFO, "Payload: " + userInfo);
			var userName = userInfo.getString("userName"); //estrae il valore del campo "userName"
			var password = userInfo.getString("password"); //estrae il valore del campo "password"
			var reply = new JsonObject(); //crea un oggetto json di risposta al client
			try {
				var session = gameService.login(userName, password); //esegue il login dell'utente
				reply.put("result", "ok"); //popola l'oggetto con un'informazione di successo
				reply.put("sessionId", session.getSessionId()); //popola l'oggetto con l'informazione della sessione utente creata con il login
				sendReply(context.response(), reply); //invia la risposta al client
			} catch (LoginFailedException ex) {
				reply.put("result", "login-failed"); //popola l'oggetto con un'informazione di errore
				reply.put("error", ex.getMessage()); //popola l'oggetto con la specifica dell'errore
				sendReply(context.response(), reply); //invia la risposta al client
			} catch (Exception ex1) {
				sendError(context.response()); //invia un errore al client
			}			
		});
	}

	//crea una nuova partita
	protected void createNewGame(RoutingContext context) {
		logger.log(Level.INFO, "CreateNewGame request - " + context.currentRoute().getPath());
		context.request().handler(buf -> { //prende il body del messaggio http inviato dal client
			JsonObject userInfo = buf.toJsonObject(); //converte il body in un oggetto json
			logger.log(Level.INFO, "Payload: " + userInfo);
			var sessionId = userInfo.getString("sessionId"); //estrae il valore del campo "sessionId"
			var reply = new JsonObject(); //crea un oggetto json di risposta al client
			try {
				var session = gameService.getUserSession(sessionId); //recupera la sessione dell'utente
				var gameId = userInfo.getString("gameId"); //estrae il valore del campo "gameId"
				session.createNewGame(gameId); //crea una partita
				reply.put("result", "ok"); //popola l'oggetto con un'informazione di successo
				sendReply(context.response(), reply); //invia la risposta al client
			} catch (GameAlreadyPresentException ex) {
				reply.put("result", "error"); //popola l'oggetto con un'informazione di errore
				reply.put("error", "game-already-present"); //popola l'oggetto con la specifica dell'errore
				sendReply(context.response(), reply); //invia la risposta al client
			} catch (Exception ex1) {
				sendError(context.response()); //invia un errore al client
			}			
		});		
	}

	//consente a un utente di unirsi a una partita
	protected void joinGame(RoutingContext context) {
		logger.log(Level.INFO, "JoinGame request - " + context.currentRoute().getPath());
		context.request().handler(buf -> { //prende il body del messaggio http inviato dal client
			JsonObject joinInfo = buf.toJsonObject(); //converte il body in un oggetto json
			logger.log(Level.INFO, "Join info: " + joinInfo);
			String sessionId = joinInfo.getString("sessionId"); //estrae il valore del campo "sessionId"
			String gameId = joinInfo.getString("gameId"); //estrae il valore del campo "gameId"
			String symbol = joinInfo.getString("symbol"); //estrae il valore del campo "symbol"

			var reply = new JsonObject(); //crea un oggetto json di risposta al client
			try {
				var session = gameService.getUserSession(sessionId); //recupera la sessione dell'utente
				var playerSession = session.joinGame(gameId, symbol.equals("X") ? TTTSymbol.X : TTTSymbol.O, new VertxPlayerSessionEventObserver(vertx.eventBus())); //esegue il join dell'utente nella partita
				reply.put("playerSessionId", playerSession.getId()); //popola l'oggetto con l'informazione della sessione giocatore creata con il join
				reply.put("result", "ok"); //popola l'oggetto con un'informazione di successo
				sendReply(context.response(), reply);
			} catch (InvalidJoinException  ex) {
				reply.put("result", "error"); //popola l'oggetto con un'informazione di errore
				reply.put("error", ex.getMessage()); //popola l'oggetto con la specifica dell'errore
				sendReply(context.response(), reply); //invia la risposta al client
			} catch (Exception ex1) {
				sendError(context.response()); //invia un errore al client
			}			
		});
	}

	//esegue una mossa
	protected void makeAMove(RoutingContext context) {
		logger.log(Level.INFO, "MakeAMove request - " + context.currentRoute().getPath());
		context.request().handler(buf -> { //prende il body del messaggio http inviato dal client
			var reply = new JsonObject(); //crea un oggetto json di risposta al client
			try {
				JsonObject moveInfo = buf.toJsonObject(); //converte il body in un oggetto json
				logger.log(Level.INFO, "move info: " + moveInfo);
				String playerSessionId = moveInfo.getString("playerSessionId"); //estrae il valore del campo "playerSessionId"
				int x = Integer.parseInt(moveInfo.getString("x")); //estrae il valore del campo "x" e lo converte in un intero
				int y = Integer.parseInt(moveInfo.getString("y")); //estrae il valore del campo "y" e lo converte in un intero
				var ps = gameService.getPlayerSession(playerSessionId); //recupera la sessione del giocatore

				ps.makeMove(x, y); //fa eseguire al giocatore una mossa
				reply.put("result", "accepted"); //popola l'oggetto con un'informazione di successo
				sendReply(context.response(), reply); //invia la risposta al client
			} catch (InvalidMoveException ex) {
				reply.put("result", "invalid-move"); //popola l'oggetto con un'informazione di errore
				sendReply(context.response(), reply); //invia la risposta al client
			} catch (Exception ex1) {
				reply.put("result", ex1.getMessage()); //popola l'oggetto con la specifica dell'errore
				try {
					sendReply(context.response(), reply); //invia la risposta al client
				} catch (Exception ex2) {
					sendError(context.response()); //invia un errore al client
				}				
			}
		});
	}

	//registra un websocket handler al server
	protected void handleEventSubscription(HttpServer server) {
		server.webSocketHandler(webSocket -> { //registra un handlder per websocket
			logger.log(Level.INFO, "New TTT subscription accepted.");
			webSocket.textMessageHandler(openMsg -> { //imposta un handler per i messaggi ricevuti dal client
				logger.log(Level.INFO, "For game: " + openMsg);
				JsonObject obj = new JsonObject(openMsg); //converte il messaggio ricevuto dal client in un oggetto json
				String playerSessionId = obj.getString("playerSessionId"); //estrae il valore del campo "playerSessionId"

				EventBus eb = vertx.eventBus(); //recupera l'event bus di vertx
				eb.consumer(playerSessionId, msg -> { //iscrive l'event bus all'indirizzo creato e, ogni volta che arriva un messaggio all'event bus...
					JsonObject ev = (JsonObject) msg.body(); //...lo converte in json
					logger.log(Level.INFO, "Event: " + ev.encodePrettily());
					webSocket.writeTextMessage(ev.encodePrettily()); //lo invia al client tramite websocket
				});
				
				var ps = gameService.getPlayerSession(playerSessionId); //recupera la sessione del giocatore corrispondente al servizio principale (ovvero l'utente in prima persona)
				var en = ps.getPlayerSessionEventNotifier(); //recupera il notificatore di eventi della sessione del giocatore
				en.enableEventNotification(playerSessionId); //abilita la notifica degli eventi per questa sessione sul bus con l'indirizzo "playerSessionId"
			});
		});
	}

	//invia la risposta al client
	private void sendReply(HttpServerResponse response, JsonObject reply) {
		response.putHeader("content-type", "application/json"); //imposta l’header del messaggio http come json
		response.end(reply.toString()); //converte l’oggetto json in stringa, lo invia al client e chiude la risposta
	}

	//invia una risposta di errore al client
	private void sendError(HttpServerResponse response) {
		response.setStatusCode(500);  //imposta lo stato della risposta a 500 (errore)
		response.putHeader("content-type", "application/json"); //imposta l’header del messaggio http come json
		response.end(); //chiude la risposta
	}
}
