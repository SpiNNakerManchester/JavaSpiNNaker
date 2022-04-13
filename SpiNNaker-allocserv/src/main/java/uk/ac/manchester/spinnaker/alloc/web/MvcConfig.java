/*
 * Copyright (c) 2021 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(wsHandler, "/srv/proxy/*");
	}
}
