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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.action.OFActionOutput;
import net.onrc.openvirtex.protocol.OVXMatch;
import net.onrc.openvirtex.util.MACAddress;

/**
 * This class is to store physical rule set aggregated rule for efficiency.
 * Rule is matched by Match and Output Number And stored by cookie number.
 */
public class PhysicalFlowEntry {
	
	private Logger log = LogManager.getLogger(PhysicalFlowEntry.class
            .getName());
	
	/* Entry is represented by Match and outaction */
	private OVXMatch ovxmatch;
	private OFActionOutput ofaction;
	
	/* Store all cookie that have same match and out-port */
	private List<Long> cookieSet = Collections.synchronizedList(new ArrayList<Long>());
    
	/**
     * Instantiates a new EntryPair.
     *
     * @param match the OVXMatch
     * @param action the output action
     * @param cookie the cookie
     */
	public PhysicalFlowEntry(OVXMatch match,OFActionOutput action, long cookie){
		this.ovxmatch = match;
		this.ofaction = action;
		this.cookieSet.add(cookie);
		log.debug("This cookie is : {} ", cookie);
	}
	
	/**
	 * Get the OVXMatch of this entry.
	 * 
	 * @return the ovxmatch
	 */
	public OVXMatch getMatch(){
		return this.ovxmatch;
	}
	
	/**
	 * Get the output action of this entry.
	 * 
	 * @return the output action
	 */
	public OFActionOutput getAction(){
		return this.ofaction;
	}

	/**
	 * Add cookie to the cookie set
	 * 
	 * @param cookie
	 */
	public void addCookie(long cookie){
		this.cookieSet.add(cookie);
	}
	
	/**
	 * Get the cookie set
	 * 
	 * @return the cookie set
	 */
	public List<Long> getCookieSet(){
		return this.cookieSet;
	}
	
	/**
	 * Check the entry with the existed entry
	 * Same entry has same ovxmatch and outport of output action
	 * 
	 * @param entity
	 * @return if same condition, return true. otherwise return false.
	 */
	public boolean equals(PhysicalFlowEntry entity){
		
		if(entity == null)
			return false;
		
		if(Arrays.equals(this.ovxmatch.getDataLayerDestination(), entity.ovxmatch.getDataLayerDestination())
				&& (this.ovxmatch.getInputPort() == entity.ovxmatch.getInputPort())
				&& (this.ofaction.getPort() == entity.ofaction.getPort())){
			return true;
		}
		return false;
	}
	

	/*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
	@Override
	public String toString() {
		String ret = "";
		ret +="Cookie : ";
		for(Long c : cookieSet){
			ret += c + "\t";
		}
		ret += "\nInport : "+ this.ovxmatch.getInputPort()
				+ "\nMAC Destination Addresse: "+ MACAddress.valueOf(this.ovxmatch.getDataLayerDestination()).toString()
				+ "\nOutput port : " + this.ofaction.getPort()
				+"\n";

        ret += "Entry \n========================\n" + ret
                + "========================\n";
        
        return ret;
	}
	
}
