<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
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
