/*
 * Copyright (c) 2021-2022 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.admin;

import static java.util.Objects.nonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.beans.factory.config.BeanDefinition.ROLE_SUPPORT;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Role;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties;
import uk.ac.manchester.spinnaker.alloc.SpallocProperties.ReportProperties;

/**
 * Basic email sending service.
 *
 * @author Donal Fellows
 */
@Component
@Role(ROLE_SUPPORT)
public class ReportMailSender {
	private static final Logger log = getLogger(ReportMailSender.class);

	@Autowired(required = false)
	private JavaMailSender emailSender;

	@Autowired
	private SpallocProperties properties;

	private ReportProperties props;

	@PostConstruct
	private void getProps() {
		props = properties.getReportEmail();
	}

	private static boolean haveProp(String propValue) {
		return nonNull(propValue) && !propValue.isBlank();
	}

	/**
	 * Send an assembled message if the service is configured to do so.
	 * <p>
	 * <strong>NB:</strong> This call may take some time; do not hold a
	 * transaction open when calling this.
	 *
	 * @param email
	 *            The message contents to send.
	 */
	public void sendServiceMail(Object email) {
		if (nonNull(emailSender) && haveProp(props.getTo()) && props.isSend()) {
			sendMessage(email.toString());
		}
	}

	private void sendMessage(String body) {
		var message = new SimpleMailMessage();
		if (haveProp(props.getFrom())) {
			message.setFrom(props.getFrom());
		}
		message.setTo(props.getTo());
		if (haveProp(props.getSubject())) {
			message.setSubject(props.getSubject());
		}
		message.setText(body);
		try {
			emailSender.send(message);
		} catch (MailException e) {
			log.warn("problem when sending email", e);
		}
	}
}
