package cn.dlut.elements.datapath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import cn.dlut.core.main.OpenVirteXController;
import cn.dlut.elements.controller.Controller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OVXQueue {
	
	public static Logger log = LogManager.getLogger(OVXQueue.class.getName());

	/*mapping of PhysicalSwitchID and ControllerSet*/
	protected static AtomicReference<HashMap<Long, HashSet<Controller>>> psw_ctrls = 
			new AtomicReference<HashMap<Long, HashSet<Controller>>>();
	
	/*mapping of Controller and OFPacketInList*/
	// ArrayList<Integer>——bufferId
	protected static AtomicReference<HashMap<Controller, ArrayList<Integer>>> ctrl_pktins = 
			new AtomicReference<HashMap<Controller, ArrayList<Integer>>>();
	
			protected static AtomicReference<Integer> pktInTotalCount=new AtomicReference<Integer>();
			
			@SuppressWarnings("unchecked")
			public static void SetPkt(Integer num) {
				pktInTotalCount.set(num);
			}
			
			@SuppressWarnings("unchecked")
			public static  Integer GetPkt() {
				return pktInTotalCount.get();
			}
			
	@SuppressWarnings("unchecked")
	public static void init_pswCtrls(Long switch_id)
	{
		HashMap<Long, HashSet<Controller>> map = (HashMap<Long, HashSet<Controller>>) GetPswCtrl().clone();
		if(!map.containsKey(switch_id))//修复添加控制器bug 如果当前没有此个switch_id的控制器队列时才新建一个
		{
			map.put(switch_id, new HashSet<Controller>());
			SetPswCtrl(map);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void  initQueue(Controller ctrl){
		HashMap<Controller, ArrayList<Integer>> map = 
				(HashMap<Controller, ArrayList<Integer>>) GetCtrlPktIn().clone();
		map.put(ctrl, new ArrayList<Integer>());
		SetCtrlPktIn(map);
	}
	
	/*add Controller with specified PhysicalSwitchID*/ 
	@SuppressWarnings("unchecked")
	public static void AddPswCtrl(Long switchID, Controller ctrl) {
		HashMap<Long, HashSet<Controller>> map = (HashMap<Long, HashSet<Controller>>) GetPswCtrl().clone();
		System.out.println("switch:"+switchID+"add ctrl:"+ctrl.getIp()+":"+ctrl.getPort()+"okkkkk");
		map.get(switchID).add(ctrl);
		System.out.println(map.get(switchID).size());
		SetPswCtrl(map);
	}
	
	/*delete Controller with specified PhysicalSwitchID*/ 
	@SuppressWarnings("unchecked")
	public static void DelPswCtrl(Long switchID, Controller ctrl) {
		HashMap<Long, HashSet<Controller>> map = (HashMap<Long, HashSet<Controller>>) GetPswCtrl().clone();	
		HashMap<Controller,  ArrayList<Integer>> map2 = (HashMap<Controller,  ArrayList<Integer>>) GetCtrlPktIn().clone();
		HashSet<Controller> leaveSet=new HashSet<Controller>();
		for(Controller c:map.get(switchID))
    	{
    		if(!(c.getIp().equals(ctrl.getIp()) && c.getPort().equals(ctrl.getPort())))
    		{
    			leaveSet.add(c);
    		}	
    		else
    		{
    			map2.remove(c);    			
    		}
    	}
		map.remove(switchID);
		map.put(switchID, leaveSet);
		SetPswCtrl(map);
		SetCtrlPktIn(map2);
	}
	
	public static void SetPswCtrl(HashMap<Long, HashSet<Controller>> map) {
		psw_ctrls.set(map);
	}
	
	public static HashMap<Long, HashSet<Controller>> GetPswCtrl() {
		if(psw_ctrls.get() == null){
			HashMap<Long, HashSet<Controller>> map  = new HashMap<Long, HashSet<Controller>>();
			SetPswCtrl(map);
		}
		return psw_ctrls.get();
	}

	
	/*add OFPacketIn with specified Controller*/
	@SuppressWarnings("unchecked")
	public static void AddCtrlPktIn(Controller ctrl, Integer bufferid) {
		HashMap<Controller,  ArrayList<Integer>> map = 
				(HashMap<Controller,  ArrayList<Integer>>) GetCtrlPktIn().clone();
		
		
		ArrayList<Integer> list = map.get(ctrl);
		//to be done
		//if(list.contains(bufferid))return;
		list.add(bufferid);
		map.put(ctrl, list);
		SetCtrlPktIn(map);
		//OVXQueue.log.info(ctrl.getIp()+":"+ctrl.getPort()+"-queue length:"+map.get(ctrl).size());
	}
	
	/*delete OFPacketIn with specified Controller*/
	@SuppressWarnings("unchecked")
	public static void DelCtrlPktIn(Controller ctrl, Integer bufferid) {
		HashMap<Controller, ArrayList<Integer>> map = 
				(HashMap<Controller, ArrayList<Integer>>) GetCtrlPktIn().clone();
		ArrayList<Integer> list = map.get(ctrl);
		
		//OVXQueue.log.info(ctrl.getIp()+":"+ctrl.getPort()+"-queue length:"+map.get(ctrl).size());
		//System.out.println("this is before queue length:"+list.size());
		if(list.size()>0){			
			list.remove(0);
		}
		//System.out.println("this is after queue length:"+list.size());
		map.put(ctrl, list);
		SetCtrlPktIn(map);
	}
	
	public static void SetCtrlPktIn(HashMap<Controller, ArrayList<Integer>> map) {
		ctrl_pktins.set(map);
	}
	
	public static HashMap<Controller, ArrayList<Integer>> GetCtrlPktIn() {
		if(ctrl_pktins.get() == null){
			HashMap<Controller, ArrayList<Integer>> map  = new HashMap<Controller, ArrayList<Integer>>();
			SetCtrlPktIn(map);
		}
		return ctrl_pktins.get();
	}
	
	@SuppressWarnings("unchecked")
	public static Controller getMinQueue(Long switchID) {
		int min=Integer.MAX_VALUE;
		Controller ctrl = null;
		HashMap<Controller, ArrayList<Integer>> map = 
				(HashMap<Controller, ArrayList<Integer>>) GetCtrlPktIn().clone();
		Set<Controller> ctrlSet=new HashSet<Controller>();
		ctrlSet=GetPswCtrl().get(switchID);
		for(Controller c : ctrlSet)
		{	
			if( map.get(c).size() < min) {
				ctrl = c;
				min = map.get(c).size();			
				OVXQueue.log.error(c.getIp()+":"+c.getPort()+"-queue length:"+map.get(c).size());
			}
		}
		return ctrl;
	}
}
