package com.ctrengine.photostash.controllers.api;

import static akka.pattern.Patterns.ask;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import com.ctrengine.photostash.conf.ShoeboxConfiguration;
import com.ctrengine.photostash.shoebox.Shoebox;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.OrganizeShoeboxMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.ResponseMessage;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ShoeboxController extends Controller {

	public static Promise<Result> organize() {
		return Promise.wrap(ask(Shoebox.INSTANCE.getShoeboxActor(), new OrganizeShoeboxMessage(ShoeboxConfiguration.INSTANCE.getShoeboxPath()), 2000)).map(new Function<Object, Result>() {
			public Result apply(Object response) {
				if (response instanceof ResponseMessage) {
					ResponseMessage responseMessage = (ResponseMessage) response;
					ObjectNode message = responseMessage.getMessage();
					switch (responseMessage.getResponseType()) {
					case INFO:
						return ok(message);
					case WARNING:
						return badRequest(message);
					case ERROR:
						return internalServerError(message);
					}
					return ok(response.toString());
				} else {
					return internalServerError(Json.newObject().put("message", response.toString()));
				}
			}
		});
	}
	
	public static Promise<Result> organizeStatus() {
		return Promise.wrap(ask(Shoebox.INSTANCE.getShoeboxActor(), new OrganizeShoeboxMessage(ShoeboxConfiguration.INSTANCE.getShoeboxPath()), 2000)).map(new Function<Object, Result>() {
			public Result apply(Object response) {
				if (response instanceof ResponseMessage) {
					ResponseMessage responseMessage = (ResponseMessage) response;
					ObjectNode message = responseMessage.getMessage();
					switch (responseMessage.getResponseType()) {
					case INFO:
						return ok(message);
					case WARNING:
						return badRequest(message);
					case ERROR:
						return internalServerError(message);
					}
					return ok(response.toString());
				} else {
					return internalServerError(Json.newObject().put("message", response.toString()));
				}
			}
		});
	}
}
