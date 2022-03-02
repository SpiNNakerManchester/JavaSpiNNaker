<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<html>
<%--
Copyright (c) 2022 The University of Manchester

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
	<jsp:param value="Group Details" name="title" />
	<jsp:param name="spalloclib" value="true" />
</jsp:include>
<body>

<h1>Spalloc Group Details</h1>

<c:choose>
	<c:when test="${ group.type.internal }">
		<form:form method="POST" modelAttribute="group">
			<form:label path="groupName">Group Name: </form:label>
			<form:input path="groupName" type="text"/>
			<span class="minordetail">
				<em>(${ group.type })</em>
			</span>
			<p>
			<input type="submit" value="Update" />
		</form:form>
	</c:when>
	<c:otherwise>
		Group Name: <c:out value="${ group.groupName }" />
		<span class="minordetail">
			<em>(${ group.type }: managed externally)</em>
		</span>
	</c:otherwise>
</c:choose>
<p>
<form method="POST" action="${ deleteUri }">
	<sec:csrfInput />
	<input type="submit" class="warningbutton" value="Delete this group" />
</form>

<c:if test="${ not empty group.quota }">
	<h2>Quota</h2>
	<form method="POST" action="${ addQuotaUri }">
		<sec:csrfInput />
		<fmt:formatNumber value="${ group.quota / 3600.0 }"
				maxFractionDigits="3" /> board-hours
		<c:if test="${ group.quota <= 0 }">
			<span class="quotawarning">Out of quota!</span>
		</c:if>
		<br>
		Add Board-Hours
		<button name="delta" value="1" type="submit">+1</button>
		<button name="delta" value="10" type="submit">+10</button>
		<button name="delta" value="100" type="submit">+100</button>
		<button name="delta" value="1000" type="submit">+1000</button>
		<button name="delta" value="1000" type="submit">+10,000</button>
		<br>
		Remove Board-Hours
		<button name="delta" value="-1" type="submit">-1</button>
		<button name="delta" value="-10" type="submit">-10</button>
		<button name="delta" value="-100" type="submit">-100</button>
		<button name="delta" value="-1000" type="submit">-1000</button>
		<button name="delta" value="-1000" type="submit">-10,000</button>
	</form>
</c:if>
<p>
<h2>Members</h2>
<c:if test="${ group.type.internal }">
	Add a <em>local</em> user to this group:
	<form method="POST" action="${ addUserUri }">
		<sec:csrfInput />
		<input name="user" type="text">
		<input type="submit" value="Add">
	</form>
	<p>
</c:if>
<c:choose>
	<c:when test="${ empty group.members }">
		<p>
			<em>No
			<c:if test="${ not group.type.internal }">currently-known</c:if>
			current members!</em>
		</p>
	</c:when>
	<c:otherwise>
		<table>
			<thead>
				<tr>
					<th>Member</th>
					<th>Actions</th>
				</tr>
			</thead>
			<tbody>
				<c:forEach items="${ group.members }" var="mem">
					<tr>
						<td>
							<a href="${ userlist[mem.key] }">
								<code><c:out value="${ mem.key }" /></code>
							</a>
						</td>
						<td>
							<form method="POST" action="${ mem.value }">
								<c:choose>
									<c:when test="${ group.type.internal }">
										<sec:csrfInput />
										<input type="submit" class="warningbutton"
											value="Remove" />
									</c:when>
									<c:otherwise>
										<input type="submit" disabled="disabled"
											value="Remove" />
									</c:otherwise>
								</c:choose>
							</form>
						</td>
					</tr>
				</c:forEach>
			</tbody>
		</table>
	</c:otherwise>
</c:choose>

<jsp:include page="footer.jsp" />
</body>
</html>
