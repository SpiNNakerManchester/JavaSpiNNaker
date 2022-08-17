<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
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
	<jsp:param name="title" value="SpiNNaker Machine" />
	<jsp:param name="spalloclib" value="true" />
</jsp:include>
<script>
<jsp:include page="data/machinedetails_obj.jsp">
	<jsp:param name="machine" value="${ machine }"/>
</jsp:include>
</script>
<body>

<h1>SpiNNaker Machine</h1>

<table>
	<tr>
		<th class="lineTitle">Name:</th>
		<td>
			<code><c:out value="${ machine.name }" escapeXml="true" /></code>
			<spring:eval var="outOfQuota" expression="
				machine.quota.present && machine.quota.get() <= 0
			" />
			<c:if test="${ outOfQuota }">
				<span class="quotawarning">Out of quota!</span>
			</c:if>
		</td>
	</tr>
	<tr>
		<th class="lineTitle">Tags:</th>
		<td>
			<c:forEach items="${ machine.tags }" var="tag" varStatus="loop">
				<%-- Careful where newlines are in this --%>
				<code><c:out escapeXml="true"
						value="${ tag }" /></code><c:if
						test="${ !loop.last }">,</c:if>
			</c:forEach>
		</td>
	</tr>
	<tr>
		<th class="lineTitle">Dimensions:</th>
		<td>${ machine.width }&times;${ machine.height }&times;3</td>
	</tr>
	<tr>
		<th class="lineTitle">Boards in use:</th>
		<td>${ machine.numInUse }</td>
	</tr>
	<tr>
		<th class="lineTitle">Running jobs:</th>
		<td>
			<spring:eval expression="machine.jobs.size()" />
		</td>
	</tr>
	<c:if test="${ machine.quota.present }">
		<spring:eval var="quotaHours"
				expression="machine.quota.get() / 3600.0" />
		<tr>
			<th class="lineTitle">Remaining quota:</th>
			<td>
				<fmt:formatNumber value="${ quotaHours }" maxFractionDigits="3" />
				board-hours
			</td>
		</tr>
	</c:if>
</table>

<p>
	<%-- TODO: Size the map according to the machine size --%>
	<%-- TODO: Redraw map with updated data periodically --%>
	<canvas id="machine_layout" width="300" height="200"></canvas>
	<canvas id="tooltip" width="100" height="50"></canvas>
	<script defer="defer">
		drawMachine("machine_layout", "tooltip", machine);
	</script>
</p>

<jsp:include page="basicfooter.jsp" />
</body>
</html>
