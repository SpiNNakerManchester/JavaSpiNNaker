<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<html>
<%--
Copyright (c) 2022 The University of Manchester

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
--%>
<jsp:include page="../head.jsp">
	<jsp:param value="Group Details" name="title" />
	<jsp:param name="spalloclib" value="true" />
</jsp:include>
<body>

<c:choose>
	<c:when test="${ group.type.internal }">
		<h1>Spalloc Local Group Details</h1>
	</c:when>
	<c:otherwise>
		<h1>Spalloc OpenID Group Details</h1>
	</c:otherwise>
</c:choose>

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

<spring:eval expression="group.quota.isPresent()" var="hasQuota" />
<c:if test="${ hasQuota }">
	<spring:eval expression="group.quota.get()" var="quotaValue" />
	<h2>Quota</h2>
	<form method="POST" action="${ addQuotaUri }">
		<sec:csrfInput />
		<fmt:formatNumber value="${ quotaValue / 3600.0 }"
				maxFractionDigits="3" /> board-hours
		<c:if test="${ quotaValue <= 0 }">
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
