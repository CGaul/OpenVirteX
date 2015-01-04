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
 ******************************************************************************/
package net.onrc.openvirtex.elements.datapath;

import net.onrc.openvirtex.core.io.OVXSendMsg;
import net.onrc.openvirtex.messages.statistics.OVXDescriptionStatistics;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.local.DefaultLocalClientChannelFactory;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPhysicalPort;

import java.util.Collections;

/**
 * An easy representation of a remote (federated) Physical-Switch
 */
public class RemotePhysicalSwitch extends PhysicalSwitch{

    /**
     * Instantiates a new remote physical switch.
     * This Switch will be used for federation, and
     * represents a Switch that is not part of this OVX-Management
     * but is discovered by its DPID in an LLDP Handling.
     *
     * @param switchId the switch id
     */
    public RemotePhysicalSwitch(long switchId) {
        super(switchId);

        this.setDescriptionStats(new OVXDescriptionStatistics());
        final OFFeaturesReply offr = new OFFeaturesReply();

        offr.setPorts(Collections.singletonList(new OFPhysicalPort()));
        this.featuresReply = offr;

        final Channel remoteChannel = new DefaultLocalClientChannelFactory().newChannel(Channels.pipeline());
        this.channel = remoteChannel;
        this.setConnected(true);
    }

    @Override
    public void sendMsg(final OFMessage msg, final OVXSendMsg from) {
        if (this.channel.isOpen() && this.isConnected) {
            this.channel.write(Collections.singletonList(msg));
        }
    }
}