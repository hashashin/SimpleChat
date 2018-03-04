package org.pale.simplechat.patterns;

import org.pale.simplechat.Pattern;
import org.pale.simplechat.PatternParseException;

public class MaybeNode extends Node {

	Node node;
	public MaybeNode(Pattern p, String lab) throws PatternParseException {
		super(p, lab);
		p.iter.next();
		node = p.parseNode();
	}
	
	@Override
	public void nextLinkChild(Node n){
		node.next = n;
	}

	@Override
	public void parse(MatchData m) {
		if(m.invalid){log("early return");return;}

		// try to match the node
		node.parse(m);
		// if we got it, handle it.
		if(!m.invalid){
			if(label!=null){
				m.setLabel(label, m.consumed);
			}
		} else {
			m.consumed="";
			// otherwise just clear the state and move on..
			m.invalid = false;
		}
	}

}
