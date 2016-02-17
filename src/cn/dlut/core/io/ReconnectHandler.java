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
package cn.dlut.core.io;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import cn.dlut.core.main.OpenVirteXController;
import cn.dlut.elements.controller.Controller;
import cn.dlut.elements.datapath.OVXSwitch;
import cn.dlut.elements.datapath.PhysicalSwitch;
import cn.dlut.exceptions.ReconnectException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;

public class ReconnectHandler extends SimpleChannelHandler {

    Logger log = LogManager.getLogger(ReconnectHandler.class.getName());

    static final ReconnectException EXCEPTION = new ReconnectException();

    final ClientBootstrap bootstrap;
    final Timer timer;
    volatile Timeout timeout;
    private final Integer maxBackOff;

    private final OVXSwitch ovxsw;

    private final ChannelGroup cg;
    
    private final PhysicalSwitch sw;

    public ReconnectHandler(final OVXSwitch ovxsw,
            final ClientBootstrap bootstrap, final Timer timer,
            final int maxBackOff, final ChannelGroup cg, final PhysicalSwitch sw) {
        super();
        this.ovxsw = ovxsw;
        this.bootstrap = bootstrap;
        this.timer = timer;
        this.maxBackOff = maxBackOff;
        this.cg = cg;
        this.sw = sw;
    }

    @Override
    public void channelClosed(final ChannelHandlerContext ctx,
            final ChannelStateEvent e) {
        if (!this.ovxsw.isConnected()) {
            return;
        }
        this.ovxsw.removeChannel(e.getChannel());
        final int retry = this.ovxsw.incrementBackOff();
        final Integer backOffTime = Math.min(1 << retry, this.maxBackOff);

        this.timeout = this.timer.newTimeout(new ReconnectTimeoutTask(this.ovxsw,
                this.cg, this.sw), backOffTime, TimeUnit.SECONDS);

        this.log.error("Backing off {} for controller {}", backOffTime,
                this.bootstrap.getOption("remoteAddress"));
        ctx.sendUpstream(e);

    }

    @Override
    public void channelDisconnected(final ChannelHandlerContext ctx,
            final ChannelStateEvent e) {
        ctx.sendUpstream(e);
    }

    @Override
    public void channelConnected(final ChannelHandlerContext ctx,
            final ChannelStateEvent e) {
        this.ovxsw.resetBackOff();
        ctx.sendUpstream(e);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx,
            final ExceptionEvent e) {

        final Throwable cause = e.getCause();
        if (cause instanceof ConnectException) {
            return;
        }

        ctx.sendUpstream(e);
    }

    private final class ReconnectTimeoutTask implements TimerTask {

        OVXSwitch ovxsw = null;
        PhysicalSwitch sw = null;
        private final ChannelGroup cg;

        public ReconnectTimeoutTask(final OVXSwitch ovxsw, final ChannelGroup cg, final PhysicalSwitch sw) {
            this.ovxsw = ovxsw;
            this.cg = cg;
            this.sw = sw;
        }

        @Override
        public void run(final Timeout timeout) throws Exception {

            final InetSocketAddress remoteAddr = (InetSocketAddress) bootstrap
                    .getOption("remoteAddress");
            final ChannelFuture cf = bootstrap.connect();

            cf.addListener(new ChannelFutureListener() {
            	
            	private boolean containsOVXSwitch(OVXSwitch ovxsw, Set<OVXSwitch> ovx_list) {
            		if(ovx_list.isEmpty())
            			return false;
	            	for(OVXSwitch ovx : ovx_list){
	            		if(ovx.equals(ovxsw))
	            			return true;
	            		}
            		return false;
            	}

                @Override
                public void operationComplete(final ChannelFuture e)
                        throws Exception {
                    if (e.isSuccess()) {
                    		ovxsw.setChannel(e.getChannel());
                    		cg.add(e.getChannel());
                    		ovxsw.setClientBootStrap(bootstrap);
                    		Controller ctrl = new Controller(remoteAddr.getAddress().toString(), remoteAddr.getPort()+"");
                    		
	        	            if(OpenVirteXController.containsCtrl(ctrl, OpenVirteXController.CtrlOVXMap.keySet()))
	        	            {
	        	            	if(!this.containsOVXSwitch(ovxsw, OpenVirteXController.CtrlOVXMap.get(ctrl)))
	        	            		OpenVirteXController.CtrlOVXMap.get(ctrl).add(ovxsw);
	        	            }
	        	            else
	        	            {
	        	            	if(!this.containsOVXSwitch(ovxsw, OpenVirteXController.CtrlOVXMap.get(ctrl))) {
	        	            		Set<OVXSwitch> ovx_list = new HashSet<OVXSwitch>();
	        	            		ovx_list.add(ovxsw);
	        	            		OpenVirteXController.CtrlOVXMap.put(ctrl, ovx_list);
	        	            	}
	        	            }
	        	            if(!this.containsOVXSwitch(ovxsw, sw.getOVXSwitchSet()))
	        	            	sw.addOVXSwitchSet(ovxsw);//在当前的PhysicalSwitch的OVXSwitch的队列中加入该OVXSwitch
	                        
	        	            ovxsw.setPhysicalSwitch(sw);//设置当前的OVXSwitch对应的PhysicalSwitch
                    }
                    else {
                    	ReconnectHandler.this.log
                             .error("Failed to connect to controller {} for switch {}",
                                   remoteAddr, ReconnectTimeoutTask.this.ovxsw.getSwitchName());
                    }
                }
            });
        }
    }
}
