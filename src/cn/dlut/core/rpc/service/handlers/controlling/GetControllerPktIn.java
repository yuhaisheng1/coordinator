package cn.dlut.core.rpc.service.handlers.controlling;

import java.util.HashMap;

import java.util.Map;

import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

import net.sf.json.JSONObject;

import cn.dlut.core.main.OpenVirteXController;
import cn.dlut.core.rpc.service.handlers.ApiHandler;
import cn.dlut.elements.controller.Controller;

public class GetControllerPktIn extends ApiHandler<String> {

	@Override
	public JSONRPC2Response process(String params) {
		// TODO Auto-generated method stub
		JSONRPC2Response resp=null;
		
		Map<Controller , Integer> controllers_pktIn=new HashMap<Controller,Integer>();
		
		controllers_pktIn=OpenVirteXController.getInstance().getControllerPktIn();
		
		/*Controller ctrl1=new Controller("192.168.0.76","10000");
		Controller ctrl2=new Controller("192.168.0.76","20000");
		Controller ctrl3=new Controller("192.168.0.76","30000");
		controllers_pktIn.put(ctrl1, 32);
		controllers_pktIn.put(ctrl2, 33);
		controllers_pktIn.put(ctrl3, 33);*/
		String pktIn_answer=JSONObject.fromObject(controllers_pktIn).toString();
		
		//System.out.println(pktIn_answer);
		resp =new JSONRPC2Response("get packet_in"); //new JSONRPC2Response(res_success, 0);
		
		resp.setResult(JSONObject.fromObject(controllers_pktIn));
		return resp;
	}

	@Override
	public JSONRPC2ParamsType getType() {
		// TODO Auto-generated method stub
		return JSONRPC2ParamsType.NO_PARAMS;
	}

	
}
