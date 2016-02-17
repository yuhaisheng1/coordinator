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
package cn.dlut.core.main;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.openflow.vendor.nicira.OFNiciraVendorExtensions;

import cn.dlut.core.cmd.CmdLineSettings;
import cn.dlut.core.io.ClientChannelPipeline;
import cn.dlut.core.io.SwitchChannelPipeline;
import cn.dlut.core.rpc.server.JettyServer;
import cn.dlut.elements.controller.Controller;
import cn.dlut.elements.datapath.OVXQueue;
import cn.dlut.elements.datapath.OVXSwitch;
import cn.dlut.elements.datapath.PhysicalSwitch;
import cn.dlut.elements.network.PhysicalNetwork;

public class OpenVirteXController implements Runnable {

    Logger log = LogManager.getLogger(OpenVirteXController.class.getName());

    private static final int SEND_BUFFER_SIZE = 1024 * 1024;
    private static OpenVirteXController instance = null;
    //select the kind of the algorithm
    private static int coordinator_arithmetic=1;


    private String ofHost = null;
    private Integer ofPort = null;
    Thread server;
    //控制器所连接的OVXSwitch的队列
    public static Map<Controller,Set<OVXSwitch>> CtrlOVXMap = new HashMap<Controller,Set<OVXSwitch>>();
    private final NioClientSocketChannelFactory clientSockets = new NioClientSocketChannelFactory(
            Executors.newCachedThreadPool(), Executors.newCachedThreadPool());

    private ThreadPoolExecutor clientThreads = null;
    private ThreadPoolExecutor serverThreads = null;

    private final ChannelGroup sg = new DefaultChannelGroup();
    private final ChannelGroup cg = new DefaultChannelGroup();

    private SwitchChannelPipeline pfact = null;
    private ClientChannelPipeline cfact = null;

    private Integer statsRefresh;

    private Integer nClientThreads;

    private Integer nServerThreads;

    private final Boolean useBDDP;
    
    public static Set<Controller> ctrls;
    
    
    public static  int ctrl1pktInnum=0;
    public static  int ctrl2pktInnum=0;
    public static  int ctrl1pktOutnum=0;
    public static  int ctrl2pktOutnum=0;
    
    public static int totalpktInCount=0;
    
    public static int allpktInCount=0;
    

    public OpenVirteXController(CmdLineSettings settings) {
        this.ofHost = settings.getOFHost();
        this.ofPort = settings.getOFPort();
        this.log.info("OpenVirtex running at {}:{}", ofHost, ofPort);
        this.statsRefresh = settings.getStatsRefresh();
        this.nClientThreads = settings.getClientThreads();
        this.nServerThreads = settings.getServerThreads();
        this.useBDDP = settings.getUseBDDP();
        this.clientThreads = new OrderedMemoryAwareThreadPoolExecutor(nClientThreads, 1048576, 1048576, 5, TimeUnit.SECONDS);
        this.serverThreads = new OrderedMemoryAwareThreadPoolExecutor(
                nServerThreads, 1048576, 1048576, 5, TimeUnit.SECONDS);
        this.pfact = new SwitchChannelPipeline(this, this.serverThreads);
        OpenVirteXController.instance = this;
        this.ctrls=new HashSet<Controller>();
    }

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new OpenVirtexShutdownHook(this));
        initVendorMessages();
        PhysicalNetwork.getInstance().boot();
        
        this.startServer();
        
        try {
            final ServerBootstrap switchServerBootStrap = this
                    .createServerBootStrap();

            this.setServerBootStrapParams(switchServerBootStrap);

            switchServerBootStrap.setPipelineFactory(this.pfact);
            final InetSocketAddress sa = this.ofHost == null ? new InetSocketAddress(
                    this.ofPort) : new InetSocketAddress(this.ofHost,
                    this.ofPort);
            this.sg.add(switchServerBootStrap.bind(sa));
            

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        OVXQueue.SetPkt(0);
    }

    public void addControllers(final Set<Controller> ctrls , Set<PhysicalSwitch> switches) {
    	//Set<PhysicalSwitch> switches = PhysicalNetwork.getInstance().getSwitches();
    	for(Controller c:ctrls)
    	{
    		if(!OpenVirteXController.containsCtrl(c, OpenVirteXController.CtrlOVXMap.keySet()))
            {
    			OVXQueue.initQueue(c);
        		OpenVirteXController.ctrls.add(c);
            }
    	}
    	
    	for(PhysicalSwitch sw : switches){
    		//初始化switch对应的controller集合
    		OVXQueue.init_pswCtrls(sw.getSwitchId());
    		
    		
	        for (Controller ctrl : ctrls) {
	        	
	            String ipAddress = ctrl.getIp();
	            String port = ctrl.getPort();
	            OVXSwitch ovxsw = new OVXSwitch(sw.getSwitchId());
	            
	            final ClientBootstrap clientBootStrap = this
	                    .createClientBootStrap();
	            this.setClientBootStrapParams(clientBootStrap);
	            final InetSocketAddress remoteAddr = new InetSocketAddress(
	                    ipAddress, Integer.parseInt(port));
	            clientBootStrap.setOption("remoteAddress", remoteAddr);
	            
	            this.cfact = new ClientChannelPipeline(this, this.cg,
	                    this.clientThreads, clientBootStrap, ovxsw, sw);
	            
	            clientBootStrap.setPipelineFactory(this.cfact);
	            /*for free memory*/
	            ovxsw.setClientBootStrap(clientBootStrap);
	            final ChannelFuture cf = clientBootStrap.connect();
	
	            cf.addListener(new ChannelFutureListener() {
	            	//监听器启动后会调用这个重写的方法
	                @Override
	                public void operationComplete(final ChannelFuture e)
	                        throws Exception {
	                    if (e.isSuccess()) {
	                        ovxsw.setChannel(e.getChannel());
	                        cg.add(e.getChannel());
	                        
	        	            //将当前的新建立的OVXSwitch放入到对应的ControllerMap中
	        	            if(OpenVirteXController.containsCtrl(ctrl, OpenVirteXController.CtrlOVXMap.keySet()))
	        	            {
	        	            	System.out.println("controller"+ctrl.getIp()+":"+ctrl.getPort()+"init+++++++++++++");
	        	            	OVXQueue.AddPswCtrl(sw.getSwitchId(), ctrl);
	        	            	ovxsw.setController(ctrl);
	        	            	OpenVirteXController.CtrlOVXMap.get(ctrl).add(ovxsw);
	        	            }
	        	            else
	        	            {
	        	            	OVXQueue.AddPswCtrl(sw.getSwitchId(), ctrl);
	        	            	ovxsw.setController(ctrl);
	        	            	Set<OVXSwitch> ovx_list = new HashSet<OVXSwitch>();
	        	            	ovx_list.add(ovxsw);
	        	            	OpenVirteXController.CtrlOVXMap.put(ctrl, ovx_list);
	        	            	ovxsw.setController(ctrl);
	        	            }
	                        sw.addOVXSwitchSet(ovxsw);//在当前的PhysicalSwitch的OVXSwitch的队列中加入该OVXSwitch
	                        ovxsw.setPhysicalSwitch(sw);//设置当前的OVXSwitch对应的PhysicalSwitch
	                    } else {
	                    	//e.getChannel().getCloseFuture().awaitUninterruptibly();
	                        OpenVirteXController.this.log
	                                .error("Failed to connect to controller {} for switch {}",
	                                        remoteAddr, sw.getSwitchName());
	                    }
	                }
	            });
	        }
    	}
    }
    
    public void addControllers(final Set<Controller> ctrls ) {
    	Set<PhysicalSwitch> switches = PhysicalNetwork.getInstance().getSwitches();
    	for(Controller c:ctrls)
    	{
    		if(!OpenVirteXController.containsCtrl(c, OpenVirteXController.CtrlOVXMap.keySet()))
            {
    			OVXQueue.initQueue(c);
        		OpenVirteXController.ctrls.add(c);
            }
    	}
    	
    	for(PhysicalSwitch sw : switches){
    		//初始化switch对应的controller集合
    		OVXQueue.init_pswCtrls(sw.getSwitchId());
    		
    		
	        for (Controller ctrl : ctrls) {
	        	
	            String ipAddress = ctrl.getIp();
	            String port = ctrl.getPort();
	            OVXSwitch ovxsw = new OVXSwitch(sw.getSwitchId());
	            
	            final ClientBootstrap clientBootStrap = this
	                    .createClientBootStrap();
	            this.setClientBootStrapParams(clientBootStrap);
	            final InetSocketAddress remoteAddr = new InetSocketAddress(
	                    ipAddress, Integer.parseInt(port));
	            clientBootStrap.setOption("remoteAddress", remoteAddr);
	            
	            this.cfact = new ClientChannelPipeline(this, this.cg,
	                    this.clientThreads, clientBootStrap, ovxsw, sw);
	            
	            clientBootStrap.setPipelineFactory(this.cfact);
	            /*for free memory*/
	            ovxsw.setClientBootStrap(clientBootStrap);
	            final ChannelFuture cf = clientBootStrap.connect();
	
	            cf.addListener(new ChannelFutureListener() {
	            	//监听器启动后会调用这个重写的方法
	                @Override
	                public void operationComplete(final ChannelFuture e)
	                        throws Exception {
	                    if (e.isSuccess()) {
	                        ovxsw.setChannel(e.getChannel());
	                        cg.add(e.getChannel());
	                        
	        	            //将当前的新建立的OVXSwitch放入到对应的ControllerMap中
	        	            if(OpenVirteXController.containsCtrl(ctrl, OpenVirteXController.CtrlOVXMap.keySet()))
	        	            {
	        	            	System.out.println("controller"+ctrl.getIp()+":"+ctrl.getPort()+"init+++++++++++++");
	        	            	OVXQueue.AddPswCtrl(sw.getSwitchId(), ctrl);
	        	            	ovxsw.setController(ctrl);
	        	            	OpenVirteXController.CtrlOVXMap.get(ctrl).add(ovxsw);
	        	            }
	        	            else
	        	            {
	        	            	OVXQueue.AddPswCtrl(sw.getSwitchId(), ctrl);
	        	            	ovxsw.setController(ctrl);
	        	            	Set<OVXSwitch> ovx_list = new HashSet<OVXSwitch>();
	        	            	ovx_list.add(ovxsw);
	        	            	OpenVirteXController.CtrlOVXMap.put(ctrl, ovx_list);
	        	            	ovxsw.setController(ctrl);
	        	            }
	                        sw.addOVXSwitchSet(ovxsw);//在当前的PhysicalSwitch的OVXSwitch的队列中加入该OVXSwitch
	                        ovxsw.setPhysicalSwitch(sw);//设置当前的OVXSwitch对应的PhysicalSwitch
	                    } else {
	                    	//e.getChannel().getCloseFuture().awaitUninterruptibly();
	                        OpenVirteXController.this.log
	                                .error("Failed to connect to controller {} for switch {}",
	                                        remoteAddr, sw.getSwitchName());
	                    }
	                }
	            });
	        }
    	}
    }
    
    //删除控制器 @author daiminglong
    public void deleteController(Controller ctrl){
    	Set<OVXSwitch> switchs=new HashSet<OVXSwitch>();
    	for(Controller c:OpenVirteXController.CtrlOVXMap.keySet())
    	{
    		//System.out.println("+++"+c.getIp()+":"+c.getPort());
    		if(c.getIp().equals(ctrl.getIp()) && c.getPort().equals(ctrl.getPort()))
    			switchs=OpenVirteXController.CtrlOVXMap.get(c);
    		
    	}
    
    	OpenVirteXController.ctrls.remove(ctrl);

    	
    		for(OVXSwitch ovxsw : switchs)
        	{
    			OVXQueue.DelPswCtrl(ovxsw.getPhysicalSwitch().getSwitchId(), ctrl);
        		         
    			ovxsw.getPhysicalSwitch().deleteOVXSwitchSet(ovxsw);
        		/*to pass reconnectHandler*/
        		ovxsw.setConnected(false);
        		ovxsw.finallize();//销毁该OVXSwitch
        		System.out.println("77777777777777");
        		ovxsw.getChannel().getCloseFuture().awaitUninterruptibly();//出不来
        		System.out.println("88888888888888888");
        		ovxsw.getClientBootStrap().releaseExternalResources();
        		
        		cg.remove(ovxsw.getChannel());
        		System.out.println("99999999999999999");
        		
        	}
     	
    }
    
    
    public void setCoordinator_arithmetic(int ath){
    	OpenVirteXController.coordinator_arithmetic=ath;
    }




    public Map<Controller ,Integer> getControllerPktIn(){
    	
    	Map<Controller , Integer> controllers_pktIn=new HashMap<Controller,Integer>();
    	for(Controller c : OpenVirteXController.ctrls)
		{
    		controllers_pktIn.put(c, OVXQueue.GetCtrlPktIn().get(c).size());
    		//System.out.println(OVXQueue.GetCtrlPktIn().get(c).size()+"+++++++++++++++++++");
		}
    	return controllers_pktIn;
    }

    private void startServer() {
        // TODO: pass this via cmd args.
        this.server = new Thread(new JettyServer(62000));
        this.server.start();
    }
    
    private void setServerBootStrapParams(final ServerBootstrap bootstrap) {
        bootstrap.setOption("reuseAddr", true);
        bootstrap.setOption("child.keepAlive", true);
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.sendBufferSize",
                OpenVirteXController.SEND_BUFFER_SIZE);

    }

    private void setClientBootStrapParams(final ClientBootstrap bootstrap) {
        bootstrap.setOption("reuseAddr", true);
        bootstrap.setOption("child.keepAlive", true);
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.sendBufferSize",
                OpenVirteXController.SEND_BUFFER_SIZE);

    }

    private ClientBootstrap createClientBootStrap() {
        return new ClientBootstrap(this.clientSockets);
    }

    private ServerBootstrap createServerBootStrap() {
        return new ServerBootstrap(new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool()));
    }

    public void terminate() {
        if (this.cg != null && this.cg.close().awaitUninterruptibly(1000)) {
            this.log.info("Shut down all controller connections. Quitting...");
        } else {
            this.log.error("Error shutting down all controller connections. Quitting anyway.");
        }

        if (this.sg != null && this.sg.close().awaitUninterruptibly(1000)) {
            this.log.info("Shut down all switch connections. Quitting...");
        } else {
            this.log.error("Error shutting down all switch connections. Quitting anyway.");
        }

        if (this.pfact != null) {
            this.pfact.releaseExternalResources();
        }
        if (this.cfact != null) {
            this.cfact.releaseExternalResources();
        }

    }
    
    public static boolean containsCtrl(Controller ctrl, Set<Controller> ctrls){
    	if(ctrls.isEmpty())
    		return false;
    	for(Controller c : ctrls){
    		if(c.getIp().equals(ctrl.getIp()) && c.getPort().equals(ctrl.getPort()))
    			return true;
    	}
    	return false;
    }

    public static OpenVirteXController getInstance() {
        if (OpenVirteXController.instance == null) {
            throw new RuntimeException(
                    "The OpenVirtexController has not been initialized; quitting.");
        }
        return OpenVirteXController.instance;
    }

    
    public Integer getStatsRefresh() {
        return this.statsRefresh;
    }

    private void initVendorMessages() {
        // Configure openflowj to be able to parse the role request/reply
        // vendor messages.
        OFNiciraVendorExtensions.initialize();

    }

    public Boolean getUseBDDP() {
        return this.useBDDP;
    }
    
    public static void  setCoordinator(int x)
    {
 	   OpenVirteXController.coordinator_arithmetic = x;
    }
    
    public static int getCoordinator()
    {
 	   return OpenVirteXController.coordinator_arithmetic;
    }


}
