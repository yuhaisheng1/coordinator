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
package cn.dlut.util;


//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFError;
import org.openflow.protocol.OFError.OFBadActionCode;
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.OFError.OFErrorType;
import org.openflow.protocol.OFError.OFFlowModFailedCode;
import org.openflow.protocol.OFError.OFPortModFailedCode;
import org.openflow.protocol.OFMessage;

/**
 * Utility class for OVX messages. Implements methods
 * for creating error messages and transaction ID
 * mappings.
 */
public final class OVXMessageUtil {

    //private static Logger log = LogManager.getLogger(OVXMessageUtil.class.getName());

    /**
     * Overrides default constructor to no-op private constructor.
     * Required by checkstyle.
     */
    private OVXMessageUtil() {
    }

    /**
     * Makes an OpenFlow error message for a bad action and
     * given OpenFlow message.
     *
     * @param code the bad action code
     * @param msg the OpenFlow message
     * @return the OpenFlow error message
     */
    public static OFMessage makeError(final OFBadActionCode code,
            final OFMessage msg) {
        final OFError err = new OFError();
        err.setErrorType(OFErrorType.OFPET_BAD_REQUEST);
        err.setErrorCode(code);
        err.setOffendingMsg(msg);
        err.setXid(msg.getXid());
        return err;
    }

    /**
     * Makes an OpenFlow error message for a failed flow mod and
     * given OpenFlow message.
     *
     * @param code the failed flow mod code
     * @param msg the OpenFlow message
     * @return the OpenFlow error message
     */
    public static OFMessage makeErrorMsg(final OFFlowModFailedCode code,
            final OFMessage msg) {
        final OFError err = new OFError();
        err.setErrorType(OFErrorType.OFPET_FLOW_MOD_FAILED);
        err.setErrorCode(code);
        err.setOffendingMsg(msg);
        err.setXid(msg.getXid());
        return err;
    }

    /**
     * Makes an OpenFlow error message for a failed port mod and
     * given OpenFlow message.
     *
     * @param code the failed port mod code
     * @param msg the OpenFlow message
     * @return the OpenFlow error message
     */
    public static OFMessage makeErrorMsg(final OFPortModFailedCode code,
            final OFMessage msg) {
        final OFError err = new OFError();
        err.setErrorType(OFErrorType.OFPET_PORT_MOD_FAILED);
        err.setErrorCode(code);
        err.setOffendingMsg(msg);
        err.setXid(msg.getXid());
        return err;
    }

    /**
     * Makes an OpenFlow error message for a bad request and
     * given OpenFlow message.
     *
     * @param code the bad request code
     * @param msg the OpenFlow message
     * @return the OpenFlow error message
     */
    public static OFMessage makeErrorMsg(final OFBadRequestCode code,
            final OFMessage msg) {
        final OFError err = new OFError();
        err.setErrorType(OFErrorType.OFPET_BAD_REQUEST);
        err.setErrorCode(code);
        err.setOffendingMsg(msg);
        err.setXid(msg.getXid());
        return err;
    }

}
