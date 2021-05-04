package uk.ac.manchester.spinnaker.alloc;

import static java.util.Arrays.asList;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.spring.JaxRsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

/**
 * Builds the Spring beans in the application.
 */
@Configuration
// @EnableGlobalMethodSecurity(prePostEnabled=true, proxyTargetClass=true)
// @EnableWebSecurity
@Import(JaxRsConfig.class)
public class ServiceConfig {

	/**
	 * The context of the application.
	 */
	@Autowired
	private ApplicationContext ctx;

	/**
	 * The REST path of the server.
	 */
	@Value("${cxf.rest.path}")
	private String restPath;

	/**
	 * The implementation of the Spalloc service.
	 *
	 * @return bean
	 */
	@Bean
	public SpallocAPI service() {
		return new SpallocImpl();
	}

	/**
	 * The JAX-RS interface.
	 *
	 * @return bean
	 */
	@Bean
	public Server jaxRsServer() {
		final JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
		factory.setAddress(restPath);
		factory.setBus(ctx.getBean(SpringBus.class));
		factory.setServiceBeans(asList(service()));
		factory.setProviders(asList(new JacksonJsonProvider()));
		return factory.create();
	}

}
