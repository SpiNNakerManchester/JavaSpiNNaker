<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="json" uri="http://www.atg.com/taglibs/json" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
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
<c:if test="${ empty param.json }"><%-- Pass json=true to emit pure JSON --%>
job = (
</c:if>
<json:object>
	<json:property name="id" value="${ job.id }" />
	<c:if test="${ job.owner.present }">
		<spring:eval var="jobOwner" expression="job.owner.get()" />
		<spring:eval var="jobOwnerHost" expression="job.ownerHost.get()" />
		<json:property name="owner" value="${ jobOwner }" />
		<json:property name="owner_host" value="${ jobOwnerHost }" />
	</c:if>
	<json:property name="state" value="${ job.state }" />
	<json:property name="start" value="${ job.startTime }" />
	<json:property name="keep_alive" value="${ job.keepAlive }" />
	<c:if test="${ job.width.present }">
		<spring:eval var="jobWidth" expression="job.width.get()" />
		<spring:eval var="jobHeight" expression="job.height.get()" />
		<json:property name="width" value="${ jobWidth }" />
		<json:property name="height" value="${ jobHeight }" />
	</c:if>
	<json:property name="powered" value="${ job.powered }" />
	<json:property name="machine" value="${ job.machine }" />
	<json:property name="machine_url" value="${ job.machineUrl }" />
	<json:array name="boards" items="${ job.boards }" var="board">
		<%-- board is a BoardCoords --%>
		<json:object>
			<json:object name="triad">
				<json:property name="x" value="${ board.x }"/>
				<json:property name="y" value="${ board.y }"/>
				<json:property name="z" value="${ board.z }"/>
			</json:object>
			<json:object name="physical">
				<json:property name="cabinet" value="${ board.cabinet }"/>
				<json:property name="frame" value="${ board.frame }"/>
				<json:property name="board" value="${ board.board }"/>
			</json:object>
			<c:if test="${ job.owner.present }">
				<json:object name="network">
					<json:property name="address" value="${ board.address }"/>
				</json:object>
			</c:if>
		</json:object>
	</json:array>
	<json:property name="triad_width" value="${ job.triadWidth }"/>
	<json:property name="triad_height" value="${ job.triadHeight }"/>
</json:object>
<c:if test="${ empty param.json }">
);
</c:if>
