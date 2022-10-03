<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
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
	<jsp:param name="title" value="SpiNNaker Machine List"/>
	<jsp:param name="refresh" value="30"/>
</jsp:include>
<body>

<h1>SpiNNaker Machines</h1>

<table border="1" class="machinelist">
	<thead>
		<tr>
			<th>Name</th>
			<th>Num boards</th>
			<th>In-use</th>
			<th>Utilisation</th>
			<th>Jobs</th>
			<th>Tags</th>
		</tr>
	</thead>
	<tbody>
		<c:choose>
			<c:when test="${ not empty machineList }">
				<c:forEach items="${ machineList }" var="machine">
					<tr>
						<td class="textColumn">
							<c:choose>
								<c:when test="${ machine.detailsUrl.present }">
									<spring:eval var="machineDetailsUrl"
											expression="machine.detailsUrl.get()" />
									<a href="${ machineDetailsUrl }">
										<code><c:out escapeXml="true"
												value="${ machine.name }" /></code>
									</a>
								</c:when>
								<c:otherwise>
									<code><c:out escapeXml="true"
											value="${ machine.name }" /></code>
								</c:otherwise>
							</c:choose>
						</td>
						<td class="numberColumn">
							${ machine.numBoards }
						</td>
						<td class="numberColumn">
							${ machine.numInUse }
						</td>
						<td class="numberColumn">
							<spring:eval var="usePercent"
									expression="(machine.numInUse * 100.0) / machine.numBoards" />
							<fmt:formatNumber maxFractionDigits="1"
									value="${ usePercent }" />%
						</td>
						<td class="numberColumn">
							${ machine.numJobs }
						</td>
						<td class="textColumn">
							<c:forEach items="${ machine.tags }" var="tag" varStatus="loop">
								<%-- Careful where newlines are placed --%>
								<code><c:out escapeXml="true"
										value="${ tag }" /></code><c:if
										test="${ !loop.last }">,</c:if>
							</c:forEach>
						</td>
					</tr>
				</c:forEach>
			</c:when>
			<c:otherwise>
				<tr>
					<td colspan="6">
						<p>No machines are defined!</p>
					</td>
				</tr>
			</c:otherwise>
		</c:choose>
	</tbody>
</table>

<jsp:include page="basicfooter.jsp" />
</body>
</html>