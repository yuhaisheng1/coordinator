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
package cn.dlut.elements.datapath;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import cn.dlut.core.main.OpenVirteX;
import cn.dlut.core.main.OpenVirteXController;
import cn.dlut.linkdiscovery.LLDPEventHandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFGetConfigReply;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFSetConfig;
import org.openflow.protocol.OFStatisticsReply;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.OFSwitchConfig;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.statistics.OFAggregateStatisticsReply;
import org.openflow.protocol.statistics.OFAggregateStatisticsRequest;
import org.openflow.protocol.statistics.OFDescriptionStatistics;
import org.openflow.protocol.statistics.OFFlowStatisticsReply;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.openflow.util.LRULinkedHashMap;

import cn.dlut.elements.controller.Controller;
import cn.dlut.elements.network.PhysicalNetwork;
import cn.dlut.elements.port.PhysicalPort;

/**
 * The base virtual switch.
 */
public  class OVXSwitch extends Switch<PhysicalPort> implements LLDPEventHandler {

    private static Logger log = LogManager.getLogger(OVXSwitch.class.getName());
    
    private ClientBootstrap clientBootStrap;
    
    /**
     * Datapath description string.
     * TODO: should this be made specific per type of virtual switch?
     */
    public static final String DPDESCSTRING = "Coordinator vSwitch";
    protected static int supportedActions = 0xFFF;
    protected static int bufferDimension = 4096;
    // default in spec is 128
    protected Short missSendLen = 128;
    // The backoff counter for this switch when unconnected
    private AtomicInteger backOffCounter = null;
    protected LRULinkedHashMap<Integer, OFPacketIn> bufferMap;
    
    private PhysicalSwitch physicalSwitch;
    private Controller ctrl;
    
    /**
     * Instantiates a new OVX switch.
     *
     * @param switchId the switch id
     * @param tenantId the tenant id
     */
    public OVXSwitch( Long switchId) {
        super(switchId);
        this.missSendLen = 0;
        this.backOffCounter = new AtomicInteger();
        this.resetBackOff();
        this.bufferMap = new LRULinkedHashMap<Integer, OFPacketIn>(
                OVXSwitch.bufferDimension);
    }
   
    //@author daiminglong
    public void setPhysicalSwitch(PhysicalSwitch phy)
    {
    	this.physicalSwitch=phy;
    }
    
    public PhysicalSwitch getPhysicalSwitch()
    {
    	return this.physicalSwitch;
    }

    public void setController(Controller c){
    	this.ctrl=c;
    }
    
    public Controller getController(){
    	return this.ctrl;
    }
    
    /**
     * Gets the miss send len.
     *
     * @return the miss send len
     */
    public short getMissSendLen() {
        return this.missSendLen;
    }

    /**
     * Sets the miss send len.
     *
     * @param missSendLen
     *            the miss send len
     * @return true, if successful
     */
    public boolean setMissSendLen(final Short missSendLen) {
        this.missSendLen = missSendLen;
        return true;
    }

    /**
     * Resets the backoff counter.
     */
    public void resetBackOff() {
        this.backOffCounter.set(-1);
    }

    /**
     * Increments the backoff counter.
     *
     * @return the backoff counter
     */
    public int incrementBackOff() {
        return this.backOffCounter.incrementAndGet();
    }
    
    /**
     * Gets a packet_in from a given buffer ID.
     *
     * @param bufId the buffer ID
     * @return packet_in packet
     */
    public OFPacketIn getFromBufferMap(final Integer bufId) {
        return this.bufferMap.get(bufId);
    }
        
    /**
     * Generate features reply.
     */
    public void generateFeaturesReply() {
      
    	OFFeaturesReply ofReply=new OFFeaturesReply();
    	ofReply=this.physicalSwitch.getFeaturesReply();
        this.setFeaturesReply(ofReply);
    }
    
    /**
     * Boots virtual switch by connecting it to the controller.
     *
     * @return true if successful, false otherwise
     */
    @Override
    public boolean boot() {
        this.generateFeaturesReply();
        return true;
    }
    
	/**
     * Unregisters switch from persistent storage, from the mapping,
     * and removes all virtual elements that rely on this switch.
     */
    public void unregister() {
       
    }

    @Override
    public void tearDown() {
        this.isConnected=false;
    }
    
    /*
     * (non-Javadoc)
     *
     * @see net.onrc.openvirtex.elements.datapath.Switch#toString()
     */
    @Override
    public String toString() {
        return "SWITCH: switchId: " + this.switchId + " - switchName: "
                + this.switchName + " - isConnected: " + this.isConnected
                 + " - missSendLength: "+ this.missSendLen +" - capabilities: "
                + this.getPhysicalSwitch().getFeaturesReply().toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((switchId == null) ? 0 : switchId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof OVXSwitch)) {
            return false;
        }
        OVXSwitch other = (OVXSwitch) obj;
        return (this.switchId == other.switchId)&&(this.ctrl.getIp().equals(other.ctrl.getIp()))&&
        		(this.ctrl.getPort().equals(other.ctrl.getPort()));
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.onrc.openvirtex.core.io.OVXSendMsg#sendMsg(org.openflow.protocol.
     * OFMessage, net.onrc.openvirtex.core.io.OVXSendMsg)
     */
    @Override
    public void sendMsg(final OFMessage msg) {
        if (this.isConnected ) {
            this.channel.write(Collections.singletonList(msg));
        } else {
            // TODO: we probably should install a drop rule here.
            log.warn("Virtual switch {} is not active or is not connected to a controller", 
            		switchName);
        }
    }

    // @author daiminglong
    public void finallize(){
    	this.physicalSwitch=null;
    }
    
    //是否还连接上
    public boolean isConnected(){
    	return this.isConnected;
    }
    
    //设置连接状态
	@Override
	public void setConnected(boolean isConnected) {
		this.isConnected=isConnected;
	}
		
	/**
     * Sets the channel.
     *
     * @param channel the channel
     */
    public void setChannel(Channel channel) {
    	this.channel=channel;
    }

    /**
     * Removes the given channel.
     *
     * @param channel the channel
     */
    public void removeChannel(Channel channel) {
    	this.channel=null;
    }
    
    //得到当前OVXSwitch的channel 用于OpenVirteXController的deleteController方法
    public Channel getChannel(){
    	return this.channel;
    }

    //通道资源的释放
	public ClientBootstrap getClientBootStrap() {
		return clientBootStrap;
	}

	public void setClientBootStrap(ClientBootstrap clientBootStrap) {
		this.clientBootStrap = clientBootStrap;
	}
	
	@Override
	public OFFeaturesReply getFeaturesReply() {
		return this.getPhysicalSwitch().getFeaturesReply();
	}
	
	//@author  daiminglong
    public void handlePacketOut(OFMessage msg,Channel channel){
//    	long start = System.currentTimeMillis();
    	OFPacketOut pktOut=(OFPacketOut)msg;
    	//System.out.println("this is a packet out to =======> "+this.getPhysicalSwitch());
    	System.out.println("pktOut.getBufferId()===>"+pktOut.getBufferId());
		System.out.println("pktOut.getInPort()===>"+pktOut.getInPort());
		System.out.println("pktOut.getLengthU()===>"+pktOut.getLengthU());
		System.out.println("pktOut.getXid()===>"+pktOut.getXid());
		System.out.println("pktOut.getDataAsString(msg)===>"+pktOut.getDataAsString(msg));

    	OVXQueue.DelCtrlPktIn(this.getController(), pktOut.getBufferId());
//		long end = System.currentTimeMillis();
//		long spend= end-start;
		//OVXSwitch.log.info("process a packout waste time===>!"+spend);    	
    	this.getPhysicalSwitch().sendMsg(pktOut);
    }
    
	public void hanleFlow_mod(OFMessage msg,Channel channel){
		this.physicalSwitch.sendMsg(msg);
	}
    
	//@author  daiminglong
	@Override
	@SuppressWarnings("rawtypes")
	public void handleLLDP(OFMessage msg, Switch sw) {
		
		final OFPacketOut po = (OFPacketOut) msg;
        final byte[] pkt = po.getPacketData();

        // Create LLDP response for each output action port
        for (final OFAction action : po.getActions()) {
            try {
                final short portNumber = ((OFActionOutput) action).getPort();
                final PhysicalPort srcPort = (PhysicalPort) ((OVXSwitch)sw).getPhysicalSwitch().getPort(portNumber);
                final PhysicalPort dstPort = PhysicalNetwork.getInstance().getNeighborPort(srcPort);
                if (dstPort != null) {
                    final OFPacketIn pi = new OFPacketIn();
                    pi.setBufferId(OFPacketOut.BUFFER_ID_NONE);
                    // Get input port from pkt_out
                    pi.setInPort(dstPort.getPortNumber());
                    pi.setReason(OFPacketIn.OFPacketInReason.NO_MATCH);
                    pi.setPacketData(pkt);
                    pi.setTotalLength((short) (OFPacketIn.MINIMUM_LENGTH + pkt.length));
                    for(OVXSwitch sx:(dstPort.getParentSwitch().getOVXSwitchSet()))
                    {
                    	if(((sx.getController().getPort().equals(((OVXSwitch)sw).getController().getPort())))&&
                    			((sx.getController().getIp().equals(((OVXSwitch)sw).getController().getIp()))))
                    	{
                    		/*System.out.println("swid: "+sx.getSwitchId()+
                    				" send lldp packetin to Controller: "+sx.getController().getIp()+":"
                    				+sx.getController().getPort());*/
                    		sx.sendMsg(pi);
                    	}
                    }
                }
            } catch (final ClassCastException c) {
                // ignore non-ActionOutput pkt_out's
                continue;
            }
        }
	}
	
	
	
    /*
     * (non-Javadoc)
     *
     * @see
     * net.onrc.openvirtex.elements.datapath.Switch#handleIO(org.openflow.protocol
     * .OFMessage)
     */
    @Override
    public void handleIO(final OFMessage msg, Channel channel) {
       switch(msg.getType()){	
		
		case SET_CONFIG:
			this.setMissSendLen(((OFSetConfig) msg).getMissSendLength());
    		OVXSwitch.log.info("Setting miss send length to {} for OVXSwitch {}",
                    ((OFSwitchConfig) msg).getMissSendLength(), this.getSwitchName());
            break;
		case PACKET_OUT:
			this.handlePacketOut(msg,channel);
			break;
            
        case STATS_REQUEST:
        	switch (((OFStatisticsRequest)msg).getStatisticType()) {
			case DESC:
				final OFStatisticsReply reply = new OFStatisticsReply();
				final OFDescriptionStatistics desc = new OFDescriptionStatistics();
				desc.setDatapathDescription(OVXSwitch.DPDESCSTRING);
		        desc.setHardwareDescription("virtual hardware");
		        desc.setManufacturerDescription("cn.dlut");
		        desc.setSerialNumber(this.getSwitchName());
		        desc.setSoftwareDescription(OpenVirteX.VERSION);
				
		        reply.setXid(msg.getXid());
		        reply.setLengthU(reply.getLength() + desc.getLength());
		        reply.setStatisticType(OFStatisticsType.DESC);
		        reply.setStatistics(Collections.singletonList(desc));
		        this.sendMsg(reply);
	            break;
	            
			case TABLE:
			case VENDOR:
				
			case FLOW:
			case PORT:
			case QUEUE:
				break;
				
			case AGGREGATE:
				final OFStatistics stat = ((OFStatisticsRequest)msg).getStatistics().get(0);
				OFAggregateStatisticsReply agstat = new OFAggregateStatisticsReply();
			    HashSet<Long> uniqueCookies = new HashSet<Long>();
			 
			    // the -1 is for beacon...
			    if((((OFAggregateStatisticsRequest) stat).getMatch().getWildcardObj().isFull() 
			    		|| ((OFAggregateStatisticsRequest) stat).getMatch().getWildcards() == -1)
		                && ((OFAggregateStatisticsRequest) stat).getOutPort() == OFPort.OFPP_NONE.getValue()) {
			    	agstat.setByteCount(0);
			    	agstat.setPacketCount(0);
			    	agstat.setFlowCount(0);
			    	List<OFFlowStatisticsReply> reps = this.physicalSwitch.getFlowStats();
			    	
			    	if (reps != null) {
			    		agstat.setFlowCount(reps.size());
	                    for (OFFlowStatisticsReply s : reps) {
	                        if (!uniqueCookies.contains(s.getCookie())) {
	                            agstat.setByteCount(agstat.getByteCount()
	                                    + s.getByteCount());
	                            agstat.setByteCount(agstat.getPacketCount()
	                                    + s.getPacketCount());
	                            uniqueCookies.add(s.getCookie());
	                        }
	                    }
	                }
			    }
			    OFStatisticsReply ofsreply = new OFStatisticsReply();
		    	ofsreply.setXid(msg.getXid());
		    	ofsreply.setStatisticType(OFStatisticsType.AGGREGATE);
		    	ofsreply.setStatistics(Collections.singletonList(agstat));
		    	ofsreply.setLengthU(OFStatisticsReply.MINIMUM_LENGTH + agstat.getLength());
		        this.sendMsg(ofsreply);
			    break;
				
	        default:
	        	OVXSwitch.log.info("HandleClientIO handle OFStatistics {} for default and break", ((OFStatisticsRequest)msg).getStatisticType());
	        	break;
	        }
        	break;
        	
        //这个下发flowmod 	
        case FLOW_MOD:
        	this.hanleFlow_mod(msg,channel);
        	break;
            
        case GET_CONFIG_REQUEST:
			final OFGetConfigReply configreply = new OFGetConfigReply();
			configreply.setMissSendLength(this.getMissSendLen());
	        configreply.setXid(msg.getXid());
	        channel.write(Collections.singletonList(configreply));
			break; 
		
        case VENDOR:
        	break;
			
		default:
			OVXSwitch.log.info("HandleClientIO handle OFMessage {} for default and break", msg.getType());
			break;
		}
    }
}
