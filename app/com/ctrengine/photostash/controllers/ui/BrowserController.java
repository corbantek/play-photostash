
package com.ctrengine.photostash.controllers.ui;

import play.mvc.Controller;
import play.mvc.Result;

public class BrowserController extends Controller {
	public static Result index(){
		return browser();
	}
	
	public static Result browser(){
		return ok(com.ctrengine.photostash.views.html.ui.browser.render());
	}
}
