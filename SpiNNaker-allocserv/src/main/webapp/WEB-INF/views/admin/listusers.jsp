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
	<jsp:param value="User List" name="title"/>
</jsp:include>
<body>

<h1>Spalloc Users</h1>
<p>
<button onclick="location.href='${ createUserUri }'"
		type="button">Create a new user</button>
<p>
<table>
	<thead>
		<tr>
			<th>Username</th>
			<th>Actions</th>
		</tr>
	</thead>
	<tbody>
	<c:forEach items="${ userlist }" var="item">
		<tr>
			<td>
				<c:out value="${ item.key }" escapeXml='true' />
			</td>
			<td>
				<button onclick="location.href='${ item.value }'"
						type="button">Manage</button>
			</td>
		</tr>
	</c:forEach>
	</tbody>
</table>

<jsp:include page="footer.jsp" />
</body>
</html>
