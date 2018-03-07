package org.pale.simplechat.actions;

import org.pale.simplechat.Conversation;
import org.pale.simplechat.Logger;
import org.pale.simplechat.Topic;

public class Commands {

	/*
	 * Stack manipulation	
	 */
	@Cmd public static void dup(Conversation c) throws ActionException{
		c.push(c.peek());
	}
	
	@Cmd public static void dp(Conversation c) throws ActionException{
		String s = c.pop().str();
		Logger.log("DP: "+s);
	}



	/*
	 * Type conversion
	 */
	@Cmd public static void str(Conversation c) throws ActionException {
		c.push(new Value(c.pop().str()));
	}

	@Cmd (name="int") public static void toint(Conversation c) throws ActionException {
		c.push(new Value(c.pop().toInt()));
	}
	@Cmd (name="double") public static void todouble(Conversation c) throws ActionException {
		c.push(new Value(c.pop().toDouble()));
	}

	/*
	 * String manipulation
	 */
	@Cmd public static void trim(Conversation c) throws ActionException{
		c.push(new Value(c.pop().str().trim()));
	}
	
	/*
	 * Pattern handling and topic manipulation
	 */
	@Cmd public static void next(Conversation c) throws ActionException{
		// tell the conversation to use the patterns we just specified to match first.
		c.specialpats =  c.popSubpats();
	}

	@Cmd public static void recurse(Conversation c) throws ActionException {
		// recurse the entire string (like SRAI in AIML)
		c.push(new Value(c.handle(c.popString())));
	}
	
	private static void doPromoteDemote(Conversation c,String name,boolean demote) throws ActionException{
		Topic t = c.instance.bot.getTopic(name);
		if(t!=null)
			c.promoteDemote(t, demote);
		else
			throw new ActionException("unknown topic: "+name);		
	}
	
	private static void doEnableDisableTopic(Conversation c,String name,boolean disable) throws ActionException{
		Topic t = c.instance.bot.getTopic(name);
		if(t!=null)
			c.enableDisableTopic(t, disable);
		else
			throw new ActionException("unknown topic: "+name);		
	}

	@Cmd public static void promote(Conversation c) throws ActionException {
		doPromoteDemote(c,c.popString(),false);
	}

	@Cmd public static void demote(Conversation c) throws ActionException {
		doPromoteDemote(c,c.popString(),true);
	}
	@Cmd public static void enabletopic(Conversation c) throws ActionException {
		doEnableDisableTopic(c,c.popString(),false);
	}

	@Cmd public static void disabletopic(Conversation c) throws ActionException {
		doEnableDisableTopic(c,c.popString(),true);
	}
	
	@Cmd public static void enablepattern(Conversation c) throws ActionException {
		// (topname patname -- )
		String patname = c.popString();
		String topname = c.popString();
		c.enableDisablePattern(topname, patname, false);
	}

	@Cmd public static void disablepattern(Conversation c) throws ActionException {
		// (topname patname -- )
		String patname = c.popString();
		String topname = c.popString();
		c.enableDisablePattern(topname, patname, true);
	}
	
	/*
	 * lists
	 */
	@Cmd public static void get(Conversation c) throws ActionException {
		// (idx list -- val)
		Value lst = c.pop();
		int key = c.pop().toInt();
		if(key>=0 && key<lst.list.size())
			c.push(lst.list.get(key));
		else
			c.push(new Value("??"));
	}
	
	@Cmd public static void len(Conversation c) throws ActionException {
	}
}
