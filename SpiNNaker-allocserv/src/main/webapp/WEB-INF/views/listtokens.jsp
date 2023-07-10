<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<html>
<%--
Copyright (c) 2023 The University of Manchester

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
<jsp:include page="head.jsp">
	<jsp:param name="title" value="SpiNNaker User Token List"/>
	<jsp:param name="spalloclib" value="true" />
</jsp:include>
<body>

<h1>User Tokens</h1>

<c:choose>
	<c:when test="${ not empty tokenList }">
		<table border="1">
			<thead>
				<tr>
					<th>Token</th>
					<th>Description</th>
					<th>Delete</th>
				</tr>
			</thead>
			<tbody>
				<c:forEach items="${ tokenList }" var="token">
					<tr>
						<td class="textColumn">${ token.token }</td>
						<td class="textColumn">${ token.description }</td>
						<td class="textColumn">
							<form method="POST" action="${ deleteUri }">
								<sec:csrfInput />
								<input type="hidden" name="token" value="${ token.token }" />
								<input type="submit" class="warningbutton" value="Delete" />
							</form>
						</td>
					</tr>
				</c:forEach>
			</tbody>
		</table>
	</c:when>
	<c:otherwise>
		<p>No tokens!</p>
	</c:otherwise>
</c:choose>
<form method="POST" action="${ createUri }">
	<sec:csrfInput />
	Description: <input type="text" name="description" />
	<input type="submit" value="Create a token" />
</form>

<jsp:include page="basicfooter.jsp" />
</body>
</html>
