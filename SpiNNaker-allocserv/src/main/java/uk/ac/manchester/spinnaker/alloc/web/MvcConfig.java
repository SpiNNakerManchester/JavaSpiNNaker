/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.web;

import static org.springframework.beans.factory.config.BeanDefinition.ROLE_SUPPORT;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import uk.ac.manchester.spinnaker.alloc.SpallocProperties;
import uk.ac.manchester.spinnaker.alloc.proxy.SpinWSHandler;

/**
 * Sets up the login page and static resource mappings. Note that paths in here
 * are relative to Spring's idea of the root of the webapp (as they're used to
 * program path matchers).
 */
@EnableWebMvc
@EnableWebSocket
@Configuration
@Role(ROLE_SUPPORT)
public class MvcConfig implements WebMvcConfigurer, WebSocketConfigurer {
	// TODO check if we should use the url path maker bean

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/system/login.html");
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/system/resources/**")
				.addResourceLocations(
						"classpath:/META-INF/public-web-resources/");
	}

	@Autowired
	private SpinWSHandler wsHandler;

	@Autowired
	private SpallocProperties props;

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		if (props.getProxy().isEnable()) {
			// It's its own interceptor!
			registry.addHandler(wsHandler, "/proxy/*")
					.addInterceptors(wsHandler)
					.setAllowedOriginPatterns("*");
		}
	}
}
