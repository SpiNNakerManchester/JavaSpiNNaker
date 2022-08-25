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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import uk.ac.manchester.spinnaker.alloc.ServiceConfig.URLPathMaker;

/**
 * A simple controller for the root of the service. Redirects to the actual web
 * interface.
 */
@Controller
@RequestMapping("/")
public class RootController {
	@Autowired
	private URLPathMaker urlMaker;

	/**
	 * @return redirect to {@link SystemController#index()}
	 */
	@GetMapping(value = "/", produces = "text/html")
	public String index() {
		return "redirect:" + urlMaker.systemUrl("");
	}
}
