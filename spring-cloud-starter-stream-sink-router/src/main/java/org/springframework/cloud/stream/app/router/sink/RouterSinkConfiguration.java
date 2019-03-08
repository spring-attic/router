/*
 * Copyright 2016-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.app.router.sink;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.groovy.GroovyScriptExecutingMessageProcessor;
import org.springframework.integration.router.AbstractMappingMessageRouter;
import org.springframework.integration.router.ExpressionEvaluatingRouter;
import org.springframework.integration.router.MessageRouter;
import org.springframework.integration.router.MethodInvokingRouter;
import org.springframework.integration.scripting.RefreshableResourceScriptSource;
import org.springframework.integration.scripting.ScriptVariableGenerator;
import org.springframework.integration.support.MutableMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.scripting.ScriptSource;

/**
 * A sink app that routes to one or more named channels.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Christian Tzolov
 * @author Soby Chacko
 */
@EnableBinding(Sink.class)
@EnableConfigurationProperties(RouterSinkProperties.class)
@Import(ScriptVariableGeneratorConfiguration.class)
public class RouterSinkConfiguration {

	@Bean
	@ServiceActivator(inputChannel = Sink.INPUT)
	public MessageRouter router(BinderAwareChannelResolver channelResolver,
			ScriptVariableGenerator scriptVariableGenerator, RouterSinkProperties properties) {
		AbstractMappingMessageRouter router;
		if (properties.getScript() != null) {
			router = new MethodInvokingRouter(scriptProcessor(scriptVariableGenerator, properties));
		}
		else {
			router = new ExpressionEvaluatingRouter(properties.getExpression()) {
				@Override
				protected void handleMessageInternal(Message<?> message) {
					if (message.getPayload() instanceof byte[]){
						String contentType = message.getHeaders().containsKey(MessageHeaders.CONTENT_TYPE)
								? message.getHeaders().get(MessageHeaders.CONTENT_TYPE).toString()
								: BindingProperties.DEFAULT_CONTENT_TYPE.toString();
						if (contentType.contains("text") || contentType.contains("json") || contentType.contains("x-spring-tuple")) {
							message = new MutableMessage<>(new String(((byte[]) message.getPayload())), message.getHeaders());
						}
					}
					super.handleMessageInternal(message);
				}
			};
		}
		router.setDefaultOutputChannelName(properties.getDefaultOutputChannel());
		router.setResolutionRequired(properties.isResolutionRequired());
		if (properties.getDestinationMappings() != null) {
			router.replaceChannelMappings(properties.getDestinationMappings());
		}
		router.setChannelResolver(channelResolver);
		return router;
	}

	@Bean
	@ConditionalOnProperty("router.script")
	public GroovyScriptExecutingMessageProcessor scriptProcessor(ScriptVariableGenerator scriptVariableGenerator,
			RouterSinkProperties properties) {
		ScriptSource scriptSource = new RefreshableResourceScriptSource(properties.getScript(),
				properties.getRefreshDelay());
		return new GroovyScriptExecutingMessageProcessor(scriptSource, scriptVariableGenerator);
	}

}
