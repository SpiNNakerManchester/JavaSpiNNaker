<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
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
<jsp:include page="../head.jsp">
	<jsp:param value="User Details" name="title" />
	<jsp:param name="spalloclib" value="true" />
</jsp:include>
<body>

<h1>Local Spalloc User Details</h1>

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
	<form:label path="isEnabled">Is enabled? </form:label>
	<form:checkbox path="isEnabled"/>
	<form:label path="isLocked">Is temporarily locked? </form:label>
	<form:checkbox path="isLocked"/>
	<br>
	Last successful login: <span id="lastSuccessfulLogin">${ user.lastSuccessfulLogin }</span>
	<br>
	Last failed login: <span id="lastFailedLogin">${ user.lastFailedLogin }</span>
	<script defer="defer">
		prettyTimestamp("lastSuccessfulLogin");
		prettyTimestamp("lastFailedLogin");
	</script>
	<p>
	<input type="submit" value="Update" />
</form:form>
<p>
<form method="POST" action="${ deleteUri }">
	<sec:csrfInput />
	<input type="submit" class="warningbutton" value="Delete this user" />
</form>

<c:if test="${ not empty user.quota }">
	<h2>Quotas</h2>
	<table>
		<thead>
			<tr>
				<th colspan="2">Quotas</th>
			</tr>
			<tr>
				<th>Machine</th>
				<th>Remaining (board seconds)
			</tr>
		</thead>
		<tbody>
			<c:forEach items="${ user.quota }" var="q">
				<tr>
					<td>
						<c:out value="${ q.key }" escapeXml="true" />
					</td>
					<td>
					<%--
						<form method="POST" action="${ addQuotaUri }">
							<sec:csrfInput />
							<input name="machine"
									value="<c:out value="${ q.key }" escapeXml="true" />"
									type="hidden"/>
					 --%>
							<fmt:formatNumber value="${ q.value / 3600.0 }"
									maxFractionDigits="3" /> board-hours
							<c:if test="${ q.value <= 0 }">
								<span class="quotawarning">Out of quota!</span>
							</c:if>
					<%--
							<br>
							Add Board-Hours
							<button name="delta" value="1" type="submit">+1</button>
							<button name="delta" value="10" type="submit">+10</button>
							<button name="delta" value="100" type="submit">+100</button>
							<button name="delta" value="1000" type="submit">+1000</button>
							<br>
							Remove Board-Hours
							<button name="delta" value="-1" type="submit">-1</button>
							<button name="delta" value="-10" type="submit">-10</button>
							<button name="delta" value="-100" type="submit">-100</button>
							<button name="delta" value="-1000" type="submit">-1000</button>
						</form>
					 --%>
					</td>
				</tr>
			</c:forEach>
		</tbody>
	</table>
</c:if>
<p>

<jsp:include page="footer.jsp" />
</body>
</html>
