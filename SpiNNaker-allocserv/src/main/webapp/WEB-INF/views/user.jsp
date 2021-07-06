<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<!--
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
-->
    <head>
        <meta http-equiv="Content-Type"
          content="text/html; charset=ISO-8859-1">
        <title>User Details</title>
    </head>
    <body>
        <form:form method="POST" modelAttribute="user">
            <form:label path="userName">User Name: </form:label>
            <form:input path="userName" type="text"/>
            <form:select path="trustLevel">
            <form:option value="">pick a level</form:option>
            <form:options value="${trustLevels }"/>
            </form:select>
            <br>
            <form:label path="password">Password: </form:label>
            <form:input path="password" type="password" />
            <form:label path="hasPassword">Has Password: </form:label>
            <form:checkbox path="hasPassword"/>
            <br>
            <form:label path="isEnabled">Is enabled? </form:label>
            <form:checkbox path="isEnabled"/>
            <form:label path="isLocked">Is temporarily locked? </form:label>
            <form:checkbox path="isLocked"/>
            <br>
            Last successful login: ${ user.lastSuccessfulLogin }
            <br>
            Last failed login: ${ user.lastFailedLogin }
            <p>
            Quotas:
            <c:forEach items="${user.quota}" var="item">
            <br> ${item.key } : ${item.value } board-seconds
            </c:forEach>
            <p>
            <input type="submit" value="Submit" />
        </form:form>
    </body>
</html>
