<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<html>
<%--
Copyright (c) 2021 The University of Manchester

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
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
		<tr><th colspan="2" class="minordetail">Local Users</th></tr>
		<c:forEach items="${ localusers }" var="item">
			<tr>
				<td>
					<c:out value="${ item.key }" escapeXml="true" />
				</td>
				<td>
					<button onclick="location.href='${ item.value }'"
							type="button">Manage</button>
				</td>
			</tr>
		</c:forEach>
		<tr><th colspan="2" class="minordetail">OpenID Users</th></tr>
		<c:forEach items="${ openidusers }" var="item">
			<tr>
				<td>
					<c:out value="${ item.key }" escapeXml="true" />
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
