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
import com.ctrengine.photostash.shoebox.ShoeboxMessages.OrganizeStatusRequestMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.ResponseMessage;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ShoeboxController extends Controller {

	public static Promise<Result> organize() {
		return requestMessage(new OrganizeShoeboxMessage(ShoeboxConfiguration.INSTANCE.getShoeboxPath()));
	}
	
	public static Promise<Result> organizeStatus() {
		return requestMessage(new OrganizeStatusRequestMessage());
	}
	
	private static Promise<Result> requestMessage(Object message){
		return Promise.wrap(ask(Shoebox.INSTANCE.getShoeboxActor(), message, 10000)).map(new Function<Object, Result>() {
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
