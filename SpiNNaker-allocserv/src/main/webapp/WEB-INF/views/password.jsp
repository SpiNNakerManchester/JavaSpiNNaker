<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
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

<%--
This is a USER form; all users may access this to alter their own password.
--%>

<jsp:include page="head.jsp">
	<jsp:param value="Change Password" name="title"/>
</jsp:include>
<body>

<h1>Change Password</h1>

<form:form method="POST" modelAttribute="user">
    User Name: ${user.username }
    <br>
    <form:label path="oldPassword">Old Password: </form:label>
    <form:input path="oldPassword" type="password" />
    <p>
    <form:label path="newPassword">New Password: </form:label>
    <form:input path="newPassword" type="password" />
    <br>
    <form:label path="newPassword2">New Password (again): </form:label>
    <form:input path="newPassword2" type="password" />
    <p>
    <input type="submit" value="Change Password" />
</form:form>

<jsp:include page="basicfooter.jsp" />
</body>
</html>
