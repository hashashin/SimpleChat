package org.pale.simplechat;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.pale.simplechat.actions.ActionException;
import org.pale.simplechat.actions.ActionLog;
import org.pale.simplechat.actions.Value;
import org.pale.simplechat.values.NoneValue;

/**
 * An actual chatting entity, which is backed by a Bot. There may be many BotInstances for one bot;
 * they will all talk the same way but may have different variables set.
 * @author white
 *
 */
public class BotInstance extends Source { // extends Source so bots can talk to each other, maybe.
	public Bot bot;
	
	// this is a map of each instance to each possible conversational partner.
	private Map<Source,Conversation> conversations = new HashMap<Source,Conversation>();

	private Object data; // data connected to the bot instance, could be anything
	
	public BotInstance(Bot b) throws BotConfigException{
		bot = b;
		try {
			// during the init action, the instance is "talking to itself" as it were.
			if(b.initAction!=null)
				b.initAction.run(new Conversation(this,this),true);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new BotConfigException("error running initialisation action: "+e.getMessage());
		} catch (ActionException e) {
			ActionLog.show();
			throw new BotConfigException("error running initialisation action: "+e.getMessage());
		}
	}
	
	// use this ctor when you want to connect some other data to the instance.
	public BotInstance(Bot b, Object data) throws BotConfigException{
		this(b); // call the other ctor
		this.data = data;
	}
	
	/// variables private (haha) to this instance.
	private Map<String,Value> vars = new HashMap<String,Value>();
	
	public Value getVar(String s){
		if(vars.containsKey(s))
			return vars.get(s);
		else
			return NoneValue.instance;
	}
	
	public void setVar(String s,Value v){
		vars.put(s, v);
	}
	
	// return the private data object you may have set; you'll need to cast.
	public Object getData(){
		return data;
	}
	
	public String handle(String s,Source p){
		Conversation c;
		
		// run any regex substitutions
		s = bot.subs.process(s);
		Logger.log("after subs: "+s);
		
		// make a new conversation or get the existing one for this source
		if(conversations.containsKey(p))
			c = conversations.get(p);
		else {
			c = new Conversation(this,p);
			conversations.put(p, c);
		}
		// pass through to the conversation
		return c.handle(s);
	}
}
