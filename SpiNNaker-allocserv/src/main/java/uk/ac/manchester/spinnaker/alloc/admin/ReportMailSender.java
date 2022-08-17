/*
 * Copyright (c) 2021-2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.admin;

import static java.util.Objects.nonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.beans.factory.config.BeanDefinition.ROLE_SUPPORT;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Role;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

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
		return nonNull(propValue) && !propValue.isEmpty();
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
		SimpleMailMessage message = new SimpleMailMessage();
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
