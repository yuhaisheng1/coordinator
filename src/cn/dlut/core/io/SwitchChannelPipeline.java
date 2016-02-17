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

import java.util.concurrent.ThreadPoolExecutor;

import cn.dlut.core.main.OpenVirteXController;
import cn.dlut.elements.network.PhysicalNetwork;
import cn.dlut.core.io.SwitchChannelHandler;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;

public class SwitchChannelPipeline extends OpenflowChannelPipeline {

    private ExecutionHandler eh = null;
    private OpenVirteXController ctrl = null;

    public SwitchChannelPipeline(
            final OpenVirteXController openVirteXController,
            final ThreadPoolExecutor pipelineExecutor) {
        super();
        this.ctrl = openVirteXController;
        this.pipelineExecutor = pipelineExecutor;
        this.timer = PhysicalNetwork.getTimer();
        this.idleHandler = new IdleStateHandler(this.timer, 20, 25, 0);
        this.readTimeoutHandler = new ReadTimeoutHandler(this.timer, 30);
        this.eh = new ExecutionHandler(this.pipelineExecutor);
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        final SwitchChannelHandler handler = new SwitchChannelHandler(this.ctrl);
        final ChannelPipeline pipeline = Channels.pipeline();
        
        pipeline.addLast("ofmessagedecoder", new OVXMessageDecoder());
        pipeline.addLast("ofmessageencoder", new OVXMessageEncoder());
        pipeline.addLast("idle", this.idleHandler);
        pipeline.addLast("timeout", this.readTimeoutHandler);
        pipeline.addLast("handshaketimeout", new HandshakeTimeoutHandler(
                handler, this.timer, 15));

        pipeline.addLast("pipelineExecutor", eh);
        pipeline.addLast("handler", handler);
        return pipeline;
    }

}
