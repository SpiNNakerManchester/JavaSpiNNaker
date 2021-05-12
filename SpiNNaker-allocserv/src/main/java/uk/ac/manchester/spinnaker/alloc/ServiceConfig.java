/*
 * Copyright (c) 2014-2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc;

import static java.util.Arrays.asList;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.spring.JaxRsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import uk.ac.manchester.spinnaker.alloc.web.SpallocAPI;

/**
 * Builds the Spring beans in the application.
 */
@Configuration
// @EnableGlobalMethodSecurity(prePostEnabled=true, proxyTargetClass=true)
// @EnableWebSecurity
@Import(JaxRsConfig.class)
@ComponentScan
@PropertySource("classpath:service.properties")
@EnableScheduling
public class ServiceConfig {
	/**
	 * The JAX-RS interface.
	 *
	 * @return bean
	 */
	@Bean
    @Profile("!test")
	@DependsOn("bus")
	public Server jaxRsServer(@Value("${cxf.rest.path}") String restPath,
			SpallocAPI service, SpringBus bus) {
		final JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
		factory.setAddress(restPath);
		factory.setBus(bus);
		factory.setServiceBeans(asList(service));
		factory.setProviders(asList(new JacksonJsonProvider()));
		return factory.create();
	}
}
