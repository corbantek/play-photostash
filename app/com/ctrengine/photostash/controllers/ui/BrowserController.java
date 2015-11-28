
package com.ctrengine.photostash.controllers.ui;

import play.Routes;
import play.mvc.Controller;
import play.mvc.Result;

public class BrowserController extends Controller {
	public static Result index(){
		return redirect(routes.BrowserController.browser().absoluteURL(request()));
	}
	
	public static Result browser(){
		return ok(com.ctrengine.photostash.views.html.ui.browser.render());
	}
	
	public static Result javascriptRoutes() {
		response().setContentType("text/javascript");
		return ok(Routes.javascriptRouter("jsRoutesBrowserController",
		/**
		 * Routes
		 */
		com.ctrengine.photostash.controllers.ui.routes.javascript.BrowserController.index(),
		
		com.ctrengine.photostash.controllers.ui.routes.javascript.BrowserController.browser()));
	}
}
