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
package cn.dlut.elements.port;


import cn.dlut.elements.datapath.PhysicalSwitch;
import cn.dlut.elements.link.PhysicalLink;

import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.OFPortStatus;
import org.openflow.protocol.OFPortStatus.OFPortReason;

/**
 * A physical port maintains the mapping of all virtual ports that are mapped to
 * it.
 */
public class PhysicalPort extends Port<PhysicalSwitch, PhysicalLink> {


    /**
     * Instantiates a physical port based on an OpenFlow physical port.
     *
     * @param port
     *            the OpenFlow physical port
     */
    private PhysicalPort(final OFPhysicalPort port) {
        super(port);
    }

    /**
     * Instantiates a physical port based on an OpenFlow physical port, the
     * physical switch it belongs to, and and set whether the port is an edge
     * port or not.
     *
     * @param port
     *            the OpenFlow physical port
     * @param sw
     *            the physical switch
     * @param isEdge
     *            edge attribute
     */
    public PhysicalPort(final OFPhysicalPort port, final PhysicalSwitch sw,
            final boolean isEdge) {
        this(port);
        this.parentSwitch = sw;
        this.isEdge = isEdge;
    }

    @Override
    public boolean equals(Object that) {
        if (that == null) {
            return false;
        }
        if (this == that) {
            return true;
        }
        if (!(that instanceof PhysicalPort)) {
            return false;
        }

        PhysicalPort port = (PhysicalPort) that;
        return this.portNumber == port.portNumber
                && this.parentSwitch.getSwitchId() == port.getParentSwitch()
                        .getSwitchId();
    }

    /**
     * Changes the attribute of this port according to a MODIFY PortStatus.
     *
     * @param portstat
     *            the port status
     */
    public void applyPortStatus(OFPortStatus portstat) {
        if (!portstat.isReason(OFPortReason.OFPPR_MODIFY)) {
            return;
        }
        OFPhysicalPort psport = portstat.getDesc();
        this.portNumber = psport.getPortNumber();
        this.hardwareAddress = psport.getHardwareAddress();
        this.name = psport.getName();
        this.config = psport.getConfig();
        this.state = psport.getState();
        this.currentFeatures = psport.getCurrentFeatures();
        this.advertisedFeatures = psport.getAdvertisedFeatures();
        this.supportedFeatures = psport.getSupportedFeatures();
        this.peerFeatures = psport.getPeerFeatures();
    }

    /**
     * Unmaps this port from the global mapping and its parent switch.
     */
    public void unregister() {
        // Remove links, if any
        if ((this.portLink != null) && (this.portLink.exists())) {
            this.portLink.egressLink.unregister();
            this.portLink.ingressLink.unregister();
        }
    }
}
