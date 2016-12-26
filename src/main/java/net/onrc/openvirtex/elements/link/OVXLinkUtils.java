/*******************************************************************************
 * Copyright 2014 Open Networking Laboratory
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
package net.onrc.openvirtex.elements.link;

import java.util.LinkedList;
import java.util.List;

import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.OVXBigSwitch;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.exceptions.SwitchMappingException;
import net.onrc.openvirtex.util.MACAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionDataLayerDestination;
import org.openflow.protocol.action.OFActionDataLayerSource;
import org.openflow.protocol.action.OFActionVirtualLanIdentifier;

/**
 * This class provides some useful methods to encapsulate/decapsulate the
 * virtual link identifiers (tenantId, linkId, flowId) inside the packet fields
 * (MAC addresses or VLAN).
 */
public class OVXLinkUtils {

    private static Logger log = LogManager.getLogger(OVXLinkUtils.class
            .getName());
    private Integer tenantId;
    private Integer linkId;
    private Integer flowId;
    private MACAddress srcMac;
    private MACAddress dstMac;
    private Short vlan;

    /**
     * Instantiates a new link utils instance. Never called by external classes.
     */
    protected OVXLinkUtils() {
        this.tenantId = 0;
        this.linkId = 0;
        this.flowId = 0;
        this.srcMac = null;
        this.dstMac = null;
        this.vlan = 0;
    }

    /**
     *  Instantiates a new LinkUtils from tenantId, linkId, flowId and virtual switch.
     *  Automatically encapsulate and set these values in the MAC addresses.
     *  This is new address assign technique that Source MAC address is tenantId , Destination MAC address is next Switch Id
     *  We find next switch by linkId, flowId and current switch.
     *  
     * @param tenantId
     * 				the tenant id
     * @param linkId
     * 				the link id
     * @param flowId
     * 				the flow id
     * @param sw
     * 				the switch called this LinkUtils
     */
    public OVXLinkUtils(final Integer tenantId, final Integer linkId, final Integer flowId, final OVXSwitch sw){
    	this();
    	this.tenantId = tenantId;
    	this.linkId = linkId;
    	this.flowId = flowId;
    	
    	OVXMap map = OVXMap.getInstance();
    	OVXSwitch dstsw;
    	try {
    		final long dst;
    		final short portNum;
			
    		OVXLink ovxLink = map.getVirtualNetwork(tenantId).getLinksById(linkId).get(0);
			
			if(ovxLink.getSrcSwitch().equals(sw)){
				dstsw = ovxLink.getDstSwitch();
				portNum = ovxLink.dstPort.getPortNumber();
			}
			else{
				dstsw = ovxLink.getSrcSwitch();
				portNum = ovxLink.srcPort.getPortNumber();
			}
			
			if(dstsw instanceof OVXBigSwitch){
				dst = dstsw.getPort(portNum).getPhysicalPort().getParentSwitch().getSwitchId();
			}else{
				dst = map.getPhysicalSwitches(dstsw).get(0).getSwitchId();
			}
			this.srcMac = MACAddress.valueOf(this.tenantId);
			this.dstMac = MACAddress.valueOf(dst);

		} catch (SwitchMappingException e) {
			log.error("This Switch can't find");
		} catch (NetworkMappingException e) {
			log.error(e);
		} 
    	// TODO: encapsulate the values in the vlan too
    }

    /**
     * Checks if the link utils instance is valid. To be valid, the instance has
     * to have tenantId, linkId and flowId set. Moreover, both MAC addresses or
     * the VLAN field has to be set too.
     *
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        if (this.tenantId != 0 && this.linkId != 0 && this.flowId != 0) {
            if (this.vlan != 0 || this.srcMac != null && this.dstMac != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the tenant id.
     *
     * @return the tenant id
     */
    public Integer getTenantId() {
        return this.tenantId;
    }

    /**
     * Gets the link id.
     *
     * @return the link id
     */
    public Integer getLinkId() {
        return this.linkId;
    }

    /**
     * Gets the flow id.
     *
     * @return the flow id
     */
    public Integer getFlowId() {
        return this.flowId;
    }

    /**
     * Gets the source MAC address.
     *
     * @return the source MAC
     */
    public MACAddress getSrcMac() {
        return this.srcMac;
    }

    /**
     * Gets the destination MAC address.
     *
     * @return the destination MAC
     */
    public MACAddress getDstMac() {
        return this.dstMac;
    }

    /**
     * Gets the VLAN.
     *
     * @return the VLAN
     */
    public Short getVlan() {
        return this.vlan;
    }

    /**
     * Gets the original MAC addresses in a list.
     *
     * @return list of original MAC addresses
     * @throws NetworkMappingException
     *             if the tenant ID is invalid
     */
    public LinkedList<MACAddress> getOriginalMacAddresses()
            throws NetworkMappingException {
        final LinkedList<MACAddress> macList = OVXMap.getInstance()
                .getVirtualNetwork(this.tenantId).getFlowManager()
                .getFlowValues(this.flowId);
        return macList;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "tenantId = " + this.tenantId + ", linkId = " + this.linkId
                + ", flowId = " + this.flowId + ", srcMac = " + this.srcMac
                + ", dstMac = " + this.dstMac + ", vlan = " + this.vlan;
    }

    /**
     * Rewrites the given match according to the current instance.
     *
     * @param match
     *            the OpenFlow match
     */
    public void rewriteMatch(final OFMatch match) {
        final OVXLinkField linkField = OpenVirteXController.getInstance()
                .getOvxLinkField();
        if (linkField == OVXLinkField.MAC_ADDRESS) {
            match.setDataLayerSource(this.getSrcMac().toBytes());
            match.setDataLayerDestination(this.getDstMac().toBytes());
        } else if (linkField == OVXLinkField.VLAN) {
            match.setDataLayerVirtualLan(this.getVlan());
        }
    }

    /**
     * Gets a list of actions based on the current instance.
     *
     * @return list of actions
     */
    public List<OFAction> setLinkFields() {
        final List<OFAction> actions = new LinkedList<OFAction>();
        final OVXLinkField linkField = OpenVirteXController.getInstance()
                .getOvxLinkField();
        if (linkField == OVXLinkField.MAC_ADDRESS) {
            actions.add(new OFActionDataLayerDestination(this.getDstMac()
                    .toBytes()));
        } else if (linkField == OVXLinkField.VLAN) {
            actions.add(new OFActionVirtualLanIdentifier(this.getVlan()));
        }
        return actions;
    }

    /**
     * Gets a list of actions based on the original MAC addresses.
     *
     * @return list of actions
     */
    public List<OFAction> unsetLinkFields() {
        final List<OFAction> actions = new LinkedList<OFAction>();
        final OVXLinkField linkField = OpenVirteXController.getInstance()
                .getOvxLinkField();
        if (linkField == OVXLinkField.MAC_ADDRESS) {
            LinkedList<MACAddress> macList;
            try {
                macList = this.getOriginalMacAddresses();
                actions.add(new OFActionDataLayerSource(macList.get(0)
                        .toBytes()));
                actions.add(new OFActionDataLayerDestination(macList.get(1)
                        .toBytes()));
            } catch (NetworkMappingException e) {
                OVXLinkUtils.log.error("Unable to restore actions: " + e);
            }
        } else {
            if (linkField == OVXLinkField.VLAN) {
                OVXLinkUtils.log
                        .warn("Unable to restore actions, VLANs not supported");
                // actions.add(new
                // OFActionVirtualLanIdentifier(getOriginalVlan()));
            }
        }
        return actions;
    }
}
