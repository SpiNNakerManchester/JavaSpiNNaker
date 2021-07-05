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
package uk.ac.manchester.spinnaker.alloc.admin;

import java.net.URI;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.impl.UriBuilderImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import uk.ac.manchester.spinnaker.alloc.SecurityConfig.TrustLevel;
import uk.ac.manchester.spinnaker.alloc.admin.AdminAPI.User;

@Controller
public class AdminController {
	@Autowired
	private AdminAPI adminAPI;

	@GetMapping("/users")
	public ModelAndView listUsers() throws SQLException {
		URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentRequestUri()
				.toUriString());
		UriInfo ui = new UriInfo() {

			@Override
			public String getPath() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getPath(boolean decode) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<PathSegment> getPathSegments() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<PathSegment> getPathSegments(boolean decode) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public URI getRequestUri() {
				// TODO Auto-generated method stub
				return uri;
			}

			@Override
			public UriBuilder getRequestUriBuilder() {
				return new UriBuilderImpl(uri);
			}

			@Override
			public URI getAbsolutePath() {
				// TODO Auto-generated method stub
				return uri;
			}

			@Override
			public UriBuilder getAbsolutePathBuilder() {
				return new UriBuilderImpl(uri);
			}

			@Override
			public URI getBaseUri() {
				// TODO Auto-generated method stub
				return uri;
			}

			@Override
			public UriBuilder getBaseUriBuilder() {
				return new UriBuilderImpl(uri);
			}

			@Override
			public MultivaluedMap<String, String> getPathParameters() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public MultivaluedMap<String, String>
					getPathParameters(boolean decode) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public MultivaluedMap<String, String> getQueryParameters() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public MultivaluedMap<String, String>
					getQueryParameters(boolean decode) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<String> getMatchedURIs() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<String> getMatchedURIs(boolean decode) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<Object> getMatchedResources() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public URI resolve(URI uri) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public URI relativize(URI uri) {
				// TODO Auto-generated method stub
				return null;
			}

		};

		Map<String, Object> map = new HashMap<>();
		map.put("userlist", adminAPI.listUsers(ui));
		map.put("user", new User());
		return new ModelAndView("users", map);
	}

	@PostMapping("/users")
	public ModelAndView createUser(@Valid @ModelAttribute("user") User user,
			BindingResult result, ModelMap model) throws SQLException {
		if (result.hasErrors()) {
			return new ModelAndView("error");
		}
		// TODO handle create user
		return new ModelAndView("users");
	}

	@GetMapping("/users/{id}")
	public ModelAndView showUserForm(@PathVariable int id) throws SQLException {
		// return "formtest";
		ModelAndView mav =
				new ModelAndView("user", "user", adminAPI.describeUser(id));
		mav.addObject("trustLevels", TrustLevel.values());
		return mav;
	}

	@PostMapping("/users/{id}")
	public String submitUserForm(@PathVariable int id,
			@Valid @ModelAttribute("user") User user, BindingResult result,
			ModelMap model) {
		if (result.hasErrors()) {
			return "error";
		}

		// return "formtest";
		return "user";
	}
}
