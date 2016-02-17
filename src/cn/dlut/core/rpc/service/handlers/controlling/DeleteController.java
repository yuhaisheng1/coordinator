package cn.dlut.core.rpc.service.handlers.controlling;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cn.dlut.core.rpc.service.handlers.ApiHandler;
import cn.dlut.core.main.OpenVirteXController;
import cn.dlut.elements.controller.Controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class DeleteController extends ApiHandler<Map<String, String>> {
	
	Logger log=LogManager.getLogger(DeleteController.class.getName());
	
	@Override
	public JSONRPC2Response process(final Map<String, String> params) {
		// TODO Auto-generated method stub
		JSONRPC2Response resp=null;
		
		 //this.log.info("receive request");
			String controllerUrl=params.get("controllerUrl");
			String controllerPort=params.get("controllerPort");
			
			Controller ctrl= new Controller(controllerUrl,controllerPort);
			OpenVirteXController.getInstance().deleteController(ctrl);
	
			this.log.info("Deleting controllers..." );
			
			List<String> res_success = new LinkedList<String>();
			res_success.add("delete controller success");
			resp = new JSONRPC2Response(res_success, 0); 

			String responseString="delete controller success!";
			resp.setResult(responseString);
	        return resp;
		
	}

	@Override
	public JSONRPC2ParamsType getType() {
		// TODO Auto-generated method stub
		return JSONRPC2ParamsType.OBJECT;
	}

}
