/*******************************************************************************
 * Copyright 2016 Libera project team in Korea University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ****************************************************************************
 * Libera Hypervisor development based OpenVirteX for SDN 2.0
 *
 * 	AggFlow, new address virtualization technique, is applied.
 *
 * This is updated by Libera Project team in Korea University
 *
 * Author: Bongyeol Yu (koreagood13@gmail.com)
 ******************************************************************************/
package net.onrc.openvirtex.elements.datapath;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.Wildcards.Flag;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionType;

import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.protocol.OVXMatch;

/**
 * Physical flow table that manage to aggregate rule and store that.
 * 
 * @author byyu
 *
 */
public class PhysicalFlowTable {
	
	private static Logger log = LogManager.getLogger(PhysicalFlowTable.class.getName());
	
	// Physical switch tied to this table
	private PhysicalSwitch physw;
	// Aggregated rule set installed in physical switch.
	private Set<PhysicalFlowEntry> entry = new HashSet<PhysicalFlowEntry>();
	
    /**
     * Instantiates a new flow table associated to the given
     * physical switch.
     *
     * @param sw the physical switch
     */
	public PhysicalFlowTable(PhysicalSwitch sw){
		this.physw = sw;
	}

	/**
	 * Add an entity to this physical table.
	 * 
	 * @param match
	 * @param action
	 */
	private void addEntry(OVXMatch match, OFActionOutput action){
		PhysicalFlowEntry entity = new PhysicalFlowEntry(match, action, match.getCookie());
		entry.add(entity);
	}
	
	/**
	 * Add an entity to this physical table. 
	 * In this case, we need to extend matching field and increase the priority of this FlowMod packet, 
	 * because the rule that has different out put with same in port already is installed.
	 * 
	 * @param fm
	 * @param match
	 * @param action
	 */
	private void addEntry(OVXFlowMod fm, OVXMatch match, OFActionOutput action){
		short prio = fm.getPriority();
		fm.setPriority(++prio);
		match.setWildcards((OFMatch.OFPFW_ALL) & (~OFMatch.OFPFW_IN_PORT)
								& (~OFMatch.OFPFW_DL_SRC)
								& (~OFMatch.OFPFW_DL_DST)
								& (~OFMatch.OFPFW_DL_TYPE)
								& (~OFMatch.OFPFW_NW_DST_MASK)
								& (~OFMatch.OFPFW_NW_SRC_MASK));
		fm.setMatch(match);
		addEntry(match, action);
	}
	
	/**
	 * Remove the flow entry and return the all cookies correspond to this FlowRemoved packet.
	 * 
	 * @param match
	 * @param action
	 * @param cookie
	 * @return the cookie set that assigned same rule
	 */
	public List<Long> removeEntry(OVXMatch match, OFActionOutput action, long cookie){
		PhysicalFlowEntry newEntity = new PhysicalFlowEntry(match, action, cookie);
		for(PhysicalFlowEntry entity : entry){
			if(entity.equals(newEntity)){
				List<Long> cookieList = entity.getCookieSet();
				entry.remove(entity);
				return cookieList;
			}
		}
		return null;
	}

	/**
	 * Check whether the FlowMod is duplicated with the installed rules.
	 * If both have same input and same output, this FlowMod is duplicated.
	 * Otherwise, both have same input but have different output, the match field of this FlowMod is extended and stored in the table.
	 * And, If both have different input or no one is installed, this FlowMod is stored in the table.
	 * 
	 * @param fm
	 * @return result of checking duplication. If the FlowMod is sent to physical switch, false is returned.
	 */
	public boolean checkduplicate(OVXFlowMod fm){
		log.info("Start checking duplicate at {}", this.physw.toString());
		OVXMatch match = new OVXMatch(fm.getMatch());
		OFActionOutput outaction = null;
		match.setCookie(fm.getCookie());
		OVXMatch oldMatch;
		
		short oldoutport, outport=0;
		
		//Gets the ActionOutput from the FlowMod.
		for(OFAction action : fm.getActions()){
			if(action.getType()==OFActionType.OUTPUT){
	    		outaction = (OFActionOutput) action;
	    		outport = outaction.getPort();
			}
		}
		
		//This FlowMod is not for forwarding.
		if(outport==0){
			return false;
		}
		
		for(PhysicalFlowEntry entity : entry){
			oldMatch = entity.getMatch();
			oldoutport = entity.getAction().getPort();
			if(oldMatch.getInputPort() == match.getInputPort()){
				if(outport == oldoutport){
					if(oldMatch.getWildcards() == match.getWildcards()){
						if(!oldMatch.getWildcardObj().isWildcarded(Flag.NW_DST)){
							if(oldMatch.getNetworkDestination() == match.getNetworkDestination()){
								entity.addCookie(match.getCookie());
								return true;
							}
						}else{
							entity.addCookie(match.getCookie());
							return true;
						}
					}
				}else{
					if(oldMatch.getWildcards() == match.getWildcards()){
						addEntry(fm, match, outaction);
						return false;
					}
					
				}
			}
		}
		
		addEntry(match, outaction);
		return false;
	}
	
	public Set<PhysicalFlowEntry> getFlowEntry(){
		return this.entry;
	}
}

