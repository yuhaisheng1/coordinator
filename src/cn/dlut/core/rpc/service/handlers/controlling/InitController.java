package cn.dlut.core.rpc.service.handlers.controlling;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

import cn.dlut.core.main.OpenVirteXController;
import cn.dlut.core.rpc.service.handlers.ApiHandler;
import cn.dlut.core.rpc.service.handlers.ControllingHandler;
import cn.dlut.core.rpc.service.handlers.HandlerUtils;
import cn.dlut.elements.controller.Controller;
import cn.dlut.elements.datapath.PhysicalSwitch;
import cn.dlut.exceptions.MissingRequiredField;

public class InitController extends ApiHandler<Map<String , Object>> {

	 Logger log = LogManager.getLogger(AddController.class.getName());

	    @Override
	    public JSONRPC2Response process(final Map<String, Object> params) {
	        JSONRPC2Response resp = null;

	        try {
	        	final String isAddController = HandlerUtils.<String>fetchField(
	                    ControllingHandler.isInitController, params, false, "no");
	        	
	        	//this.log.info("receive request");
	        	
	        	if(!isAddController.equals("yes")){
	        		List<String> res_fail = new LinkedList<String>();
	                res_fail.add("adding controller fail");
	                resp = new JSONRPC2Response(res_fail, 0);
	                return resp;
	        	}
	        	//Controller c1 = new Controller("192.168.0.168","6633");
	        	Controller c1 = new Controller("192.168.0.74","10000");
	        	Controller c2 = new Controller("192.168.0.74","20000");
	        	Controller c3 = new Controller("192.168.0.74","30000");
	        	//Controller c3 = new Controller("127.0.0.1","6688");
	        	Set<Controller> ctrls = new HashSet<Controller>();
	        	ctrls.add(c1);
	        	ctrls.add(c2);
	        	ctrls.add(c3);
	   
	        	
	            OpenVirteXController.getInstance().addControllers(ctrls);

	            this.log.info("Adding controllers for switches..." );
	            
	            List<String> res_success = new LinkedList<String>();
	            res_success.add("adding controller success");
	            resp = new JSONRPC2Response(res_success, 0);
	            
                  	           

	        } catch (final MissingRequiredField e) {
	            resp = new JSONRPC2Response(
	                    new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(),
	                            this.cmdName() + ": Unable to add controllers : "
	                                    + e.getMessage()), 0);
	        } 

	        return resp;
	    }

	@Override
	public JSONRPC2ParamsType getType() {
		// TODO Auto-generated method stub
		return JSONRPC2ParamsType.OBJECT;
	}

}
