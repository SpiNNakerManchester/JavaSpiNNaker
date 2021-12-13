<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<html>
<%--
Copyright (c) 2021 The University of Manchester

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
--%>
<jsp:include page="head.jsp">
	<jsp:param value="Log in with HBP/EBRAINS" name="title"/>
</jsp:include>
<body>

<h1>Login with HBP/EBRAINS</h1>
<c:if test="${ error != null }">
	<p>Please try again...</p>
</c:if>
<form name='f' action='<c:url value="/spalloc/system/perform_oidc/auth/hbp"/>' method='POST'>
	<sec:csrfInput />
	<table>
		<tr>
			<td>User:</td>
			<td><input type='text' name='username' value=''></td>
		</tr>
		<tr>
			<td>Password:</td>
			<td><input type='password' name='password' /></td>
		</tr>
		<tr>
			<td colspan=2>
				<input name="submit" type="submit" value="submit" />
			</td>
		</tr>
	</table>
</form>
<p>
Alternatively, <a href="<c:url value="/spalloc/system/login.html"/>">log in with a Spalloc System Account</a>

</body>
</html>
