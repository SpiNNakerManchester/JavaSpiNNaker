<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
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
<jsp:include page="../head.jsp">
	<jsp:param value="Create Local User" name="title"/>
</jsp:include>
<body>

<h1>Create User</h1>

<form:form method="POST" modelAttribute="user">
	<form:label path="userName">User Name: </form:label>
	<form:input path="userName" type="text"/>
	<form:select path="trustLevel">
		<form:option value="">pick a level</form:option>
		<form:options items="${ trustLevels }"/>
	</form:select>
	<br>
	<form:label path="password">Password: </form:label>
	<form:input path="password" type="password" />
	<form:label path="hasPassword">Has Password: </form:label>
	<form:checkbox path="hasPassword"/>
	<br>
	<form:label path="enabled">Is enabled? </form:label>
	<form:checkbox path="enabled"/>
	<p>
	<input type="submit" value="Create" />
</form:form>

<p>
<em>Note that users created with this form are not added to any group by default.</em>

<jsp:include page="footer.jsp" />
</body>
</html>
