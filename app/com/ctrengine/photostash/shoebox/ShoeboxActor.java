package com.ctrengine.photostash.shoebox;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;

import com.ctrengine.photostash.shoebox.ShoeboxMessages.OrganizeMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.ResponseMessage;
import com.ctrengine.photostash.shoebox.ShoeboxMessages.ResponseType;

public class ShoeboxActor extends UntypedActor {
	static Props props(){
		return Props.create(new Creator<ShoeboxActor>() {
			private static final long serialVersionUID = 6309187420249216567L;

			@Override
			public ShoeboxActor create() throws Exception {
				return new ShoeboxActor();
			}		
		});
	}
	
	private Map<String, ActorRef> albumActors;
	
	private ShoeboxActor(){
		albumActors = new TreeMap<String, ActorRef>();
	}
	
	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof OrganizeMessage){
			organize((OrganizeMessage)message);
		}else{
			unhandled(message);
		}
	}
	
	private void organize(OrganizeMessage organizeMessage){
		/**
		 * First detect if another organize is running
		 */
		
		/**
		 * Now create a bunch of AlbumActors to organize their albums/stories
		 */
		File shoeboxDirectory = new File(organizeMessage.getShoeboxPath());
		if(shoeboxDirectory.exists() && shoeboxDirectory.isDirectory()){
			for(File album: shoeboxDirectory.listFiles()){
				if(album.isDirectory()){
					Shoebox.LOGGER.info("Found Album: "+album.getAbsolutePath());
					String actorName = Shoebox.generateActorName(album);
					ActorRef albumActor= albumActors.get(actorName); 
					if(albumActor == null){
						/**
						 * Create the Album actor if he doesn't exist anymore
						 */
						albumActor = getContext().actorOf(AlbumActor.props(album), actorName);
						albumActors.put(actorName, albumActor);
					}
					albumActor.tell(organizeMessage, getSelf());
				}
			}
			getSender().tell(new ResponseMessage(ResponseType.INFO, "Shoebox organize started."), getSelf());
		}else{
			String message = "Shoebox Directory does not exist: "+organizeMessage.getShoeboxPath();
			Shoebox.LOGGER.error(message);
			getSender().tell(new ResponseMessage(ResponseType.ERROR, message), getSelf());
		}
	}

}
