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
package cn.dlut.core.rpc.service.handlers.controlling;


import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.dlut.core.rpc.service.handlers.ApiHandler;
import cn.dlut.core.rpc.service.handlers.HandlerUtils;
import cn.dlut.core.rpc.service.handlers.ControllingHandler;
import cn.dlut.core.main.OpenVirteXController;
import cn.dlut.elements.controller.Controller;
import cn.dlut.elements.network.PhysicalNetwork;
import cn.dlut.exceptions.MissingRequiredField;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class AddController extends ApiHandler<Map<String, String>> {

    Logger log = LogManager.getLogger(AddController.class.getName());

    @Override
	public JSONRPC2Response process(final Map<String, String> params) {
		// TODO Auto-generated method stub
		JSONRPC2Response resp=null;
		
		 //this.log.info("receive request");
			String controllerUrl=params.get("controllerUrl");
			String controllerPort=params.get("controllerPort");
			
			Set<Controller> ctrls = new HashSet<Controller>();
			Controller ctrl= new Controller(controllerUrl,controllerPort);
			ctrls.add(ctrl);
			OpenVirteXController.getInstance().addControllers(ctrls);
	
			this.log.info("add controllers..." );
			
			List<String> res_success = new LinkedList<String>();
			res_success.add("add controller success");
			resp = new JSONRPC2Response(res_success, 0); 
			
			String responseString="add controller success!";
			resp.setResult(responseString);
	        return resp;
		
	}

    @Override
    public JSONRPC2ParamsType getType() {
        return JSONRPC2ParamsType.OBJECT;
    }

}
